package com.ioristudios.filemanager.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset

/**
 * Calibrated Motion Tokens for Apple Vision / iOS Glass Fluidity motion design.
 */
object MotionTokens {
    // Easing curves
    val EaseFluidOut: Easing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)
    val EaseSpatialPush: Easing = CubicBezierEasing(0.32f, 0.94f, 0.6f, 1.0f)
    val EaseExpressive: Easing = FastOutSlowInEasing

    // Core Spring Specs
    val SpringElasticPress = spring<Float>(
        dampingRatio = 0.65f,
        stiffness = Spring.StiffnessMediumLow
    )

    val SpringSpatialHierarchy = spring<Float>(
        dampingRatio = 0.82f,
        stiffness = Spring.StiffnessLow
    )

    val SpringSpatialOffset = spring<IntOffset>(
        dampingRatio = 0.82f,
        stiffness = Spring.StiffnessLow
    )

    val SpringItemReorder = spring<IntOffset>(
        dampingRatio = 0.78f,
        stiffness = Spring.StiffnessMediumLow
    )

    val SpringFloat = spring<Float>(
        dampingRatio = 0.78f,
        stiffness = Spring.StiffnessMediumLow
    )

    val SpringDialogEntrance = spring<Float>(
        dampingRatio = 0.72f,
        stiffness = Spring.StiffnessMedium
    )

    val SpringThumbToggle = spring<Float>(
        dampingRatio = 0.68f,
        stiffness = Spring.StiffnessMedium
    )

    val SpringCheckboxPop = spring<Float>(
        dampingRatio = 0.60f,
        stiffness = Spring.StiffnessMedium
    )

    val SpringBouncyJuicy = spring<Float>(
        dampingRatio = 0.58f,
        stiffness = Spring.StiffnessMediumLow
    )

    val SpringOvershoot = spring<Float>(
        dampingRatio = 0.62f,
        stiffness = Spring.StiffnessLow
    )

    val SpringPillSlide = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = 0.68f,
        stiffness = Spring.StiffnessMediumLow
    )

    // Duration constants (in ms)
    const val DurationFast = 200
    const val DurationMedium = 320
    const val DurationSlow = 500
    const val StaggerDelayPerItem = 22 // ms per item in LazyColumn / Grid
    const val MaxStaggerCap = 280 // ms maximum total stagger cap

    @Composable
    fun <T> resolveSpec(spec: AnimationSpec<T>): AnimationSpec<T> {
        val motion = LocalMotionSensitivity.current
        return if (motion.isReducedMotion) {
            snap()
        } else {
            spec
        }
    }

    /**
     * Resolves a tween with scaled duration according to system animator scale.
     */
    @Composable
    fun <T> resolveTween(
        durationMillis: Int = DurationMedium,
        delayMillis: Int = 0,
        easing: Easing = EaseFluidOut
    ): AnimationSpec<T> {
        val motion = LocalMotionSensitivity.current
        return if (motion.isReducedMotion) {
            snap()
        } else {
            val scaledDuration = (durationMillis * motion.durationMultiplier).toInt().coerceAtLeast(1)
            val scaledDelay = (delayMillis * motion.durationMultiplier).toInt()
            tween(
                durationMillis = scaledDuration,
                delayMillis = scaledDelay,
                easing = easing
            )
        }
    }
}
