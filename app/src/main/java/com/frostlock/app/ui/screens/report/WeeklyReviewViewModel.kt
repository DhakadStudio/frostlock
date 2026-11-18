package com.frostlock.app.ui.screens.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostlock.app.data.local.db.SessionDao
import com.frostlock.app.data.local.prefs.FocusSettings
import com.frostlock.app.data.local.prefs.SubjectStore
import com.frostlock.app.data.local.prefs.subjectKey
import com.frostlock.app.ui.util.ImprovementReport
import com.frostlock.app.ui.util.WeeklyReport
import com.frostlock.app.ui.util.buildImprovementReport
import com.frostlock.app.ui.util.buildWeeklyReport
import com.frostlock.app.ui.util.startOfWeek
import com.frostlock.app.ui.util.weekLabel
import com.frostlock.app.ui.util.weekRangeLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class WeeklyReviewUiState(
    val report: WeeklyReport = buildWeeklyReport(emptyList()),
    val improvement: ImprovementReport = ImprovementReport(),
    val label: String = "This week",
    val range: String = "",
    val canGoForward: Boolean = false,
    val isCurrentWeek: Boolean = true
)

@HiltViewModel
class WeeklyReviewViewModel @Inject constructor(
    sessionDao: SessionDao,
    private val subjectStore: SubjectStore,
    focusSettings: FocusSettings
) : ViewModel() {

    /** 0 = this week, 1 = last week, 2 = two weeks ago… */
    private val weeksAgo = MutableStateFlow(0)

    val uiState: StateFlow<WeeklyReviewUiState> = combine(
        sessionDao.recentSessions(2_000),
        weeksAgo,
        focusSettings.dailyGoalMinutes
    ) { sessions, offset, dailyGoalMinutes ->
        val today = LocalDate.now()
        val weekStart = startOfWeek(today).minusWeeks(offset.toLong())
        val colors = subjectStore.subjects().associateBy { subjectKey(it.name) }

        WeeklyReviewUiState(
            report = buildWeeklyReport(
                sessions = sessions,
                weekStart = weekStart,
                today = today,
                colorFor = { tag -> colors[subjectKey(tag)]?.colorArgb }
            ),
            improvement = buildImprovementReport(
                sessions = sessions,
                dailyGoalMinutes = dailyGoalMinutes,
                today = today
            ),
            label = weekLabel(weekStart, today),
            range = weekRangeLabel(weekStart),
            canGoForward = offset > 0,
            isCurrentWeek = offset == 0
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WeeklyReviewUiState()
    )

    fun previousWeek() {
        weeksAgo.value += 1
    }

    fun nextWeek() {
        if (weeksAgo.value > 0) weeksAgo.value -= 1
    }
}
