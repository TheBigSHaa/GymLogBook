package com.ironlog.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val ProYouTypography =
    Typography(
        // ─── DISPLAY — huge headlines (e.g., "LOWER POWER" hero text) ──
        displayLarge =
            TextStyle(
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 48.sp,
                lineHeight = 52.sp,
                letterSpacing = (-1.5).sp,
            ),
        displayMedium =
            TextStyle(
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 36.sp,
                lineHeight = 40.sp,
                letterSpacing = (-1).sp,
            ),
        displaySmall =
            TextStyle(
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.5).sp,
            ),
        // ─── HEADLINE — section titles, stat numbers ──────────────────
        headlineLarge =
            TextStyle(
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                lineHeight = 28.sp,
                letterSpacing = (-0.25).sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 24.sp,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 22.sp,
            ),
        // ─── TITLE — card titles ──────────────────────────────────────
        titleLarge =
            TextStyle(
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = InterFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
        titleSmall =
            TextStyle(
                fontFamily = InterFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.1.sp,
            ),
        // ─── BODY — general text ──────────────────────────────────────
        bodyLarge =
            TextStyle(
                fontFamily = InterFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = InterFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.25.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = InterFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.4.sp,
            ),
        // ─── LABEL — small uppercase labels, nav, chips ───────────────
        labelLarge =
            TextStyle(
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                letterSpacing = 2.sp, // wide tracking for uppercase labels like "DURATION", "WEEK"
            ),
    )

// Legacy alias — do not delete until screen rebuilds are complete
val IronTypography = ProYouTypography

