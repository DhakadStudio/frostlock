package com.frostlock.app.ui.components.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frostlock.app.ui.theme.FrostShapes
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Week-over-week change.
 *
 * A missing baseline is never rendered as `+∞%`; it becomes "Fresh start".
 */
@Composable
fun DeltaChip(
    percent: Int?,
    modifier: Modifier = Modifier,
    freshStartLabel: String = "Fresh start"
) {
    val colors = FrostTheme.colors
    val (label, tint, icon) = when {
        percent == null -> Triple(freshStartLabel, colors.secondary, Icons.Filled.TrendingFlat)
        percent > 0 -> Triple("+$percent% vs last week", colors.success, Icons.Filled.TrendingUp)
        percent < 0 -> Triple("$percent% vs last week", colors.warning, Icons.Filled.TrendingDown)
        else -> Triple("Same as last week", colors.textSecondary, Icons.Filled.TrendingFlat)
    }

    Row(
        modifier = modifier
            .clip(FrostShapes.pill)
            .background(tint.copy(alpha = 0.14f))
            .padding(horizontal = FrostTheme.spacing.sm, vertical = FrostTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(FrostTheme.spacing.xs))
        Text(
            text = label,
            style = FrostTheme.typography.labelMedium,
            color = tint
        )
    }
}

/** Current streak as a chip, never as a second progress ring. */
@Composable
fun StreakChip(
    days: Int,
    modifier: Modifier = Modifier
) {
    if (days <= 0) return
    val colors = FrostTheme.colors
    Row(
        modifier = modifier
            .clip(FrostShapes.pill)
            .background(colors.secondary.copy(alpha = 0.14f))
            .padding(horizontal = FrostTheme.spacing.sm, vertical = FrostTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.AcUnit,
            contentDescription = null,
            tint = colors.secondary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(FrostTheme.spacing.xs))
        Text(
            text = "$days-day streak",
            style = FrostTheme.typography.labelMedium,
            color = colors.secondary
        )
    }
}

/**
 * One number with its label, used in a plain row instead of a grid of cards.
 *
 * Four of these in a row read as a summary, not four competing tiles.
 */
@Composable
fun MetricValue(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = FrostTheme.colors.textPrimary
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = FrostTheme.typography.titleLarge,
            color = valueColor,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = FrostTheme.typography.bodySmall,
            color = FrostTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/** Small colour-dot legend entry shared by the subject charts. */
@Composable
fun LegendDot(color: Color, modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}
