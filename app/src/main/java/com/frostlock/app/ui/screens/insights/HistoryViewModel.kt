package com.frostlock.app.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostlock.app.data.local.db.SessionDao
import com.frostlock.app.data.local.db.SessionEntity
import com.frostlock.app.ui.util.FocusMonth
import com.frostlock.app.ui.util.buildFocusMonth
import com.frostlock.app.ui.util.sessionsOn
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

data class HistoryUiState(
    val month: YearMonth = YearMonth.now(),
    val selected: LocalDate = LocalDate.now(),
    val calendar: FocusMonth = buildFocusMonth(YearMonth.now(), emptyList()),
    val daySessions: List<SessionEntity> = emptyList(),
    val allSessions: List<SessionEntity> = emptyList(),
    val canGoNext: Boolean = false,
    val totalSeconds: Long = 0L
) {
    val hasHistory: Boolean get() = allSessions.isNotEmpty()
}

/**
 * History: the month heat-map, the selected day and the full session archive.
 *
 * The archive lives here so Reflections can stay about notes only.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    sessionDao: SessionDao
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    private val selected = MutableStateFlow(LocalDate.now())

    val state: StateFlow<HistoryUiState> = combine(
        sessionDao.recentSessions(2_000),
        month,
        selected
    ) { sessions, currentMonth, currentDay ->
        HistoryUiState(
            month = currentMonth,
            selected = currentDay,
            calendar = buildFocusMonth(currentMonth, sessions),
            daySessions = sessionsOn(currentDay, sessions),
            allSessions = sessions.sortedByDescending { it.endedAt },
            canGoNext = currentMonth < YearMonth.now(),
            totalSeconds = sessions.sumOf { it.durationSeconds.toLong() }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState()
    )

    fun previousMonth() {
        val target = month.value.minusMonths(1)
        month.value = target
        selected.value = if (target == YearMonth.now()) LocalDate.now() else target.atDay(1)
    }

    fun nextMonth() {
        val current = YearMonth.now()
        if (month.value >= current) return
        val target = month.value.plusMonths(1)
        month.value = target
        selected.value = if (target == current) LocalDate.now() else target.atDay(1)
    }

    /** Selecting a leading or trailing day also moves the calendar to its month. */
    fun select(date: LocalDate) {
        selected.value = date
        val target = YearMonth.from(date)
        if (target != month.value) month.value = target
    }
}
