package com.ioristudios.filemanager.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.ioristudios.filemanager.data.ThemeMode

val LocalThemeIsDark = staticCompositionLocalOf { true }

private val DarkColorScheme = darkColorScheme(
    primary = ElectricPurple,
    onPrimary = SurfaceContainerLowest,
    primaryContainer = PurpleContainer,
    onPrimaryContainer = TextPrimary,
    secondary = ElectricCyan,
    onSecondary = SurfaceContainerLowest,
    tertiary = ElectricMagenta,
    background = BgDeepDark,
    onBackground = TextPrimary,
    surface = BgDeepDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceContainerHigh,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = CardBorderPurple
)

private val LightColorScheme = lightColorScheme(
    primary = PurpleContainer,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9D5FF),
    onPrimaryContainer = Color(0xFF3B0764),
    secondary = ElectricCyan,
    onSecondary = Color.White,
    tertiary = ElectricMagenta,
    background = BgLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceLightHigh,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderSubtleLight,
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

    CompositionLocalProvider(LocalThemeIsDark provides isDark) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
