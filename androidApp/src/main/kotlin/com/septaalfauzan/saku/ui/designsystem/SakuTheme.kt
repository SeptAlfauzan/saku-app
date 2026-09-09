package com.septaalfauzan.saku.ui.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LocalPalette = staticCompositionLocalOf { Light.palette() }
private val LocalType = staticCompositionLocalOf { sakuTypography(SakuFontFamily.hankenGrotesk) }

@Immutable
class SakuColors(
    val palette: SakuPalette,
    val colorScheme: androidx.compose.material3.ColorScheme,
)

@Composable
fun sakuColorScheme(palette: SakuPalette): androidx.compose.material3.ColorScheme =
    if (palette.isDark) {
        darkColorScheme(
            primary = palette.ink,
            onPrimary = palette.canvas,
            secondary = palette.crimson,
            onSecondary = Color.White,
            tertiary = palette.slate,
            onTertiary = palette.canvas,
            background = palette.canvas,
            onBackground = palette.ink,
            surface = palette.surfaceLow,
            onSurface = palette.ink,
            surfaceVariant = palette.chalk,
            onSurfaceVariant = palette.slate,
            outline = palette.hairline,
            error = Color(0xFFBA1A1A),
        )
    } else {
        lightColorScheme(
            primary = palette.ink,
            onPrimary = palette.canvas,
            secondary = palette.crimson,
            onSecondary = Color.White,
            tertiary = palette.slate,
            onTertiary = palette.canvas,
            background = palette.canvas,
            onBackground = palette.ink,
            surface = palette.surfaceLow,
            onSurface = palette.ink,
            surfaceVariant = palette.chalk,
            onSurfaceVariant = palette.slate,
            outline = palette.hairline,
            error = Color(0xFFBA1A1A),
        )
    }

object SakuTheme {
    val palette: SakuPalette
        @Composable @ReadOnlyComposable get() = LocalPalette.current

    val type: SakuType
        @Composable @ReadOnlyComposable get() = LocalType.current
}

@Composable
fun SakuTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val palette = if (dark) Dark.palette() else Light.palette()
    val sakuType = sakuTypography(SakuFontFamily.hankenGrotesk)
    val colorScheme = sakuColorScheme(palette)
    CompositionLocalProvider(
        LocalPalette provides palette,
        LocalType provides sakuType,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = sakuMaterialTypography(sakuType),
            content = content,
        )
    }
}
