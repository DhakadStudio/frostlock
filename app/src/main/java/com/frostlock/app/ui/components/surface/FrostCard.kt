package com.frostlock.app.ui.components.surface

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.frostlock.app.ui.theme.FrostTheme

/** Visual weight of a [FrostCard]. */
enum class FrostCardVariant {
    /** Standard card on the app background. */
    FILLED,

    /** Card with a one-pixel outline and no fill contrast. */
    OUTLINED,

    /** Raised card for sheets and emphasis. */
    ELEVATED
}

/**
 * Standard content card.
 *
 * Defaults to [FrostCardVariant.OUTLINED] so lists read as one calm surface
 * rather than a stack of competing blocks.
 */
@Composable
fun FrostCard(
    modifier: Modifier = Modifier,
    variant: FrostCardVariant = FrostCardVariant.OUTLINED,
    shape: Shape = FrostTheme.shapes.large,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(FrostTheme.spacing.lg),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = FrostTheme.colors
    val container = when (variant) {
        FrostCardVariant.FILLED -> colors.surfaceContainer
        FrostCardVariant.OUTLINED -> colors.surfaceContainerLow
        FrostCardVariant.ELEVATED -> colors.surfaceContainerHigh
    }
    val border = if (variant == FrostCardVariant.OUTLINED) {
        BorderStroke(1.dp, colors.outline)
    } else {
        null
    }
    val elevation = if (variant == FrostCardVariant.ELEVATED) 2.dp else 0.dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = shape,
        color = container,
        contentColor = colors.textPrimary,
        border = border,
        tonalElevation = elevation,
        shadowElevation = elevation
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}
