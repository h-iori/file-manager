package com.ioristudios.filemanager.sharing.engine

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.system.Os
import android.util.Log
import android.webkit.MimeTypeMap
import com.ioristudios.filemanager.sharing.discovery.DiscoveredPeer
import com.ioristudios.filemanager.sharing.protocol.Hkdf
import com.ioristudios.filemanager.sharing.protocol.NearbyFrames
import com.ioristudios.filemanager.sharing.protocol.PinCalculator
import com.ioristudios.filemanager.sharing.protocol.SecureMessageCodec
import com.ioristudios.filemanager.sharing.protocol.Ukey2Handshake
import com.ioristudios.filemanager.sharing.resilience.DataIntegrityVerifier
import com.ioristudios.filemanager.sharing.resilience.TransferLifecycleStatus
import com.ioristudios.filemanager.sharing.resilience.TransferSessionManager
import com.ioristudios.filemanager.sharing.transport.SocketTransport
import com.ioristudios.filemanager.sharing.transport.TransferPerformanceLocks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.coroutineContext

/**
 * High-throughput, authenticated multi-connection P2P file transfer engine.
 *
 * Performance:
 * - Sender pipeline: disk reader -> pooled 512 KB buffers -> one encrypt+write worker per socket.
 * - Files stream back-to-back; acks are collected in the background.
 * - Receiver decrypts in place and pwrite()s chunks straight to their file; fsync/rename/scan
 *   run off the receive path. Wi-Fi + wake locks are held for the transfer.
 *
 * Security:
 * - UKEY2 with verified key commitment (see [Ukey2Handshake]); user compares the PIN.
 * - Every socket has exactly one codec per direction with strict sequence numbers, so frames
 *   cannot be replayed, reordered, dropped or moved between sockets undetected.
 * - Parallel channels use their own HKDF-derived keys and a derived (non-PIN) session token,
 *   and are only used after the receiver confirms which channels it accepted.
 * - The receiver sees the file list (names, count, size) before approving; the list is
 *   validated and names are sanitized; storage space is checked before accepting.
 * - Unauthenticated connections get a short handshake timeout and are dropped without
 *   ending the receive session.
 */
class ParallelTransferEngine(
    private val context: Context,
    val sessionManager: TransferSessionManager,
    private val parallelChannels: Int = 4
) {

    companion object {
        private const val TAG = "ParallelTransferEngine"
        const val CHUNK_SIZE = 512 * 1024 // 512 KB chunks: few syscalls/frames, bounded memory
        private const val BUFFERS_PER_CHANNEL = 4
        private const val PARALLEL_MEDIUM = "PARALLEL_CHANNELS"
        private const val PARALLEL_ACK_MEDIUM = "PARALLEL_CHANNELS_ACK"
        private const val PARALLEL_READY_MEDIUM = "PARALLEL_CHANNELS_READY"
        private const val PROGRESS_INTERVAL_MS = 250L
        private const val SIGNAL_CANCEL = Long.MIN_VALUE
        private const val SIGNAL_FAILURE = Long.MIN_VALUE + 1

        private const val HANDSHAKE_TIMEOUT_MS = 10_000
        private const val APPROVAL_TIMEOUT_MS = 120_000
        private const val TRANSFER_READ_TIMEOUT_MS = 45_000
        private const val RECEIVER_APPROVAL_WINDOW_MS = 110_000L // < sender's APPROVAL_TIMEOUT_MS

        private const val MAX_FILES_PER_TRANSFER = 10_000
        private const val STORAGE_SAFETY_MARGIN = 64L * 1024L * 1024L
        private const val MAX_FILE_NAME_CHARS = 150

        /** Session token for parallel channels, derived so the PIN secret itself is never sent. */
        private fun deriveChannelToken(authToken: ByteArray): ByteArray = Hkdf.deriveKey(
            "FastShare channel token v1".toByteArray(Charsets.UTF_8),
            authToken,
            "parallel-channels".toByteArray(Charsets.UTF_8),
            32
        )

        private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

        /**
         * Makes a peer-supplied file name safe: strips any path, control and Unicode format
         * characters (e.g. U+202E right-to-left override used to disguise "evil.apk" as
         * "kpa.jpg"), reserved characters and leading dots (hidden files, "." and ".."),
         * and caps the length while keeping the extension.
         */
        internal fun sanitizeIncomingFileName(raw: String, payloadId: Long): String {
            var name = raw.substringAfterLast('/').substringAfterLast('\\')
            name = name.filter { ch ->
                !Character.isISOControl(ch) &&
                    Character.getType(ch) != Character.FORMAT.toInt() &&
                    ch !in "\\/:*?\"<>|"
            }
            name = name.trim().trimStart('.').trim()
            if (name.isEmpty()) name = "received_$payloadId"
            if (name.length > MAX_FILE_NAME_CHARS) {
                val ext = name.substringAfterLast('.', "").take(16)
                val base = if (ext.isEmpty()) name else name.substringBeforeLast('.')
                val keep = MAX_FILE_NAME_CHARS - (if (ext.isEmpty()) 0 else ext.length + 1)
                name = base.take(keep) + (if (ext.isEmpty()) "" else ".$ext")
            }
            return name
        }

        private fun formatBytes(bytes: Long): String {
            val mb = bytes / (1024.0 * 1024.0)
            return if (mb >= 1024) "%.1f GB".format(mb / 1024.0) else "%.1f MB".format(mb)
        }
    }

    var pendingPin: String? = null
        private set
    var pendingPeerName: String? = null
        private set
    var pendingFiles: List<NearbyFrames.FilePayloadMeta> = emptyList()
        private set

    @Volatile private var primaryTransport: SocketTransport? = null
    private val dataTransports = mutableListOf<SocketTransport>()
    private var approvalContinuation: ((Boolean) -> Unit)? = null
    private val perfLocks = TransferPerformanceLocks(context)

    fun getDownloadDir(): File {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val shareDir = File(downloadDir, "FilesShare")
        if (!shareDir.exists()) {
            shareDir.mkdirs()
        }
        return if (shareDir.exists() && shareDir.canWrite()) {
            shareDir
        } else if (downloadDir.exists() && downloadDir.canWrite()) {
            downloadDir
        } else {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        }
    }

    fun answerApproval(accepted: Boolean) {
        approvalContinuation?.invoke(accepted)
        approvalContinuation = null
    }

    fun resolveNameConflict(file: File): File {
        if (!file.exists()) return file
        val parent = file.parentFile ?: return file
        val nameWithoutExt = file.nameWithoutExtension
        val ext = file.extension
        var counter = 1
        var candidate: File
        do {
            val suffix = if (ext.isNotEmpty()) ".$ext" else ""
            candidate = File(parent, "$nameWithoutExt ($counter)$suffix")
            counter++
        } while (candidate.exists() && counter < 1000)
        return candidate
    }

    // ---------------------------------------------------------------------------------
    // SENDER
    // ---------------------------------------------------------------------------------

    /**
     * SENDER FLOW: Connects to peer, performs UKEY2 handshake, determines fast multi-channel
     * or single-stream mode, and transmits files with maximum throughput.
     */
    suspend fun sendFiles(
        peer: DiscoveredPeer,
        filesToSend: List<File>,
        onPinGenerated: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val host = peer.hostAddress ?: throw IllegalArgumentException("Peer host address is null")
        val port = peer.port
        sessionManager.reset()
        sessionManager.updateStatus(TransferLifecycleStatus.CONNECTING)

        var controlTransport: SocketTransport? = null
        val activeWorkers = mutableListOf<Pair<SocketTransport, SecureMessageCodec>>()
        var locksHeld = false

        try {
            Log.d(TAG, "Connecting to ${peer.deviceName} at $host:$port (isFastApp=${peer.isFastApp})")
            controlTransport = SocketTransport.connect(host, port, timeoutMs = 12000)
            primaryTransport = controlTransport
            controlTransport.setReadTimeout(HANDSHAKE_TIMEOUT_MS)

            // 1. UKEY2 Handshake
            sessionManager.updateStatus(TransferLifecycleStatus.HANDSHAKING)
            val ukey = Ukey2Handshake(isClient = true)
            val clientInit = ukey.createClientInit()
            controlTransport.sendFrame(clientInit)

            val serverInit = controlTransport.receiveFrame()
            val clientFinish = ukey.processServerInitAndGenerateClientFinish(serverInit)
            controlTransport.sendFrame(clientFinish)

            // 2. Derive PIN and Session Keys
            val authToken = ukey.authToken ?: throw IllegalStateException("Auth token is null")
            val pin = PinCalculator.calculatePin(authToken)
            val channelTokenHex = deriveChannelToken(authToken).toHex()
            pendingPin = pin
            pendingPeerName = peer.deviceName
            onPinGenerated(pin)

            // One codec for every frame on the control socket (continuous sequence numbers)
            val primaryCodec = SecureMessageCodec(
                encryptionKey = ukey.getEncryptionKey(),
                decryptionKey = ukey.getDecryptionKey(),
                signingHmacKey = ukey.getSigningHmacKey(),
                verifyingHmacKey = ukey.getVerifyingHmacKey()
            )

            // From here on the receiver may be waiting for a human to decide
            controlTransport.setReadTimeout(APPROVAL_TIMEOUT_MS)

            // 3. Send ConnectionRequest
            val reqFrame = NearbyFrames.encodeConnectionRequest(
                NearbyFrames.ConnectionRequest("CLNT", Build.MODEL ?: "Android Device")
            )
            controlTransport.sendFrame(primaryCodec.encrypt(reqFrame))

            // 4. Await ConnectionResponse
            sessionManager.updateStatus(TransferLifecycleStatus.WAITING_APPROVAL)
            val respRaw = controlTransport.receiveFrame()
            val respDecrypted = primaryCodec.decrypt(respRaw)
            val parsedResp = NearbyFrames.decodeFrame(respDecrypted)
            if (parsedResp !is NearbyFrames.ParsedFrame.ConnResponse || !parsedResp.response.accepted) {
                sessionManager.markFailed("Transfer rejected by ${peer.deviceName}")
                return@withContext
            }

            // 5. Send Introduction with accurate MIME types (the receiver approves based on it)
            val payloadMetaList = filesToSend.mapIndexed { idx, f ->
                val ext = f.extension.lowercase()
                val mime = if (ext.isNotEmpty()) {
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
                } else null
                NearbyFrames.FilePayloadMeta(
                    payloadId = System.currentTimeMillis() + idx,
                    fileName = f.name,
                    fileSize = f.length(),
                    mimeType = mime ?: "*/*"
                )
            }
            val introFrame = NearbyFrames.encodeIntroduction(NearbyFrames.Introduction(payloadMetaList))
            controlTransport.sendFrame(primaryCodec.encrypt(introFrame))

            // 6. Await TransferAccept (user is reviewing the file list on the receiver)
            val acceptRaw = controlTransport.receiveFrame()
            val acceptDecrypted = primaryCodec.decrypt(acceptRaw)
            when (NearbyFrames.decodeFrame(acceptDecrypted)) {
                is NearbyFrames.ParsedFrame.TransferAccepted -> Unit
                is NearbyFrames.ParsedFrame.TransferRejected -> {
                    sessionManager.markFailed("${peer.deviceName} declined the transfer")
                    return@withContext
                }
                else -> {
                    sessionManager.markFailed("Peer did not accept transfer files")
                    return@withContext
                }
            }
            controlTransport.setReadTimeout(TRANSFER_READ_TIMEOUT_MS)

            // 7. Establish Parallel Channels if peer is our app
            sessionManager.updateStatus(TransferLifecycleStatus.TRANSFERRING)
            perfLocks.acquire()
            locksHeld = true

            if (peer.isFastApp && parallelChannels > 1) {
                try {
                    val upgradeProbe = NearbyFrames.encodeBandwidthUpgrade(
                        NearbyFrames.BandwidthUpgrade(
                            medium = PARALLEL_MEDIUM,
                            ssid = channelTokenHex,
                            password = "",
                            ipAddress = host,
                            port = parallelChannels - 1
                        )
                    )
                    controlTransport.sendFrame(primaryCodec.encrypt(upgradeProbe))

                    val ackRaw = controlTransport.receiveFrame()
                    val parsedAck = NearbyFrames.decodeFrame(primaryCodec.decrypt(ackRaw))

                    if (parsedAck is NearbyFrames.ParsedFrame.Upgrade &&
                        parsedAck.upgrade.medium == PARALLEL_ACK_MEDIUM
                    ) {
                        val numWorkersToConnect = parsedAck.upgrade.port.coerceIn(0, parallelChannels - 1)
                        val connected = mutableListOf<Triple<Int, SocketTransport, SecureMessageCodec>>()
                        for (ch in 1..numWorkersToConnect) {
                            try {
                                val worker = SocketTransport.connect(host, port, timeoutMs = 3000, maxRetries = 1)
                                val initCodec = primaryCodec.deriveChannelCodec("init")
                                val initFrame = NearbyFrames.encodeChannelInit(
                                    NearbyFrames.ChannelInit(channelId = ch, sessionToken = channelTokenHex)
                                )
                                worker.sendFrame(initCodec.encrypt(initFrame))
                                connected.add(Triple(ch, worker, primaryCodec.deriveChannelCodec("channel-$ch")))
                            } catch (e: Exception) {
                                Log.w(TAG, "Worker $ch connect failed: ${e.message}")
                                break
                            }
                        }

                        // Only use the channels the receiver confirms it authenticated
                        val readyRaw = controlTransport.receiveFrame()
                        val ready = NearbyFrames.decodeFrame(primaryCodec.decrypt(readyRaw))
                        val acceptedMask = if (ready is NearbyFrames.ParsedFrame.Upgrade &&
                            ready.upgrade.medium == PARALLEL_READY_MEDIUM
                        ) ready.upgrade.port else 0
                        for ((ch, worker, codec) in connected) {
                            if (acceptedMask and (1 shl ch) != 0) {
                                activeWorkers.add(worker to codec)
                            } else {
                                worker.close()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Parallel channel setup skipped: ${e.message}")
                    activeWorkers.forEach { it.first.close() }
                    activeWorkers.clear()
                }
            }

            synchronized(dataTransports) {
                dataTransports.clear()
                dataTransports.addAll(activeWorkers.map { it.first })
            }
            val allChannels = listOf(controlTransport to primaryCodec) + activeWorkers
            Log.d(TAG, "Streaming using ${allChannels.size} TCP channels (fastMode=${activeWorkers.isNotEmpty()})")

            // 8. Stream all files through the pipeline
            streamFiles(filesToSend, payloadMetaList, controlTransport, primaryCodec, allChannels)

            if (sessionManager.isCancelled) return@withContext
            sessionManager.markCompleted()
            Log.d(TAG, "All files transferred successfully!")

        } catch (e: Exception) {
            Log.e(TAG, "Send files error", e)
            if (!sessionManager.isCancelled) {
                sessionManager.markFailed(e.message ?: "Transfer connection failed", canResume = true)
            }
        } finally {
            primaryTransport = null
            controlTransport?.close()
            activeWorkers.forEach { it.first.close() }
            synchronized(dataTransports) { dataTransports.clear() }
            if (locksHeld) perfLocks.release()
        }
    }

    private class OutChunk(
        val payloadId: Long,
        val offset: Long,
        val buffer: ByteArray,
        val length: Int,
        val isLast: Boolean
    )

    /**
     * Pipelined sender: a reader fills pooled buffers from disk while one worker per socket
     * encrypts and writes. Acks are consumed concurrently; returns once every file is acked.
     */
    private suspend fun streamFiles(
        files: List<File>,
        metas: List<NearbyFrames.FilePayloadMeta>,
        controlTransport: SocketTransport,
        controlCodec: SecureMessageCodec,
        allChannels: List<Pair<SocketTransport, SecureMessageCodec>>
    ) = coroutineScope {
        val totalBytes = metas.sumOf { it.fileSize }
        val sentBytes = AtomicLong(0L)
        val writesDone = AtomicBoolean(false)
        val ackError = AtomicReference<String?>(null)

        val poolSize = allChannels.size * BUFFERS_PER_CHANNEL
        val freeBuffers = Channel<ByteArray>(poolSize)
        repeat(poolSize) { freeBuffers.trySend(ByteArray(CHUNK_SIZE)) }
        val work = Channel<OutChunk>(poolSize)

        fun abortAll() {
            allChannels.forEach { it.first.close() }
        }

        // Background ack collector on the control socket
        val acks = async(Dispatchers.IO) {
            var acked = 0
            try {
                while (acked < metas.size) {
                    val raw = try {
                        controlTransport.receiveFrameDirect()
                    } catch (e: SocketTimeoutException) {
                        // A large file can legitimately take longer than the read timeout to ack.
                        if (!writesDone.get() && !sessionManager.isCancelled) continue
                        throw e
                    }
                    when (val frame = NearbyFrames.decodeFrame(controlCodec.decrypt(raw))) {
                        is NearbyFrames.ParsedFrame.Ack -> {
                            if (!frame.ack.success) {
                                val name = metas.firstOrNull { it.payloadId == frame.ack.payloadId }?.fileName ?: "file"
                                ackError.compareAndSet(null, "Payload verification failed for $name")
                                abortAll()
                                return@async false
                            }
                            acked++
                        }
                        is NearbyFrames.ParsedFrame.Control -> if (frame.control.action == "CANCEL") {
                            ackError.compareAndSet(null, "Transfer cancelled by receiver")
                            abortAll()
                            return@async false
                        }
                        else -> Unit
                    }
                }
                true
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                ackError.compareAndSet(null, e.message ?: "Connection lost while awaiting confirmation")
                abortAll()
                false
            }
        }

        // One encrypt+write worker per socket, pulling from a shared queue
        val writers = allChannels.map { (transport, codec) ->
            launch(Dispatchers.IO) {
                val out = ByteArray(4 + SecureMessageCodec.maxEncryptedPayloadSize(CHUNK_SIZE))
                try {
                    for (chunk in work) {
                        synchronized(transport.sendLock) {
                            val len = codec.encryptPayloadChunk(
                                chunk.payloadId, chunk.offset, chunk.buffer, 0, chunk.length,
                                chunk.isLast, out, 4
                            )
                            freeBuffers.trySend(chunk.buffer) // plaintext no longer needed
                            out[0] = (len ushr 24).toByte()
                            out[1] = (len ushr 16).toByte()
                            out[2] = (len ushr 8).toByte()
                            out[3] = len.toByte()
                            transport.sendPrefixedFrame(out, 0, len + 4)
                        }
                        sentBytes.addAndGet(chunk.length.toLong())
                    }
                } catch (e: Throwable) {
                    if (e !is CancellationException) abortAll()
                    throw e
                }
            }
        }

        val progress = launch(Dispatchers.IO) {
            while (isActive) {
                reportProgress(sentBytes.get(), metas, totalBytes)
                delay(PROGRESS_INTERVAL_MS)
            }
        }

        // Disk reader (this coroutine)
        try {
            for ((index, file) in files.withIndex()) {
                val meta = metas[index]
                val fileLength = meta.fileSize
                if (fileLength <= 0L) continue // empty files carry no chunks; receiver acks from metadata

                FileInputStream(file).use { fis ->
                    var fileOffset = 0L
                    while (fileOffset < fileLength) {
                        if (sessionManager.isCancelled) {
                            try {
                                val cancelFrame = NearbyFrames.encodeTransferControl(
                                    NearbyFrames.TransferControl(meta.payloadId, "CANCEL")
                                )
                                synchronized(controlTransport.sendLock) {
                                    controlTransport.sendFrameDirect(controlCodec.encrypt(cancelFrame))
                                }
                            } catch (_: Exception) {}
                            abortAll()
                            throw CancellationException("Transfer cancelled")
                        }
                        while (sessionManager.isPaused && !sessionManager.isCancelled) {
                            delay(200)
                        }

                        val buffer = freeBuffers.receive()
                        val want = minOf(CHUNK_SIZE.toLong(), fileLength - fileOffset).toInt()
                        var filled = 0
                        while (filled < want) {
                            val r = fis.read(buffer, filled, want - filled)
                            if (r < 0) break
                            filled += r
                        }
                        if (filled < want) {
                            throw IOException("${file.name} changed size during transfer")
                        }

                        val isLast = fileOffset + filled >= fileLength
                        work.send(OutChunk(meta.payloadId, fileOffset, buffer, filled, isLast))
                        fileOffset += filled
                    }
                }
            }
        } finally {
            work.close()
        }

        writers.joinAll()
        writesDone.set(true)
        val allAcked = acks.await()
        progress.cancelAndJoin()
        if (!allAcked) throw IOException(ackError.get() ?: "Receiver reported a failure")
        reportProgress(totalBytes, metas, totalBytes)
    }

    /** Maps a byte count onto the file currently in flight and publishes progress. */
    private fun reportProgress(transferred: Long, metas: List<NearbyFrames.FilePayloadMeta>, totalBytes: Long) {
        if (metas.isEmpty()) return
        var cumulative = 0L
        var idx = metas.lastIndex
        for (i in metas.indices) {
            cumulative += metas[i].fileSize
            if (transferred < cumulative) {
                idx = i
                break
            }
        }
        sessionManager.updateMetrics(
            fileName = metas[idx].fileName,
            fileIndex = idx + 1,
            totalFiles = metas.size,
            transferredBytes = transferred,
            totalBytes = totalBytes
        )
    }

    // ---------------------------------------------------------------------------------
    // RECEIVER
    // ---------------------------------------------------------------------------------

    /** A file being received: positional writes from any socket, completed by byte count. */
    private class FileSink(val meta: NearbyFrames.FilePayloadMeta, val tempFile: File) {
        val raf: RandomAccessFile
        val received = AtomicLong(0L)
        val completed = AtomicBoolean(false)
        /** Offsets already written: a repeated chunk must never count twice toward completion. */
        val seenOffsets: MutableSet<Long> = ConcurrentHashMap.newKeySet()
        @Volatile var closed = false

        init {
            if (tempFile.exists()) tempFile.delete()
            raf = RandomAccessFile(tempFile, "rw")
            if (meta.fileSize > 0L) {
                // Reserve the full length up front: chunks can land at any offset in any order.
                try { raf.setLength(meta.fileSize) } catch (_: Exception) {}
            }
        }

        fun write(offset: Long, buf: ByteArray, off: Int, len: Int) {
            var written = 0
            while (written < len) {
                written += Os.pwrite(raf.fd, buf, off + written, len - written, offset + written)
            }
        }

        fun close() {
            if (closed) return
            closed = true
            try { raf.close() } catch (_: Exception) {}
        }
    }

    /** Result of an authenticated, pre-approval receiver handshake. */
    private class IncomingSession(
        val transport: SocketTransport,
        val codec: SecureMessageCodec,
        val authToken: ByteArray,
        val pin: String,
        val peerName: String,
        val files: List<NearbyFrames.FilePayloadMeta>
    )

    /**
     * Runs UKEY2, reads the ConnectionRequest and the Introduction, and validates the file
     * list - all before the user is asked anything. Throws on any protocol violation.
     */
    private suspend fun performReceiverHandshake(transport: SocketTransport): IncomingSession {
        sessionManager.updateStatus(TransferLifecycleStatus.HANDSHAKING)
        val ukey = Ukey2Handshake(isClient = false)
        val clientInit = transport.receiveFrame()
        val serverInit = ukey.processClientInitAndGenerateServerInit(clientInit)
        transport.sendFrame(serverInit)

        val clientFinish = transport.receiveFrame()
        ukey.processClientFinished(clientFinish)

        val authToken = ukey.authToken ?: throw IllegalStateException("Auth token is null")
        val pin = PinCalculator.calculatePin(authToken)
        val codec = SecureMessageCodec(
            encryptionKey = ukey.getEncryptionKey(),
            decryptionKey = ukey.getDecryptionKey(),
            signingHmacKey = ukey.getSigningHmacKey(),
            verifyingHmacKey = ukey.getVerifyingHmacKey()
        )

        // ConnectionRequest
        val parsedReq = NearbyFrames.decodeFrame(codec.decrypt(transport.receiveFrame()))
        val peerName = (parsedReq as? NearbyFrames.ParsedFrame.ConnRequest)?.request?.endpointName
            ?.filter { !Character.isISOControl(it) && Character.getType(it) != Character.FORMAT.toInt() }
            ?.take(64)
            ?.ifBlank { null }
            ?: "Nearby Device"

        // Accept the connection so the sender reveals what it wants to send; the user decides
        // on the actual transfer once they can see the file list.
        transport.sendFrame(
            codec.encrypt(NearbyFrames.encodeConnectionResponse(NearbyFrames.ConnectionResponse(accepted = true)))
        )

        val parsedIntro = NearbyFrames.decodeFrame(codec.decrypt(transport.receiveFrame()))
        val rawFiles = (parsedIntro as? NearbyFrames.ParsedFrame.Intro)?.introduction?.files
            ?: throw SecurityException("Expected file introduction from sender")
        return IncomingSession(transport, codec, authToken, pin, peerName, validateIncomingFiles(rawFiles))
    }

    /** Rejects malformed introductions and returns the list with sanitized file names. */
    private fun validateIncomingFiles(files: List<NearbyFrames.FilePayloadMeta>): List<NearbyFrames.FilePayloadMeta> {
        if (files.isEmpty()) throw SecurityException("Sender offered no files")
        if (files.size > MAX_FILES_PER_TRANSFER) {
            throw SecurityException("Sender offered too many files (${files.size})")
        }
        val ids = HashSet<Long>()
        var total = 0L
        return files.map { meta ->
            if (meta.fileSize < 0L) throw SecurityException("Invalid size for ${meta.fileName}")
            if (!ids.add(meta.payloadId)) throw SecurityException("Duplicate payload id ${meta.payloadId}")
            total += meta.fileSize
            if (total < 0L) throw SecurityException("Total transfer size overflow")
            meta.copy(fileName = sanitizeIncomingFileName(meta.fileName, meta.payloadId))
        }
    }

    /**
     * RECEIVER FLOW: Handles incoming connections, shows the user the incoming file list
     * for approval, accepts authenticated parallel channels, and saves files reliably.
     */
    suspend fun receiveFiles(
        serverSocket: ServerSocket,
        onRequireApproval: (peerName: String, pin: String, files: List<NearbyFrames.FilePayloadMeta>) -> Unit,
        onApprovalExpired: () -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        var controlTransport: SocketTransport? = null
        val activeWorkers = mutableListOf<Pair<SocketTransport, SecureMessageCodec>>()
        var locksHeld = false

        try {
            sessionManager.reset()
            sessionManager.updateStatus(TransferLifecycleStatus.DISCOVERING)
            Log.d(TAG, "Receiver listening on port ${serverSocket.localPort}")

            // 1. Accept connections until one completes an authenticated handshake. Junk or
            //    hostile connections are dropped after a short timeout and do not end the session.
            var session: IncomingSession? = null
            while (session == null) {
                coroutineContext.ensureActive()
                if (sessionManager.isCancelled) return@withContext
                val socket = serverSocket.accept() // throws once the server socket is closed
                SocketTransport.configureSocket(socket)
                val candidate = SocketTransport(socket)
                candidate.setReadTimeout(HANDSHAKE_TIMEOUT_MS)
                primaryTransport = candidate
                try {
                    session = performReceiverHandshake(candidate)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.w(TAG, "Dropped incoming connection from ${socket.inetAddress?.hostAddress}: ${e.message}")
                    candidate.close()
                    primaryTransport = null
                    if (sessionManager.isCancelled) return@withContext
                    sessionManager.updateStatus(TransferLifecycleStatus.DISCOVERING)
                }
            }

            val established: IncomingSession = session
            val control = established.transport
            controlTransport = control
            val primaryCodec = established.codec
            val incomingFiles = established.files
            val channelToken = deriveChannelToken(established.authToken)
            pendingPin = established.pin
            pendingPeerName = established.peerName
            pendingFiles = incomingFiles

            val downloadDir = getDownloadDir()
            if (!downloadDir.exists()) downloadDir.mkdirs()
            val totalBytes = incomingFiles.sumOf { it.fileSize }

            // 2. Refuse up front if the files cannot fit
            val usable = try { downloadDir.usableSpace } catch (_: Exception) { Long.MAX_VALUE }
            if (totalBytes > usable - STORAGE_SAFETY_MARGIN) {
                control.sendFrame(primaryCodec.encrypt(NearbyFrames.encodeTransferReject()))
                sessionManager.markFailed(
                    "Not enough storage: ${formatBytes(totalBytes)} needed, ${formatBytes(maxOf(usable, 0L))} free"
                )
                return@withContext
            }

            // 3. Ask the user, now that names, count and size are known
            sessionManager.updateStatus(TransferLifecycleStatus.WAITING_APPROVAL)
            val userApproved = withTimeoutOrNull(RECEIVER_APPROVAL_WINDOW_MS) {
                suspendCancellableCoroutine<Boolean> { cont ->
                    approvalContinuation = { accepted ->
                        if (cont.isActive) cont.resumeWith(Result.success(accepted))
                    }
                    onRequireApproval(established.peerName, established.pin, incomingFiles)
                }
            }
            if (userApproved == null) {
                approvalContinuation = null
                onApprovalExpired()
            }

            if (userApproved != true) {
                try {
                    control.sendFrame(primaryCodec.encrypt(NearbyFrames.encodeTransferReject()))
                } catch (_: Exception) {}
                sessionManager.updateStatus(TransferLifecycleStatus.CANCELLED)
                return@withContext
            }

            control.sendFrame(primaryCodec.encrypt(NearbyFrames.encodeTransferAccept()))
            control.setReadTimeout(TRANSFER_READ_TIMEOUT_MS)

            // 4. Check if sender offers Parallel Upgrade
            sessionManager.updateStatus(TransferLifecycleStatus.TRANSFERRING)
            perfLocks.acquire()
            locksHeld = true

            val firstFrameRaw = control.receiveFrameDirect()
            val firstFrame = NearbyFrames.decodeFrame(primaryCodec.decrypt(firstFrameRaw))

            if (firstFrame is NearbyFrames.ParsedFrame.Upgrade &&
                firstFrame.upgrade.medium == PARALLEL_MEDIUM
            ) {
                val offered = firstFrame.upgrade.ssid.toByteArray(Charsets.UTF_8)
                val tokenOk = MessageDigest.isEqual(offered, channelToken.toHex().toByteArray(Charsets.UTF_8))
                val requestedWorkers = if (tokenOk) {
                    firstFrame.upgrade.port.coerceIn(0, parallelChannels - 1)
                } else 0
                control.sendFrameDirect(
                    primaryCodec.encrypt(
                        NearbyFrames.encodeBandwidthUpgrade(
                            NearbyFrames.BandwidthUpgrade(
                                medium = PARALLEL_ACK_MEDIUM,
                                ssid = "",
                                password = "",
                                ipAddress = "",
                                port = requestedWorkers
                            )
                        )
                    )
                )

                // Accept worker sockets (2.5s accept timeout, never hangs). Every worker must
                // prove it holds the session keys and a fresh, in-range channel id.
                var acceptedMask = 0
                try {
                    serverSocket.soTimeout = 2500
                    var attempts = 0
                    while (activeWorkers.size < requestedWorkers && attempts < requestedWorkers * 3) {
                        attempts++
                        val wSocket = try {
                            serverSocket.accept()
                        } catch (_: SocketTimeoutException) {
                            break
                        }
                        try {
                            SocketTransport.configureSocket(wSocket)
                            wSocket.soTimeout = 2000
                            val wTransport = SocketTransport(wSocket)
                            val initCodec = primaryCodec.deriveChannelCodec("init")
                            val initFrame = NearbyFrames.decodeFrame(initCodec.decrypt(wTransport.receiveFrameDirect()))
                            val ch = (initFrame as? NearbyFrames.ParsedFrame.ChannelInitFrame)?.init
                            val valid = ch != null &&
                                ch.channelId in 1..requestedWorkers &&
                                (acceptedMask and (1 shl ch.channelId)) == 0 &&
                                MessageDigest.isEqual(
                                    ch.sessionToken.toByteArray(Charsets.UTF_8),
                                    channelToken.toHex().toByteArray(Charsets.UTF_8)
                                )
                            if (valid) {
                                wSocket.soTimeout = TRANSFER_READ_TIMEOUT_MS
                                acceptedMask = acceptedMask or (1 shl ch.channelId)
                                activeWorkers.add(wTransport to primaryCodec.deriveChannelCodec("channel-${ch.channelId}"))
                            } else {
                                wSocket.close()
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Rejected worker connection: ${e.message}")
                            try { wSocket.close() } catch (_: Exception) {}
                        }
                    }
                } finally {
                    try {
                        serverSocket.soTimeout = 0
                    } catch (_: Exception) {}
                }

                // Tell the sender exactly which channels to use
                control.sendFrameDirect(
                    primaryCodec.encrypt(
                        NearbyFrames.encodeBandwidthUpgrade(
                            NearbyFrames.BandwidthUpgrade(
                                medium = PARALLEL_READY_MEDIUM,
                                ssid = "",
                                password = "",
                                ipAddress = "",
                                port = acceptedMask
                            )
                        )
                    )
                )
            }

            synchronized(dataTransports) {
                dataTransports.clear()
                dataTransports.addAll(activeWorkers.map { it.first })
            }

            // 5. Prepare sinks and per-socket readers
            val allReceivers = listOf(control to primaryCodec) + activeWorkers
            val transferredTotal = AtomicLong(0L)

            // Clean stale temporary files
            try {
                downloadDir.listFiles { f -> f.name.startsWith(".share_tmp_") && f.name.endsWith(".part") }
                    ?.forEach { it.delete() }
            } catch (_: Exception) {}

            val metaById = incomingFiles.associateBy { it.payloadId }
            val sinks = ConcurrentHashMap<Long, FileSink>()
            val completions = Channel<Long>(Channel.UNLIMITED)
            val receiving = AtomicBoolean(true)
            val failure = AtomicReference<String?>(null)

            fun sinkFor(meta: NearbyFrames.FilePayloadMeta): FileSink =
                sinks.computeIfAbsent(meta.payloadId) {
                    FileSink(meta, DataIntegrityVerifier.getTempResumeFile(downloadDir, meta.payloadId))
                }

            fun writeChunk(payloadId: Long, offset: Long, buf: ByteArray, off: Int, len: Int) {
                val meta = metaById[payloadId] ?: throw SecurityException("Chunk for unannounced payload $payloadId")
                if (offset < 0L || len < 0 || offset + len > meta.fileSize) {
                    throw SecurityException("Chunk outside bounds of ${meta.fileName}")
                }
                val sink = sinkFor(meta)
                if (sink.completed.get() || len == 0) return
                if (!sink.seenOffsets.add(offset)) return // duplicate: never double-count
                sink.write(offset, buf, off, len)
                transferredTotal.addAndGet(len.toLong())
                if (sink.received.addAndGet(len.toLong()) >= meta.fileSize &&
                    sink.completed.compareAndSet(false, true)
                ) {
                    completions.trySend(payloadId)
                }
            }

            // Empty files carry no chunks: complete them straight from the metadata
            for (meta in incomingFiles) {
                if (meta.fileSize == 0L) {
                    val sink = sinkFor(meta)
                    if (sink.completed.compareAndSet(false, true)) completions.trySend(meta.payloadId)
                }
            }

            // If the first frame was already a payload chunk (e.g. from Google Files), write it
            when (firstFrame) {
                is NearbyFrames.ParsedFrame.Payload -> {
                    val p = firstFrame.transfer
                    writeChunk(p.payloadId, p.offset, p.chunkData, 0, p.chunkData.size)
                }
                is NearbyFrames.ParsedFrame.Control -> if (firstFrame.control.action == "CANCEL") {
                    sessionManager.cancelTransfer()
                    return@withContext
                }
                else -> Unit
            }

            allReceivers.forEach { (transport, codec) ->
                launch(Dispatchers.IO) {
                    val frame = SocketTransport.FrameBuffer(CHUNK_SIZE + 4096)
                    var plain = ByteArray(CHUNK_SIZE + 4096)
                    val view = NearbyFrames.PayloadView()
                    try {
                        while (receiving.get() && !sessionManager.isCancelled) {
                            while (sessionManager.isPaused && !sessionManager.isCancelled) {
                                delay(200)
                            }
                            transport.receiveFrameInto(frame)
                            if (plain.size < frame.length) plain = ByteArray(frame.length)
                            val plainLen = codec.decryptInto(frame.data, frame.length, plain)

                            if (NearbyFrames.parsePayloadInPlace(plain, plainLen, view)) {
                                writeChunk(view.payloadId, view.offset, plain, view.dataOffset, view.dataLength)
                            } else {
                                val other = NearbyFrames.decodeFrame(plain.copyOf(plainLen))
                                if (other is NearbyFrames.ParsedFrame.Control && other.control.action == "CANCEL") {
                                    sessionManager.cancelTransfer()
                                    completions.trySend(SIGNAL_CANCEL)
                                    break
                                }
                            }
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        if (receiving.get() && !sessionManager.isCancelled) {
                            failure.compareAndSet(null, e.message ?: "Connection lost")
                            completions.trySend(SIGNAL_FAILURE)
                        }
                    }
                }
            }

            val progress = launch(Dispatchers.IO) {
                while (isActive) {
                    reportProgress(transferredTotal.get(), incomingFiles, totalBytes)
                    delay(PROGRESS_INTERVAL_MS)
                }
            }

            // 6. Finalize files as they complete (off the network receive path)
            try {
                var finalized = 0
                while (finalized < incomingFiles.size) {
                    val id = completions.receive()
                    if (id == SIGNAL_CANCEL || sessionManager.isCancelled) return@withContext
                    if (id == SIGNAL_FAILURE) throw IOException(failure.get() ?: "Connection lost")
                    val sink = sinks[id] ?: continue
                    if (!finalizeReceivedFile(sink, downloadDir, control, primaryCodec)) {
                        return@withContext
                    }
                    finalized++
                }
            } finally {
                receiving.set(false)
                progress.cancel()
                sinks.values.forEach { it.close() }
            }

            progress.join()
            reportProgress(totalBytes, incomingFiles, totalBytes)
            sessionManager.markCompleted()
            Log.d(TAG, "All files received successfully!")

        } catch (e: Exception) {
            Log.e(TAG, "Receive files error", e)
            if (!sessionManager.isCancelled) {
                sessionManager.markFailed(e.message ?: "Receive connection failed", canResume = true)
            }
        } finally {
            primaryTransport?.close()
            primaryTransport = null
            // Closing the sockets also unblocks any reader still parked in a blocking read
            controlTransport?.close()
            activeWorkers.forEach { it.first.close() }
            synchronized(dataTransports) { dataTransports.clear() }
            if (locksHeld) perfLocks.release()
        }
    }

    /**
     * Flushes, moves into place, indexes and acks one completed file.
     * Returns false (after marking the session failed) if the file could not be saved.
     */
    private fun finalizeReceivedFile(
        sink: FileSink,
        downloadDir: File,
        controlTransport: SocketTransport,
        codec: SecureMessageCodec
    ): Boolean {
        val meta = sink.meta
        val tempFile = sink.tempFile
        try {
            sink.raf.fd.sync()
        } catch (_: Exception) {}
        sink.close()

        // Name was sanitized during validation; File(...).name is a second line of defence
        val safeFileName = File(meta.fileName).name.ifBlank { "received_${meta.payloadId}" }
        val targetFile = resolveNameConflict(File(downloadDir, safeFileName))
        if (targetFile.canonicalFile.parentFile != downloadDir.canonicalFile) {
            tempFile.delete()
            sessionManager.markFailed("Refused unsafe file name ${meta.fileName}")
            return false
        }
        var savedFile = targetFile

        val moved = tempFile.renameTo(targetFile)
        if (!moved) {
            try {
                tempFile.copyTo(targetFile, overwrite = false)
                tempFile.delete()
                savedFile = targetFile
            } catch (e: Exception) {
                Log.e(TAG, "Fallback move failed: ${e.message}", e)
                val unhiddenFile = resolveNameConflict(File(downloadDir, "received_${meta.payloadId}"))
                savedFile = if (tempFile.renameTo(unhiddenFile)) unhiddenFile else tempFile
            }
        }

        // Verify file was saved with the expected size
        if (!savedFile.exists() || savedFile.length() != meta.fileSize) {
            Log.e(TAG, "File save verification failed: ${savedFile.absolutePath}")
            val failAck = NearbyFrames.encodePayloadAck(NearbyFrames.PayloadAck(meta.payloadId, false))
            try {
                controlTransport.sendFrameDirect(codec.encrypt(failAck))
            } catch (_: Exception) {}
            sessionManager.markFailed("Failed to save ${meta.fileName}")
            return false
        }

        // Trigger Android MediaStore indexer
        val ext = savedFile.extension.lowercase()
        val mime = if (ext.isNotEmpty()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        } else {
            meta.mimeType.takeIf { it != "*/*" }
        }

        MediaScannerConnection.scanFile(
            context,
            arrayOf(savedFile.absolutePath),
            if (mime != null) arrayOf(mime) else null
        ) { path, uri ->
            Log.d(TAG, "Indexed received file into MediaStore: $path -> $uri")
        }

        try {
            @Suppress("DEPRECATION")
            val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                data = Uri.fromFile(savedFile)
            }
            context.sendBroadcast(scanIntent)
        } catch (_: Exception) {}

        // Send PayloadAck on controlTransport
        val ack = NearbyFrames.encodePayloadAck(NearbyFrames.PayloadAck(meta.payloadId, true))
        controlTransport.sendFrameDirect(codec.encrypt(ack))
        return true
    }

    fun cancelActiveTransfer() {
        sessionManager.cancelTransfer()
        primaryTransport?.close()
        synchronized(dataTransports) {
            dataTransports.forEach { it.close() }
            dataTransports.clear()
        }
    }

    fun pauseActiveTransfer() {
        sessionManager.pauseTransfer()
    }

    fun resumeActiveTransfer() {
        sessionManager.resumeTransfer()
    }
}
