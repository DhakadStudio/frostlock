package com.frostlock.app.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.frostlock.app.data.local.db.SessionEntity
import com.frostlock.app.ui.components.AppCard
import com.frostlock.app.ui.components.SessionRow
import com.frostlock.app.ui.theme.AccentFrost
import com.frostlock.app.ui.theme.BackgroundDark
import com.frostlock.app.ui.theme.InkOnIce
import com.frostlock.app.ui.theme.PrimaryIce
import com.frostlock.app.ui.theme.StrokeSoft
import com.frostlock.app.ui.theme.SurfaceHigh
import com.frostlock.app.ui.theme.TextPrimary
import com.frostlock.app.ui.theme.TextSecondary
import com.frostlock.app.ui.util.CalendarDay
import com.frostlock.app.ui.util.FocusMonth
import com.frostlock.app.ui.util.formatDuration
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val WEEKDAY_LABELS = listOf("M", "T", "W", "T", "F", "S", "S")

/**
 * A month heat-map of focus time. Tapping any day shows its sessions below, so
 * old work is browsable without leaving the local database.
 */
@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val calendar = state.calendar

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Focus calendar",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Every day you froze out the noise.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        item {
            MonthCard(
                month = state.month,
                calendar = calendar,
                selected = state.selected,
                canGoNext = state.canGoNext,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth,
                onSelect = viewModel::select
            )
        }

        item {
            DayHeader(
                date = state.selected,
                sessions = state.daySessions,
                isBestDay = calendar.bestDay?.date == state.selected
            )
        }

        if (state.daySessions.isEmpty()) {
            item {
                AppCard {
                    Text(
                        text = "No focus sessions on this day. Pick another day, or start a session to fill the grid.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            items(state.daySessions, key = { it.id }) { session ->
                SessionRow(session = session)
            }
        }

        item { Spacer(modifier = Modifier.height(4.dp)) }
    }
}

@Composable
private fun MonthCard(
    month: YearMonth,
    calendar: FocusMonth,
    selected: LocalDate,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelect: (LocalDate) -> Unit
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous month",
                        tint = AccentFrost
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = month.format(
                            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "${formatDuration(calendar.totalSeconds)} · " +
                            "${calendar.activeDays} ${plural(calendar.activeDays, "active day", "active days")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onNext, enabled = canGoNext) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next month",
                        tint = if (canGoNext) AccentFrost else TextSecondary.copy(alpha = 0.35f)
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                WEEKDAY_LABELS.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            calendar.days.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Less",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                (0..4).forEach { level ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(cellFill(level))
                    )
                }
                Text(
                    text = "More",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${calendar.sessions} sessions",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentFrost
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(StrokeSoft.copy(alpha = 0.6f))
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                MiniStat(
                    value = formatDuration(calendar.totalSeconds),
                    label = "This month",
                    modifier = Modifier.weight(1f)
                )
                MiniStat(
                    value = calendar.activeDays.toString(),
                    label = "Active days",
                    modifier = Modifier.weight(1f)
                )
                MiniStat(
                    value = calendar.bestDay?.date?.format(
                        DateTimeFormatter.ofPattern("d MMM", Locale.US)
                    ) ?: "—",
                    label = "Best day",
                    modifier = Modifier.weight(1f)
                )
            }
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
    val shape = RoundedCornerShape(12.dp)
    val borderColor = when {
        selected -> AccentFrost
        day.isToday -> PrimaryIce.copy(alpha = 0.7f)
        else -> Color.Transparent
    }
    val textColor = when {
        !day.inMonth -> TextSecondary.copy(alpha = 0.4f)
        day.level >= 3 -> InkOnIce
        else -> TextPrimary
    }

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(cellFill(day.level))
            .border(if (selected) 2.dp else 1.dp, borderColor, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (day.isToday || selected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
            if (day.hasFocus) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (day.level >= 3) InkOnIce else AccentFrost)
                )
            }
        }
    }
}

@Composable
private fun DayHeader(date: LocalDate, sessions: List<SessionEntity>, isBestDay: Boolean) {
    val total = sessions.sumOf { it.durationSeconds.toLong() }
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = AccentFrost,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = date.format(
                        DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.US)
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = if (sessions.isEmpty()) {
                        "Nothing logged yet"
                    } else {
                        "${formatDuration(total)} focused · ${sessions.size} " +
                            plural(sessions.size, "session", "sessions")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            if (isBestDay) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(AccentFrost.copy(alpha = 0.14f))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = AccentFrost,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Best",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentFrost
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

private fun cellFill(level: Int): Color = when (level) {
    0 -> SurfaceHigh.copy(alpha = 0.35f)
    1 -> PrimaryIce.copy(alpha = 0.16f)
    2 -> PrimaryIce.copy(alpha = 0.30f)
    3 -> PrimaryIce.copy(alpha = 0.52f)
    else -> PrimaryIce.copy(alpha = 0.80f)
}

private fun plural(count: Int, one: String, many: String): String =
    if (count == 1) one else many
