package com.septaalfauzan.saku.ui.designsystem

import androidx.compose.ui.graphics.Color

data class SakuPalette(
    val ink: Color,
    val crimson: Color,
    val slate: Color,
    val chalk: Color,
    val canvas: Color,
    val surfaceLow: Color,
    val hairline: Color,
) {
    val isDark: Boolean get() = canvas == Dark.canvas
}

object Light {
    val canvas: Color = Color(0xFFFFFFFF)

    fun palette() = SakuPalette(
        ink = Color(0xFF0D0D11),
        crimson = Color(0xFFFF005E),
        slate = Color(0xFF6B6E7B),
        chalk = Color(0xFFF4F4F6),
        canvas = canvas,
        surfaceLow = Color(0xFFF9F9FB),
        hairline = Color(0x0F0D0D11),
    )
}

object Dark {
    val canvas: Color = Color(0xFF121216)

    fun palette() = SakuPalette(
        ink = Color(0xFFF4F4F6),
        crimson = Color(0xFFFF005E),
        slate = Color(0xFF9C9CA4),
        chalk = Color(0xFF232329),
        canvas = canvas,
        surfaceLow = Color(0xFF1A1A1F),
        hairline = Color(0x14FFFFFF),
    )
}
