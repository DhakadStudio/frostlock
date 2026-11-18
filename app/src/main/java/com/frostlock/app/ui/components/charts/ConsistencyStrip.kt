package com.frostlock.app.ui.components.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.frostlock.app.insights.DayFocus
import com.frostlock.app.ui.util.focusIntensity

/**
 * Twelve weeks of focus as a compact grid.
 *
 * Columns are weeks (oldest first) and rows are Monday–Sunday. It is a different
 * period from the month calendar on purpose: one answers "am I consistent?",
 * the other "what happened this month?".
 */
@Composable
fun ConsistencyStrip(
    days: List<DayFocus>,
    modifier: Modifier = Modifier,
    weeks: Int = 12,
    cellSize: androidx.compose.ui.unit.Dp = 12.dp
) {
    val activeDays = days.count { it.hasFocus }
    val cellShape = RoundedCornerShape(3.dp)

    Row(
        modifier = modifier.semantics {
            contentDescription = "$activeDays active days in the last $weeks weeks"
        },
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        (0 until weeks).forEach { week ->
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                (0 until 7).forEach { weekday ->
                    val day = days.getOrNull(week * 7 + weekday)
                    val level = day?.let { focusIntensity(it.seconds) } ?: 0
                    Box(
                        modifier = Modifier
                            .size(cellSize)
                            .clip(cellShape)
                            .background(chartCellColor(level))
                    )
                }
            }
        }
    }
}
