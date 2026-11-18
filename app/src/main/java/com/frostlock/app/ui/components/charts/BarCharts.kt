package com.frostlock.app.ui.components.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frostlock.app.insights.MonthFocus
import com.frostlock.app.ui.theme.FrostTheme
import com.frostlock.app.ui.util.formatDuration
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One bar in [WeeklyBarChart]. */
data class ChartBar(
    val label: String,
    val seconds: Long,
    val highlighted: Boolean = false,
    val isFuture: Boolean = false
)

/**
 * Seven daily bars, Monday-first.
 *
 * A day with no focus keeps a 2dp baseline track, so the chart never collapses
 * into invisible gaps. The highlighted bar is today.
 */
@Composable
fun WeeklyBarChart(
    bars: List<ChartBar>,
    modifier: Modifier = Modifier,
    height: Dp = 132.dp,
    selectedIndex: Int? = null,
    onSelect: ((Int) -> Unit)? = null
) {
    BarChart(
        bars = bars,
        modifier = modifier,
        height = height,
        selectedIndex = selectedIndex,
        onSelect = onSelect
    )
}

/** Six monthly bars, current month highlighted. */
@Composable
fun MonthBarChart(
    months: List<MonthFocus>,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
    onSelect: ((MonthFocus) -> Unit)? = null
) {
    val formatter = DateTimeFormatter.ofPattern("MMM", Locale.US)
    val bars = months.map { month ->
        ChartBar(
            label = month.month.format(formatter),
            seconds = month.seconds,
            highlighted = month == months.last()
        )
    }
    BarChart(
        bars = bars,
        modifier = modifier,
        height = height,
        selectedIndex = null,
        onSelect = onSelect?.let { callback -> { index -> callback(months[index]) } }
    )
}

@Composable
private fun BarChart(
    bars: List<ChartBar>,
    modifier: Modifier,
    height: Dp,
    selectedIndex: Int?,
    onSelect: ((Int) -> Unit)?
) {
    val colors = FrostTheme.colors
    val maxSeconds = bars.maxOfOrNull { it.seconds }?.coerceAtLeast(1L) ?: 1L
    val barShape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(FrostTheme.spacing.xs),
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEachIndexed { index, bar ->
            val selected = selectedIndex == index
            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (onSelect != null) {
                            Modifier.clickable(
                                role = Role.Button,
                                onClickLabel = "Show ${bar.label}",
                                onClick = { onSelect(index) }
                            )
                        } else {
                            Modifier
                        }
                    )
                    .semantics(mergeDescendants = true) {
                        contentDescription =
                            "${bar.label}, ${formatDuration(bar.seconds)}"
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .clip(barShape)
                            .background(colors.surfaceContainerHigh.copy(alpha = 0.30f))
                    )
                    val fraction = (bar.seconds.toFloat() / maxSeconds).coerceIn(0f, 1f)
                    if (fraction > 0f && !bar.isFuture) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction)
                                .clip(barShape)
                                .background(
                                    brush = if (bar.highlighted) {
                                        Brush.verticalGradient(colors.actionGradient)
                                    } else {
                                        Brush.verticalGradient(
                                            listOf(
                                                colors.primary.copy(alpha = 0.72f),
                                                colors.primary.copy(alpha = 0.45f)
                                            )
                                        )
                                    }
                                )
                        )
                    } else if (!bar.isFuture) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .clip(barShape)
                                .background(colors.surfaceContainerHigh)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(FrostTheme.spacing.xs))
                Text(
                    text = bar.label,
                    style = FrostTheme.typography.labelMedium,
                    color = when {
                        selected -> colors.primary
                        bar.highlighted -> colors.textPrimary
                        else -> colors.textSecondary
                    },
                    fontWeight = if (selected || bar.highlighted) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}
