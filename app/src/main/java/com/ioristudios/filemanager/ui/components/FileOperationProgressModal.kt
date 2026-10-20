package com.ioristudios.filemanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.ioristudios.filemanager.data.FileOpProgress
import com.ioristudios.filemanager.data.FileOpStatus
import com.ioristudios.filemanager.data.FileOpType
import com.ioristudios.filemanager.haptics.HapticType
import com.ioristudios.filemanager.haptics.LocalAppHaptics
import com.ioristudios.filemanager.theme.ElectricCyan
import com.ioristudios.filemanager.theme.ElectricLavender
import com.ioristudios.filemanager.theme.ElectricPurple
import com.ioristudios.filemanager.theme.FilesTheme
import com.ioristudios.filemanager.theme.MotionTokens
import com.ioristudios.filemanager.theme.OutfitFontFamily

/**
 * State-of-the-art Glassmorphic File Operation Progress Modal displaying real-time KB/MB transfer,
 * live throughput speed, percentage, ETA, and interactive Pause, Resume, Cancel, and Run-in-Background controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileOperationProgressModal(
    progress: FileOpProgress,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onDismissToBackground: () -> Unit
) {
    if (!progress.isActive && progress.status != FileOpStatus.COMPLETED && progress.status != FileOpStatus.FAILED) {
        return
    }

    val haptics = LocalAppHaptics.current
    var showCancelConfirm by remember { mutableStateOf(false) }

    val icon: ImageVector = when (progress.type) {
        FileOpType.COPY -> Icons.Default.ContentCopy
        FileOpType.MOVE -> Icons.Default.ContentCut
        FileOpType.DELETE -> Icons.Default.Delete
        FileOpType.COMPRESS -> Icons.Default.Archive
        FileOpType.EXTRACT -> Icons.Default.Unarchive
    }

    val accentColor = when (progress.status) {
        FileOpStatus.PAUSED -> Color(0xFFF59E0B) // Amber
        FileOpStatus.COMPLETED -> Color(0xFF10B981) // Emerald
        FileOpStatus.FAILED -> Color(0xFFEF4444) // Red
        else -> ElectricPurple
    }

    BasicAlertDialog(
        onDismissRequest = {
            // Dismissing the dialog keeps the operation running in the background!
            onDismissToBackground()
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(
                    if (FilesTheme.isDark) {
                        Brush.verticalGradient(
                            listOf(Color(0xFF1C1335), Color(0xFF100A22))
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(Color(0xFFFAF5FF), Color(0xFFF3E8FF))
                        )
                    }
                )
                .border(
                    1.2.dp,
                    accentColor.copy(alpha = 0.45f),
                    RoundedCornerShape(26.dp)
                )
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header: Icon + Title + Status Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(accentColor.copy(alpha = 0.20f))
                                .border(1.dp, accentColor.copy(alpha = 0.40f), RoundedCornerShape(13.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = progress.title.ifEmpty { "File Operation" },
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = FilesTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (progress.currentFileName.isNotEmpty()) {
                                Text(
                                    text = progress.currentFileName,
                                    fontFamily = OutfitFontFamily,
                                    fontSize = 12.sp,
                                    color = FilesTheme.colors.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Status Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(accentColor.copy(alpha = 0.18f))
                            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = when (progress.status) {
                                FileOpStatus.PAUSED -> "PAUSED"
                                FileOpStatus.COMPLETED -> "DONE"
                                FileOpStatus.FAILED -> "FAILED"
                                FileOpStatus.CANCELLED -> "CANCELLED"
                                else -> "RUNNING"
                            },
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = accentColor
                        )
                    }
                }

                // Main Metric: Percentage + Speed + Processed Bytes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val percent = (progress.progressFraction * 100f).toInt().coerceIn(0, 100)
                    Text(
                        text = if (progress.isIndeterminate) "Processing..." else "$percent%",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                        color = FilesTheme.colors.textPrimary
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = progress.formattedSpeed,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = FilesTheme.colors.primaryAccent
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${progress.formattedProcessedBytes} of ${progress.formattedTotalBytes}",
                            fontFamily = OutfitFontFamily,
                            fontSize = 11.5.sp,
                            color = FilesTheme.colors.textSecondary
                        )
                    }
                }

                // Liquid Shimmer Progress Bar
                LiquidShimmerProgressBar(
                    progress = progress.progressFraction,
                    modifier = Modifier.fillMaxWidth(),
                    height = 9.dp,
                    barColors = listOf(accentColor, Color(0xFFC084FC))
                )

                // Sub-info: File count & ETA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (progress.totalFiles > 0) {
                        Text(
                            text = "Item ${progress.currentFileIndex} of ${progress.totalFiles}",
                            fontFamily = OutfitFontFamily,
                            fontSize = 11.5.sp,
                            color = FilesTheme.colors.textMuted
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (progress.formattedEta.isNotEmpty() && progress.status == FileOpStatus.RUNNING) {
                        Text(
                            text = progress.formattedEta,
                            fontFamily = OutfitFontFamily,
                            fontSize = 11.5.sp,
                            color = FilesTheme.colors.textMuted
                        )
                    }
                }

                // Action Controls Row
                if (progress.status == FileOpStatus.COMPLETED) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF10B981))
                            .clickable {
                                haptics.trigger(HapticType.SUCCESS_CONFIRM)
                                onDismissToBackground()
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("Done", color = Color.White, fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                } else if (progress.status == FileOpStatus.FAILED) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEF4444))
                            .clickable {
                                haptics.trigger(HapticType.ERROR_REJECT)
                                onDismissToBackground()
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Dismiss", color = Color.White, fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pause / Resume Toggle Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (progress.status == FileOpStatus.PAUSED) Color(0xFFF59E0B) else Color(0x25FFFFFF)
                                )
                                .border(
                                    1.dp,
                                    if (progress.status == FileOpStatus.PAUSED) Color(0xFFF59E0B) else Color(0x35FFFFFF),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    haptics.trigger(HapticType.CLICK)
                                    if (progress.status == FileOpStatus.PAUSED) onResume() else onPause()
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (progress.status == FileOpStatus.PAUSED) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = if (progress.status == FileOpStatus.PAUSED) "Resume" else "Pause",
                                    tint = if (progress.status == FileOpStatus.PAUSED) Color.Black else FilesTheme.colors.textPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (progress.status == FileOpStatus.PAUSED) "Resume" else "Pause",
                                    color = if (progress.status == FileOpStatus.PAUSED) Color.Black else FilesTheme.colors.textPrimary,
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Cancel Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x22EF4444))
                                .border(1.dp, Color(0x50EF4444), RoundedCornerShape(12.dp))
                            .clickable {
                                haptics.trigger(HapticType.DESTRUCTIVE)
                                showCancelConfirm = true
                            }
                            .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Cancel",
                                    color = Color(0xFFEF4444),
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Background Button (Dismisses UI modal, keeps service running)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.horizontalGradient(listOf(Color(0xFF9333EA), Color(0xFFC084FC)))
                                )
                                .clickable {
                                    haptics.trigger(HapticType.CLICK)
                                    onDismissToBackground()
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Background",
                                color = Color.White,
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Cancel Safeguard Confirmation Dialog
    if (showCancelConfirm) {
        BasicAlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (FilesTheme.isDark) Color(0xFF221235) else Color(0xFFFAF5FF))
                    .border(1.dp, Color(0x60EF4444), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Cancel Operation?",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = FilesTheme.colors.textPrimary
                    )
                    Text(
                        text = "Are you sure you want to stop this operation? Any partially written files will be cleaned up safely.",
                        fontFamily = OutfitFontFamily,
                        fontSize = 13.sp,
                        color = FilesTheme.colors.textSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Continue Working",
                            color = FilesTheme.colors.textMuted,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clickable { showCancelConfirm = false }
                                .padding(8.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEF4444))
                                .clickable {
                                    showCancelConfirm = false
                                    onCancel()
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Yes, Cancel",
                                color = Color.White,
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Compact operation status that remains visible after the full dialog is sent to background. */
@Composable
fun FloatingProgressPill(
    progress: FileOpProgress,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (progress.type) {
        FileOpType.COPY -> Icons.Default.ContentCopy
        FileOpType.MOVE -> Icons.Default.ContentCut
        FileOpType.DELETE -> Icons.Default.Delete
        FileOpType.COMPRESS -> Icons.Default.Archive
        FileOpType.EXTRACT -> Icons.Default.Unarchive
    }
    val accent = if (progress.status == FileOpStatus.PAUSED) Color(0xFFF59E0B) else ElectricPurple
    val percent = (progress.progressFraction * 100).toInt().coerceIn(0, 100)

    Row(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .clip(RoundedCornerShape(18.dp))
            .background(if (FilesTheme.isDark) Color(0xF018102B) else Color(0xFFF9F5FF))
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = progress.title.ifBlank { "File operation" },
                    color = FilesTheme.colors.textPrimary,
                    fontFamily = OutfitFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (progress.isIndeterminate) "Working" else "$percent%",
                    color = accent,
                    fontFamily = OutfitFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            LiquidShimmerProgressBar(
                progress = progress.progressFraction,
                modifier = Modifier.fillMaxWidth(),
                height = 4.dp,
                barColors = listOf(accent, Color(0xFFC084FC))
            )
        }
    }
}
