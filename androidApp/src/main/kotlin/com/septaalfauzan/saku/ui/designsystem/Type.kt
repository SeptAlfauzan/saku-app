package com.septaalfauzan.saku.ui.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.septaalfauzan.saku.R

object SakuFontFamily {
    val hankenGrotesk: FontFamily = FontFamily(
        Font(R.font.hanken_grotesk_regular, FontWeight.Normal),
        Font(R.font.hanken_grotesk_medium, FontWeight.Medium),
        Font(R.font.hanken_grotesk_semibold, FontWeight.SemiBold),
        Font(R.font.hanken_grotesk_bold, FontWeight.Bold),
    )
}

data class SakuType(
    val displayCurrency: TextStyle,
    val displayCurrencyMobile: TextStyle,
    val headlineLg: TextStyle,
    val headlineMd: TextStyle,
    val headlineSm: TextStyle,
    val bodyLg: TextStyle,
    val bodyMd: TextStyle,
    val bodySm: TextStyle,
    val labelCaps: TextStyle,
    val labelMd: TextStyle,
    val numericTable: TextStyle,
)

fun sakuTypography(families: FontFamily): SakuType = SakuType(
    displayCurrency = TextStyle(
        fontFamily = families, fontWeight = FontWeight.SemiBold, fontSize = 44.sp,
        lineHeight = 48.sp, letterSpacing = (-0.03).em,
    ),
    displayCurrencyMobile = TextStyle(
        fontFamily = families, fontWeight = FontWeight.SemiBold, fontSize = 36.sp,
        lineHeight = 40.sp, letterSpacing = (-0.025).em,
    ),
    headlineLg = TextStyle(
        fontFamily = families, fontWeight = FontWeight.SemiBold, fontSize = 28.sp,
        lineHeight = 34.sp, letterSpacing = (-0.02).em,
    ),
    headlineMd = TextStyle(
        fontFamily = families, fontWeight = FontWeight.SemiBold, fontSize = 22.sp,
        lineHeight = 28.sp, letterSpacing = (-0.015).em,
    ),
    headlineSm = TextStyle(
        fontFamily = families, fontWeight = FontWeight.SemiBold, fontSize = 18.sp,
        lineHeight = 24.sp, letterSpacing = (-0.01).em,
    ),
    bodyLg = TextStyle(
        fontFamily = families, fontWeight = FontWeight.Normal, fontSize = 16.sp,
        lineHeight = 24.sp, letterSpacing = (-0.005).em,
    ),
    bodyMd = TextStyle(
        fontFamily = families, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp,
    ),
    bodySm = TextStyle(
        fontFamily = families, fontWeight = FontWeight.Normal, fontSize = 12.sp,
        lineHeight = 16.sp, letterSpacing = 0.01.em,
    ),
    labelCaps = TextStyle(
        fontFamily = families, fontWeight = FontWeight.SemiBold, fontSize = 11.sp,
        lineHeight = 14.sp, letterSpacing = 0.08.em,
    ),
    labelMd = TextStyle(
        fontFamily = families, fontWeight = FontWeight.Medium, fontSize = 13.sp,
        lineHeight = 16.sp, letterSpacing = (-0.005).em,
    ),
    numericTable = TextStyle(
        fontFamily = families, fontWeight = FontWeight.Medium, fontSize = 15.sp,
        lineHeight = 20.sp, letterSpacing = (-0.01).em,
    ),
)

fun sakuMaterialTypography(sakuType: SakuType): Typography = Typography(
    displayLarge = sakuType.displayCurrency,
    displayMedium = sakuType.displayCurrencyMobile,
    headlineLarge = sakuType.headlineLg,
    headlineMedium = sakuType.headlineMd,
    headlineSmall = sakuType.headlineSm,
    bodyLarge = sakuType.bodyLg,
    bodyMedium = sakuType.bodyMd,
    bodySmall = sakuType.bodySm,
    labelLarge = sakuType.labelMd,
    labelMedium = sakuType.bodySm,
    titleLarge = sakuType.headlineSm,
    titleMedium = sakuType.numericTable,
    titleSmall = sakuType.labelMd,
    labelSmall = sakuType.labelCaps,
)
