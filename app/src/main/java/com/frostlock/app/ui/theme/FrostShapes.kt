package com.frostlock.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape tokens for the redesign.
 *
 * Kept separate from [AppShapes] because the legacy Material shapes use
 * `extraLarge` as a pill, so changing it would turn every existing card into a
 * capsule. New components use these tokens directly.
 */
object FrostShapes {
    val extraSmall = RoundedCornerShape(8.dp)
    val small = RoundedCornerShape(12.dp)
    val medium = RoundedCornerShape(16.dp)
    val large = RoundedCornerShape(20.dp)
    val extraLarge = RoundedCornerShape(28.dp)
    val hero = RoundedCornerShape(36.dp)
    val heroLarge = RoundedCornerShape(48.dp)
    val pill = RoundedCornerShape(100.dp)

    /** Sheets round only their top corners. */
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    /** Material slots mapped to the new scale for components that need them. */
    val material: Shapes = Shapes(
        extraSmall = extraSmall,
        small = small,
        medium = medium,
        large = large,
        extraLarge = extraLarge
    )
}
