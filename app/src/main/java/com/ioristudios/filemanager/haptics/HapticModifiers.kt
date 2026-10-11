package com.ioristudios.filemanager.haptics

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * CompositionLocal providing access to the app-wide HapticFeedbackManager.
 */
val LocalAppHaptics = staticCompositionLocalOf<HapticFeedbackManager> {
    error("LocalAppHaptics has not been initialized.")
}

/**
 * Custom Compose clickable modifier that instantaneously triggers professional-grade
 * tactile feedback before invoking onClick.
 */
fun Modifier.hapticClickable(
    type: HapticType = HapticType.CLICK,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val hapticManager = LocalAppHaptics.current

    this.clickable(
        enabled = enabled
    ) {
        hapticManager.trigger(type)
        onClick()
    }
}
