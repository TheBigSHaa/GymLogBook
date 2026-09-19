package com.ironlog.app.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.ironlog.app.R

val googleFontProvider =
    GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs,
    )

private val manropeFont = GoogleFont("Manrope")
private val interFont = GoogleFont("Inter")

val ManropeFamily =
    FontFamily(
        Font(googleFont = manropeFont, fontProvider = googleFontProvider, weight = FontWeight.Normal),
        Font(googleFont = manropeFont, fontProvider = googleFontProvider, weight = FontWeight.Medium),
        Font(googleFont = manropeFont, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
        Font(googleFont = manropeFont, fontProvider = googleFontProvider, weight = FontWeight.Bold),
        Font(googleFont = manropeFont, fontProvider = googleFontProvider, weight = FontWeight.ExtraBold),
        Font(googleFont = manropeFont, fontProvider = googleFontProvider, weight = FontWeight.Black),
    )

val InterFamily =
    FontFamily(
        Font(googleFont = interFont, fontProvider = googleFontProvider, weight = FontWeight.Light),
        Font(googleFont = interFont, fontProvider = googleFontProvider, weight = FontWeight.Normal),
        Font(googleFont = interFont, fontProvider = googleFontProvider, weight = FontWeight.Medium),
        Font(googleFont = interFont, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
        Font(googleFont = interFont, fontProvider = googleFontProvider, weight = FontWeight.Bold),
    )

