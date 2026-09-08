package com.monetracka.shared.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand Colors
val MintGreen = Color(0xFF00D09C)
val MintGreenDark = Color(0xFF00B386)
val DeepNavy = Color(0xFF0D1B2A)
val DarkSurface = Color(0xFF1B2838)
val DarkCard = Color(0xFF243447)
val LightBackground = Color(0xFFF8F9FA)
val LightSurface = Color(0xFFFFFFFF)
val LightCard = Color(0xFFFFFFFF)
val CoralRed = Color(0xFFFF6B6B)
val SunsetOrange = Color(0xFFFF9F43)
val SkyBlue = Color(0xFF54A0FF)
val LavenderPurple = Color(0xFF9B59B6)
val TextPrimary = Color(0xFF2D3436)
val TextSecondary = Color(0xFF636E72)
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

// Category colors for charts/icons
val CategoryColors = listOf(
    MintGreen,
    SkyBlue,
    SunsetOrange,
    CoralRed,
    LavenderPurple,
    Color(0xFF00CEC9),  // Teal
    Color(0xFFFDCB6E),  // Gold
    Color(0xFFE17055),  // Burnt Orange
    Color(0xFF6C5CE7),  // Indigo
    Color(0xFF55EFC4),  // Light Mint
)
