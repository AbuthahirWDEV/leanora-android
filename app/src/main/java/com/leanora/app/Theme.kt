package com.leanora.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val Cream = Color(0xFFFFF8F1)
internal val Ink = Color(0xFF29231F)
internal val Muted = Color(0xFF6B635C)
internal val Mint = Color(0xFFFFE0CA)
internal val Lavender = Color(0xFFF8E7DA)
internal val BrandOrange = Color(0xFFFF7A32)
internal val SplashBackground = Color(0xFF28211D)
internal val WaterBlue = Color(0xFF2196F3)

private val LightColors = lightColorScheme(
    primary = Color(0xFFB5471A),
    onPrimary = Color.White,
    secondary = Mint,
    background = Cream,
    surface = Color.White,
    onSurface = Ink,
    onSurfaceVariant = Muted,
    primaryContainer = Lavender,
    onPrimaryContainer = Ink,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFA36F),
    onPrimary = Color(0xFF3A1706),
    secondary = Mint,
    background = Color(0xFF191817),
    surface = Color(0xFF292623),
    onSurface = Color(0xFFFFF8F1),
    onSurfaceVariant = Color(0xFFD0C5BC),
    primaryContainer = Color(0xFF5A2D1B),
    onPrimaryContainer = Color(0xFFFFE0CA),
)

@Composable
internal fun LeanoraTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
