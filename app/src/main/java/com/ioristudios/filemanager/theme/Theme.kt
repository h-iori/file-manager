package com.ioristudios.filemanager.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.ioristudios.filemanager.data.ThemeMode

@Immutable
data class FilesColors(
    val isDark: Boolean,
    val bg: Color,
    val bgSecondary: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val cardBgStart: Color,
    val cardBgEnd: Color,
    val cardBorder: Color,
    val borderSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val bottomNavBg: Color,
    val bottomNavBorder: Color,
    val bottomNavActiveBg: Color,
    val bottomNavActiveIcon: Color,
    val bottomNavInactiveIcon: Color,
    val headerIconBgStart: Color,
    val headerIconBgEnd: Color,
    val headerIconBorder: Color,
    val glassTrackColor: Color,
    val settingCardBg: Color,
    val settingCardBorder: Color,
    val settingIconBg: Color,
    val settingSegmentedBg: Color,
    val settingSwitchTrackOff: Color,
    val dialogBg: Color,
    val dialogBorder: Color,
    val searchBarBg: Color,
    val searchBarBorder: Color,
    val chipBg: Color,
    val chipBorder: Color,
    val bannerBgStart: Color,
    val bannerBgEnd: Color
)

val DarkFilesColors = FilesColors(
    isDark = true,
    bg = Color(0xFF090611),
    bgSecondary = Color(0xFF120D22),
    surface = Color(0xFF130E24),
    surfaceHigh = Color(0xFF1C1434),
    cardBgStart = Color(0xFF181130),
    cardBgEnd = Color(0xFF120C24),
    cardBorder = Color(0xFF4C2882),
    borderSubtle = Color(0xFF3B2563),
    textPrimary = Color(0xFFF8F6FF),
    textSecondary = Color(0xFFA79FC2),
    textMuted = Color(0xFF7E749C),
    primaryAccent = Color(0xFFC084FC),
    secondaryAccent = Color(0xFFA855F7),
    bottomNavBg = Color(0xEE140E26),
    bottomNavBorder = Color(0xFF4C2882),
    bottomNavActiveBg = Color(0x33A855F7),
    bottomNavActiveIcon = Color(0xFFC084FC),
    bottomNavInactiveIcon = Color(0xFFA79FC2),
    headerIconBgStart = Color(0xFF351C66),
    headerIconBgEnd = Color(0xFF130A24),
    headerIconBorder = Color(0x80C084FC),
    glassTrackColor = Color(0xFF1B1033),
    settingCardBg = Color(0xFF130E24),
    settingCardBorder = Color(0xFF2E2050),
    settingIconBg = Color(0xFF1D1438),
    settingSegmentedBg = Color(0xFF0D091A),
    settingSwitchTrackOff = Color(0xFF231B3E),
    dialogBg = Color(0xFF140E26),
    dialogBorder = Color(0xFF4C2882),
    searchBarBg = Color(0xFF160E2A),
    searchBarBorder = Color(0xFF3B2563),
    chipBg = Color(0xFF1D1438),
    chipBorder = Color(0xFF3B2563),
    bannerBgStart = Color(0xFF191136),
    bannerBgEnd = Color(0xFF120C24)
)

val LightFilesColors = FilesColors(
    isDark = false,
    bg = BgLight,
    bgSecondary = BgSecondaryLight,
    surface = SurfaceLight,
    surfaceHigh = SurfaceLightHigh,
    cardBgStart = Color(0xFFFFFFFF),
    cardBgEnd = Color(0xFFFFFFFF),
    cardBorder = CardBorderLight,
    borderSubtle = BorderSubtleLight,
    textPrimary = TextPrimaryLight,
    textSecondary = TextSecondaryLight,
    textMuted = TextMutedLight,
    primaryAccent = AccentPrimaryLight,
    secondaryAccent = AccentSecondaryLight,
    bottomNavBg = Color(0xF8FFFFFF),
    bottomNavBorder = CardBorderLight,
    bottomNavActiveBg = Color(0xFFEEF2FF),
    bottomNavActiveIcon = AccentPrimaryLight,
    bottomNavInactiveIcon = TextMutedLight,
    headerIconBgStart = AccentPrimaryLight,
    headerIconBgEnd = AccentAzureLight,
    headerIconBorder = Color(0xFF818CF8),
    glassTrackColor = Color(0xFFE2E8F0),
    settingCardBg = Color(0xFFFFFFFF),
    settingCardBorder = CardBorderLight,
    settingIconBg = Color(0xFFEEF2FF),
    settingSegmentedBg = Color(0xFFF1F5F9),
    settingSwitchTrackOff = Color(0xFFCBD5E1),
    dialogBg = Color(0xFFFFFFFF),
    dialogBorder = CardBorderLight,
    searchBarBg = Color(0xFFF1F5F9),
    searchBarBorder = CardBorderLight,
    chipBg = Color(0xFFEEF2FF),
    chipBorder = Color(0xFFC7D2FE),
    bannerBgStart = Color(0xFFEFF6FF),
    bannerBgEnd = Color(0xFFF5F3FF)
)

val LocalThemeIsDark = staticCompositionLocalOf { true }
val LocalFilesColors = staticCompositionLocalOf { DarkFilesColors }

object FilesTheme {
    val colors: FilesColors
        @Composable
        @ReadOnlyComposable
        get() = LocalFilesColors.current

    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalThemeIsDark.current
}

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFC084FC),
    onPrimary = Color(0xFF07040D),
    primaryContainer = Color(0xFFA855F7),
    onPrimaryContainer = Color(0xFFF8F6FF),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF07040D),
    tertiary = Color(0xFFD946EF),
    background = Color(0xFF090611),
    onBackground = Color(0xFFF8F6FF),
    surface = Color(0xFF090611),
    onSurface = Color(0xFFF8F6FF),
    surfaceVariant = Color(0xFF20173D),
    onSurfaceVariant = Color(0xFFA79FC2),
    outline = Color(0xFF3B2563),
    outlineVariant = Color(0xFF4C2882)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF9333EA),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9D5FF),
    onPrimaryContainer = Color(0xFF3B0764),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    tertiary = Color(0xFFD946EF),
    background = Color(0xFFF6F4FB),
    onBackground = Color(0xFF150D2A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF150D2A),
    surfaceVariant = Color(0xFFEDE8F8),
    onSurfaceVariant = Color(0xFF5E527B),
    outline = Color(0x33A855F7),
    outlineVariant = Color(0x40A855F7)
)

@Composable
fun FilesTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AUTO -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val filesColors = if (isDark) DarkFilesColors else LightFilesColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            activity?.window?.let { window ->
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    ProvideMotionSensitivity {
        CompositionLocalProvider(
            LocalThemeIsDark provides isDark,
            LocalFilesColors provides filesColors
        ) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = Typography,
                content = content
            )
        }
    }
}
