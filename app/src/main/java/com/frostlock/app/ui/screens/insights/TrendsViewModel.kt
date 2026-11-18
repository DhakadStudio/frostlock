package com.frostlock.app.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostlock.app.data.local.db.SessionDao
import com.frostlock.app.data.local.prefs.SubjectStore
import com.frostlock.app.data.local.prefs.subjectKey
import com.frostlock.app.insights.DayFocus
import com.frostlock.app.insights.FocusDayBuckets
import com.frostlock.app.insights.InsightsRepository
import com.frostlock.app.insights.MonthFocus
import com.frostlock.app.insights.SubjectFocus
import com.frostlock.app.insights.subjectBreakdown
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

/** The three periods Trends can explain. */
enum class TrendsPeriod(val label: String) {
    WEEK("7 days"),
    MONTH("30 days"),
    SIX_MONTHS("6 months")
}

/** One bar for the selected period. */
data class TrendsBar(
    val label: String,
    val seconds: Long,
    val highlighted: Boolean = false
)

data class TrendsUiState(
    val loading: Boolean = true,
    val hasHistory: Boolean = false,
    val period: TrendsPeriod = TrendsPeriod.WEEK,
    val days: List<TrendsBar> = emptyList(),
    val months: List<MonthFocus> = emptyList(),
    val totalSeconds: Long = 0L,
    val activeDays: Int = 0,
    val sessions: Int = 0,
    val subjects: List<SubjectFocus> = emptyList(),
    val bestDay: DayFocus? = null,
    val lastFocusDate: LocalDate? = null
) {
    val averagePerActiveDaySeconds: Long
        get() = if (activeDays == 0) 0L else totalSeconds / activeDays

    val isEmptyPeriod: Boolean get() = totalSeconds == 0L
}

@HiltViewModel
class TrendsViewModel @Inject constructor(
    repository: InsightsRepository,
    sessionDao: SessionDao,
    subjectStore: SubjectStore
) : ViewModel() {

    private val period = MutableStateFlow(TrendsPeriod.WEEK)

    val state: StateFlow<TrendsUiState> = combine(
        repository.snapshot,
        sessionDao.recentSessions(2_000),
        subjectStore.subjectsFlow,
        period
    ) { snapshot, sessions, subjects, selected ->
        val today = snapshot.today.date
        val (start, end) = TrendsRanges.range(selected, today)
        val colors = subjects.associate { subjectKey(it.name) to it.colorArgb }
        val periodDays = generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .map { date -> snapshot.days[date] ?: DayFocus(date) }
            .toList()

        val totalSeconds = periodDays.sumOf { it.seconds }
        val sessionsInPeriod = sessions.filter { session ->
            FocusDayBuckets.countDate(session, java.time.ZoneId.systemDefault()) in start..end
        }

        TrendsUiState(
            loading = false,
            hasHistory = snapshot.totalSessions > 0,
            period = selected,
            days = periodDays.map { day ->
                TrendsBar(
                    label = TrendsRanges.label(selected, day.date),
                    seconds = day.seconds,
                    highlighted = day.date == today
                )
            },
            months = snapshot.months,
            totalSeconds = totalSeconds,
            activeDays = periodDays.count { it.hasFocus },
            sessions = sessionsInPeriod.size,
            subjects = subjectBreakdown(
                sessions = sessions,
                start = start,
                end = end,
                zone = java.time.ZoneId.systemDefault(),
                colorFor = { name -> colors[subjectKey(name)] }
            ),
            bestDay = periodDays.filter { it.hasFocus }.maxByOrNull { it.seconds },
            lastFocusDate = snapshot.days.values
                .filter { it.hasFocus && !it.date.isAfter(today) }
                .maxOfOrNull { it.date }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrendsUiState()
    )

    fun selectPeriod(value: TrendsPeriod) {
        period.value = value
    }
}

/** Pure period maths, kept out of the ViewModel so it can be unit tested. */
internal object TrendsRanges {

    fun range(period: TrendsPeriod, today: LocalDate): Pair<LocalDate, LocalDate> =
        when (period) {
            TrendsPeriod.WEEK -> today.minusDays(6) to today
            TrendsPeriod.MONTH -> today.minusDays(29) to today
            TrendsPeriod.SIX_MONTHS -> {
                val start = YearMonth.from(today).minusMonths(5).atDay(1)
                start to today
            }
        }

    fun label(period: TrendsPeriod, date: LocalDate): String = when (period) {
        TrendsPeriod.WEEK -> date.dayOfWeek.name.take(1)
        // Thirty thin bars cannot carry a label each; the card captions the range.
        TrendsPeriod.MONTH -> ""
        TrendsPeriod.SIX_MONTHS -> date.month.name.take(1)
    }
}
