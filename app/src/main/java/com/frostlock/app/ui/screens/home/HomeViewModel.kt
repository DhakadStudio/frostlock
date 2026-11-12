package com.frostlock.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostlock.app.data.local.db.SessionDao
import com.frostlock.app.data.local.db.SessionEntity
import com.frostlock.app.data.local.prefs.FocusSettings
import com.frostlock.app.ui.util.computeStreak
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class HomeUiState(
    val streak: Int = 0,
    val todaySeconds: Long = 0L,
    val weekSeconds: Long = 0L,
    val totalSeconds: Long = 0L,
    val totalSessions: Int = 0,
    val recent: List<SessionEntity> = emptyList(),
    val dailyGoalSeconds: Long = FocusSettings.DEFAULT_GOAL_MINUTES * 60L
) {
    val goalProgress: Float
        get() = if (dailyGoalSeconds <= 0L) 0f
        else (todaySeconds.toFloat() / dailyGoalSeconds.toFloat()).coerceIn(0f, 1f)

    val goalReached: Boolean
        get() = dailyGoalSeconds > 0L && todaySeconds >= dailyGoalSeconds
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    sessionDao: SessionDao,
    focusSettings: FocusSettings
) : ViewModel() {

    private val todayStart = startOfTodayMillis()
    private val weekStart = startOfWeekMillis()

    private val statsFlow = combine(
        sessionDao.recentSessions(200),
        sessionDao.totalSeconds(),
        sessionDao.totalSecondsSince(todayStart),
        sessionDao.totalSecondsSince(weekStart),
        sessionDao.totalCount()
    ) { recent, total, today, week, count ->
        HomeUiState(
            streak = computeStreak(recent.map { it.startedAt }),
            todaySeconds = today,
            weekSeconds = week,
            totalSeconds = total,
            totalSessions = count,
            recent = recent.take(3)
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        statsFlow,
        focusSettings.dailyGoalMinutes
    ) { state, goalMinutes ->
        state.copy(dailyGoalSeconds = goalMinutes * 60L)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    private fun startOfTodayMillis(): Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun startOfWeekMillis(): Long =
        LocalDate.now()
            .with(DayOfWeek.MONDAY)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
}
