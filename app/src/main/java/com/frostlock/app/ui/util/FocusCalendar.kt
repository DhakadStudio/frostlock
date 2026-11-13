package com.frostlock.app.ui.util

import com.frostlock.app.data.local.db.SessionEntity
import java.time.LocalDate
import java.time.YearMonth

/** One cell in the month grid. Leading/trailing days are kept for context. */
data class CalendarDay(
    val date: LocalDate,
    val inMonth: Boolean,
    val isToday: Boolean,
    val seconds: Long,
    val sessions: Int,
    val completed: Int
) {
    val hasFocus: Boolean get() = seconds > 0L
    val level: Int get() = focusIntensity(seconds)
}

/** Everything the calendar screen needs for one month. */
data class FocusMonth(
    val month: YearMonth,
    val days: List<CalendarDay>,
    val totalSeconds: Long,
    val sessions: Int,
    val activeDays: Int,
    val bestDay: CalendarDay?
) {
    /** Average focus time per day that actually had a session. */
    val averageSeconds: Long
        get() = if (activeDays == 0) 0L else totalSeconds / activeDays

    val inMonth: List<CalendarDay>
        get() = days.filter { it.inMonth }
}

/**
 * Five buckets used for the heat-map cells: none, under 30m, under an hour,
 * under two hours, and two hours or more.
 */
fun focusIntensity(seconds: Long): Int = when {
    seconds <= 0L -> 0
    seconds < 30 * 60L -> 1
    seconds < 60 * 60L -> 2
    seconds < 120 * 60L -> 3
    else -> 4
}

/**
 * Builds a stable six-week, Monday-first grid for [month]. Sessions outside the
 * month still paint the leading/trailing cells, but month totals only count the
 * days that belong to [month].
 */
fun buildFocusMonth(
    month: YearMonth,
    sessions: List<SessionEntity>,
    today: LocalDate = LocalDate.now()
): FocusMonth {
    val first = month.atDay(1)
    val leading = first.dayOfWeek.value - 1
    val gridStart = first.minusDays(leading.toLong())
    val byDate = sessions.groupBy { localDateOf(it.startedAt) }

    val days = (0 until 42).map { index ->
        val date = gridStart.plusDays(index.toLong())
        val entries = byDate[date].orEmpty()
        CalendarDay(
            date = date,
            inMonth = YearMonth.from(date) == month,
            isToday = date == today,
            seconds = entries.sumOf { it.durationSeconds.toLong() },
            sessions = entries.size,
            completed = entries.count { it.completed }
        )
    }

    val monthDays = days.filter { it.inMonth }
    return FocusMonth(
        month = month,
        days = days,
        totalSeconds = monthDays.sumOf { it.seconds },
        sessions = monthDays.sumOf { it.sessions },
        activeDays = monthDays.count { it.hasFocus },
        bestDay = monthDays.filter { it.hasFocus }.maxByOrNull { it.seconds }
    )
}

/** Sessions that started on [date], newest first. */
fun sessionsOn(date: LocalDate, sessions: List<SessionEntity>): List<SessionEntity> =
    sessions
        .filter { localDateOf(it.startedAt) == date }
        .sortedByDescending { it.startedAt }
