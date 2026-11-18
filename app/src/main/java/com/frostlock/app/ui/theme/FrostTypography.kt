package com.frostlock.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography tokens for the redesign.
 *
 * The rule that matters: one emphasized style per screen. Routine lists and
 * settings stay on the baseline styles so the hero moment keeps its weight.
 */
@Immutable
data class FrostTypography(
    val displayTimer: TextStyle,
    val displayLarge: TextStyle,
    val displayLargeEmphasized: TextStyle,
    val headlineLarge: TextStyle,
    val headlineMedium: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle
)

private fun style(
    size: Int,
    line: Int,
    weight: FontWeight,
    letterSpacing: Double = 0.0,
    tabular: Boolean = false
) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = letterSpacing.sp,
    fontFeatureSettings = if (tabular) "tnum" else null
)

val FrostType = FrostTypography(
    displayTimer = style(64, 70, FontWeight.Medium, letterSpacing = -1.0, tabular = true),
    displayLarge = style(40, 46, FontWeight.Bold, letterSpacing = -0.5, tabular = true),
    displayLargeEmphasized = style(40, 46, FontWeight.ExtraBold, letterSpacing = -0.5, tabular = true),
    headlineLarge = style(32, 38, FontWeight.Bold, letterSpacing = -0.25),
    headlineMedium = style(26, 32, FontWeight.Bold),
    titleLarge = style(20, 26, FontWeight.SemiBold),
    titleMedium = style(16, 22, FontWeight.SemiBold),
    bodyLarge = style(15, 22, FontWeight.Normal),
    bodyMedium = style(13, 18, FontWeight.Normal),
    bodySmall = style(12, 16, FontWeight.Normal),
    labelLarge = style(14, 18, FontWeight.SemiBold),
    labelMedium = style(11, 14, FontWeight.SemiBold, letterSpacing = 0.6)
)
