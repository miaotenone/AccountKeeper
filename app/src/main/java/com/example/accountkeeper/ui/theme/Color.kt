package com.example.accountkeeper.ui.theme

import androidx.compose.ui.graphics.Color

// ============ Premium Light Theme ============
// Inspired by modern fintech apps like Nubank, Revolut
val LightPrimary = Color(0xFF6C5DD3)        // Premium Purple
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFE8E6FC)
val LightOnPrimaryContainer = Color(0xFF251B58)

val LightSecondary = Color(0xFF00B5A4)      // Teal accent
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFD3F8F5)
val LightOnSecondaryContainer = Color(0xFF00201C)

val LightTertiary = Color(0xFFFF7D42)       // Vibrant Orange
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFFDBCC)
val LightOnTertiaryContainer = Color(0xFF3F1500)

val LightBackground = Color(0xFFFAFBFF)     // Ultra light background
val LightOnBackground = Color(0xFF1A1B26)
val LightSurface = Color(0xFFF8F9FF)        // Surface with subtle tint
val LightOnSurface = Color(0xFF1A1B26)
val LightSurfaceVariant = Color(0xFFE4E8F3)
val LightOnSurfaceVariant = Color(0xFF454854)

val LightError = Color(0xFFE63946)
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF410002)

// Premium gradients for light theme
val LightGradientPrimary = listOf(
    Color(0xFF6C5DD3),
    Color(0xFF8B7CF8),
    Color(0xFFA89BFA)
)

val LightGradientIncome = listOf(
    Color(0xFF00B5A4),
    Color(0xFF00D4C0),
    Color(0xFF5CE8DC)
)

val LightGradientExpense = listOf(
    Color(0xFFE63946),
    Color(0xFFF06A75),
    Color(0xFFFA9CA5)
)

// ============ Premium Dark Theme ============
val DarkPrimary = Color(0xFF9C8FE3)
val DarkOnPrimary = Color(0xFF1F1A4A)
val DarkPrimaryContainer = Color(0xFF3B3069)
val DarkOnPrimaryContainer = Color(0xFFE8E6FC)

val DarkSecondary = Color(0xFF5BD9CA)
val DarkOnSecondary = Color(0xFF003731)
val DarkSecondaryContainer = Color(0xFF005048)
val DarkOnSecondaryContainer = Color(0xFFD3F8F5)

val DarkTertiary = Color(0xFFFFB69B)
val DarkOnTertiary = Color(0xFF5C2600)
val DarkTertiaryContainer = Color(0xFF823B00)
val DarkOnTertiaryContainer = Color(0xFFFFDBCC)

val DarkBackground = Color(0xFF0D0E17)      // Deep rich dark
val DarkOnBackground = Color(0xFFE3E3F0)
val DarkSurface = Color(0xFF151724)         // Surface with depth
val DarkOnSurface = Color(0xFFE3E3F0)
val DarkSurfaceVariant = Color(0xFF2D3040)
val DarkOnSurfaceVariant = Color(0xFFB8BBC8)

val DarkError = Color(0xFFFF6B6B)
val DarkErrorContainer = Color(0xFF9C1A1A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

// Premium gradients for dark theme
val DarkGradientPrimary = listOf(
    Color(0xFF9C8FE3),
    Color(0xFFB8ADF0),
    Color(0xFFD4CBF8)
)

val DarkGradientIncome = listOf(
    Color(0xFF5BD9CA),
    Color(0xFF7AE8DB),
    Color(0xAA99F8ED)
)

val DarkGradientExpense = listOf(
    Color(0xFFFF6B6B),
    Color(0xFFFF8C8C),
    Color(0xAAFFADAD)
)

// ============ Premium Category Colors ============
// Soft, harmonious color palette
val CategoryColors = listOf(
    Color(0xFF6C5DD3), // Purple
    Color(0xFF00B5A4), // Teal
    Color(0xFFFF7D42), // Orange
    Color(0xFFE63946), // Red
    Color(0xFF3B82F6), // Blue
    Color(0xFF8B5CF6), // Violet
    Color(0xFFF59E0B), // Amber
    Color(0xFF10B981), // Emerald
    Color(0xFFEC4899), // Pink
    Color(0xFF06B6D4), // Cyan
    Color(0xFF8B5A2B), // Brown
    Color(0xFF64748B)  // Slate
)

// ============ Premium Chart Colors ============
// Optimized for data visualization
val ChartColors = listOf(
    Color(0xFF6C5DD3),
    Color(0xFF00B5A4),
    Color(0xFFFF7D42),
    Color(0xFFE63946),
    Color(0xFF3B82F6),
    Color(0xFF8B5CF6),
    Color(0xFFF59E0B),
    Color(0xFF10B981)
)

// ============ Special Effects ============
val GlassSurfaceLight = Color(0x80FFFFFF)     // 50% white
val GlassSurfaceDark = Color(0x80151724)      // 50% dark surface

val ShadowLight = Color(0x1A000000)          // 10% black
val ShadowDark = Color(0x33000000)           // 20% black

val DividerLight = Color(0x1A1A1B26)         // 10% text
val DividerDark = Color(0x1AE3E3F0)          // 10% text

val GradientStart = Color(0xFF6C5DD3)
val GradientEnd = Color(0xFF8B7CF8)

val IncomeColor = Color(0xFF00B5A4)
val ExpenseColor = Color(0xFFE63946)