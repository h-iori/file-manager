package com.ioristudios.filemanager.sharing.engine

import android.content.Context
import android.os.Build
import android.util.Log
import com.ioristudios.filemanager.sharing.discovery.DiscoveredPeer
import com.ioristudios.filemanager.sharing.discovery.NearbyMdnsManager
import com.ioristudios.filemanager.sharing.protocol.NearbyFrames
import com.ioristudios.filemanager.sharing.resilience.TransferLifecycleStatus
import com.ioristudios.filemanager.sharing.resilience.TransferProgressSnapshot
import com.ioristudios.filemanager.sharing.resilience.TransferSessionManager
import com.ioristudios.filemanager.sharing.service.ShareTransferService
import com.ioristudios.filemanager.sharing.transport.SocketTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.net.ServerSocket

enum class ShareMode {
    SEND,
    RECEIVE
}

/**
 * Top-level coordinator for wireless P2P file sharing.
 */
class ShareTransferManager(private val context: Context) {

    companion object {
        private const val TAG = "ShareTransferManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var transferJob: Job? = null
    private var serverSocket: ServerSocket? = null

    val mdnsManager = NearbyMdnsManager(context)
    val sessionManager = TransferSessionManager()
    val transferEngine = FileTransferEngine(context, sessionManager)

    private val _shareMode = MutableStateFlow(ShareMode.SEND)
    val shareMode: StateFlow<ShareMode> = _shareMode.asStateFlow()

    private val _selectedFiles = MutableStateFlow<List<File>>(emptyList())
    val selectedFiles: StateFlow<List<File>> = _selectedFiles.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _pairingPin = MutableStateFlow<String?>(null)
    val pairingPin: StateFlow<String?> = _pairingPin.asStateFlow()

    private val _pairingPeerName = MutableStateFlow<String?>(null)
    val pairingPeerName: StateFlow<String?> = _pairingPeerName.asStateFlow()

    private val _showApprovalSheet = MutableStateFlow(false)
    val showApprovalSheet: StateFlow<Boolean> = _showApprovalSheet.asStateFlow()

    init {
        // Wire notification actions from ShareTransferService
        ShareTransferService.onActionReceived = { action ->
            when (action) {
                ShareTransferService.ACTION_PAUSE -> pauseTransfer()
                ShareTransferService.ACTION_RESUME -> resumeTransfer()
                ShareTransferService.ACTION_CANCEL -> cancelTransfer()
            }
        }
    }

    fun setShareMode(mode: ShareMode) {
        if (_shareMode.value == mode) return
        _shareMode.value = mode
        stopAll()
        if (mode == ShareMode.RECEIVE) {
            startAdvertising()
        }
    }

    fun setSelectedFiles(files: List<File>) {
        _selectedFiles.value = files
    }

    fun addSelectedFile(file: File) {
        if (!_selectedFiles.value.contains(file)) {
            _selectedFiles.value = _selectedFiles.value + file
        }
    }

    fun removeSelectedFile(file: File) {
        _selectedFiles.value = _selectedFiles.value.filter { it.absolutePath != file.absolutePath }
    }

    fun clearSelectedFiles() {
        _selectedFiles.value = emptyList()
        stopScanningForPeers()
    }

    fun startScanningForPeers() {
        if (_selectedFiles.value.isEmpty()) return
        _isScanning.value = true
        mdnsManager.startDiscovery()
    }

    fun stopScanningForPeers() {
        _isScanning.value = false
        mdnsManager.stopDiscovery()
    }

    fun startDiscovery() {
        startScanningForPeers()
    }

    fun startAdvertising() {
        scope.launch(Dispatchers.IO) {
            try {
                serverSocket?.close()
                val (ss, port) = SocketTransport.createServer(0)
                serverSocket = ss
                val devName = Build.MODEL ?: "Android Quick Share"
                mdnsManager.startAdvertising(port, devName)

                // Start receiver loop
                transferJob = scope.launch {
                    transferEngine.receiveFiles(
                        serverSocket = ss,
                        onRequireApproval = { peerName, pin, files ->
                            _pairingPeerName.value = peerName
                            _pairingPin.value = pin
                            _showApprovalSheet.value = true
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start advertising receiver", e)
            }
        }
    }

    fun connectAndSend(peer: DiscoveredPeer) {
        val files = _selectedFiles.value
        if (files.isEmpty()) return

        transferJob?.cancel()
        ShareTransferService.startService(context)

        transferJob = scope.launch {
            transferEngine.sendFiles(
                peer = peer,
                filesToSend = files,
                onPinGenerated = { pin ->
                    _pairingPin.value = pin
                    _pairingPeerName.value = peer.deviceName
                }
            )
        }
    }

    fun answerApproval(accepted: Boolean) {
        _showApprovalSheet.value = false
        if (accepted) {
            ShareTransferService.startService(context)
        }
        transferEngine.answerApproval(accepted)
    }

    fun pauseTransfer() {
        transferEngine.pauseActiveTransfer()
    }

    fun resumeTransfer() {
        transferEngine.resumeActiveTransfer()
    }

    fun cancelTransfer() {
        transferJob?.cancel()
        transferEngine.cancelActiveTransfer()
        ShareTransferService.stopService(context)
        _showApprovalSheet.value = false
        _pairingPin.value = null
        _pairingPeerName.value = null
    }

    fun dismissCompleted() {
        sessionManager.reset()
        _pairingPin.value = null
        _pairingPeerName.value = null
        _showApprovalSheet.value = false
        ShareTransferService.stopService(context)
        if (_shareMode.value == ShareMode.RECEIVE) {
            startAdvertising()
        }
    }

    fun stopAll() {
        cancelTransfer()
        stopScanningForPeers()
        mdnsManager.clear()
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
    }
}

