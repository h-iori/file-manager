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
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ioristudios.filemanager.haptics.HapticType
import com.ioristudios.filemanager.haptics.LocalAppHaptics
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
import com.ioristudios.filemanager.theme.*
import com.ioristudios.filemanager.theme.TextMuted
import com.ioristudios.filemanager.theme.TextPrimary
import com.ioristudios.filemanager.theme.TextSecondary
import com.ioristudios.filemanager.ui.components.GlassIconButton
import com.ioristudios.filemanager.ui.components.elasticBounce
import com.ioristudios.filemanager.ui.components.staggeredEntrance
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
            .background(FilesTheme.colors.bg)
    ) {
        // Dedicated Settings Header with Back Arrow Button
        SettingsHeader(
            onBackClick = onBackClick
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

            SettingsCard(modifier = Modifier.staggeredEntrance(index = 0)) {
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
                                color = FilesTheme.colors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = when (state.themeMode) {
                                    ThemeMode.LIGHT -> "Luminous Crystal Light"
                                    ThemeMode.DARK -> "Electric Obsidian Nocturne"
                                    ThemeMode.AUTO -> "Follow System Default"
                                },
                                fontFamily = OutfitFontFamily,
                                color = FilesTheme.colors.textSecondary,
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

                HorizontalDivider(color = FilesTheme.colors.cardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

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

                HorizontalDivider(color = FilesTheme.colors.cardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

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

            SettingsCard(modifier = Modifier.staggeredEntrance(index = 1)) {
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
                                    if (FilesTheme.isDark) {
                                        Brush.linearGradient(
                                            listOf(Color(0xFF8B5CF6).copy(alpha = 0.25f), Color(0xFF3B0764).copy(alpha = 0.45f))
                                        )
                                    } else {
                                        Brush.linearGradient(
                                            listOf(Color(0xFFEDE9FE), Color(0xFFDDD6FE))
                                        )
                                    }
                                )
                                .border(1.dp, FilesTheme.colors.settingCardBorder, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Safe Folder",
                                tint = FilesTheme.colors.primaryAccent,
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
                                    color = FilesTheme.colors.textPrimary,
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
                                        color = FilesTheme.colors.primaryAccent,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "PIN protected · Biometrics enabled",
                                fontFamily = OutfitFontFamily,
                                color = FilesTheme.colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = FilesTheme.colors.textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Section 3: Haptics & Tactile Physics
            SettingsSectionHeader(
                title = "Haptics & Tactile Physics",
                tag = "TACTILE ENGINE"
            )

            val haptics = LocalAppHaptics.current
            var hapticsActive by remember { mutableStateOf(haptics.isEnabled) }

            SettingsCard(modifier = Modifier.staggeredEntrance(index = 2)) {
                // Master Haptic Feedback Switch
                SettingsToggleRow(
                    icon = Icons.Default.Vibration,
                    title = "Haptic Feedback",
                    subtitle = "Professional-grade tactile responses on click & gesture",
                    checked = hapticsActive,
                    onCheckedChange = { checked ->
                        haptics.isEnabled = checked
                        hapticsActive = checked
                        if (checked) {
                            haptics.trigger(HapticType.SWITCH_TOGGLE)
                        }
                        showMessage(if (checked) "Haptic feedback enabled" else "Haptic feedback muted")
                    }
                )

                HorizontalDivider(color = FilesTheme.colors.cardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

                // Interactive Tactile Playground
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = FilesTheme.colors.primaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Interactive Tactile Playground",
                            fontFamily = OutfitFontFamily,
                            color = FilesTheme.colors.textPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "Tap buttons below to feel calibrated physical waveforms on your device's motor.",
                        fontFamily = OutfitFontFamily,
                        color = FilesTheme.colors.textSecondary,
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HapticPlaygroundChip(
                            label = "Tab Tick",
                            type = HapticType.NAV_TICK,
                            modifier = Modifier.weight(1f)
                        )
                        HapticPlaygroundChip(
                            label = "Button Pop",
                            type = HapticType.CLICK,
                            modifier = Modifier.weight(1f)
                        )
                        HapticPlaygroundChip(
                            label = "Switch Snap",
                            type = HapticType.SWITCH_TOGGLE,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HapticPlaygroundChip(
                            label = "Folder Open",
                            type = HapticType.FOLDER_OPEN,
                            modifier = Modifier.weight(1f)
                        )
                        HapticPlaygroundChip(
                            label = "Storage Clean",
                            type = HapticType.HERO_CLEAN_PULSE,
                            modifier = Modifier.weight(1f)
                        )
                        HapticPlaygroundChip(
                            label = "Delete Thud",
                            type = HapticType.DESTRUCTIVE,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Section 4: About Files
            SettingsSectionHeader(
                title = "About Files",
                tag = "IORI STUDIOS"
            )

            SettingsCard(modifier = Modifier.staggeredEntrance(index = 3)) {
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
                                    listOf(FilesTheme.colors.headerIconBgStart, FilesTheme.colors.headerIconBgEnd)
                                )
                            )
                            .border(1.2.dp, FilesTheme.colors.headerIconBorder, RoundedCornerShape(18.dp)),
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
                        color = FilesTheme.colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "High-Performance Obsidian Edition",
                        fontFamily = OutfitFontFamily,
                        color = FilesTheme.colors.textSecondary,
                        fontSize = 12.5.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Version Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (FilesTheme.isDark) Color(0xFF1B1335) else Color(0xFFEDE4FF))
                            .border(1.dp, FilesTheme.colors.settingCardBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "v2.4.0 (Build 8291)",
                            fontFamily = OutfitFontFamily,
                            color = FilesTheme.colors.primaryAccent,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Spacer(modifier = Modifier.height(12.dp))

                    // Footer note
                    Text(
                        text = "CRAFTED WITH PRECISION BY IORI STUDIOS",
                        fontFamily = OrbitronFontFamily,
                        color = FilesTheme.colors.textMuted.copy(alpha = 0.65f),
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
 * The Search button is removed per user specification.
 */
@Composable
fun SettingsHeader(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
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
            color = FilesTheme.colors.textPrimary
        )
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
            color = FilesTheme.colors.primaryAccent,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = tag,
            fontFamily = OutfitFontFamily,
            color = FilesTheme.colors.textMuted,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(FilesTheme.colors.settingCardBg)
            .border(1.dp, FilesTheme.colors.settingCardBorder, RoundedCornerShape(22.dp))
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
            .background(FilesTheme.colors.settingIconBg)
            .border(1.dp, FilesTheme.colors.settingCardBorder, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = FilesTheme.colors.primaryAccent,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Custom animated 3-way Segmented Control matching Stitch Design with sliding glass pill
 */
@Composable
fun ThemeSegmentedControl(
    selectedMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit
) {
    val motion = LocalMotionSensitivity.current
    val modes = remember { listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.AUTO) }
    val selectedIndex = modes.indexOf(selectedMode).coerceAtLeast(0)

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FilesTheme.colors.settingSegmentedBg)
            .border(0.8.dp, FilesTheme.colors.settingCardBorder, RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        val segmentWidth = maxWidth / 3
        val pillOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = if (motion.isReducedMotion) tween(80) else MotionTokens.SpringPillSlide,
            label = "ThemePillOffset"
        )

        // Sliding Active Gradient Pill
        Box(
            modifier = Modifier
                .offset(x = pillOffset)
                .width(segmentWidth)
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (FilesTheme.isDark) {
                        Brush.horizontalGradient(listOf(PurpleContainer, Color(0xFF9333EA)))
                    } else {
                        Brush.horizontalGradient(listOf(AccentPrimaryLight, AccentAzureLight))
                    }
                )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            ThemeSegmentItem(
                title = "Light",
                icon = Icons.Default.LightMode,
                isSelected = selectedMode == ThemeMode.LIGHT,
                modifier = Modifier.width(segmentWidth),
                onClick = { onModeSelected(ThemeMode.LIGHT) }
            )
            ThemeSegmentItem(
                title = "Dark",
                icon = Icons.Default.DarkMode,
                isSelected = selectedMode == ThemeMode.DARK,
                modifier = Modifier.width(segmentWidth),
                onClick = { onModeSelected(ThemeMode.DARK) }
            )
            ThemeSegmentItem(
                title = "Auto",
                icon = Icons.Default.BrightnessAuto,
                isSelected = selectedMode == ThemeMode.AUTO,
                modifier = Modifier.width(segmentWidth),
                onClick = { onModeSelected(ThemeMode.AUTO) }
            )
        }
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
    val tint by animateColorAsState(
        targetValue = if (isSelected) Color.White else FilesTheme.colors.textSecondary,
        animationSpec = MotionTokens.resolveTween(durationMillis = MotionTokens.DurationFast),
        label = "ThemeSegmentTint"
    )

    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .elasticBounce(
                targetScale = 0.94f,
                hapticType = HapticType.SEGMENT_SLIDE,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tint,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = title,
                fontFamily = OutfitFontFamily,
                color = tint,
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
                    color = FilesTheme.colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    fontFamily = OutfitFontFamily,
                    color = FilesTheme.colors.textSecondary,
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
 * Custom Animated Toggle Switch matching Stitch Design with physical magnetic latch haptics
 */
@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val motion = LocalMotionSensitivity.current

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 0.dp,
        animationSpec = if (motion.isReducedMotion) tween(80) else androidx.compose.animation.core.spring(
            dampingRatio = 0.68f,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "thumbOffset"
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) (if (FilesTheme.isDark) PurpleContainer else AccentPrimaryLight) else FilesTheme.colors.settingSwitchTrackOff,
        animationSpec = MotionTokens.resolveTween(durationMillis = MotionTokens.DurationFast),
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
                if (checked) (if (FilesTheme.isDark) PurpleContainer.copy(alpha = 0.5f) else AccentPrimaryLight.copy(alpha = 0.5f)) else FilesTheme.colors.settingCardBorder,
                CircleShape
            )
            .elasticBounce(
                targetScale = 0.90f,
                hapticType = HapticType.SWITCH_TOGGLE,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(horizontal = 2.dp, vertical = 3.dp),
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

/**
 * Interactive playground chip for testing tactile haptic profiles in real-time.
 */
@Composable
fun HapticPlaygroundChip(
    label: String,
    type: HapticType,
    modifier: Modifier = Modifier
) {
    val haptics = LocalAppHaptics.current
    Box(
        modifier = modifier
            .elasticBounce(targetScale = 0.90f, hapticType = type, onClick = {})
            .clip(RoundedCornerShape(10.dp))
            .background(if (FilesTheme.isDark) Color(0x18FFFFFF) else Color(0xFFF1F5F9))
            .border(1.dp, FilesTheme.colors.settingCardBorder, RoundedCornerShape(10.dp))
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = OutfitFontFamily,
            color = FilesTheme.colors.textPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
