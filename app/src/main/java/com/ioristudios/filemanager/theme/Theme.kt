package com.ioristudios.filemanager.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

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

@Composable
fun FilesTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
