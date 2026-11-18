package com.frostlock.app.ui.components.charts

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Shared colour ramp for every focus-intensity chart.
 *
 * Level 0 is an empty track, levels 1–4 step up the primary colour. Charts use
 * the same ramp as the calendar so a colour always means the same amount of
 * focus.
 */
@Composable
fun chartCellColor(level: Int): Color {
    val colors = FrostTheme.colors
    return when (level) {
        0 -> colors.surfaceContainerHigh.copy(alpha = 0.45f)
        1 -> colors.primary.copy(alpha = 0.18f)
        2 -> colors.primary.copy(alpha = 0.36f)
        3 -> colors.primary.copy(alpha = 0.58f)
        else -> colors.primary.copy(alpha = 0.82f)
    }
}

/** Colour for a stored ARGB subject colour, with a safe fallback. */
fun subjectColor(argb: Int): Color = Color(argb)

/** Text colour that stays readable on a filled chart cell. */
@Composable
fun chartCellContentColor(level: Int): Color =
    if (level >= 3) FrostTheme.colors.onPrimary else FrostTheme.colors.textPrimary
