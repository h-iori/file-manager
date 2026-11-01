package com.ioristudios.filemanager.ui.share

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ioristudios.filemanager.haptics.HapticType
import com.ioristudios.filemanager.haptics.LocalAppHaptics
import com.ioristudios.filemanager.sharing.discovery.DeviceType
import com.ioristudios.filemanager.sharing.discovery.DiscoveredPeer
import com.ioristudios.filemanager.sharing.engine.ShareMode
import com.ioristudios.filemanager.sharing.resilience.TransferLifecycleStatus
import com.ioristudios.filemanager.theme.BgDeepDark
import com.ioristudios.filemanager.theme.CardBorderPurple
import com.ioristudios.filemanager.theme.CardGradientEnd
import com.ioristudios.filemanager.theme.CardGradientStart
import com.ioristudios.filemanager.theme.ChartAvailableEmerald
import com.ioristudios.filemanager.theme.ElectricCyan
import com.ioristudios.filemanager.theme.ElectricPurple
import com.ioristudios.filemanager.theme.ElectricTeal
import com.ioristudios.filemanager.theme.OrbitronFontFamily
import com.ioristudios.filemanager.theme.OutfitFontFamily
import com.ioristudios.filemanager.theme.PdfRed
import com.ioristudios.filemanager.theme.TextMuted
import com.ioristudios.filemanager.theme.TextPrimary
import com.ioristudios.filemanager.theme.TextSecondary
import com.ioristudios.filemanager.ui.main.MainScreenViewModel
import com.ioristudios.filemanager.ui.share.components.PinVerificationDialog
import com.ioristudios.filemanager.ui.share.components.ShareFilePickerSheet
import com.ioristudios.filemanager.ui.share.components.TransferProgressModal
import java.io.File

/**
 * Enterprise Luxury Cyberpunk Quick Share Screen.
 * Two-stage flow: File Selection -> Radar Scanning.
 * Button positioning strictly respects the FloatingBottomNav capsule.
 * High-precision Outfit typography and haptic feedback throughout.
 */
@Composable
fun ShareScreen(
    viewModel: MainScreenViewModel,
    showMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val shareManager = remember { viewModel.shareTransferManager }
    val haptics = LocalAppHaptics.current

    val mode by shareManager.shareMode.collectAsStateWithLifecycle()
    val isScanning by shareManager.isScanning.collectAsStateWithLifecycle()
    val peersMap by shareManager.mdnsManager.discoveredPeers.collectAsStateWithLifecycle()
    val selectedFiles by shareManager.selectedFiles.collectAsStateWithLifecycle()
    val transferProgress by shareManager.sessionManager.progress.collectAsStateWithLifecycle()
    val showApprovalSheet by shareManager.showApprovalSheet.collectAsStateWithLifecycle()
    val pairingPin by shareManager.pairingPin.collectAsStateWithLifecycle()
    val pairingPeerName by shareManager.pairingPeerName.collectAsStateWithLifecycle()

    var showFileManagerPicker by remember { mutableStateOf(false) }

    // Receiver Mode Advertising Lifecycle
    DisposableEffect(mode) {
        if (mode == ShareMode.RECEIVE) {
            shareManager.startAdvertising()
        }
        onDispose {
            // Keep state clean
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgDeepDark)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // --- Top Header: Quick Share Brand Title ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "QUICK SHARE",
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        letterSpacing = 1.5.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Google Files Interoperable P2P",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp,
                        color = ElectricCyan
                    )
                }

                // Active Status Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            when {
                                mode == ShareMode.RECEIVE -> ChartAvailableEmerald.copy(alpha = 0.15f)
                                isScanning -> ElectricCyan.copy(alpha = 0.15f)
                                else -> Color(0xFF1E1638)
                            }
                        )
                        .border(
                            1.dp,
                            when {
                                mode == ShareMode.RECEIVE -> ChartAvailableEmerald.copy(alpha = 0.4f)
                                isScanning -> ElectricCyan.copy(alpha = 0.4f)
                                else -> CardBorderPurple.copy(alpha = 0.4f)
                            },
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        mode == ShareMode.RECEIVE -> ChartAvailableEmerald
                                        isScanning -> ElectricCyan
                                        else -> TextMuted
                                    }
                                )
                        )
                        Text(
                            text = when {
                                mode == ShareMode.RECEIVE -> "RECEIVING"
                                isScanning -> "SCANNING"
                                else -> "READY"
                            },
                            fontFamily = OrbitronFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = when {
                                mode == ShareMode.RECEIVE -> ChartAvailableEmerald
                                isScanning -> ElectricCyan
                                else -> TextSecondary
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Mode Switcher (Send / Receive) Segmented Control ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F0B1E))
                    .border(1.dp, CardBorderPurple.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(4.dp)
            ) {
                // Send Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (mode == ShareMode.SEND) {
                                Modifier.background(
                                    Brush.horizontalGradient(
                                        listOf(ElectricCyan.copy(alpha = 0.9f), ElectricPurple.copy(alpha = 0.9f))
                                    )
                                )
                            } else {
                                Modifier.background(Color.Transparent)
                            }
                        )
                        .clickable {
                            haptics.trigger(HapticType.SWITCH_TOGGLE)
                            shareManager.setShareMode(ShareMode.SEND)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (mode == ShareMode.SEND) Color(0xFF07040D) else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Send Files",
                            fontFamily = OutfitFontFamily,
                            fontWeight = if (mode == ShareMode.SEND) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp,
                            color = if (mode == ShareMode.SEND) Color(0xFF07040D) else TextSecondary
                        )
                    }
                }

                // Receive Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (mode == ShareMode.RECEIVE) {
                                Modifier.background(
                                    Brush.horizontalGradient(
                                        listOf(ChartAvailableEmerald.copy(alpha = 0.9f), ElectricTeal.copy(alpha = 0.9f))
                                    )
                                )
                            } else {
                                Modifier.background(Color.Transparent)
                            }
                        )
                        .clickable {
                            haptics.trigger(HapticType.SWITCH_TOGGLE)
                            shareManager.setShareMode(ShareMode.RECEIVE)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Receive",
                            tint = if (mode == ShareMode.RECEIVE) Color(0xFF07040D) else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Receive",
                            fontFamily = OutfitFontFamily,
                            fontWeight = if (mode == ShareMode.RECEIVE) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp,
                            color = if (mode == ShareMode.RECEIVE) Color(0xFF07040D) else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Main Content Area (Flexible Weight above Bottom Nav) ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (mode == ShareMode.RECEIVE) {
                    // RECEIVE MODE
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            ReceiveModeBeaconView()
                        }
                        ReceiveDirectoryInfoCard(shareManager.transferEngine.getDownloadDir())
                    }
                } else {
                    // SEND MODE: Gated by confirmation
                    if (isScanning) {
                        // STAGE 2: Scanning Radar active with peers
                        ScanningRadarStage(
                            peers = peersMap.values.toList(),
                            selectedFiles = selectedFiles,
                            onBackToSelection = {
                                haptics.trigger(HapticType.NAV_TICK)
                                shareManager.stopScanningForPeers()
                            },
                            onPeerClick = { peer ->
                                haptics.trigger(HapticType.CLICK)
                                shareManager.connectAndSend(peer)
                            }
                        )
                    } else {
                        // STAGE 1: File Selection & Confirmation Stage
                        FileSelectionStage(
                            selectedFiles = selectedFiles,
                            onOpenFileManagerPicker = {
                                showFileManagerPicker = true
                            },
                            onRemoveFile = { file ->
                                shareManager.removeSelectedFile(file)
                            },
                            onConfirmAndScan = {
                                if (selectedFiles.isEmpty()) {
                                    showMessage("Please select files before scanning")
                                } else {
                                    shareManager.startScanningForPeers()
                                }
                            }
                        )
                    }
                }
            }

            // Dedicated clearance spacer: accounts for FloatingBottomNav (54dp + 16dp) + navigationBars
            Spacer(
                modifier = Modifier
                    .navigationBarsPadding()
                    .height(96.dp)
            )
        }

        // --- Dialogs & Modals ---

        // 1. In-App File Manager Picker Sheet
        if (showFileManagerPicker) {
            ShareFilePickerSheet(
                viewModel = viewModel,
                initiallySelectedFiles = selectedFiles,
                onDismiss = { showFileManagerPicker = false },
                onConfirmSelection = { chosenFiles ->
                    shareManager.setSelectedFiles(chosenFiles)
                }
            )
        }

        // 2. PIN Verification Dialog
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

        // 3. Active Transfer Modal
        if (transferProgress.status != TransferLifecycleStatus.IDLE &&
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

/**
 * Stage 1: File Selection & Confirmation View (Send Mode)
 */
@Composable
private fun FileSelectionStage(
    selectedFiles: List<File>,
    onOpenFileManagerPicker: () -> Unit,
    onRemoveFile: (File) -> Unit,
    onConfirmAndScan: () -> Unit
) {
    val haptics = LocalAppHaptics.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Action banner to open internal File Manager picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF191136), Color(0xFF100A24))
                        )
                    )
                    .border(1.dp, CardBorderPurple.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .clickable {
                        haptics.trigger(HapticType.CLICK)
                        onOpenFileManagerPicker()
                    }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan.copy(alpha = 0.15f))
                            .border(1.dp, ElectricCyan.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "File Manager",
                            tint = ElectricCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Browse in File Manager",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Select any photos, videos, docs, or APKs",
                            fontFamily = OutfitFontFamily,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Selected Files Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FILES QUEUE (${selectedFiles.size})",
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    color = ElectricPurple
                )
                val totalBytes = selectedFiles.sumOf { it.length() }
                Text(
                    text = if (selectedFiles.isEmpty()) "0 B" else formatTotalBytes(totalBytes),
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Queue List or Clean Empty Placeholder
            if (selectedFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0C071C))
                        .border(1.dp, CardBorderPurple.copy(alpha = 0.25f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF16102A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                contentDescription = "Empty",
                                tint = ElectricPurple.copy(alpha = 0.7f),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "No files selected yet",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tap 'Browse in File Manager' or share files directly from other apps into Files.",
                            fontFamily = OutfitFontFamily,
                            fontSize = 13.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(selectedFiles, key = { it.absolutePath }) { file ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF140D2A))
                                .border(1.dp, CardBorderPurple.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(ElectricCyan.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                        contentDescription = file.name,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatTotalBytes(file.length()),
                                        fontFamily = OutfitFontFamily,
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(PdfRed.copy(alpha = 0.1f))
                                        .clickable {
                                            haptics.trigger(HapticType.CLICK)
                                            onRemoveFile(file)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = PdfRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- Signature Send Action Button ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (selectedFiles.isNotEmpty()) {
                        Brush.horizontalGradient(listOf(ElectricCyan, ElectricPurple))
                    } else {
                        Brush.horizontalGradient(listOf(Color(0xFF22173D), Color(0xFF17102C)))
                    }
                )
                .border(
                    1.dp,
                    if (selectedFiles.isNotEmpty()) Color.White.copy(alpha = 0.3f) else CardBorderPurple.copy(alpha = 0.3f),
                    RoundedCornerShape(16.dp)
                )
                .clickable(enabled = selectedFiles.isNotEmpty()) {
                    haptics.trigger(HapticType.CLICK)
                    onConfirmAndScan()
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (selectedFiles.isNotEmpty()) Icons.Default.Wifi else Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Scan",
                    tint = if (selectedFiles.isNotEmpty()) Color(0xFF07040D) else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (selectedFiles.isNotEmpty()) {
                        "Scan for Nearby Devices (${selectedFiles.size})"
                    } else {
                        "Select Files to Share"
                    },
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp,
                    color = if (selectedFiles.isNotEmpty()) Color(0xFF07040D) else TextMuted
                )
            }
        }
    }
}

/**
 * Stage 2: Scanning Radar & Peer Selection View (Send Mode)
 */
@Composable
private fun ScanningRadarStage(
    peers: List<DiscoveredPeer>,
    selectedFiles: List<File>,
    onBackToSelection: () -> Unit,
    onPeerClick: (DiscoveredPeer) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Top Selection Status Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF130E29))
                .border(1.dp, CardBorderPurple.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.clickable { onBackToSelection() }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Edit Selection",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = ElectricCyan
                )
            }

            val totalBytes = selectedFiles.sumOf { it.length() }
            Text(
                text = "${selectedFiles.size} items • ${formatTotalBytes(totalBytes)}",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Center Radar Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            SendModeRadarView(
                peers = peers,
                onPeerClick = onPeerClick
            )
        }
    }
}

/**
 * Animated Cyberpunk Radar Scanner View
 */
@Composable
private fun SendModeRadarView(
    peers: List<DiscoveredPeer>,
    onPeerClick: (DiscoveredPeer) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarTransition")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarSweep"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RadarPulse"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Radar Canvas Rings and Sweep
        Canvas(modifier = Modifier.size(280.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.width / 2

            // Concentric Glowing Rings
            for (step in 1..4) {
                val r = (maxRadius / 4) * step
                drawCircle(
                    color = ElectricCyan.copy(alpha = (pulseAlpha * (1f - (step * 0.15f))).coerceIn(0.05f, 0.5f)),
                    radius = r,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Crosshair Axes
            drawLine(
                color = ElectricCyan.copy(alpha = 0.2f),
                start = Offset(center.x, 0f),
                end = Offset(center.x, size.height),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = ElectricCyan.copy(alpha = 0.2f),
                start = Offset(0f, center.y),
                end = Offset(size.width, center.y),
                strokeWidth = 1.dp.toPx()
            )

            // Rotating Sweep Line
            val rad = Math.toRadians(rotation.toDouble())
            val sweepX = center.x + (maxRadius * Math.cos(rad)).toFloat()
            val sweepY = center.y + (maxRadius * Math.sin(rad)).toFloat()
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(ElectricCyan, Color.Transparent),
                    start = center,
                    end = Offset(sweepX, sweepY)
                ),
                start = center,
                end = Offset(sweepX, sweepY),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Center Device Node
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color(0xFF140D2B))
                .border(1.5.dp, ElectricCyan.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Devices,
                contentDescription = "My Device",
                tint = ElectricCyan,
                modifier = Modifier.size(24.dp)
            )
        }

        // Discovered Peer Galaxy Nodes
        if (peers.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(peers) { peer ->
                        PeerGalaxyCard(peer = peer, onClick = { onPeerClick(peer) })
                    }
                }
            }
        } else {
            // Empty Scan Text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = "Searching for nearby devices...",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Text(
                    text = "Ensure Quick Share or Google Files is active on peer",
                    fontFamily = OutfitFontFamily,
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }
    }
}

/**
 * Peer Card Orb
 */
@Composable
private fun PeerGalaxyCard(
    peer: DiscoveredPeer,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(135.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        CardGradientStart.copy(alpha = 0.9f),
                        CardGradientEnd.copy(alpha = 0.95f)
                    )
                )
            )
            .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(ElectricCyan.copy(alpha = 0.15f))
                    .border(1.dp, ElectricCyan.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (peer.deviceType) {
                        DeviceType.TABLET -> Icons.Default.Tablet
                        DeviceType.COMPUTER -> Icons.Default.Computer
                        else -> Icons.Default.PhoneAndroid
                    },
                    contentDescription = peer.deviceName,
                    tint = ElectricCyan,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = peer.deviceName,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Tap to Send",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = ChartAvailableEmerald,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Receiver Mode Center Beacon View
 */
@Composable
private fun ReceiveModeBeaconView() {
    val infiniteTransition = rememberInfiniteTransition(label = "ReceiveTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ReceivePulseScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Glowing Beacon
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer Aura
            Box(
                modifier = Modifier
                    .size((140 * pulseScale).dp)
                    .clip(CircleShape)
                    .background(ChartAvailableEmerald.copy(alpha = 0.12f))
                    .border(1.dp, ChartAvailableEmerald.copy(alpha = 0.3f), CircleShape)
            )

            // Inner Core
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(ChartAvailableEmerald.copy(alpha = 0.4f), Color(0xFF0F1E16))
                        )
                    )
                    .border(1.5.dp, ChartAvailableEmerald, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Receive Ready",
                    tint = ChartAvailableEmerald,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = Build.MODEL ?: "This Device",
            fontFamily = OrbitronFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Visible to nearby Google Files & Quick Share devices",
            fontFamily = OutfitFontFamily,
            fontSize = 13.sp,
            color = ChartAvailableEmerald,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Keep Wi-Fi or Local Hotspot enabled for maximum speed",
            fontFamily = OutfitFontFamily,
            fontSize = 11.sp,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Target Storage Directory Information Card (Receive Mode)
 */
@Composable
private fun ReceiveDirectoryInfoCard(downloadDir: File) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0E1A14))
            .border(1.dp, ChartAvailableEmerald.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(ChartAvailableEmerald.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = "Save Folder",
                tint = ChartAvailableEmerald,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "SAVING RECEIVED FILES TO",
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = ChartAvailableEmerald
            )
            Text(
                text = downloadDir.absolutePath,
                fontFamily = OutfitFontFamily,
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatTotalBytes(bytes: Long): String {
    return when {
        bytes >= 1024L * 1024L * 1024L -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
