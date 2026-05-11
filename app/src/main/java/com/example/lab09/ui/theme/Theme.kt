package com.example.lab09.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color

// Premium Color Palette
val Primary = Color(0xFFF59E0B) // Amber 500
val Secondary = Color(0xFF10B981) // Emerald 500
val Background = Color(0xFF0F172A) // Slate 900
val Surface = Color(0xFF1E293B) // Slate 800
val OnPrimary = Color.Black
val OnBackground = Color(0xFFF8FAFC) // Slate 50
val OnSurface = Color(0xFFF1F5F9) // Slate 100
val TextMuted = Color(0xFF94A3B8) // Slate 400

val AppTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 28.sp,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )
)
