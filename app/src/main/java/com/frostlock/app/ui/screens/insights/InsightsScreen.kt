package com.frostlock.app.ui.screens.insights

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.frostlock.app.insights.DayFocus
import com.frostlock.app.insights.PeriodFocus
import com.frostlock.app.ui.components.charts.ChartBar
import com.frostlock.app.ui.components.charts.ConsistencyStrip
import com.frostlock.app.ui.components.charts.DeltaChip
import com.frostlock.app.ui.components.charts.GoalRing
import com.frostlock.app.ui.components.charts.StreakChip
import com.frostlock.app.ui.components.charts.SubjectBreakdownList
import com.frostlock.app.ui.components.charts.SubjectMixBar
import com.frostlock.app.ui.components.charts.WeeklyBarChart
import com.frostlock.app.ui.components.controls.FrostButton
import com.frostlock.app.ui.components.session.FrostSessionRow
import com.frostlock.app.ui.components.surface.FrostCard
import com.frostlock.app.ui.components.surface.HeroCard
import com.frostlock.app.ui.shell.FrostTabScaffold
import com.frostlock.app.ui.theme.FrostShapes
import com.frostlock.app.ui.theme.FrostTheme
import com.frostlock.app.ui.util.Achievement
import com.frostlock.app.ui.util.FocusLevel
import com.frostlock.app.ui.util.formatDuration
import com.frostlock.app.ui.util.formatDurationOrZero
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Insights: how focus is going now and whether this week is working.
 *
 * It merges the old Home and Stats pages into one dashboard. Detail screens own
 * their own periods; this page only routes to them.
 */
@Composable
fun InsightsScreen(
    onOpenTrends: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenWeekly: () -> Unit,
    onOpenReflections: () -> Unit,
    onOpenBadges: () -> Unit,
    onOpenLevels: () -> Unit,
    onStartFocus: () -> Unit,
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    FrostTabScaffold(
        title = "Insights",
        subtitle = "Your focus, in perspective."
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = FrostTheme.spacing.screenHorizontal,
                end = FrostTheme.spacing.screenHorizontal,
                top = FrostTheme.spacing.md,
                bottom = FrostTheme.spacing.huge
            ),
            verticalArrangement = Arrangement.spacedBy(FrostTheme.spacing.lg)
        ) {
            when {
                state.loading -> {
                    item { LoadingCard() }
                    item { LoadingCard() }
                }
                state.error -> item {
                    FrostCard {
                        Text(
                            text = "Couldn't read your local history.",
                            style = FrostTheme.typography.titleMedium,
                            color = FrostTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))
                        FrostButton(
                            text = "Try again",
                            onClick = viewModel::retry,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                !state.hasHistory -> item {
                    BaselineCard(onStartFocus = onStartFocus)
                }
                else -> {
                    item { TodayHero(state = state, onStartFocus = onStartFocus) }
                    state.week?.let { week ->
                        item {
                            ThisWeekCard(
                                week = week,
                                deltaPercent = state.weekDeltaPercent,
                                onOpenWeekly = onOpenWeekly
                            )
                        }
                    }
                    if (state.subjects.isNotEmpty() && (state.week?.seconds ?: 0L) > 0L) {
                        item {
                            FocusMixCard(
                                subjects = state.subjects,
                                onOpenTrends = onOpenTrends
                            )
                        }
                    }
                    if (state.consistencyActiveDays > 0) {
                        item {
                            ConsistencyCard(
                                state = state,
                                onOpenHistory = onOpenHistory
                            )
                        }
                    }
                    item {
                        MomentumRow(
                            level = state.level,
                            nextBadge = state.nextBadge,
                            unlocked = state.unlockedBadges,
                            total = state.totalBadges,
                            onOpenLevels = onOpenLevels,
                            onOpenBadges = onOpenBadges
                        )
                    }
                    item {
                        RecentSessions(
                            state = state,
                            onOpenReflections = onOpenReflections,
                            onStartFocus = onStartFocus
                        )
                    }
                }
            }

            item {
                Text(
                    text = "All insights are calculated on this device.",
                    style = FrostTheme.typography.bodySmall,
                    color = FrostTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = FrostTheme.spacing.md)
                )
            }
        }
    }
}

@Composable
private fun TodayHero(state: InsightsUiState, onStartFocus: () -> Unit) {
    HeroCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "TODAY",
                style = FrostTheme.typography.labelMedium,
                color = FrostTheme.colors.textSecondary,
                modifier = Modifier.weight(1f)
            )
            StreakChip(days = state.currentStreak)
        }
        Spacer(modifier = Modifier.height(FrostTheme.spacing.md))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            GoalRing(
                progress = state.goalProgress,
                diameter = 156.dp,
                contentDescription = buildString {
                    append("Today ")
                    append(formatDurationOrZero(state.today.seconds))
                    append(" of a ")
                    append(formatDuration(state.goalSeconds))
                    append(" goal")
                }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatDurationOrZero(state.today.seconds),
                        style = FrostTheme.typography.displayLarge,
                        color = FrostTheme.colors.textPrimary
                    )
                    Text(
                        text = "of ${formatDuration(state.goalSeconds)}",
                        style = FrostTheme.typography.bodySmall,
                        color = FrostTheme.colors.textSecondary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(FrostTheme.spacing.md))
        Text(
            text = when {
                state.goalReached ->
                    "Goal reached · ${sessionCount(state.today.sessions)} today"
                state.today.seconds == 0L && state.currentStreak > 0 ->
                    "Start a session to keep your ${state.currentStreak}-day streak."
                state.today.seconds == 0L ->
                    "Start a session to begin today's focus."
                else ->
                    "${sessionCount(state.today.sessions)} today · ${formatDuration(state.goalSeconds)} goal"
            },
            style = FrostTheme.typography.bodyMedium,
            color = FrostTheme.colors.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (state.today.seconds == 0L) {
            Spacer(modifier = Modifier.height(FrostTheme.spacing.md))
            FrostButton(
                text = "Start focus",
                onClick = onStartFocus,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ThisWeekCard(
    week: PeriodFocus,
    deltaPercent: Int?,
    onOpenWeekly: () -> Unit
) {
    var selected by rememberSaveable(week.start) { mutableIntStateOf(-1) }
    val bars = week.days.map { day ->
        ChartBar(
            label = day.date.format(DateTimeFormatter.ofPattern("EEE", Locale.US)).take(1),
            seconds = day.seconds,
            highlighted = day.date == LocalDate.now()
        )
    }
    val selectedDay: DayFocus? = week.days.getOrNull(selected)

    FrostCard(onClick = onOpenWeekly) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "This week",
                style = FrostTheme.typography.titleLarge,
                color = FrostTheme.colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = formatDurationOrZero(week.seconds),
                style = FrostTheme.typography.titleLarge,
                color = FrostTheme.colors.secondary
            )
        }
        Spacer(modifier = Modifier.height(FrostTheme.spacing.xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = weekRange(week),
                style = FrostTheme.typography.bodySmall,
                color = FrostTheme.colors.textSecondary,
                modifier = Modifier.weight(1f)
            )
            DeltaChip(percent = deltaPercent)
        }
        Spacer(modifier = Modifier.height(FrostTheme.spacing.lg))
        WeeklyBarChart(
            bars = bars,
            selectedIndex = selected.takeIf { it >= 0 },
            onSelect = { selected = it }
        )
        Spacer(modifier = Modifier.height(FrostTheme.spacing.md))
        Text(
            text = if (selectedDay != null) {
                "${selectedDay.date.format(DateTimeFormatter.ofPattern("EEEE", Locale.US))} · " +
                    "${formatDuration(selectedDay.seconds)} · ${sessionCount(selectedDay.sessions)}"
            } else if (week.seconds == 0L) {
                "No focus this week yet."
            } else {
                "Tap a bar to inspect a day."
            },
            style = FrostTheme.typography.bodyMedium,
            color = FrostTheme.colors.textSecondary
        )
        TextButton(
            onClick = onOpenWeekly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Weekly review",
                style = FrostTheme.typography.labelLarge,
                color = FrostTheme.colors.secondary
            )
        }
    }
}

@Composable
private fun FocusMixCard(
    subjects: List<com.frostlock.app.insights.SubjectFocus>,
    onOpenTrends: () -> Unit
) {
    FrostCard(onClick = onOpenTrends) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Focus mix",
                style = FrostTheme.typography.titleLarge,
                color = FrostTheme.colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "See all",
                style = FrostTheme.typography.labelLarge,
                color = FrostTheme.colors.secondary
            )
        }
        Spacer(modifier = Modifier.height(FrostTheme.spacing.md))
        SubjectMixBar(subjects = subjects)
        Spacer(modifier = Modifier.height(FrostTheme.spacing.md))
        SubjectBreakdownList(
            subjects = subjects,
            showDurations = false,
            maxRows = 4
        )
        if (subjects.size == 1 && subjects.first().tag == "Untagged") {
            Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))
            Text(
                text = "Tag sessions to see your mix.",
                style = FrostTheme.typography.bodySmall,
                color = FrostTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun ConsistencyCard(state: InsightsUiState, onOpenHistory: () -> Unit) {
    FrostCard(onClick = onOpenHistory) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = FrostTheme.colors.secondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(FrostTheme.spacing.sm))
            Text(
                text = "Consistency",
                style = FrostTheme.typography.titleLarge,
                color = FrostTheme.colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(FrostTheme.spacing.xs))
        Text(
            text = "${state.consistencyActiveDays} active days · last 12 weeks",
            style = FrostTheme.typography.bodySmall,
            color = FrostTheme.colors.textSecondary
        )
        Spacer(modifier = Modifier.height(FrostTheme.spacing.md))
        ConsistencyStrip(days = state.consistency)
    }
}

@Composable
private fun MomentumRow(
    level: FocusLevel,
    nextBadge: Achievement?,
    unlocked: Int,
    total: Int,
    onOpenLevels: () -> Unit,
    onOpenBadges: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(FrostTheme.spacing.md)) {
        FrostCard(
            modifier = Modifier.weight(1f),
            onClick = onOpenLevels,
            contentPadding = PaddingValues(FrostTheme.spacing.lg)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AcUnit,
                    contentDescription = null,
                    tint = FrostTheme.colors.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(FrostTheme.spacing.xs))
                Text(
                    text = "LEVEL",
                    style = FrostTheme.typography.labelMedium,
                    color = FrostTheme.colors.textSecondary
                )
            }
            Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))
            Text(
                text = "${level.level} · ${level.title}",
                style = FrostTheme.typography.titleMedium,
                color = FrostTheme.colors.textPrimary,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))
            LinearProgressIndicator(
                progress = { level.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(FrostShapes.pill),
                color = FrostTheme.colors.primary,
                trackColor = FrostTheme.colors.surfaceContainerHigh
            )
            Spacer(modifier = Modifier.height(FrostTheme.spacing.xs))
            Text(
                text = "${formatDuration(level.minutesToNextLevel * 60L)} to level ${level.level + 1}",
                style = FrostTheme.typography.bodySmall,
                color = FrostTheme.colors.textSecondary
            )
        }

        FrostCard(
            modifier = Modifier.weight(1f),
            onClick = onOpenBadges,
            contentPadding = PaddingValues(FrostTheme.spacing.lg)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = FrostTheme.colors.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(FrostTheme.spacing.xs))
                Text(
                    text = "NEXT BADGE",
                    style = FrostTheme.typography.labelMedium,
                    color = FrostTheme.colors.textSecondary
                )
            }
            Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))
            Text(
                text = nextBadge?.title ?: "All badges earned",
                style = FrostTheme.typography.titleMedium,
                color = FrostTheme.colors.textPrimary,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))
            if (nextBadge != null) {
                LinearProgressIndicator(
                    progress = { nextBadge.progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(FrostShapes.pill),
                    color = FrostTheme.colors.primary,
                    trackColor = FrostTheme.colors.surfaceContainerHigh
                )
                Spacer(modifier = Modifier.height(FrostTheme.spacing.xs))
                Text(
                    text = nextBadge.progressLabel,
                    style = FrostTheme.typography.bodySmall,
                    color = FrostTheme.colors.textSecondary
                )
            } else {
                Text(
                    text = "$unlocked of $total",
                    style = FrostTheme.typography.bodySmall,
                    color = FrostTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun RecentSessions(
    state: InsightsUiState,
    onOpenReflections: () -> Unit,
    onStartFocus: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(FrostTheme.spacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Recent sessions",
                style = FrostTheme.typography.titleLarge,
                color = FrostTheme.colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onOpenReflections) {
                Text(
                    text = "Reflections",
                    style = FrostTheme.typography.labelLarge,
                    color = FrostTheme.colors.secondary
                )
            }
        }
        if (state.recent.isEmpty()) {
            FrostCard {
                Text(
                    text = "No sessions yet. Your first one will appear here.",
                    style = FrostTheme.typography.bodyMedium,
                    color = FrostTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(FrostTheme.spacing.md))
                FrostButton(
                    text = "Start focus",
                    onClick = onStartFocus,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            state.recent.forEach { session ->
                FrostSessionRow(session = session)
            }
        }
    }
}

@Composable
private fun BaselineCard(onStartFocus: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(FrostTheme.spacing.lg)) {
        FrostCard {
            Text(
                text = "YOUR BASELINE",
                style = FrostTheme.typography.labelMedium,
                color = FrostTheme.colors.secondary
            )
            Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))
            Text(
                text = "Your baseline starts today",
                style = FrostTheme.typography.headlineMedium,
                color = FrostTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))
            Text(
                text = "Focus for 7 days and FrostLock will show how your focus " +
                    "time, sessions and streak improve. Everything stays on this device.",
                style = FrostTheme.typography.bodyMedium,
                color = FrostTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(FrostTheme.spacing.lg))
            FrostButton(
                text = "Start your first session",
                onClick = onStartFocus,
                modifier = Modifier.fillMaxWidth()
            )
        }
        FrostCard {
            Text(
                text = "What you'll see",
                style = FrostTheme.typography.titleMedium,
                color = FrostTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(FrostTheme.spacing.md))
            listOf(
                "Today's focus and goal",
                "Your weekly rhythm",
                "A calendar of every focus day"
            ).forEach { line ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AcUnit,
                        contentDescription = null,
                        tint = FrostTheme.colors.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(FrostTheme.spacing.sm))
                    Text(
                        text = line,
                        style = FrostTheme.typography.bodyMedium,
                        color = FrostTheme.colors.textSecondary
                    )
                }
                Spacer(modifier = Modifier.height(FrostTheme.spacing.xs))
            }
        }
    }
}

@Composable
private fun LoadingCard() {
    FrostCard {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )
    }
}

private fun sessionCount(count: Int): String =
    "$count ${if (count == 1) "session" else "sessions"}"

private fun weekRange(week: PeriodFocus): String {
    val formatter = DateTimeFormatter.ofPattern("d MMM", Locale.US)
    return "${week.start.format(formatter)} – ${week.end.format(formatter)}"
}
