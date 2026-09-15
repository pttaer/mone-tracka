package com.monetracka.shared.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Brand Colors
val MintGreen = Color(0xFF00D09C)
val MintGreenDark = Color(0xFF00B386)
val DeepNavy = Color(0xFF0D1B2A)
val DarkSurface = Color(0xFF1B2838)
val DarkCard = Color(0xFF243447)
val LightBackground = Color(0xFFF5F7FA)
val LightSurface = Color(0xFFFFFFFF)
val LightCard = Color(0xFFFFFFFF)
val CoralRed = Color(0xFFFF6B6B)
val SunsetOrange = Color(0xFFFF9F43)
val SkyBlue = Color(0xFF54A0FF)
val LavenderPurple = Color(0xFF9B59B6)
val TextPrimary = Color(0xFF1A1F36)
val TextSecondary = Color(0xFF6B7280)
val TextOnDark = Color(0xFFE8ECEF)
val TextOnDarkSecondary = Color(0xFFA0AEC0)

val DarkColorScheme = darkColorScheme(
    primary = MintGreen,
    onPrimary = DeepNavy,
    secondary = SkyBlue,
    tertiary = SunsetOrange,
    background = DeepNavy,
    surface = DarkSurface,
    surfaceVariant = DarkCard,
    onBackground = TextOnDark,
    onSurface = TextOnDark,
    onSurfaceVariant = TextOnDarkSecondary,
    error = CoralRed,
    onError = Color.White,
)

val LightColorScheme = lightColorScheme(
    primary = MintGreen,
    onPrimary = Color.White,
    secondary = SkyBlue,
    tertiary = SunsetOrange,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightCard,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error = CoralRed,
    onError = Color.White,
)

val CategoryColorHexes = listOf(
    0xFF00D09CL,
    0xFF00B4D8L,
    0xFFFF7A00L,
    0xFFFF5A79L,
    0xFFA29BFEL,
    0xFF00CEC9L,
    0xFFFDCB6EL,
    0xFFE17055L,
    0xFF6C5CE7L,
    0xFF55EFC4L,
)

// Category colors for charts/icons derived from single source of truth
val CategoryColors = CategoryColorHexes.map { Color(it) }

object MoneTrackaColors {
    // ── Light-first surfaces ──
    val BackgroundLight = Color(0xFFF5F7FA)
    val CardWhite = Color(0xFFFFFFFF)
    val SurfaceSecondary = Color(0xFFF0F2F5)
    val ProgressTrack = Color(0xFFE5E7EB)
    val CardShadowColor = Color(0x1A000000) // 10% black

    // ── Brand accent ──
    val MintPrimary = Color(0xFF00D09C)
    val MintDark = Color(0xFF00A87E)
    val MintLight = Color(0xFFE6FAF5)
    val CyanAccent = Color(0xFF00B4D8)
    val CoralDanger = Color(0xFFFF5A79)
    val AmberWarning = Color(0xFFFFB300)
    val VioletInsight = Color(0xFF9D65FF)

    // ── Text ──
    val TextDark = Color(0xFF1A1F36)
    val TextGray = Color(0xFF6B7280)
    val TextLight = Color(0xFF9CA3AF)
    val TextWhite = Color(0xFFFFFFFF)

    // ── Gradients ──
    val MintGradient = Brush.linearGradient(listOf(MintPrimary, MintDark))
    val HeroGradientBrush = Brush.linearGradient(
        listOf(Color(0xFF00D09C), Color(0xFF00B4D8), Color(0xFF0096C7))
    )
}
