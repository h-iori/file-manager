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
import com.ioristudios.filemanager.sharing.transport.HotspotManager
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

enum class ShareScreenState {
    LANDING,        // Hero Send & Receive Cards, device status, zero scanning/advertising
    SEND_PICKER,    // In-app File Manager multi-select picker + bottom SEND CTA button
    SEND_RADAR,     // Radar Scanner actively discovering nearby peers; tap peer to send
    RECEIVE_BEACON, // Receiver beacon actively advertising and listening; waiting for sender
    TRANSFER_ACTIVE // Transfer Progress Screen with real-time MB/s, progress bar, pause, resume, cancel
}

/**
 * Top-level coordinator for wireless P2P file sharing.
 * Implements strict 5-state lifecycle: zero background scanning/advertising when idle.
 * Supports Wi-Fi LAN direct P2P as well as direct Hotspot P2P for non-Wi-Fi environments.
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
    val parallelEngine = ParallelTransferEngine(context, sessionManager, parallelChannels = 4)
    val hotspotManager = HotspotManager(context)

    private val _screenState = MutableStateFlow(ShareScreenState.LANDING)
    val screenState: StateFlow<ShareScreenState> = _screenState.asStateFlow()

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

    private val _hotspotSsid = MutableStateFlow<String?>(null)
    val hotspotSsid: StateFlow<String?> = _hotspotSsid.asStateFlow()

    private val _hotspotPassword = MutableStateFlow<String?>(null)
    val hotspotPassword: StateFlow<String?> = _hotspotPassword.asStateFlow()

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

    // --- State Machine Navigation ---

    /**
     * User taps Hero "Send" on Landing Screen -> Opens In-App File Picker
     */
    fun openSendFlow() {
        stopAll()
        sessionManager.reset()
        _selectedFiles.value = emptyList()
        _shareMode.value = ShareMode.SEND
        _screenState.value = ShareScreenState.SEND_PICKER
    }

    /**
     * User clicks bottom "SEND" in File Picker -> Transitions to Radar Screen & begins scanning
     */
    fun confirmSendAndScan() {
        if (_selectedFiles.value.isEmpty()) return
        sessionManager.reset()
        _screenState.value = ShareScreenState.SEND_RADAR
        startScanningForPeers()
    }

    /**
     * User taps "Edit Selection" on Radar Screen -> Returns to Picker, pauses scanning
     */
    fun backToPicker() {
        stopScanningForPeers()
        sessionManager.reset()
        _screenState.value = ShareScreenState.SEND_PICKER
    }

    /**
     * User taps Hero "Receive" on Landing Screen -> Opens Receiving Beacon & begins advertising
     */
    fun openReceiveFlow() {
        stopAll()
        sessionManager.reset()
        _shareMode.value = ShareMode.RECEIVE
        _screenState.value = ShareScreenState.RECEIVE_BEACON
        startAdvertising()
    }

    /**
     * User taps Back from any stage -> Returns to clean Landing Screen and stops all network work
     */
    fun backToLanding() {
        stopAll()
        sessionManager.reset()
        _screenState.value = ShareScreenState.LANDING
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
        _isScanning.value = true
        mdnsManager.startDiscovery()
    }

    fun stopScanningForPeers() {
        _isScanning.value = false
        mdnsManager.stopDiscovery()
    }

    fun startAdvertising() {
        scope.launch(Dispatchers.IO) {
            try {
                serverSocket?.close()
                val (ss, port) = SocketTransport.createServer(0)
                serverSocket = ss
                val devName = Build.MODEL ?: "Android Quick Share"
                mdnsManager.startAdvertising(port, devName)

                // Start receiver loop with high-speed parallel engine & Google Files fallback
                transferJob = scope.launch {
                    parallelEngine.receiveFiles(
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

    /**
     * Toggles a Direct Local-Only Hotspot on this device (for non-Wi-Fi environments)
     */
    fun startDirectHotspot(onResult: (Boolean, String) -> Unit) {
        hotspotManager.startHotspot(
            onStarted = { ssid, pass ->
                _hotspotSsid.value = ssid
                _hotspotPassword.value = pass
                onResult(true, "Hotspot active: $ssid")
            },
            onFailed = { reason ->
                onResult(false, reason)
            }
        )
    }

    fun stopDirectHotspot() {
        hotspotManager.stopHotspot()
        _hotspotSsid.value = null
        _hotspotPassword.value = null
    }

    fun connectAndSend(peer: DiscoveredPeer) {
        val files = _selectedFiles.value
        if (files.isEmpty()) return

        transferJob?.cancel()
        _screenState.value = ShareScreenState.TRANSFER_ACTIVE
        ShareTransferService.startService(context)

        transferJob = scope.launch {
            parallelEngine.sendFiles(
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
            _screenState.value = ShareScreenState.TRANSFER_ACTIVE
            ShareTransferService.startService(context)
        }
        parallelEngine.answerApproval(accepted)
        transferEngine.answerApproval(accepted)
    }

    fun pauseTransfer() {
        parallelEngine.pauseActiveTransfer()
        transferEngine.pauseActiveTransfer()
    }

    fun resumeTransfer() {
        parallelEngine.resumeActiveTransfer()
        transferEngine.resumeActiveTransfer()
    }

    fun cancelTransfer() {
        transferJob?.cancel()
        transferJob = null
        parallelEngine.cancelActiveTransfer()
        transferEngine.cancelActiveTransfer()
        sessionManager.reset()
        ShareTransferService.stopService(context)
        _showApprovalSheet.value = false
        _pairingPin.value = null
        _pairingPeerName.value = null
        _screenState.value = ShareScreenState.LANDING
    }

    fun dismissCompleted() {
        transferJob?.cancel()
        transferJob = null
        sessionManager.reset()
        _pairingPin.value = null
        _pairingPeerName.value = null
        _showApprovalSheet.value = false
        ShareTransferService.stopService(context)
        _screenState.value = ShareScreenState.LANDING
    }

    fun stopAll() {
        transferJob?.cancel()
        transferJob = null
        parallelEngine.cancelActiveTransfer()
        transferEngine.cancelActiveTransfer()
        sessionManager.reset()
        stopScanningForPeers()
        stopDirectHotspot()
        mdnsManager.clear()
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        _pairingPin.value = null
        _pairingPeerName.value = null
        _showApprovalSheet.value = false
    }
}
