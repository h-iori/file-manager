package com.ioristudios.filemanager.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log

/**
 * Professional-grade haptic physics engine for Files by IORI STUDIOS.
 * Auto-detects device hardware capabilities:
 * - On Linear Resonant Actuator (LRA) flagships with API 30+ primitives, composes micro-tick primitives.
 * - On ERM rotor motors & devices with capabilitiesFlags = 0 (such as POCO F1, Xiaomi, etc.),
 *   deploys calibrated duration-modulated waveforms using DEFAULT_AMPLITUDE to overcome
 *   motor static inertia and deliver tactile sensations.
 */
class HapticFeedbackManager(private val context: Context) {

    private val tag = "HapticFeedbackManager"

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator ?: @Suppress("DEPRECATION") (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /**
     * Master app-level toggle for haptic feedback
     */
    var isEnabled: Boolean = true

    /**
     * Global tactile intensity scaler
     */
    var intensityScale: Float = 1.0f

    /**
     * Checks if OS-level system haptics are active in Android Settings
     */
    val isSystemHapticsEnabled: Boolean
        get() {
            return try {
                Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.HAPTIC_FEEDBACK_ENABLED,
                    1
                ) == 1
            } catch (_: Exception) {
                true
            }
        }

    val hasHapticHardware: Boolean
        get() = vibrator?.hasVibrator() == true

    /**
     * Checks whether the physical device driver supports modern composition primitives.
     * Flagship LRA devices (Pixel 6+, Galaxy S21+) return true.
     * Standard ERM devices (POCO F1, etc.) return false and seamlessly use calibrated waveforms.
     */
    val supportsPrimitives: Boolean by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vibrator != null) {
            try {
                vibrator.areAllPrimitivesSupported(
                    VibrationEffect.Composition.PRIMITIVE_CLICK,
                    VibrationEffect.Composition.PRIMITIVE_TICK
                )
            } catch (_: Exception) {
                false
            }
        } else false
    }

    private var lastTriggerTimeMs: Long = 0L
    private val debounceThresholdMs: Long = 15L

    /**
     * Triggers a semantic tactile feedback waveform.
     */
    fun trigger(type: HapticType, customScale: Float = 1.0f) {
        if (!isEnabled || vibrator == null || !vibrator.hasVibrator()) {
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastTriggerTimeMs < debounceThresholdMs) {
            return
        }
        lastTriggerTimeMs = now

        val effectiveScale = (intensityScale * customScale).coerceIn(0.1f, 1.5f)

        try {
            if (supportsPrimitives && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                triggerModernComposition(type, effectiveScale)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                triggerCalibratedWaveform(type)
            } else {
                triggerLegacyPreOreo(type)
            }
        } catch (e: Exception) {
            Log.w(tag, "Haptic trigger failed for $type: ${e.message}, executing fallback")
            try {
                @Suppress("DEPRECATION")
                vibrator.vibrate(32L)
            } catch (_: Exception) {}
        }
    }

    /**
     * For flagship Linear Resonant Actuator hardware (Pixel, Galaxy S-series)
     */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.R)
    private fun triggerModernComposition(type: HapticType, scale: Float) {
        val composition = VibrationEffect.startComposition()

        when (type) {
            HapticType.NAV_TICK -> {
                composition.addPrimitive(
                    VibrationEffect.Composition.PRIMITIVE_TICK,
                    (0.40f * scale).coerceIn(0.05f, 1.0f)
                )
            }
            HapticType.CLICK -> {
                composition.addPrimitive(
                    VibrationEffect.Composition.PRIMITIVE_CLICK,
                    (0.65f * scale).coerceIn(0.1f, 1.0f)
                )
            }
            HapticType.MENU_POP -> {
                composition.addPrimitive(
                    VibrationEffect.Composition.PRIMITIVE_CLICK,
                    (0.55f * scale).coerceIn(0.1f, 1.0f)
                )
            }
            HapticType.FOLDER_OPEN -> {
                composition.addPrimitive(
                    VibrationEffect.Composition.PRIMITIVE_CLICK,
                    (0.70f * scale).coerceIn(0.1f, 1.0f)
                )
                composition.addPrimitive(
                    VibrationEffect.Composition.PRIMITIVE_TICK,
                    (0.35f * scale).coerceIn(0.05f, 1.0f),
                    12
                )
            }
            HapticType.SWITCH_TOGGLE -> {
                if (vibrator?.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL) == true) {
                    composition.addPrimitive(
                        VibrationEffect.Composition.PRIMITIVE_QUICK_FALL,
                        (0.75f * scale).coerceIn(0.1f, 1.0f)
                    )
                } else {
                    composition.addPrimitive(
                        VibrationEffect.Composition.PRIMITIVE_CLICK,
                        (0.65f * scale).coerceIn(0.1f, 1.0f)
                    )
                }
            }
            HapticType.SEGMENT_SLIDE -> {
                composition.addPrimitive(
                    VibrationEffect.Composition.PRIMITIVE_TICK,
                    (0.35f * scale).coerceIn(0.05f, 1.0f)
                )
            }
            HapticType.SELECTION_NOTCH -> {
                composition.addPrimitive(
                    VibrationEffect.Composition.PRIMITIVE_TICK,
                    (0.45f * scale).coerceIn(0.05f, 1.0f)
                )
            }
            HapticType.LONG_PRESS -> {
                if (vibrator?.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_THUD) == true) {
                    composition.addPrimitive(
                        VibrationEffect.Composition.PRIMITIVE_THUD,
                        (0.90f * scale).coerceIn(0.1f, 1.0f)
                    )
                } else {
                    composition.addPrimitive(
                        VibrationEffect.Composition.PRIMITIVE_CLICK,
                        1.0f
                    )
                }
            }
            HapticType.DESTRUCTIVE -> {
                if (vibrator?.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_THUD) == true) {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 1.0f)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 0.85f, 40)
                } else {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.85f, 40)
                }
            }
            HapticType.HERO_CLEAN_PULSE -> {
                if (vibrator?.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE) == true) {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, (0.70f * scale).coerceIn(0.1f, 1.0f))
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, (0.85f * scale).coerceIn(0.1f, 1.0f), 15)
                } else {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, (0.80f * scale).coerceIn(0.1f, 1.0f))
                }
            }
            HapticType.SUCCESS_CONFIRM -> {
                composition.addPrimitive(
                    VibrationEffect.Composition.PRIMITIVE_CLICK,
                    (0.55f * scale).coerceIn(0.1f, 1.0f)
                )
                composition.addPrimitive(
                    VibrationEffect.Composition.PRIMITIVE_TICK,
                    (0.40f * scale).coerceIn(0.05f, 1.0f),
                    15
                )
            }
            HapticType.ERROR_REJECT -> {
                composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.80f)
                composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.80f, 30)
                composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.80f, 30)
            }
        }

        val effect = composition.compose()
        vibrator?.vibrate(effect)
    }

    /**
     * For all standard Android devices, ERM motors, and devices without primitive HAL support.
     * Uses hardware-calibrated pulse timings and DEFAULT_AMPLITUDE to ensure physical responsiveness.
     */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
    private fun triggerCalibratedWaveform(type: HapticType) {
        val effect = when (type) {
            HapticType.NAV_TICK, HapticType.SEGMENT_SLIDE -> {
                // Snappy 26ms micro-tick for navigation
                VibrationEffect.createOneShot(26L, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            HapticType.CLICK -> {
                // Crisp 36ms tactile click for buttons
                VibrationEffect.createOneShot(36L, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            HapticType.MENU_POP -> {
                // 30ms pop for 3-dot dropdowns
                VibrationEffect.createOneShot(30L, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            HapticType.SELECTION_NOTCH -> {
                // 28ms index notch for sort selection
                VibrationEffect.createOneShot(28L, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            HapticType.SWITCH_TOGGLE -> {
                // 32ms mechanical switch snap
                VibrationEffect.createOneShot(32L, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            HapticType.FOLDER_OPEN -> {
                // Double pulse: wait 0ms, on 22ms, wait 26ms, on 24ms
                VibrationEffect.createWaveform(longArrayOf(0, 22, 26, 24), -1)
            }
            HapticType.LONG_PRESS -> {
                // Heavy 58ms thud
                VibrationEffect.createOneShot(58L, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            HapticType.DESTRUCTIVE -> {
                // Urgent double buzz: wait 0ms, on 42ms, wait 38ms, on 48ms
                VibrationEffect.createWaveform(longArrayOf(0, 42, 38, 48), -1)
            }
            HapticType.HERO_CLEAN_PULSE -> {
                // Kinetic rising sweep: wait 0ms, on 20ms, wait 20ms, on 32ms, wait 20ms, on 45ms
                VibrationEffect.createWaveform(longArrayOf(0, 20, 20, 32, 20, 45), -1)
            }
            HapticType.SUCCESS_CONFIRM -> {
                // Confirmation double notch: wait 0ms, on 25ms, wait 28ms, on 38ms
                VibrationEffect.createWaveform(longArrayOf(0, 25, 28, 38), -1)
            }
            HapticType.ERROR_REJECT -> {
                // Staccato triple buzz
                VibrationEffect.createWaveform(longArrayOf(0, 18, 22, 18, 22, 18), -1)
            }
        }

        vibrator?.vibrate(effect)
    }

    private fun triggerLegacyPreOreo(type: HapticType) {
        @Suppress("DEPRECATION")
        when (type) {
            HapticType.NAV_TICK, HapticType.SEGMENT_SLIDE, HapticType.SELECTION_NOTCH -> vibrator?.vibrate(26L)
            HapticType.CLICK, HapticType.MENU_POP, HapticType.SWITCH_TOGGLE -> vibrator?.vibrate(36L)
            HapticType.FOLDER_OPEN -> vibrator?.vibrate(longArrayOf(0, 22, 26, 24), -1)
            HapticType.LONG_PRESS -> vibrator?.vibrate(58L)
            HapticType.DESTRUCTIVE -> vibrator?.vibrate(longArrayOf(0, 42, 38, 48), -1)
            HapticType.HERO_CLEAN_PULSE -> vibrator?.vibrate(longArrayOf(0, 20, 20, 32, 20, 45), -1)
            HapticType.SUCCESS_CONFIRM -> vibrator?.vibrate(longArrayOf(0, 25, 28, 38), -1)
            HapticType.ERROR_REJECT -> vibrator?.vibrate(longArrayOf(0, 18, 22, 18, 22, 18), -1)
        }
    }
}
