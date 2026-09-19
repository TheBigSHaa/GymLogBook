package com.ironlog.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * ProYou motion system — one vocabulary for every animation in the app.
 *
 * Principles:
 *  - Entrances decelerate (fast start, gentle settle) — content "arrives".
 *  - Exits accelerate and are FASTER than entrances — the app never feels like
 *    it's holding the user back.
 *  - Springs are reserved for direct-manipulation feedback (press, drag, toggle).
 */
object MotionTokens {
    const val DurationFast = 150
    const val DurationMedium = 300
    const val DurationSlow = 450
    const val DurationExtraSlow = 700

    /** Material 3 emphasized-decelerate: for anything entering the screen. */
    val EasingEmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Material 3 emphasized-accelerate: for anything leaving the screen. */
    val EasingEmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Standard easing: for in-place property changes (color, size). */
    val EasingStandard = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    fun <T> enterSpec(): FiniteAnimationSpec<T> =
        tween(DurationSlow, easing = EasingEmphasizedDecelerate)

    fun <T> exitSpec(): FiniteAnimationSpec<T> =
        tween(DurationFast, easing = EasingEmphasizedAccelerate)

    fun <T> standardSpec(): FiniteAnimationSpec<T> =
        tween(DurationMedium, easing = EasingStandard)

    /** Settle without overshoot — content-level spring. */
    fun springGentle(): SpringSpec<Float> = spring(dampingRatio = 0.85f, stiffness = 380f)

    /** Slight overshoot — tactile feedback on press/selection. */
    fun springBouncy(): SpringSpec<Float> = spring(dampingRatio = 0.6f, stiffness = 320f)
}
