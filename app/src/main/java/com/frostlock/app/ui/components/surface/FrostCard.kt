package com.frostlock.app.ui.components.surface

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
 * All three variants share the frosted-glass treatment: a translucent fill that
 * lets the page wash read through, and a hairline edge that is bright on top
 * and fades down. The variants only change how much light the card catches.
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
    val strong = variant == FrostCardVariant.ELEVATED

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .frostGlass(shape = shape, strong = strong)
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = shape,
        // The glass modifier paints the surface; Surface only owns the shape
        // clip, content colour and click semantics.
        color = Color.Transparent,
        contentColor = colors.textPrimary,
        shadowElevation = if (strong) 12.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}
