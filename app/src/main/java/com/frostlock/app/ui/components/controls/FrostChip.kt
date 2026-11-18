package com.frostlock.app.ui.components.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.frostlock.app.ui.theme.FrostShapes
import com.frostlock.app.ui.theme.FrostTheme

/**
 * One selectable pill.
 *
 * Used for timer lengths, subjects and short settings choices. Selection changes
 * the fill, not only the text colour, so it reads without relying on hue.
 */
@Composable
fun FrostChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    dotColor: Color? = null
) {
    val colors = FrostTheme.colors
    Row(
        modifier = modifier
            .clip(FrostShapes.pill)
            .background(if (selected) colors.primary else colors.surfaceContainerHigh)
            .border(
                width = 1.dp,
                color = if (selected) colors.primary else colors.outline,
                shape = FrostShapes.pill
            )
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = FrostTheme.spacing.md, vertical = FrostTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dotColor != null) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(FrostTheme.spacing.xs))
        }
        Text(
            text = text,
            style = FrostTheme.typography.labelLarge,
            color = if (selected) colors.onPrimary else colors.textSecondary,
            maxLines = 1
        )
    }
}
