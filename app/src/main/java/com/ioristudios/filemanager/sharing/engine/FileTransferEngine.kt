package com.ioristudios.filemanager.sharing.engine

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
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
        private const val CHUNK_SIZE = 64 * 1024 // 64 KB chunks
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
        return shareDir
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
                NearbyFrames.FilePayloadMeta(
                    payloadId = System.currentTimeMillis() + idx,
                    fileName = f.name,
                    fileSize = f.length(),
                    mimeType = "*/*"
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

            for ((index, file) in filesToSend.withIndex()) {
                val payloadId = payloadMetaList[index].payloadId
                val fileLength = file.length()
                var fileOffset = 0L

                val raf = RandomAccessFile(file, "r")
                val buffer = ByteArray(CHUNK_SIZE)

                try {
                    while (fileOffset < fileLength) {
                        // Check cancellation
                        if (sessionManager.isCancelled) {
                            val cancelFrame = NearbyFrames.encodeTransferControl(
                                NearbyFrames.TransferControl(payloadId, "CANCEL")
                            )
                            transport.sendFrame(codec.encrypt(cancelFrame))
                            return@withContext
                        }

                        // Check pause
                        while (sessionManager.isPaused && !sessionManager.isCancelled) {
                            delay(200)
                        }

                        val bytesRead = raf.read(buffer)
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
                        transport.sendFrame(codec.encrypt(payloadPacket))

                        fileOffset += bytesRead
                        transferredTotal += bytesRead

                        sessionManager.updateMetrics(
                            fileName = file.name,
                            fileIndex = index + 1,
                            totalFiles = filesToSend.size,
                            transferredBytes = transferredTotal,
                            totalBytes = totalBytes
                        )
                    }
                } finally {
                    raf.close()
                }

                // Await PayloadAck
                val ackRaw = transport.receiveFrame()
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

            // Send TransferAccept
            transport.sendFrame(codec.encrypt(NearbyFrames.encodeTransferAccept()))

            // 7. Receive Payload Chunks
            sessionManager.updateStatus(TransferLifecycleStatus.TRANSFERRING)
            val totalBytes = incomingFiles.sumOf { it.fileSize }
            var transferredTotal = 0L
            val downloadDir = getDownloadDir()

            for ((index, meta) in incomingFiles.withIndex()) {
                val tempFile = DataIntegrityVerifier.getTempResumeFile(context.cacheDir, meta.payloadId)
                val raf = RandomAccessFile(tempFile, "rw")
                var fileBytesReceived = raf.length()
                raf.seek(fileBytesReceived)

                var isDone = false
                while (!isDone && coroutineContext.isActive) {
                    if (sessionManager.isCancelled) {
                        return@withContext
                    }

                    while (sessionManager.isPaused && !sessionManager.isCancelled) {
                        delay(200)
                    }

                    val chunkRaw = transport.receiveFrame()
                    val chunkDecrypted = codec.decrypt(chunkRaw)
                    val frame = NearbyFrames.decodeFrame(chunkDecrypted)

                    if (frame is NearbyFrames.ParsedFrame.Control && frame.control.action == "CANCEL") {
                        sessionManager.cancelTransfer()
                        return@withContext
                    }

                    if (frame is NearbyFrames.ParsedFrame.Payload) {
                        val p = frame.transfer
                        raf.write(p.chunkData)
                        fileBytesReceived += p.chunkData.size
                        transferredTotal += p.chunkData.size

                        sessionManager.updateMetrics(
                            fileName = meta.fileName,
                            fileIndex = index + 1,
                            totalFiles = incomingFiles.size,
                            transferredBytes = transferredTotal,
                            totalBytes = totalBytes
                        )

                        if (p.isLastChunk) {
                            isDone = true
                        }
                    }
                }
                raf.close()

                // Move temp file to final destination
                val destinationFile = File(downloadDir, meta.fileName)
                tempFile.renameTo(destinationFile)

                // Trigger Android MediaStore indexer
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(destinationFile.absolutePath),
                    arrayOf(meta.mimeType)
                ) { path, uri ->
                    Log.d(TAG, "Indexed received file into MediaStore: $path -> $uri")
                }

                // Send PayloadAck
                val ack = NearbyFrames.encodePayloadAck(NearbyFrames.PayloadAck(meta.payloadId, true))
                transport.sendFrame(codec.encrypt(ack))
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

