package com.ioristudios.filemanager.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ioristudios.filemanager.R
import com.ioristudios.filemanager.data.ThemeMode
import com.ioristudios.filemanager.theme.BorderSubtle
import com.ioristudios.filemanager.theme.CardBorderPurple
import com.ioristudios.filemanager.theme.ElectricPurple
import com.ioristudios.filemanager.theme.OrbitronFontFamily
import com.ioristudios.filemanager.theme.OutfitFontFamily
import com.ioristudios.filemanager.theme.PurpleContainer
import com.ioristudios.filemanager.theme.SettingAccentLight
import com.ioristudios.filemanager.theme.SettingAccentLilac
import com.ioristudios.filemanager.theme.SettingBorderDeep
import com.ioristudios.filemanager.theme.SettingBorderSubtle
import com.ioristudios.filemanager.theme.SettingCardBg
import com.ioristudios.filemanager.theme.SettingIconBg
import com.ioristudios.filemanager.theme.SettingSegmentedBg
import com.ioristudios.filemanager.theme.SettingSwitchTrackOff
import com.ioristudios.filemanager.theme.TextMuted
import com.ioristudios.filemanager.theme.TextPrimary
import com.ioristudios.filemanager.theme.TextSecondary
import com.ioristudios.filemanager.ui.components.GlassIconButton
import com.ioristudios.filemanager.ui.main.MainScreenViewModel

@Composable
fun SettingsScreen(
    viewModel: MainScreenViewModel,
    showMessage: (String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.settingsUiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090611))
    ) {
        // Dedicated Settings Header with Back Arrow Button
        SettingsHeader(
            onBackClick = onBackClick,
            onSearchClick = { showMessage("Search settings coming soon") },
            onMenuClick = { showMessage("More options") }
        )

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Appearance & Theme
            SettingsSectionHeader(
                title = "Appearance & Theme",
                tag = "UI / GRAPHICS"
            )

            SettingsCard {
                // Theme Mode Segmented Switcher
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SettingsIconSquircle(icon = Icons.Default.DarkMode)
                        Column {
                            Text(
                                text = "Theme Mode",
                                fontFamily = OutfitFontFamily,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = when (state.themeMode) {
                                    ThemeMode.LIGHT -> "Frosted Lavender Light"
                                    ThemeMode.DARK -> "Electric Obsidian Nocturne"
                                    ThemeMode.AUTO -> "Follow System Default"
                                },
                                fontFamily = OutfitFontFamily,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // 3-Way Segmented Switcher
                    ThemeSegmentedControl(
                        selectedMode = state.themeMode,
                        onModeSelected = { mode ->
                            viewModel.setThemeMode(mode)
                            showMessage("Theme set to ${mode.name.lowercase().replaceFirstChar { it.uppercase() }}")
                        }
                    )
                }

                HorizontalDivider(color = SettingBorderSubtle.copy(alpha = 0.5f), thickness = 0.8.dp)

                // Show Hidden Files Toggle
                SettingsToggleRow(
                    icon = Icons.Default.Visibility,
                    title = "Show Hidden Files",
                    subtitle = "Expose dotfiles & system caches",
                    checked = state.showHiddenFiles,
                    onCheckedChange = { checked ->
                        viewModel.toggleShowHiddenFiles(checked)
                        showMessage(if (checked) "Hidden files enabled" else "Hidden files hidden")
                    }
                )

                HorizontalDivider(color = SettingBorderSubtle.copy(alpha = 0.5f), thickness = 0.8.dp)

                // Show File Extensions Toggle
                SettingsToggleRow(
                    icon = Icons.Default.Extension,
                    title = "Show File Extensions",
                    subtitle = "Display .pdf, .zip, .apk suffixes",
                    checked = state.showFileExtensions,
                    onCheckedChange = { checked ->
                        viewModel.toggleShowFileExtensions(checked)
                        showMessage(if (checked) "File extensions shown" else "File extensions hidden")
                    }
                )
            }

            // Section 2: Security & Storage
            SettingsSectionHeader(
                title = "Security & Storage",
                tag = "HARDWARE & DISK"
            )

            SettingsCard {
                // Recycle Bin / Trash Toggle
                SettingsToggleRow(
                    icon = Icons.Default.DeleteOutline,
                    title = "Recycle Bin / Trash",
                    subtitle = "Retain deleted items for 30 days",
                    checked = state.recycleBinEnabled,
                    onCheckedChange = { checked ->
                        viewModel.toggleRecycleBin(checked)
                        showMessage(if (checked) "Recycle bin enabled" else "Direct deletion active")
                    }
                )

                HorizontalDivider(color = SettingBorderSubtle.copy(alpha = 0.5f), thickness = 0.8.dp)

                // Safe Folder & Encryption Row (Coming Soon per user decision)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMessage("Safe Folder (AES-256) is coming soon in v2.5") }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF8B5CF6).copy(alpha = 0.25f), Color(0xFF3B0764).copy(alpha = 0.45f))
                                    )
                                )
                                .border(1.dp, SettingBorderDeep, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Safe Folder",
                                tint = SettingAccentLilac,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Safe Folder & Encryption",
                                    fontFamily = OutfitFontFamily,
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(PurpleContainer.copy(alpha = 0.2f))
                                        .border(0.8.dp, PurpleContainer.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "AES-256",
                                        fontFamily = OutfitFontFamily,
                                        color = SettingAccentLight,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "PIN protected · Biometrics enabled",
                                fontFamily = OutfitFontFamily,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Section 3: About Files
            SettingsSectionHeader(
                title = "About Files",
                tag = "IORI STUDIOS"
            )

            SettingsCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App Squircle Avatar with glowing radiant aura
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .shadow(20.dp, RoundedCornerShape(18.dp), ambientColor = PurpleContainer, spotColor = PurpleContainer)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF351C66), Color(0xFF130A24))
                                )
                            )
                            .border(1.2.dp, CardBorderPurple, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_brand_logo),
                            contentDescription = "Files Logo",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Files by IORI STUDIOS",
                        fontFamily = OrbitronFontFamily,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "High-Performance Obsidian Edition",
                        fontFamily = OutfitFontFamily,
                        color = TextSecondary,
                        fontSize = 12.5.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Version Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1B1335))
                            .border(1.dp, CardBorderPurple.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "v2.4.0 (Build 8291)",
                            fontFamily = OutfitFontFamily,
                            color = SettingAccentLight,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = SettingBorderSubtle.copy(alpha = 0.5f), thickness = 0.8.dp)

                    // Check for updates action row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMessage("You are using the latest version (v2.4.0)") }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Check updates",
                                tint = ElectricPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Check for updates",
                                fontFamily = OutfitFontFamily,
                                color = TextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Check",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    HorizontalDivider(color = SettingBorderSubtle.copy(alpha = 0.5f), thickness = 0.8.dp)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Footer note
                    Text(
                        text = "CRAFTED WITH PRECISION BY IORI STUDIOS",
                        fontFamily = OrbitronFontFamily,
                        color = TextMuted.copy(alpha = 0.65f),
                        fontSize = 9.sp,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Dedicated Settings Header with back arrow and Settings title in Outfit typography.
 */
@Composable
fun SettingsHeader(
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBackClick
            )

            Text(
                text = "Settings",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Color.White
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconButton(
                icon = Icons.Default.Search,
                contentDescription = "Search",
                onClick = onSearchClick
            )
            GlassIconButton(
                icon = Icons.Default.MoreVert,
                contentDescription = "More",
                onClick = onMenuClick
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(
    title: String,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            fontFamily = OutfitFontFamily,
            color = SettingAccentLight,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = tag,
            fontFamily = OutfitFontFamily,
            color = TextMuted,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
fun SettingsCard(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(SettingCardBg)
            .border(1.dp, SettingBorderDeep, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            content()
        }
    }
}

@Composable
fun SettingsIconSquircle(
    icon: ImageVector
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SettingIconBg)
            .border(1.dp, SettingBorderDeep, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SettingAccentLilac,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Custom animated 3-way Segmented Control matching Stitch Design
 */
@Composable
fun ThemeSegmentedControl(
    selectedMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SettingSegmentedBg)
            .border(0.8.dp, SettingBorderDeep, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ThemeSegmentItem(
            title = "Light",
            icon = Icons.Default.LightMode,
            isSelected = selectedMode == ThemeMode.LIGHT,
            modifier = Modifier.weight(1f),
            onClick = { onModeSelected(ThemeMode.LIGHT) }
        )
        ThemeSegmentItem(
            title = "Dark",
            icon = Icons.Default.DarkMode,
            isSelected = selectedMode == ThemeMode.DARK,
            modifier = Modifier.weight(1f),
            onClick = { onModeSelected(ThemeMode.DARK) }
        )
        ThemeSegmentItem(
            title = "Auto",
            icon = Icons.Default.BrightnessAuto,
            isSelected = selectedMode == ThemeMode.AUTO,
            modifier = Modifier.weight(1f),
            onClick = { onModeSelected(ThemeMode.AUTO) }
        )
    }
}

@Composable
fun ThemeSegmentItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgModifier = if (isSelected) {
        Modifier.background(
            Brush.horizontalGradient(
                listOf(PurpleContainer, Color(0xFF9333EA))
            )
        )
    } else {
        Modifier.background(Color.Transparent)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(bgModifier)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) Color.White else TextMuted,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = title,
                fontFamily = OutfitFontFamily,
                color = if (isSelected) Color.White else TextMuted,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

/**
 * Settings Row with Toggle Switch matching Stitch Design
 */
@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsIconSquircle(icon = icon)
            Column {
                Text(
                    text = title,
                    fontFamily = OutfitFontFamily,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    fontFamily = OutfitFontFamily,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        GlassSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Custom Animated Toggle Switch matching Stitch Design
 */
@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = tween(durationMillis = 180),
        label = "thumbOffset"
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) PurpleContainer else SettingSwitchTrackOff,
        animationSpec = tween(durationMillis = 180),
        label = "trackColor"
    )

    Box(
        modifier = Modifier
            .width(46.dp)
            .height(26.dp)
            .clip(CircleShape)
            .background(trackColor)
            .border(
                1.dp,
                if (checked) PurpleContainer.copy(alpha = 0.5f) else SettingBorderDeep,
                CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) }
            .padding(vertical = 3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(20.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
