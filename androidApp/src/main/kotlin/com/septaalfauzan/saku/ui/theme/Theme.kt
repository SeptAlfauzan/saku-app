package com.septaalfauzan.saku.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF166534),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCFCE7),
    onPrimaryContainer = Color(0xFF052E16),
    secondary = Color(0xFF0F766E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF042F2E),
    background = Color(0xFFF8FAF8),
    onBackground = Color(0xFF111812),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111812),
    surfaceVariant = Color(0xFFE8EFE9),
    onSurfaceVariant = Color(0xFF3F4943),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4ADE80),
    onPrimary = Color(0xFF052E16),
    primaryContainer = Color(0xFF14532D),
    onPrimaryContainer = Color(0xFFDCFCE7),
    secondary = Color(0xFF5EEAD4),
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF134E4A),
    onSecondaryContainer = Color(0xFFCCFBF1),
    background = Color(0xFF101412),
    onBackground = Color(0xFFE2E8E2),
    surface = Color(0xFF171B18),
    onSurface = Color(0xFFE2E8E2),
    surfaceVariant = Color(0xFF3F4943),
    onSurfaceVariant = Color(0xFFBFCCC4),
    error = Color(0xFFF2B8B5),
)

@Composable
fun SakuTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content,
    )
}
