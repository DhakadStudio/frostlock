package com.frostlock.app.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostlock.app.data.local.db.SessionDao
import com.frostlock.app.data.local.db.SessionEntity
import com.frostlock.app.data.local.prefs.SubjectStore
import com.frostlock.app.data.local.prefs.subjectKey
import com.frostlock.app.ui.util.Achievement
import com.frostlock.app.ui.util.FocusLevel
import com.frostlock.app.ui.util.buildAchievements
import com.frostlock.app.ui.util.focusLevelFor
import com.frostlock.app.ui.util.computeStreak
import com.frostlock.app.ui.util.localDateOf
import com.frostlock.app.ui.util.tagBreakdown
import com.frostlock.app.ui.util.TagTotal
import com.frostlock.app.ui.util.buildMonthlySummary
import com.frostlock.app.ui.util.MonthlySummary
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
    val tags: List<TagTotal> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val level: FocusLevel = focusLevelFor(0L),
    val monthly: MonthlySummary = buildMonthlySummary(emptyList()),
    val recent: List<SessionEntity> = emptyList()
) {
    val averageSeconds: Long
        get() = if (totalSessions == 0) 0L else totalSeconds / totalSessions

    val unlockedCount: Int
        get() = achievements.count { it.unlocked }
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
    sessionDao: SessionDao,
    private val subjectStore: SubjectStore
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
        val subjectColors = subjectStore.subjects()
            .associateBy { subjectKey(it.name) }
        StatsUiState(
            todaySeconds = partial.todaySeconds,
            weekSeconds = partial.weekSeconds,
            totalSeconds = partial.totalSeconds,
            totalSessions = partial.totalSessions,
            completedSessions = completed,
            streak = computeStreak(partial.recent.map { it.startedAt }),
            days = buildDays(partial.recent),
            tags = tagBreakdown(partial.recent) { tag ->
                subjectColors[subjectKey(tag)]?.colorArgb
            },
            achievements = buildAchievements(
                totalSeconds = partial.totalSeconds,
                totalSessions = partial.totalSessions,
                startTimes = partial.recent.map { it.startedAt }
            ),
            level = focusLevelFor(partial.totalSeconds),
            monthly = buildMonthlySummary(partial.recent),
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
