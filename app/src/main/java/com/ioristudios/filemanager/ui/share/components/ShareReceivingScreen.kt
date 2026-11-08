package com.ioristudios.filemanager.ui.share.components

import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.filemanager.haptics.HapticType
import com.ioristudios.filemanager.haptics.LocalAppHaptics
import com.ioristudios.filemanager.theme.CardBorderPurple
import com.ioristudios.filemanager.theme.ChartAvailableEmerald
import com.ioristudios.filemanager.theme.ElectricTeal
import com.ioristudios.filemanager.theme.OrbitronFontFamily
import com.ioristudios.filemanager.theme.OutfitFontFamily
import com.ioristudios.filemanager.theme.TextMuted
import com.ioristudios.filemanager.theme.TextPrimary
import com.ioristudios.filemanager.theme.TextSecondary
import java.io.File

/**
 * Screen 4: Dedicated Receiving Screen (Receive Flow).
 * Activated strictly after user clicks RECEIVE on the landing screen.
 * ServerSocket is bound and mDNS advertising is active ONLY on this screen.
 * Includes Direct Hotspot (Off-Grid) fallback for non-Wi-Fi environments.
 */
@Composable
fun ShareReceivingScreen(
    downloadDir: File,
    isHotspotActive: Boolean,
    hotspotSsid: String?,
    hotspotPassword: String?,
    onBack: () -> Unit,
    onToggleHotspot: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalAppHaptics.current

    val infiniteTransition = rememberInfiniteTransition(label = "ReceiveBeaconTransition")
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
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(14.dp))

            // --- Top Bar ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F1E16))
                    .border(1.dp, ChartAvailableEmerald.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable {
                        haptics.trigger(HapticType.NAV_TICK)
                        onBack()
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ChartAvailableEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Stop Receiving",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = ChartAvailableEmerald
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ChartAvailableEmerald.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "DISCOVERABLE",
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = ChartAvailableEmerald
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // --- Center Pulsing Beacon ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer Aura
                Box(
                    modifier = Modifier
                        .size((160 * pulseScale).dp)
                        .clip(CircleShape)
                        .background(ChartAvailableEmerald.copy(alpha = 0.12f))
                        .border(1.5.dp, ChartAvailableEmerald.copy(alpha = 0.35f), CircleShape)
                )

                // Middle Ring
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(ChartAvailableEmerald.copy(alpha = 0.18f))
                        .border(1.dp, ChartAvailableEmerald.copy(alpha = 0.5f), CircleShape)
                )

                // Core Orb
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(ChartAvailableEmerald.copy(alpha = 0.85f), Color(0xFF091F14))
                            )
                        )
                        .border(2.dp, ChartAvailableEmerald, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Receive Ready",
                        tint = Color(0xFF07040D),
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Device Name & Status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = Build.MODEL ?: "This Device",
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Visible to nearby Google Files & Quick Share senders",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = ChartAvailableEmerald,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Waiting for sender to select your device...",
                    fontFamily = OutfitFontFamily,
                    fontSize = 12.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }

        // --- Bottom Off-Grid Hotspot & Directory Info Cards ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Direct Hotspot Card (for non-Wi-Fi environments)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isHotspotActive) Color(0xFF16251C) else Color(0xFF0F0B1E))
                    .border(
                        1.dp,
                        if (isHotspotActive) ChartAvailableEmerald.copy(alpha = 0.6f) else CardBorderPurple.copy(alpha = 0.35f),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        haptics.trigger(HapticType.SWITCH_TOGGLE)
                        onToggleHotspot()
                    }
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isHotspotActive) ChartAvailableEmerald.copy(alpha = 0.2f) else Color(0xFF1E1735)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiTethering,
                            contentDescription = "Hotspot",
                            tint = if (isHotspotActive) ChartAvailableEmerald else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHotspotActive) "Direct Hotspot Active (Off-Grid)" else "Not on same Wi-Fi? Use Direct Hotspot",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isHotspotActive) ChartAvailableEmerald else TextPrimary
                        )
                        Text(
                            text = if (isHotspotActive && hotspotSsid != null) {
                                "Connect sender to '$hotspotSsid' (Pass: ${hotspotPassword ?: "None"})"
                            } else {
                                "Tap to create direct P2P link without a Wi-Fi router"
                            },
                            fontFamily = OutfitFontFamily,
                            fontSize = 11.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Storage Directory Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0E1A14))
                    .border(1.dp, ChartAvailableEmerald.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = "Folder",
                    tint = ChartAvailableEmerald,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Saving to: ${downloadDir.absolutePath}",
                    fontFamily = OutfitFontFamily,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

