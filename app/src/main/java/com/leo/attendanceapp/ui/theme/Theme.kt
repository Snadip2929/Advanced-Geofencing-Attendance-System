package com.leo.attendanceapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Neon Colors ──────────────────────────────────────────────
val NeonBlue = Color(0xFF00D4FF)
val NeonPurple = Color(0xFF9B59FF)
val NeonGlow = Color(0xFF6C63FF)
val DeepBlack = Color(0xFF050510)
val SpaceBlack = Color(0xFF0A0A1A)
val CardGlass = Color(0x1AFFFFFF)
val CardBorder = Color(0x33FFFFFF)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB0B0CC)
val NeonGreen = Color(0xFF00FF9F)
val NeonRed = Color(0xFFFF4B6E)
val NeonOrange = Color(0xFFFF9F00)

// ── Dark Color Scheme ─────────────────────────────────────────
private val FutureColorScheme = darkColorScheme(
    primary = NeonBlue,
    secondary = NeonPurple,
    tertiary = NeonGreen,
    background = DeepBlack,
    surface = SpaceBlack,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = NeonRed
)

// ── App Theme ─────────────────────────────────────────────────
@Composable
fun AttendanceAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FutureColorScheme,
        typography = Typography,
        content = content
    )
}