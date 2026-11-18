package com.frostlock.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Semantic colour roles for the redesign.
 *
 * Screens ask for a role (surface, text, danger), never for a palette slot.
 * That is what lets a palette change, or a future light theme, restyle every
 * screen without touching a composable.
 */
@Immutable
data class FrostColors(
    val background: Color,
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val tertiary: Color,
    val glacier: Color,
    val outline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val actionGradient: List<Color>,
    val heroGradient: List<Color>
) {
    companion object {
        /** Maps an ice palette onto the semantic roles. */
        fun from(palette: IcePalette): FrostColors = FrostColors(
            background = palette.background,
            surfaceContainerLowest = palette.background,
            surfaceContainerLow = palette.backgroundElevated,
            surfaceContainer = palette.surfaceCard,
            surfaceContainerHigh = palette.surfaceHigh,
            surfaceContainerHighest = palette.surfaceHigh,
            primary = palette.primary,
            onPrimary = palette.ink,
            secondary = palette.accent,
            tertiary = palette.glow,
            glacier = palette.glacier,
            outline = palette.strokeSoft,
            textPrimary = palette.textPrimary,
            textSecondary = palette.textSecondary,
            success = Color(0xFF2DD4BF),
            warning = Color(0xFFFCD34D),
            danger = Color(0xFFFB7185),
            actionGradient = listOf(palette.primary, palette.glacier),
            heroGradient = listOf(
                palette.glacier.copy(alpha = 0.55f),
                palette.backgroundElevated
            )
        )
    }
}
