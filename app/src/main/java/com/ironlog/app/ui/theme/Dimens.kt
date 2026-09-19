package com.ironlog.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * ProYou dimension scale — the only spacing/radius values screens should use.
 * 4dp grid: 4 / 8 / 12 / 16 / 20 / 24 / 32.
 */
object Dimens {
    // ── Screen structure ────────────────────────────────────────────
    val ScreenPaddingH = 20.dp

    /** Bottom content padding for scrollables — clears the floating nav bar. */
    val ScreenPaddingBottom = 140.dp

    val SectionSpacing = 24.dp
    val ItemSpacing = 12.dp

    // ── Corner radii ────────────────────────────────────────────────
    /** Hero surfaces (workout hero, featured charts). */
    val RadiusXL = 28.dp

    /** Standard cards. */
    val RadiusL = 20.dp

    /** Inner elements, buttons, inputs. */
    val RadiusM = 14.dp

    /** Chips, small tags. */
    val RadiusS = 10.dp

    // ── Card interiors ──────────────────────────────────────────────
    val CardPadding = 20.dp
    val CardPaddingLarge = 24.dp

    // ── Controls ────────────────────────────────────────────────────
    val CtaHeight = 60.dp
    val TouchTarget = 48.dp

    // ── Strokes ─────────────────────────────────────────────────────
    const val HairlineAlpha = 0.25f
}
