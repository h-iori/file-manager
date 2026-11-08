package com.ioristudios.filemanager.sharing.engine

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import com.ioristudios.filemanager.sharing.discovery.DiscoveredPeer
import com.ioristudios.filemanager.sharing.protocol.NearbyFrames
import com.ioristudios.filemanager.sharing.protocol.PinCalculator
import com.ioristudios.filemanager.sharing.protocol.SecureMessageCodec
import com.ioristudios.filemanager.sharing.protocol.Ukey2Handshake
import com.ioristudios.filemanager.sharing.resilience.DataIntegrityVerifier
import com.ioristudios.filemanager.sharing.resilience.TransferLifecycleStatus
import com.ioristudios.filemanager.sharing.resilience.TransferSessionManager
import com.ioristudios.filemanager.sharing.transport.SocketTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import kotlin.coroutines.coroutineContext

/**
 * Core P2P file transfer engine for Google Files / Quick Share interoperable sharing.
 */
class FileTransferEngine(
    private val context: Context,
    val sessionManager: TransferSessionManager
) {

    companion object {
        private const val TAG = "FileTransferEngine"
        const val CHUNK_SIZE = 1024 * 1024 // 1 MB high-speed chunks for maximum throughput
    }

    var pendingPin: String? = null
        private set
    var pendingPeerName: String? = null
        private set
    var pendingFiles: List<NearbyFrames.FilePayloadMeta> = emptyList()
        private set

    private var currentSocketTransport: SocketTransport? = null
    private var approvalContinuation: ((Boolean) -> Unit)? = null

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

    /**
     * Called by UI when user taps Decline or Accept on PIN verification dialog
     */
    fun answerApproval(accepted: Boolean) {
        approvalContinuation?.invoke(accepted)
        approvalContinuation = null
    }

    /**
     * SENDER FLOW: Connects to peer, performs UKEY2 handshake, and streams files.
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

        var transport: SocketTransport? = null
        try {
            Log.d(TAG, "Connecting to peer ${peer.deviceName} at $host:$port")
            transport = SocketTransport.connect(host, port, timeoutMs = 12000)
            currentSocketTransport = transport

            // 1. UKEY2 Handshake (Client role)
            sessionManager.updateStatus(TransferLifecycleStatus.HANDSHAKING)
            val ukey = Ukey2Handshake(isClient = true)
            val clientInit = ukey.createClientInit()
            transport.sendFrame(clientInit)

            val serverInit = transport.receiveFrame()
            val clientFinish = ukey.processServerInitAndGenerateClientFinish(serverInit)
            transport.sendFrame(clientFinish)

            // 2. Derive PIN and Session Keys
            val authToken = ukey.authToken ?: throw IllegalStateException("Auth token is null")
            val pin = PinCalculator.calculatePin(authToken)
            pendingPin = pin
            pendingPeerName = peer.deviceName
            onPinGenerated(pin)

            val codec = SecureMessageCodec(
                encryptionKey = ukey.getEncryptionKey(),
                decryptionKey = ukey.getDecryptionKey(),
                signingHmacKey = ukey.getSigningHmacKey(),
                verifyingHmacKey = ukey.getVerifyingHmacKey()
            )

            // 3. Send ConnectionRequest inside SecureMessage
            val reqFrame = NearbyFrames.encodeConnectionRequest(
                NearbyFrames.ConnectionRequest("CLNT", android.os.Build.MODEL)
            )
            transport.sendFrame(codec.encrypt(reqFrame))

            // 4. Await ConnectionResponse
            sessionManager.updateStatus(TransferLifecycleStatus.WAITING_APPROVAL)
            val respRaw = transport.receiveFrame()
            val respDecrypted = codec.decrypt(respRaw)
            val parsedResp = NearbyFrames.decodeFrame(respDecrypted)
            if (parsedResp !is NearbyFrames.ParsedFrame.ConnResponse || !parsedResp.response.accepted) {
                sessionManager.markFailed("Transfer rejected by ${peer.deviceName}")
                return@withContext
            }

            // 5. Send Introduction with file metadata
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
            transport.sendFrame(codec.encrypt(introFrame))

            // 6. Await TransferAccept
            val acceptRaw = transport.receiveFrame()
            val acceptDecrypted = codec.decrypt(acceptRaw)
            val parsedAccept = NearbyFrames.decodeFrame(acceptDecrypted)
            if (parsedAccept !is NearbyFrames.ParsedFrame.TransferAccepted) {
                sessionManager.markFailed("Peer did not accept transfer files")
                return@withContext
            }

            // 7. Stream Files
            sessionManager.updateStatus(TransferLifecycleStatus.TRANSFERRING)
            val totalBytes = filesToSend.sumOf { it.length() }
            var transferredTotal = 0L
            var lastMetricsUpdateMs = 0L

            for ((index, file) in filesToSend.withIndex()) {
                val payloadId = payloadMetaList[index].payloadId
                val fileLength = file.length()
                var fileOffset = 0L

                val bis = BufferedInputStream(FileInputStream(file), 512 * 1024)
                val buffer = ByteArray(CHUNK_SIZE)

                try {
                    while (fileOffset < fileLength) {
                        // Check cancellation
                        if (sessionManager.isCancelled) {
                            val cancelFrame = NearbyFrames.encodeTransferControl(
                                NearbyFrames.TransferControl(payloadId, "CANCEL")
                            )
                            transport.sendFrameDirect(codec.encrypt(cancelFrame))
                            return@withContext
                        }

                        // Check pause
                        while (sessionManager.isPaused && !sessionManager.isCancelled) {
                            delay(200)
                        }

                        val bytesRead = bis.read(buffer)
                        if (bytesRead == -1) break

                        val chunk = if (bytesRead == CHUNK_SIZE) buffer else buffer.copyOf(bytesRead)
                        val isLast = (fileOffset + bytesRead) >= fileLength

                        val payloadPacket = NearbyFrames.encodePayloadTransfer(
                            NearbyFrames.PayloadTransfer(
                                payloadId = payloadId,
                                offset = fileOffset,
                                chunkData = chunk,
                                isLastChunk = isLast
                            )
                        )
                        transport.sendFrameDirect(codec.encrypt(payloadPacket))

                        fileOffset += bytesRead
                        transferredTotal += bytesRead

                        val now = System.currentTimeMillis()
                        if (isLast || now - lastMetricsUpdateMs >= 100L) {
                            lastMetricsUpdateMs = now
                            sessionManager.updateMetrics(
                                fileName = file.name,
                                fileIndex = index + 1,
                                totalFiles = filesToSend.size,
                                transferredBytes = transferredTotal,
                                totalBytes = totalBytes
                            )
                        }
                    }
                } finally {
                    bis.close()
                }

                // Await PayloadAck
                val ackRaw = transport.receiveFrameDirect()
                val ackDecrypted = codec.decrypt(ackRaw)
                val parsedAck = NearbyFrames.decodeFrame(ackDecrypted)
                if (parsedAck is NearbyFrames.ParsedFrame.Ack && !parsedAck.ack.success) {
                    sessionManager.markFailed("Payload verification failed for ${file.name}")
                    return@withContext
                }
            }

            sessionManager.markCompleted()
            Log.d(TAG, "All files sent successfully!")

        } catch (e: Exception) {
            Log.e(TAG, "Send files error", e)
            if (!sessionManager.isCancelled) {
                sessionManager.markFailed(e.message ?: "Transfer connection failed", canResume = true)
            }
        } finally {
            currentSocketTransport = null
            transport?.close()
        }
    }

    /**
     * RECEIVER FLOW: Listens on server socket, handles incoming connection, prompts user approval, receives files.
     */
    suspend fun receiveFiles(
        serverSocket: ServerSocket,
        onRequireApproval: (peerName: String, pin: String, files: List<NearbyFrames.FilePayloadMeta>) -> Unit
    ) = withContext(Dispatchers.IO) {
        var clientSocket: Socket? = null
        var transport: SocketTransport? = null
        try {
            sessionManager.reset()
            sessionManager.updateStatus(TransferLifecycleStatus.DISCOVERING)
            Log.d(TAG, "Receiver listening on port ${serverSocket.localPort}")

            clientSocket = serverSocket.accept()
            try {
                clientSocket.tcpNoDelay = true
                clientSocket.soTimeout = 45000
                clientSocket.sendBufferSize = 1024 * 1024
                clientSocket.receiveBufferSize = 1024 * 1024
            } catch (_: Exception) {}
            transport = SocketTransport(clientSocket)
            currentSocketTransport = transport

            // 1. UKEY2 Handshake (Server role)
            sessionManager.updateStatus(TransferLifecycleStatus.HANDSHAKING)
            val ukey = Ukey2Handshake(isClient = false)
            val clientInit = transport.receiveFrame()
            val serverInit = ukey.processClientInitAndGenerateServerInit(clientInit)
            transport.sendFrame(serverInit)

            val clientFinish = transport.receiveFrame()
            ukey.processClientFinished(clientFinish)

            // 2. Derive PIN and Session Keys
            val authToken = ukey.authToken ?: throw IllegalStateException("Auth token is null")
            val pin = PinCalculator.calculatePin(authToken)
            pendingPin = pin

            val codec = SecureMessageCodec(
                encryptionKey = ukey.getEncryptionKey(),
                decryptionKey = ukey.getDecryptionKey(),
                signingHmacKey = ukey.getSigningHmacKey(),
                verifyingHmacKey = ukey.getVerifyingHmacKey()
            )

            // 3. Receive ConnectionRequest
            val reqRaw = transport.receiveFrame()
            val reqDecrypted = codec.decrypt(reqRaw)
            val parsedReq = NearbyFrames.decodeFrame(reqDecrypted)
            val peerName = if (parsedReq is NearbyFrames.ParsedFrame.ConnRequest) {
                parsedReq.request.endpointName
            } else "Nearby Device"
            pendingPeerName = peerName

            // 4. Prompt User for Approval
            sessionManager.updateStatus(TransferLifecycleStatus.WAITING_APPROVAL)
            val userApproved = kotlinx.coroutines.suspendCancellableCoroutine<Boolean> { cont ->
                approvalContinuation = { accepted ->
                    if (cont.isActive) cont.resumeWith(Result.success(accepted))
                }
                onRequireApproval(peerName, pin, emptyList())
            }

            // 5. Send ConnectionResponse
            val respFrame = NearbyFrames.encodeConnectionResponse(
                NearbyFrames.ConnectionResponse(accepted = userApproved)
            )
            transport.sendFrame(codec.encrypt(respFrame))

            if (!userApproved) {
                sessionManager.updateStatus(TransferLifecycleStatus.CANCELLED)
                return@withContext
            }

            // 6. Receive Introduction
            val introRaw = transport.receiveFrame()
            val introDecrypted = codec.decrypt(introRaw)
            val parsedIntro = NearbyFrames.decodeFrame(introDecrypted)
            val incomingFiles = if (parsedIntro is NearbyFrames.ParsedFrame.Intro) {
                parsedIntro.introduction.files
            } else emptyList()
            pendingFiles = incomingFiles

            if (incomingFiles.isEmpty()) {
                sessionManager.markFailed("No files to receive from peer")
                return@withContext
            }

            // Send TransferAccept
            transport.sendFrame(codec.encrypt(NearbyFrames.encodeTransferAccept()))

            // 7. Receive Payload Chunks
            sessionManager.updateStatus(TransferLifecycleStatus.TRANSFERRING)
            val totalBytes = incomingFiles.sumOf { it.fileSize }
            var transferredTotal = 0L
            val downloadDir = getDownloadDir()

            // Clean any stale temp files from previous failed transfers
            try {
                downloadDir.listFiles { f -> f.name.startsWith(".share_tmp_") && f.name.endsWith(".part") }
                    ?.forEach { stale ->
                        Log.d(TAG, "Cleaning stale temp file: ${stale.name}")
                        stale.delete()
                    }
            } catch (_: Exception) {}

            for ((index, meta) in incomingFiles.withIndex()) {
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }

                val tempFile = DataIntegrityVerifier.getTempResumeFile(downloadDir, meta.payloadId)
                if (tempFile.exists()) {
                    tempFile.delete()
                }

                val fos = FileOutputStream(tempFile, false)
                val bos = BufferedOutputStream(fos, 1024 * 1024)
                var fileBytesReceived = 0L
                var lastMetricsUpdateMs = 0L

                var isDone = false
                try {
                    while (!isDone && coroutineContext.isActive) {
                        if (sessionManager.isCancelled) {
                            return@withContext
                        }

                        while (sessionManager.isPaused && !sessionManager.isCancelled) {
                            delay(200)
                        }

                        val chunkRaw = transport.receiveFrameDirect()
                        val chunkDecrypted = codec.decrypt(chunkRaw)
                        val frame = NearbyFrames.decodeFrame(chunkDecrypted)

                        if (frame is NearbyFrames.ParsedFrame.Control && frame.control.action == "CANCEL") {
                            sessionManager.cancelTransfer()
                            return@withContext
                        }

                        if (frame is NearbyFrames.ParsedFrame.Payload) {
                            val p = frame.transfer
                            bos.write(p.chunkData)
                            fileBytesReceived += p.chunkData.size
                            transferredTotal += p.chunkData.size

                            val now = System.currentTimeMillis()
                            if (p.isLastChunk || now - lastMetricsUpdateMs >= 100L) {
                                lastMetricsUpdateMs = now
                                sessionManager.updateMetrics(
                                    fileName = meta.fileName,
                                    fileIndex = index + 1,
                                    totalFiles = incomingFiles.size,
                                    transferredBytes = transferredTotal,
                                    totalBytes = totalBytes
                                )
                            }

                            if (p.isLastChunk) {
                                isDone = true
                            }
                        }
                    }
                    bos.flush()
                    try {
                        fos.fd.sync()
                    } catch (_: Exception) {}
                } finally {
                    try {
                        bos.close()
                    } catch (_: Exception) {}
                    try {
                        fos.close()
                    } catch (_: Exception) {}
                }

                // Move temp file to final destination
                val safeFileName = File(meta.fileName).name.ifBlank { "received_${meta.payloadId}" }
                val targetFile = resolveNameConflict(File(downloadDir, safeFileName))
                var savedFile = targetFile

                val moved = tempFile.renameTo(targetFile)
                if (!moved) {
                    try {
                        tempFile.copyTo(targetFile, overwrite = true)
                        tempFile.delete()
                        savedFile = targetFile
                    } catch (e: Exception) {
                        Log.e(TAG, "Fallback move failed: ${e.message}", e)
                        // Make sure file isn't left as hidden dot-file
                        val unhiddenFile = File(downloadDir, safeFileName)
                        if (tempFile.renameTo(unhiddenFile)) {
                            savedFile = unhiddenFile
                        } else {
                            savedFile = tempFile
                        }
                    }
                }

                // Verify file exists and has data
                if (!savedFile.exists() || (meta.fileSize > 0L && savedFile.length() == 0L)) {
                    Log.e(TAG, "File save verification failed: ${savedFile.absolutePath} does not exist or empty")
                    val failAck = NearbyFrames.encodePayloadAck(NearbyFrames.PayloadAck(meta.payloadId, false))
                    transport.sendFrameDirect(codec.encrypt(failAck))
                    sessionManager.markFailed("Failed to save ${meta.fileName}")
                    return@withContext
                }

                // Determine accurate MIME type for Android MediaStore
                val ext = savedFile.extension.lowercase()
                val mime = if (ext.isNotEmpty()) {
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
                } else {
                    meta.mimeType.takeIf { it != "*/*" }
                }

                // Trigger Android MediaStore indexer
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(savedFile.absolutePath),
                    if (mime != null) arrayOf(mime) else null
                ) { path, uri ->
                    Log.d(TAG, "Indexed received file into MediaStore: $path -> $uri")
                }

                // Additional fallback scan broadcast for custom Android skins (MIUI, ColorOS)
                try {
                    val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                        data = Uri.fromFile(savedFile)
                    }
                    context.sendBroadcast(scanIntent)
                } catch (_: Exception) {}

                // Send PayloadAck
                val ack = NearbyFrames.encodePayloadAck(NearbyFrames.PayloadAck(meta.payloadId, true))
                transport.sendFrameDirect(codec.encrypt(ack))
            }

            sessionManager.markCompleted()
            Log.d(TAG, "All files received successfully!")

        } catch (e: Exception) {
            Log.e(TAG, "Receive files error", e)
            if (!sessionManager.isCancelled) {
                sessionManager.markFailed(e.message ?: "Receive connection failed", canResume = true)
            }
        } finally {
            currentSocketTransport = null
            transport?.close()
        }
    }

    fun cancelActiveTransfer() {
        sessionManager.cancelTransfer()
        currentSocketTransport?.close()
    }

    fun pauseActiveTransfer() {
        sessionManager.pauseTransfer()
    }

    fun resumeActiveTransfer() {
        sessionManager.resumeTransfer()
    }
}

