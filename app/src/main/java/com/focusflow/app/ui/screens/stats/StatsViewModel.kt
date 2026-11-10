package com.focusflow.app.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusflow.app.data.local.db.SessionDao
import com.focusflow.app.data.local.db.SessionEntity
import com.focusflow.app.ui.util.computeStreak
import com.focusflow.app.ui.util.localDateOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class DayBar(
    val label: String,
    val seconds: Long,
    val isToday: Boolean
)

data class StatsUiState(
    val todaySeconds: Long = 0L,
    val weekSeconds: Long = 0L,
    val totalSeconds: Long = 0L,
    val totalSessions: Int = 0,
    val completedSessions: Int = 0,
    val streak: Int = 0,
    val days: List<DayBar> = emptyList(),
    val recent: List<SessionEntity> = emptyList()
) {
    val averageSeconds: Long
        get() = if (totalSessions == 0) 0L else totalSeconds / totalSessions
}

private data class PartialStats(
    val recent: List<SessionEntity>,
    val totalSeconds: Long,
    val todaySeconds: Long,
    val weekSeconds: Long,
    val totalSessions: Int
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    sessionDao: SessionDao
) : ViewModel() {

    private val todayStart = startOfTodayMillis()
    private val weekStart = startOfWeekMillis()

    val uiState: StateFlow<StatsUiState> = combine(
        sessionDao.recentSessions(500),
        sessionDao.totalSeconds(),
        sessionDao.totalSecondsSince(todayStart),
        sessionDao.totalSecondsSince(weekStart),
        sessionDao.totalCount()
    ) { recent, total, today, week, count ->
        PartialStats(recent, total, today, week, count)
    }.combine(sessionDao.completedCount()) { partial, completed ->
        StatsUiState(
            todaySeconds = partial.todaySeconds,
            weekSeconds = partial.weekSeconds,
            totalSeconds = partial.totalSeconds,
            totalSessions = partial.totalSessions,
            completedSessions = completed,
            streak = computeStreak(partial.recent.map { it.startedAt }),
            days = buildDays(partial.recent),
            recent = partial.recent.take(10)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState()
    )

    private fun buildDays(recent: List<SessionEntity>): List<DayBar> {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("EEE", Locale.US)
        val totals = recent
            .groupBy { localDateOf(it.startedAt) }
            .mapValues { entry -> entry.value.sumOf { it.durationSeconds.toLong() } }

        return (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            DayBar(
                label = date.format(formatter),
                seconds = totals[date] ?: 0L,
                isToday = offset == 0
            )
        }
    }

    private fun startOfTodayMillis(): Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun startOfWeekMillis(): Long =
        LocalDate.now()
            .with(DayOfWeek.MONDAY)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
}
