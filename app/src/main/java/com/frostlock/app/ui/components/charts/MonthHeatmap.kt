package com.frostlock.app.ui.components.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frostlock.app.ui.theme.FrostTheme
import com.frostlock.app.ui.util.CalendarDay
import com.frostlock.app.ui.util.FocusMonth
import com.frostlock.app.ui.util.formatDuration
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val WEEKDAY_LABELS = listOf("M", "T", "W", "T", "F", "S", "S")

/**
 * Month heat-map with month navigation and an intensity legend.
 *
 * The grid still renders for a month with no focus: a calendar is useful even
 * when it is empty, and an empty grid is honest rather than hidden.
 */
@Composable
fun MonthHeatmap(
    month: YearMonth,
    calendar: FocusMonth,
    selected: LocalDate,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FrostTheme.spacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous month",
                    tint = FrostTheme.colors.secondary
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)),
                    style = FrostTheme.typography.titleLarge,
                    color = FrostTheme.colors.textPrimary
                )
                Text(
                    text = "${formatDuration(calendar.totalSeconds)} · " +
                        "${calendar.activeDays} ${if (calendar.activeDays == 1) "active day" else "active days"}",
                    style = FrostTheme.typography.bodySmall,
                    color = FrostTheme.colors.textSecondary
                )
            }
            IconButton(onClick = onNext, enabled = canGoNext) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next month",
                    tint = if (canGoNext) {
                        FrostTheme.colors.secondary
                    } else {
                        FrostTheme.colors.textSecondary.copy(alpha = 0.35f)
                    }
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            WEEKDAY_LABELS.forEach { label ->
                Text(
                    text = label,
                    style = FrostTheme.typography.labelMedium,
                    color = FrostTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        calendar.days.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FrostTheme.spacing.xs)
            ) {
                week.forEach { day ->
                    DayCell(
                        day = day,
                        selected = day.date == selected,
                        onClick = { onSelect(day.date) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FrostTheme.spacing.xs)
        ) {
            Text(
                text = "Less",
                style = FrostTheme.typography.labelMedium,
                color = FrostTheme.colors.textSecondary
            )
            (0..4).forEach { level ->
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(chartCellColor(level))
                )
            }
            Text(
                text = "More",
                style = FrostTheme.typography.labelMedium,
                color = FrostTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${calendar.sessions} ${if (calendar.sessions == 1) "session" else "sessions"}",
                style = FrostTheme.typography.labelMedium,
                color = FrostTheme.colors.secondary
            )
        }
    }
}

@Composable
private fun DayCell(
    day: CalendarDay,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    val borderColor = when {
        selected -> FrostTheme.colors.secondary
        day.isToday -> FrostTheme.colors.primary.copy(alpha = 0.7f)
        else -> Color.Transparent
    }
    val textColor = when {
        !day.inMonth -> FrostTheme.colors.textSecondary.copy(alpha = 0.4f)
        else -> chartCellContentColor(day.level)
    }

    Box(
        modifier = modifier
            .height(42.dp)
            .clip(shape)
            .background(chartCellColor(day.level))
            .border(if (selected) 2.dp else 1.dp, borderColor, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                this.selected = selected
                contentDescription = buildString {
                    append(day.date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.US)))
                    append(", ")
                    append(
                        if (day.hasFocus) formatDuration(day.seconds) else "no focus"
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = FrostTheme.typography.bodyMedium,
                fontWeight = if (day.isToday || selected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
            if (day.hasFocus) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(chartCellContentColor(day.level))
                )
            }
        }
    }
}

/** Legend swatch row reused by other screens if they need the same ramp. */
@Composable
fun HeatmapLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FrostTheme.spacing.xs)
    ) {
        Text(
            text = "Less",
            style = FrostTheme.typography.labelMedium,
            color = FrostTheme.colors.textSecondary
        )
        (0..4).forEach { level ->
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(chartCellColor(level))
            )
        }
        Text(
            text = "More",
            style = FrostTheme.typography.labelMedium,
            color = FrostTheme.colors.textSecondary
        )
    }
}
