package com.frostlock.app.ui.util

import com.frostlock.app.data.local.db.SessionEntity
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One bar in the monthly rhythm card. */
data class MonthTotal(
    val month: YearMonth,
    val label: String,
    val seconds: Long,
    val sessions: Int,
    val activeDays: Int,
    val bestDaySeconds: Long
) {
    val hasFocus: Boolean get() = seconds > 0L

    /** Average across days that actually had a session. */
    val averagePerActiveDay: Long
        get() = if (activeDays == 0) 0L else seconds / activeDays
}

/** The last [MonthlySummary.months] months plus the current month's totals. */
data class MonthlySummary(
    val months: List<MonthTotal>,
    val current: MonthTotal
) {
    val averagePerActiveDay: Long
        get() = current.averagePerActiveDay
}

/**
 * Builds a local, dependency-free month rollup from the sessions already in the
 * database. Months are ordered oldest to newest so they can be drawn left to
 * right; [monthsBack] is clamped to a sensible range.
 */
fun buildMonthlySummary(
    sessions: List<SessionEntity>,
    today: LocalDate = LocalDate.now(),
    monthsBack: Int = 6
): MonthlySummary {
    val count = monthsBack.coerceIn(1, 24)
    val currentMonth = YearMonth.from(today)
    val formatter = DateTimeFormatter.ofPattern("MMM", Locale.US)
    val byMonth = sessions.groupBy { YearMonth.from(localDateOf(it.startedAt)) }

    val months = (count - 1 downTo 0).map { offset ->
        val month = currentMonth.minusMonths(offset.toLong())
        val entries = byMonth[month].orEmpty()
        val byDate = entries.groupBy { localDateOf(it.startedAt) }
        MonthTotal(
            month = month,
            label = month.format(formatter),
            seconds = entries.sumOf { it.durationSeconds.toLong() },
            sessions = entries.size,
            activeDays = byDate.count { (_, day) -> day.any { it.durationSeconds > 0 } },
            bestDaySeconds = byDate.values.maxOfOrNull { day ->
                day.sumOf { it.durationSeconds.toLong() }
            } ?: 0L
        )
    }

    return MonthlySummary(months = months, current = months.last())
}
