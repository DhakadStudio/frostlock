package com.frostlock.app.insights

import com.frostlock.app.data.local.db.SessionEntity
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/** One local calendar day of focus, after sessions have been split at midnight. */
data class DayFocus(
    val date: LocalDate,
    val seconds: Long = 0L,
    val sessions: Int = 0
) {
    val hasFocus: Boolean get() = seconds > 0L
}

/** A closed local date range with its daily buckets. */
data class PeriodFocus(
    val start: LocalDate,
    val end: LocalDate,
    val seconds: Long,
    val sessions: Int,
    val activeDays: Int,
    val goalDays: Int,
    val days: List<DayFocus>
) {
    val averageSessionSeconds: Long
        get() = if (sessions == 0) 0L else seconds / sessions

    val bestDay: DayFocus?
        get() = days.filter { it.hasFocus }.maxByOrNull { it.seconds }
}

/** One month of the rhythm chart. */
data class MonthFocus(
    val month: YearMonth,
    val seconds: Long,
    val sessions: Int,
    val activeDays: Int,
    val bestDaySeconds: Long
) {
    val hasFocus: Boolean get() = seconds > 0L
}

/** A subject's share of a period, top five plus Other. */
data class SubjectFocus(
    val tag: String,
    val seconds: Long,
    val sessions: Int,
    val share: Float,
    val colorArgb: Int
)

/**
 * Everything Insights, Trends, Weekly review, History and the widgets need,
 * computed once from the local session table.
 *
 * Level and badge values are intentionally derived from [totalSeconds] and
 * [totalSessions] by their existing pure helpers, so the snapshot stays free of
 * UI types and cannot disagree with the ladder or cabinet.
 */
data class InsightsSnapshot(
    val generatedAt: Instant,
    val today: DayFocus,
    val week: PeriodFocus,
    val last7: PeriodFocus,
    val months: List<MonthFocus>,
    val subjects: List<SubjectFocus>,
    val days: Map<LocalDate, DayFocus>,
    val recent: List<SessionEntity>,
    val currentStreak: Int,
    val bestStreak: Int,
    val totalSeconds: Long,
    val totalSessions: Int,
    val reflections: Int,
    val weekReflections: Int
)
