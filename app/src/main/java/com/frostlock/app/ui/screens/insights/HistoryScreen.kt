package com.frostlock.app.ui.screens.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.frostlock.app.data.local.db.SessionEntity
import com.frostlock.app.ui.components.charts.MonthHeatmap
import com.frostlock.app.ui.components.session.FrostSessionRow
import com.frostlock.app.ui.components.surface.FrostCard
import com.frostlock.app.ui.shell.FrostDetailScaffold
import com.frostlock.app.ui.theme.FrostTheme
import com.frostlock.app.ui.util.formatDuration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * History: when focus happened, and every session behind it.
 *
 * The calendar answers "which days?", the archive answers "what did I do?". The
 * month summary is allowed here because its scope is the month itself.
 */
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val calendar = state.calendar

    FrostDetailScaffold(title = "History", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = FrostTheme.spacing.screenHorizontal,
                end = FrostTheme.spacing.screenHorizontal,
                top = FrostTheme.spacing.sm,
                bottom = FrostTheme.spacing.huge
            ),
            verticalArrangement = Arrangement.spacedBy(FrostTheme.spacing.md)
        ) {
            item {
                Text(
                    text = "Every day you froze out the noise.",
                    style = FrostTheme.typography.bodyMedium,
                    color = FrostTheme.colors.textSecondary
                )
            }

            item {
                FrostCard {
                    MonthHeatmap(
                        month = state.month,
                        calendar = calendar,
                        selected = state.selected,
                        canGoNext = state.canGoNext,
                        onPrevious = viewModel::previousMonth,
                        onNext = viewModel::nextMonth,
                        onSelect = viewModel::select
                    )
                }
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
                    FrostCard {
                        Text(
                            text = if (state.hasHistory) {
                                "No focus on this day. Pick another day or start a session."
                            } else {
                                "Your first focus day will light up here."
                            },
                            style = FrostTheme.typography.bodyMedium,
                            color = FrostTheme.colors.textSecondary
                        )
                    }
                }
            } else {
                items(state.daySessions, key = { "day-${it.id}" }) { session ->
                    FrostSessionRow(session = session)
                }
            }

            item {
                Spacer(modifier = Modifier.padding(top = FrostTheme.spacing.md))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "All sessions",
                        style = FrostTheme.typography.titleLarge,
                        color = FrostTheme.colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${state.allSessions.size} · ${formatDuration(state.totalSeconds)}",
                        style = FrostTheme.typography.bodySmall,
                        color = FrostTheme.colors.textSecondary
                    )
                }
            }

            if (state.allSessions.isEmpty()) {
                item {
                    FrostCard {
                        Text(
                            text = "No sessions yet. Your first one will appear here.",
                            style = FrostTheme.typography.bodyMedium,
                            color = FrostTheme.colors.textSecondary
                        )
                    }
                }
            } else {
                items(state.allSessions, key = { "all-${it.id}" }) { session ->
                    FrostSessionRow(session = session)
                }
            }

            item {
                Text(
                    text = "All history stays on this device.",
                    style = FrostTheme.typography.bodySmall,
                    color = FrostTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = FrostTheme.spacing.lg)
                )
            }
        }
    }
}

@Composable
private fun DayHeader(
    date: LocalDate,
    sessions: List<SessionEntity>,
    isBestDay: Boolean
) {
    val total = sessions.sumOf { it.durationSeconds.toLong() }
    FrostCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.US)),
                    style = FrostTheme.typography.titleLarge,
                    color = FrostTheme.colors.textPrimary
                )
                Text(
                    text = if (sessions.isEmpty()) {
                        "Nothing logged yet"
                    } else {
                        "${formatDuration(total)} focused · " +
                            "${sessions.size} ${if (sessions.size == 1) "session" else "sessions"}"
                    },
                    style = FrostTheme.typography.bodyMedium,
                    color = FrostTheme.colors.textSecondary
                )
            }
            if (isBestDay) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(FrostTheme.colors.secondary.copy(alpha = 0.14f))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = FrostTheme.colors.secondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Best",
                        style = FrostTheme.typography.labelMedium,
                        color = FrostTheme.colors.secondary
                    )
                }
            }
        }
    }
}
