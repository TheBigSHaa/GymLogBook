package com.ironlog.app.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════
// ProYou design system — refined, professional palette.
//
// Dark theme: graphite surfaces with a subtle blue undertone
// (reads as "engineered" rather than pure-black OLED).
// Light theme: airy neutral surfaces with DEEPER accent variants —
// the dark-theme accents are too pale to pass contrast on white.
// Day accents: universal mid-tones legible on both themes.
// ═══════════════════════════════════════════════════════════════

// ─── DARK · CORE SURFACES ───────────────────────────────────────
val Background = Color(0xFF0F1214)
val Surface = Color(0xFF0F1214)
val SurfaceContainerLowest = Color(0xFF0A0C0E)
val SurfaceContainerLow = Color(0xFF15181B)
val SurfaceContainer = Color(0xFF1B1F23)
val SurfaceContainerHigh = Color(0xFF22262B)
val SurfaceContainerHighest = Color(0xFF2A2F35)
val SurfaceVariant = Color(0xFF2A2F35)
val SurfaceBright = Color(0xFF31373E)

// ─── DARK · TEXT & OUTLINE ──────────────────────────────────────
val OnSurface = Color(0xFFF2F4F5)
val OnBackground = Color(0xFFF2F4F5)
val OnSurfaceVariant = Color(0xFFA6ADB4)
val Outline = Color(0xFF737A82)
val OutlineVariant = Color(0xFF3D434A)

// ─── LIGHT · CORE SURFACES ──────────────────────────────────────
val LightBackground = Color(0xFFF7F8FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFF1F3F6)
val LightSurfaceContainer = Color(0xFFEAEDF1)
val LightSurfaceContainerHigh = Color(0xFFE2E6EB)
val LightSurfaceContainerHighest = Color(0xFFD9DEE4)
val LightSurfaceVariant = Color(0xFFE2E6EB)
val LightSurfaceBright = Color(0xFFFFFFFF)

// ─── LIGHT · TEXT & OUTLINE ─────────────────────────────────────
val LightOnSurface = Color(0xFF17191C)
val LightOnBackground = Color(0xFF17191C)
val LightOnSurfaceVariant = Color(0xFF4B5259)
val LightOutline = Color(0xFF6F767D)
val LightOutlineVariant = Color(0xFFC5CBD2)

// ─── DARK · PRIMARY (CORAL) — LOWER BODY & MAIN CTA ────────────
val Primary = Color(0xFFFF7B6F)
val PrimaryDim = Color(0xFFF2655B)
val PrimaryContainer = Color(0xFF7A2A22)
val OnPrimary = Color(0xFF3F0B06)
val OnPrimaryContainer = Color(0xFFFFDAD5)

// ─── DARK · SECONDARY (TEAL) — UPPER BODY ──────────────────────
val Secondary = Color(0xFF35D0BA)
val SecondaryDim = Color(0xFF21B5A1)
val SecondaryContainer = Color(0xFF00504A)
val OnSecondary = Color(0xFF00332D)
val OnSecondaryContainer = Color(0xFFB8FFF2)

// ─── DARK · TERTIARY (GOLD) — BONUS DAY & HIGHLIGHTS ───────────
val Tertiary = Color(0xFFF2C14E)
val TertiaryDim = Color(0xFFDCA92F)
val TertiaryContainer = Color(0xFF5C4400)
val OnTertiary = Color(0xFF3F2E00)
val OnTertiaryContainer = Color(0xFFFFE9B8)

// ─── DARK · ERROR ───────────────────────────────────────────────
val ErrorColor = Color(0xFFEF6A6A)
val ErrorDim = Color(0xFFD64F57)
val ErrorContainer = Color(0xFF8C1D2F)
val OnError = Color(0xFF3F0A0A)
val OnErrorContainer = Color(0xFFFFDADA)

// ─── LIGHT · ACCENTS (deeper variants for contrast on white) ────
val LightPrimary = Color(0xFFBF4036)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFFFDAD5)
val LightOnPrimaryContainer = Color(0xFF40100A)

val LightSecondary = Color(0xFF0B7A6D)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFBDF2E7)
val LightOnSecondaryContainer = Color(0xFF00332D)

val LightTertiary = Color(0xFF8F6400)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFFE9B8)
val LightOnTertiaryContainer = Color(0xFF3F2E00)

val LightError = Color(0xFFB3261E)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFF9DEDC)
val LightOnErrorContainer = Color(0xFF410E0B)

// ─── SUCCESS / POSITIVE ACCENT ─────────────────────────────────
val SuccessGreen = Color(0xFF3FBF7F)

// ─── DAY-TYPE ACCENT MAPPING ───────────────────────────────────
// Universal mid-tones: dark enough for light surfaces, bright
// enough for dark surfaces. Each WorkoutDay maps to one of these
// for borders, labels, dots and CTA fills:
val DayUpperPower = Color(0xFF16A99A) // teal
val DayLowerPower = Color(0xFFE4584B) // coral
val DayUpperHypertrophy = Color(0xFF0E8A7E) // deep teal
val DayLowerHypertrophy = Color(0xFFC44B40) // deep coral
val DayBonus = Color(0xFFD9930D) // amber

// Helper to resolve day color by WorkoutDay id (1..5)
fun dayColorForId(id: Int): Color =
    when (id) {
        1 -> DayUpperPower
        2 -> DayLowerPower
        3 -> DayUpperHypertrophy
        4 -> DayLowerHypertrophy
        5 -> DayBonus
        else -> Primary
    }

// Hex string version (for DB seed data). Keep in sync with the
// Color values above — IronLogSeedCallback rewrites the stored
// hexes from this function on every app open.
fun dayColorHexForId(id: Int): String =
    when (id) {
        1 -> "#16A99A"
        2 -> "#E4584B"
        3 -> "#0E8A7E"
        4 -> "#C44B40"
        5 -> "#D9930D"
        else -> "#E4584B"
    }

// Legacy aliases — do not delete until screen rebuilds are complete
val IronBg = Background
val IronSurface = SurfaceContainer
val IronTextPrimary = OnSurface
val IronTextSecondary = OnSurfaceVariant
val IronSuccess = SuccessGreen

// Accent colors per day (legacy names used by screens)
val Day1UpperPower = DayUpperPower
val Day2LowerPower = DayLowerPower
val Day3UpperHypertrophy = DayUpperHypertrophy
val Day4LowerHypertrophy = DayLowerHypertrophy
