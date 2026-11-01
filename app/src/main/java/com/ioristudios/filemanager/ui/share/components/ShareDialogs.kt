package com.ioristudios.filemanager.ui.share.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ioristudios.filemanager.sharing.resilience.TransferLifecycleStatus
import com.ioristudios.filemanager.sharing.resilience.TransferProgressSnapshot
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
import com.ioristudios.filemanager.theme.SurfaceContainer
import com.ioristudios.filemanager.theme.TextMuted
import com.ioristudios.filemanager.theme.TextPrimary
import com.ioristudios.filemanager.theme.TextSecondary

/**
 * Luxury Glass PIN Verification Bottom Sheet / Dialog matching Google Files PIN security flow.
 */
@Composable
fun PinVerificationDialog(
    peerName: String,
    pinCode: String,
    onDecline: () -> Unit,
    onAccept: () -> Unit
) {
    Dialog(onDismissRequest = onDecline) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CardGradientStart.copy(alpha = 0.96f),
                            CardGradientEnd.copy(alpha = 0.98f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            ElectricCyan.copy(alpha = 0.7f),
                            ElectricPurple.copy(alpha = 0.4f),
                            CardBorderPurple.copy(alpha = 0.2f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header Shield Icon
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(ElectricCyan.copy(alpha = 0.15f))
                        .border(1.dp, ElectricCyan.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security PIN",
                        tint = ElectricCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "QUICK SHARE VERIFICATION",
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.2.sp,
                    color = ElectricCyan
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Incoming connection from",
                    fontFamily = OutfitFontFamily,
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                Text(
                    text = peerName,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(18.dp))

                // PIN Code Display Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0A0614))
                        .border(1.dp, ElectricPurple.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = pinCode.chunked(2).joinToString(" "),
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                        letterSpacing = 4.sp,
                        color = Color(0xFFE0E7FF)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Confirm that this 4-digit code matches the sender's screen before accepting.",
                    fontFamily = OutfitFontFamily,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Decline
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF26101B))
                            .border(1.dp, PdfRed.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .clickable { onDecline() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Decline",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = PdfRed
                        )
                    }

                    // Accept
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(ElectricCyan, ElectricPurple)
                                )
                            )
                            .clickable { onAccept() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Accept",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF07040D)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Live Transfer Progress Glass Modal with Pause, Resume, Cancel, and Retry controls.
 */
@Composable
fun TransferProgressModal(
    snapshot: TransferProgressSnapshot,
    peerName: String?,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = {
        if (snapshot.status == TransferLifecycleStatus.COMPLETED || snapshot.status == TransferLifecycleStatus.FAILED) {
            onDismiss()
        }
    }) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CardGradientStart.copy(alpha = 0.98f),
                            CardGradientEnd.copy(alpha = 0.99f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            when (snapshot.status) {
                                TransferLifecycleStatus.COMPLETED -> ChartAvailableEmerald.copy(alpha = 0.7f)
                                TransferLifecycleStatus.FAILED -> PdfRed.copy(alpha = 0.7f)
                                else -> ElectricCyan.copy(alpha = 0.5f)
                            },
                            CardBorderPurple.copy(alpha = 0.3f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Circular Progress Ring
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (snapshot.status) {
                        TransferLifecycleStatus.COMPLETED -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = ChartAvailableEmerald,
                                modifier = Modifier.size(54.dp)
                            )
                        }
                        TransferLifecycleStatus.FAILED -> {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Failed",
                                tint = PdfRed,
                                modifier = Modifier.size(54.dp)
                            )
                        }
                        else -> {
                            CircularProgressIndicator(
                                progress = { snapshot.percent },
                                modifier = Modifier.fillMaxSize(),
                                color = if (snapshot.status == TransferLifecycleStatus.PAUSED) Color(0xFFFBBF24) else ElectricCyan,
                                trackColor = SurfaceContainer,
                                strokeWidth = 7.dp
                            )
                            Text(
                                text = "${(snapshot.percent * 100).toInt()}%",
                                fontFamily = OrbitronFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title Status
                Text(
                    text = when (snapshot.status) {
                        TransferLifecycleStatus.TRANSFERRING -> "TRANSFERRING..."
                        TransferLifecycleStatus.PAUSED -> "TRANSFER PAUSED"
                        TransferLifecycleStatus.COMPLETED -> "TRANSFER COMPLETE"
                        TransferLifecycleStatus.FAILED -> "TRANSFER FAILED"
                        else -> "CONNECTING..."
                    },
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp,
                    color = when (snapshot.status) {
                        TransferLifecycleStatus.COMPLETED -> ChartAvailableEmerald
                        TransferLifecycleStatus.FAILED -> PdfRed
                        TransferLifecycleStatus.PAUSED -> Color(0xFFFBBF24)
                        else -> ElectricCyan
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // File Name
                Text(
                    text = snapshot.currentFileName.ifEmpty { peerName ?: "Wireless P2P Transfer" },
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Speed and ETA
                if (snapshot.status == TransferLifecycleStatus.TRANSFERRING) {
                    Text(
                        text = "${snapshot.speedFormatted} • ${snapshot.etaFormatted} (${snapshot.fileIndex}/${snapshot.totalFiles})",
                        fontFamily = OutfitFontFamily,
                        fontSize = 13.sp,
                        color = ElectricPurple
                    )
                } else if (snapshot.status == TransferLifecycleStatus.FAILED && snapshot.errorMessage != null) {
                    Text(
                        text = snapshot.errorMessage,
                        fontFamily = OutfitFontFamily,
                        fontSize = 12.sp,
                        color = PdfRed,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Control Action Row (Pause/Resume, Cancel, Done)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (snapshot.status) {
                        TransferLifecycleStatus.TRANSFERRING -> {
                            // Pause Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF281F08))
                                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .clickable { onPause() },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Color(0xFFFBBF24), modifier = Modifier.size(18.dp))
                                    Text("Pause", fontFamily = OutfitFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFFFBBF24))
                                }
                            }

                            // Cancel Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF26101B))
                                    .border(1.dp, PdfRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .clickable { onCancel() },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = PdfRed, modifier = Modifier.size(18.dp))
                                    Text("Cancel", fontFamily = OutfitFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = PdfRed)
                                }
                            }
                        }

                        TransferLifecycleStatus.PAUSED -> {
                            // Resume Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.horizontalGradient(listOf(ElectricCyan, ElectricPurple)))
                                    .clickable { onResume() },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = Color(0xFF07040D), modifier = Modifier.size(20.dp))
                                    Text("Resume", fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF07040D))
                                }
                            }

                            // Cancel Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF26101B))
                                    .border(1.dp, PdfRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .clickable { onCancel() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Cancel", fontFamily = OutfitFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = PdfRed)
                            }
                        }

                        TransferLifecycleStatus.COMPLETED -> {
                            // Done Button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.horizontalGradient(listOf(ChartAvailableEmerald, ElectricTeal)))
                                    .clickable { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Done", fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF07040D))
                            }
                        }

                        TransferLifecycleStatus.FAILED -> {
                            // Dismiss Button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF20173D))
                                    .border(1.dp, CardBorderPurple, RoundedCornerShape(12.dp))
                                    .clickable { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Dismiss", fontFamily = OutfitFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                            }
                        }

                        else -> {
                            // Cancel during connection/handshake
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF26101B))
                                    .border(1.dp, PdfRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .clickable { onCancel() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Cancel", fontFamily = OutfitFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = PdfRed)
                            }
                        }
                    }
                }
            }
        }
    }
}

