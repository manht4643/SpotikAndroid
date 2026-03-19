package com.spotik.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Spotik glass palette ──
val SurfaceDark = Color(0xFF0A0A0F)
val CardDark = Color(0xFF16161E)
val BorderLight = Color(0x33FFFFFF)
val BorderDark = Color(0x1AFFFFFF)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xB3FFFFFF)
val TextMuted = Color(0x66FFFFFF)
val Accent = Color(0xFF6C5CE7)
val AccentLike = Color(0xFFFF6B81)
val Premium = Color(0xFFFFD700)
val NavGlow = Color(0xFF00D2FF)

private val SpotikDarkScheme = darkColorScheme(
    primary = Accent,
    secondary = NavGlow,
    tertiary = AccentLike,
    background = SurfaceDark,
    surface = CardDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = BorderLight,
    outlineVariant = BorderDark,
)

@Composable
fun SpotikTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = SpotikDarkScheme,
        content = content,
    )
}
