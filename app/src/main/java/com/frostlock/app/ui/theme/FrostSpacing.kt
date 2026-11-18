package com.frostlock.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing tokens. Screens use the semantic helpers, not raw numbers, so padding
 * stays consistent when the layout adapts.
 */
object FrostSpacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val xxxl: Dp = 32.dp
    val huge: Dp = 48.dp

    /** Horizontal screen padding at compact width. */
    val screenHorizontal: Dp = xl

    /** Gap between major sections. */
    val sectionGap: Dp = xxl

    /** Gap between rows in a list. */
    val listGap: Dp = sm

    /** Content cap so lines stay readable on tablets. */
    val contentMaxWidth: Dp = 720.dp

    /** Focus stays a centred column even on wide screens. */
    val focusMaxWidth: Dp = 560.dp
}
