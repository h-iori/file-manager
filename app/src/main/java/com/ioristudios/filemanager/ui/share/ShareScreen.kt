package com.ioristudios.filemanager.ui.share

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ioristudios.filemanager.haptics.HapticType
import com.ioristudios.filemanager.haptics.LocalAppHaptics
import com.ioristudios.filemanager.sharing.engine.ShareScreenState
import com.ioristudios.filemanager.sharing.resilience.TransferLifecycleStatus
import com.ioristudios.filemanager.theme.BgDeepDark
import com.ioristudios.filemanager.ui.main.MainScreenViewModel
import com.ioristudios.filemanager.ui.share.components.PinVerificationDialog
import com.ioristudios.filemanager.ui.share.components.ShareFilePickerSheet
import com.ioristudios.filemanager.ui.share.components.ShareLandingView
import com.ioristudios.filemanager.ui.share.components.ShareRadarScreen
import com.ioristudios.filemanager.ui.share.components.ShareReceivingScreen
import com.ioristudios.filemanager.ui.share.components.TransferProgressModal

/**
 * World-Class Cyberpunk / Luxury Glassmorphic Quick Share Screen.
 * Implements strict 5-stage flow:
 * 1. LANDING: Hero Send & Receive Cards, completely idle (zero scanning/advertising).
 * 2. SEND_PICKER: Multi-select files in internal File Manager, click bottom SEND.
 * 3. SEND_RADAR: Radar scanning active ONLY here, discovered peer galaxy orbs.
 * 4. RECEIVE_BEACON: Receiving beacon & mDNS advertising active ONLY here.
 * 5. TRANSFER_ACTIVE: Full transfer progress dashboard with MB/s, ETA, and Pause/Cancel.
 *
 * Full AP-isolation bypass with Direct Local Hotspot support for off-grid sharing.
 * Guaranteed 96dp bottom clearance above FloatingBottomNav.
 */
@Composable
fun ShareScreen(
    viewModel: MainScreenViewModel,
    showMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val shareManager = remember { viewModel.shareTransferManager }
    val haptics = LocalAppHaptics.current

    val screenState by shareManager.screenState.collectAsStateWithLifecycle()
    val selectedFiles by shareManager.selectedFiles.collectAsStateWithLifecycle()
    val peersMap by shareManager.mdnsManager.discoveredPeers.collectAsStateWithLifecycle()
    val transferProgress by shareManager.sessionManager.progress.collectAsStateWithLifecycle()
    val showApprovalSheet by shareManager.showApprovalSheet.collectAsStateWithLifecycle()
    val pairingPin by shareManager.pairingPin.collectAsStateWithLifecycle()
    val pairingPeerName by shareManager.pairingPeerName.collectAsStateWithLifecycle()
    val hotspotSsid by shareManager.hotspotSsid.collectAsStateWithLifecycle()
    val hotspotPassword by shareManager.hotspotPassword.collectAsStateWithLifecycle()

    // Clean up all background network sockets when leaving the Share section entirely
    DisposableEffect(Unit) {
        onDispose {
            shareManager.stopAll()
        }
    }

    // Hardware / Gesture Back Handling through the 5-state hierarchy
    BackHandler(enabled = screenState != ShareScreenState.LANDING) {
        haptics.trigger(HapticType.NAV_TICK)
        when (screenState) {
            ShareScreenState.SEND_PICKER -> shareManager.backToLanding()
            ShareScreenState.SEND_RADAR -> shareManager.backToPicker()
            ShareScreenState.RECEIVE_BEACON -> shareManager.backToLanding()
            ShareScreenState.TRANSFER_ACTIVE -> {
                shareManager.cancelTransfer()
            }
            ShareScreenState.LANDING -> { /* Handled by outer container */ }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgDeepDark)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                AnimatedContent(
                    targetState = screenState,
                    transitionSpec = {
                        if (targetState.ordinal > initialState.ordinal) {
                            (slideInHorizontally { width -> width / 3 } + fadeIn()) togetherWith
                                    (slideOutHorizontally { width -> -width / 3 } + fadeOut())
                        } else {
                            (slideInHorizontally { width -> -width / 3 } + fadeIn()) togetherWith
                                    (slideOutHorizontally { width -> width / 3 } + fadeOut())
                        }
                    },
                    label = "ShareFlowStateAnimation"
                ) { state ->
                    when (state) {
                        // 1. Pristine Hero Landing View
                        ShareScreenState.LANDING -> {
                            ShareLandingView(
                                downloadDir = shareManager.transferEngine.getDownloadDir(),
                                localIpAddress = shareManager.hotspotManager.getLocalIpAddress(),
                                onSendClick = {
                                    shareManager.openSendFlow()
                                },
                                onReceiveClick = {
                                    shareManager.openReceiveFlow()
                                }
                            )
                        }

                        // 2. In-App File Manager Picker
                        ShareScreenState.SEND_PICKER -> {
                            ShareFilePickerSheet(
                                viewModel = viewModel,
                                initiallySelectedFiles = selectedFiles,
                                onDismiss = {
                                    shareManager.backToLanding()
                                },
                                onConfirmSelection = { chosenFiles ->
                                    if (chosenFiles.isEmpty()) {
                                        showMessage("Please select files before proceeding")
                                    } else {
                                        shareManager.setSelectedFiles(chosenFiles)
                                        shareManager.confirmSendAndScan()
                                    }
                                }
                            )
                        }

                        // 3. Gated Radar Scanner View (Scanning begins ONLY here)
                        ShareScreenState.SEND_RADAR -> {
                            ShareRadarScreen(
                                peers = peersMap.values.toList(),
                                selectedFiles = selectedFiles,
                                onBackToSelection = {
                                    shareManager.backToPicker()
                                },
                                onClose = {
                                    shareManager.backToLanding()
                                },
                                onPeerClick = { peer ->
                                    shareManager.connectAndSend(peer)
                                }
                            )
                        }

                        // 4. Discoverable Receiver Beacon (Listening & mDNS starts ONLY here)
                        ShareScreenState.RECEIVE_BEACON -> {
                            ShareReceivingScreen(
                                downloadDir = shareManager.transferEngine.getDownloadDir(),
                                isHotspotActive = shareManager.hotspotManager.isHotspotActive,
                                hotspotSsid = hotspotSsid,
                                hotspotPassword = hotspotPassword,
                                onBack = {
                                    shareManager.backToLanding()
                                },
                                onToggleHotspot = {
                                    if (shareManager.hotspotManager.isHotspotActive) {
                                        shareManager.stopDirectHotspot()
                                        showMessage("Direct Hotspot stopped")
                                    } else {
                                        shareManager.startDirectHotspot { success, msg ->
                                            showMessage(msg)
                                        }
                                    }
                                }
                            )
                        }

                        // 5. Live Full Transfer Active State
                        ShareScreenState.TRANSFER_ACTIVE -> {
                            // Render radar or beacon background underneath modal
                            if (shareManager.shareMode.value == com.ioristudios.filemanager.sharing.engine.ShareMode.RECEIVE) {
                                ShareReceivingScreen(
                                    downloadDir = shareManager.transferEngine.getDownloadDir(),
                                    isHotspotActive = shareManager.hotspotManager.isHotspotActive,
                                    hotspotSsid = hotspotSsid,
                                    hotspotPassword = hotspotPassword,
                                    onBack = { shareManager.cancelTransfer() },
                                    onToggleHotspot = {}
                                )
                            } else {
                                ShareRadarScreen(
                                    peers = peersMap.values.toList(),
                                    selectedFiles = selectedFiles,
                                    onBackToSelection = { shareManager.cancelTransfer() },
                                    onClose = { shareManager.cancelTransfer() },
                                    onPeerClick = {}
                                )
                            }
                        }
                    }
                }
            }

            // Dedicated bottom clearance spacer: strictly prevents FloatingBottomNav overlap
            Spacer(
                modifier = Modifier
                    .navigationBarsPadding()
                    .height(80.dp)
            )
        }

        // --- Modals & Overlay Dialogs ---

        // 1. Receiver Accept / Reject Popup Modal
        if (showApprovalSheet && pairingPin != null) {
            PinVerificationDialog(
                peerName = pairingPeerName ?: "Nearby Device",
                pinCode = pairingPin!!,
                onDecline = {
                    haptics.trigger(HapticType.DESTRUCTIVE)
                    shareManager.answerApproval(false)
                },
                onAccept = {
                    haptics.trigger(HapticType.SUCCESS_CONFIRM)
                    shareManager.answerApproval(true)
                }
            )
        }

        // 2. Live Transfer Progress Modal (Gated strictly to TRANSFER_ACTIVE)
        if (screenState == ShareScreenState.TRANSFER_ACTIVE &&
            transferProgress.status != TransferLifecycleStatus.IDLE &&
            transferProgress.status != TransferLifecycleStatus.DISCOVERING &&
            !showApprovalSheet
        ) {
            TransferProgressModal(
                snapshot = transferProgress,
                peerName = pairingPeerName,
                onPause = {
                    haptics.trigger(HapticType.CLICK)
                    shareManager.pauseTransfer()
                },
                onResume = {
                    haptics.trigger(HapticType.CLICK)
                    shareManager.resumeTransfer()
                },
                onCancel = {
                    haptics.trigger(HapticType.DESTRUCTIVE)
                    shareManager.cancelTransfer()
                },
                onDismiss = {
                    haptics.trigger(HapticType.NAV_TICK)
                    shareManager.dismissCompleted()
                }
            )
        }
    }
}
