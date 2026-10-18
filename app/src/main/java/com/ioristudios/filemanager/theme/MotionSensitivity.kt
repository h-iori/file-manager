package com.ioristudios.filemanager.theme

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Immutable
data class MotionSensitivityState(
    val isReducedMotion: Boolean,
    val durationMultiplier: Float
)

val LocalMotionSensitivity = compositionLocalOf {
    MotionSensitivityState(isReducedMotion = false, durationMultiplier = 1.0f)
}

/**
 * Automatically syncs motion sensitivity with Android OS animation scale and accessibility settings.
 * Responds reactively if user changes system animation scales.
 */
@Composable
fun ProvideMotionSensitivity(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(getSystemMotionState(context)) }

    DisposableEffect(context) {
        val resolver = context.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                state = getSystemMotionState(context)
            }
        }

        try {
            resolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
                false,
                observer
            )
        } catch (_: Exception) {
            // Safe fallback on restricted environments or tests
        }

        onDispose {
            try {
                resolver.unregisterContentObserver(observer)
            } catch (_: Exception) {}
        }
    }

    CompositionLocalProvider(LocalMotionSensitivity provides state) {
        content()
    }
}

private fun getSystemMotionState(context: Context): MotionSensitivityState {
    return try {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f
        )
        val isReduced = scale <= 0.05f
        MotionSensitivityState(
            isReducedMotion = isReduced,
            durationMultiplier = if (isReduced) 0.0f else scale.coerceIn(0.1f, 5.0f)
        )
    } catch (_: Exception) {
        MotionSensitivityState(isReducedMotion = false, durationMultiplier = 1.0f)
    }
}
