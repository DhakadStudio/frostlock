package com.frostlock.app.ui.theme

import androidx.compose.ui.graphics.Color

// Base surfaces — polar night with frozen depth. These read the active
// IcePalette so switching themes restyles every screen without touching each
// composable. Feedback colours stay constant for recognisability.
val BackgroundDark: Color get() = FrostThemeState.palette.background
val BackgroundElevated: Color get() = FrostThemeState.palette.backgroundElevated
val SurfaceCard: Color get() = FrostThemeState.palette.surfaceCard
val SurfaceHigh: Color get() = FrostThemeState.palette.surfaceHigh
val StrokeSoft: Color get() = FrostThemeState.palette.strokeSoft

// Accents — ice, glacier and frost
val PrimaryIce: Color get() = FrostThemeState.palette.primary
val GlacierBlue: Color get() = FrostThemeState.palette.glacier
val AccentFrost: Color get() = FrostThemeState.palette.accent
val GlowIce: Color get() = FrostThemeState.palette.glow
val InkOnIce: Color get() = FrostThemeState.palette.ink

// Text
val TextPrimary: Color get() = FrostThemeState.palette.textPrimary
val TextSecondary: Color get() = FrostThemeState.palette.textSecondary

// Feedback
val SuccessGreen = Color(0xFF2DD4BF)
val DangerRed = Color(0xFFFB7185)
val WarnAmber = Color(0xFFFCD34D)

// Gradients
val GradientFrost: List<Color> get() = FrostThemeState.palette.gradient
val GradientHero: List<Color> get() = FrostThemeState.palette.heroGradient
