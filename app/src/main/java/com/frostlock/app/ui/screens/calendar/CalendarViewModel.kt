package com.frostlock.app.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostlock.app.data.local.db.SessionDao
import com.frostlock.app.data.local.db.SessionEntity
import com.frostlock.app.ui.util.FocusMonth
import com.frostlock.app.ui.util.buildFocusMonth
import com.frostlock.app.ui.util.sessionsOn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val selected: LocalDate = LocalDate.now(),
    val calendar: FocusMonth = buildFocusMonth(YearMonth.now(), emptyList()),
    val daySessions: List<SessionEntity> = emptyList(),
    val canGoNext: Boolean = false
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    sessionDao: SessionDao
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    private val selected = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<CalendarUiState> = combine(
        sessionDao.recentSessions(2000),
        month,
        selected
    ) { sessions, currentMonth, currentDay ->
        CalendarUiState(
            month = currentMonth,
            selected = currentDay,
            calendar = buildFocusMonth(currentMonth, sessions),
            daySessions = sessionsOn(currentDay, sessions),
            canGoNext = currentMonth < YearMonth.now()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalendarUiState()
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

    /** Selecting a leading/trailing day also moves the calendar to that month. */
    fun select(date: LocalDate) {
        selected.value = date
        val target = YearMonth.from(date)
        if (target != month.value) month.value = target
    }
}
