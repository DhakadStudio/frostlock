package com.frostlock.app.ui.screens.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.frostlock.app.ui.components.charts.ChartBar
import com.frostlock.app.ui.components.charts.MetricValue
import com.frostlock.app.ui.components.charts.MonthBarChart
import com.frostlock.app.ui.components.charts.SubjectBreakdownList
import com.frostlock.app.ui.components.charts.SubjectMixBar
import com.frostlock.app.ui.components.charts.WeeklyBarChart
import com.frostlock.app.ui.components.controls.FrostSegmentedControl
import com.frostlock.app.ui.components.surface.FrostCard
import com.frostlock.app.ui.shell.FrostDetailScaffold
import com.frostlock.app.ui.theme.FrostTheme
import com.frostlock.app.ui.util.formatDuration
import com.frostlock.app.ui.util.formatDurationOrZero
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Trends: the patterns behind focus time and subjects.
 *
 * It owns period totals and the full subject breakdown. Today, streak and
 * badges stay on the dashboard, so this page never repeats them.
 */
@Composable
fun TrendsScreen(
    onBack: () -> Unit,
    viewModel: TrendsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    FrostDetailScaffold(title = "Trends", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = FrostTheme.spacing.screenHorizontal,
                    end = FrostTheme.spacing.screenHorizontal,
                    top = FrostTheme.spacing.sm,
                    bottom = FrostTheme.spacing.huge
                ),
            verticalArrangement = Arrangement.spacedBy(FrostTheme.spacing.lg)
        ) {
            Text(
                text = "Patterns behind your focus.",
                style = FrostTheme.typography.bodyMedium,
                color = FrostTheme.colors.textSecondary
            )

            if (!state.loading && !state.hasHistory) {
                FrostCard {
                    Text(
                        text = "Your trends start with your first session.",
                        style = FrostTheme.typography.titleMedium,
                        color = FrostTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))
                    Text(
                        text = "Focus once and this page will show your rhythm, " +
                            "subjects and best days. Everything stays on this device.",
                        style = FrostTheme.typography.bodyMedium,
                        color = FrostTheme.colors.textSecondary
                    )
                }
                OfflineNote()
                return@Column
            }

            FrostSegmentedControl(
                options = TrendsPeriod.entries.toList(),
                selected = state.period,
                onSelect = viewModel::selectPeriod,
                label = { it.label }
            )

            FrostCard {
                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricValue(
                        value = formatDurationOrZero(state.totalSeconds),
                        label = "Total",
                        modifier = Modifier.weight(1f)
                    )
                    MetricValue(
                        value = state.activeDays.toString(),
                        label = "Active days",
                        modifier = Modifier.weight(1f)
                    )
                    MetricValue(
                        value = state.sessions.toString(),
                        label = "Sessions",
                        modifier = Modifier.weight(1f)
                    )
                    MetricValue(
                        value = formatDurationOrZero(state.averagePerActiveDaySeconds),
                        label = "Avg / day",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            FrostCard {
                Text(
                    text = when (state.period) {
                        TrendsPeriod.WEEK -> "Last 7 days"
                        TrendsPeriod.MONTH -> "Last 30 days"
                        TrendsPeriod.SIX_MONTHS -> "Last 6 months"
                    },
                    style = FrostTheme.typography.titleLarge,
                    color = FrostTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(FrostTheme.spacing.lg))

                if (state.period == TrendsPeriod.SIX_MONTHS) {
                    MonthBarChart(months = state.months)
                    Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))
                    Text(
                        text = "Tap a month on the calendar to browse it.",
                        style = FrostTheme.typography.bodySmall,
                        color = FrostTheme.colors.textSecondary
                    )
                } else {
                    WeeklyBarChart(
                        bars = state.days.map { day ->
                            ChartBar(label = day.label, seconds = day.seconds, highlighted = day.highlighted)
                        },
                        height = 150.dp
                    )
                    if (state.period == TrendsPeriod.MONTH) {
                        Spacer(modifier = Modifier.height(FrostTheme.spacing.xs))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "30 days ago",
                                style = FrostTheme.typography.bodySmall,
                                color = FrostTheme.colors.textSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Today",
                                style = FrostTheme.typography.bodySmall,
                                color = FrostTheme.colors.textSecondary
                            )
                        }
                    }
                }

                if (state.isEmptyPeriod) {
                    Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))
                    Text(
                        text = emptyPeriodCopy(state.period, state.lastFocusDate),
                        style = FrostTheme.typography.bodyMedium,
                        color = FrostTheme.colors.secondary
                    )
                }
            }

            if (state.subjects.isNotEmpty()) {
                FrostCard {
                    Text(
                        text = "By subject",
                        style = FrostTheme.typography.titleLarge,
                        color = FrostTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(FrostTheme.spacing.md))
                    SubjectMixBar(subjects = state.subjects)
                    Spacer(modifier = Modifier.height(FrostTheme.spacing.md))
                    SubjectBreakdownList(subjects = state.subjects)
                    if (state.subjects.size == 1 && state.subjects.first().tag == "Untagged") {
                        Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))
                        Text(
                            text = "Tag sessions to compare subjects.",
                            style = FrostTheme.typography.bodySmall,
                            color = FrostTheme.colors.textSecondary
                        )
                    }
                }
            }

            BestPeriodCard(state)

            OfflineNote()
        }
    }
}

@Composable
private fun BestPeriodCard(state: TrendsUiState) {
    if (state.period == TrendsPeriod.SIX_MONTHS) {
        val bestMonth = state.months.filter { it.hasFocus }.maxByOrNull { it.seconds } ?: return
        FrostCard {
            Text(
                text = "Best month",
                style = FrostTheme.typography.labelMedium,
                color = FrostTheme.colors.secondary
            )
            Spacer(modifier = Modifier.height(FrostTheme.spacing.xs))
            Text(
                text = bestMonth.month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)),
                style = FrostTheme.typography.titleLarge,
                color = FrostTheme.colors.textPrimary
            )
            Text(
                text = "${formatDuration(bestMonth.seconds)} · " +
                    "${bestMonth.sessions} ${if (bestMonth.sessions == 1) "session" else "sessions"}",
                style = FrostTheme.typography.bodyMedium,
                color = FrostTheme.colors.textSecondary
            )
        }
        return
    }

    val bestDay = state.bestDay ?: return
    FrostCard {
        Text(
            text = "Best day",
            style = FrostTheme.typography.labelMedium,
            color = FrostTheme.colors.secondary
        )
        Spacer(modifier = Modifier.height(FrostTheme.spacing.xs))
        Text(
            text = bestDay.date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.US)),
            style = FrostTheme.typography.titleLarge,
            color = FrostTheme.colors.textPrimary
        )
        Text(
            text = "${formatDuration(bestDay.seconds)} · " +
                "${bestDay.sessions} ${if (bestDay.sessions == 1) "session" else "sessions"}",
            style = FrostTheme.typography.bodyMedium,
            color = FrostTheme.colors.textSecondary
        )
    }
}

@Composable
private fun OfflineNote() {
    Text(
        text = "All insights are calculated on this device.",
        style = FrostTheme.typography.bodySmall,
        color = FrostTheme.colors.textSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun emptyPeriodCopy(period: TrendsPeriod, lastFocus: LocalDate?): String {
    val label = when (period) {
        TrendsPeriod.WEEK -> "the last 7 days"
        TrendsPeriod.MONTH -> "the last 30 days"
        TrendsPeriod.SIX_MONTHS -> "the last 6 months"
    }
    if (lastFocus == null) {
        return "Your monthly rhythm will appear after your first session."
    }
    val daysAgo = ChronoUnit.DAYS.between(lastFocus, LocalDate.now()).toInt()
    val ago = when (daysAgo) {
        1 -> "yesterday"
        else -> "$daysAgo days ago"
    }
    return "No focus in $label. Last focus $ago."
}
