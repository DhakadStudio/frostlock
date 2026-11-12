package com.frostlock.app.ui.util

import com.frostlock.app.data.local.db.SessionEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

/** One bar in the weekly chart. */
data class WeekDay(
    val date: LocalDate,
    val label: String,
    val seconds: Long,
    val sessions: Int,
    val isToday: Boolean
)

/** A subject's share of the week, already sorted by time. */
data class WeekSubject(
    val tag: String,
    val seconds: Long,
    val sessions: Int,
    val colorArgb: Int
)

/** Everything the weekly report screen renders for one Monday–Sunday week. */
data class WeeklyReport(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val totalSeconds: Long,
    val previousSeconds: Long,
    val sessions: Int,
    val activeDays: Int,
    val reflections: Int,
    val days: List<WeekDay>,
    val subjects: List<WeekSubject>
) {
    val averageSessionSeconds: Long
        get() = if (sessions == 0) 0L else totalSeconds / sessions

    val bestDay: WeekDay?
        get() = days.filter { it.seconds > 0 }.maxByOrNull { it.seconds }

    /**
     * Percentage change against the previous week. Null means there is no
     * meaningful baseline (last week had no focus time).
     */
    val changePercent: Int?
        get() = if (previousSeconds <= 0L) {
            null
        } else {
            (((totalSeconds - previousSeconds).toDouble() / previousSeconds) * 100).roundToInt()
        }
}

/** Monday 00:00 of the week containing [date]. */
fun startOfWeek(date: LocalDate = LocalDate.now()): LocalDate =
    date.with(DayOfWeek.MONDAY)

/** Human label used by the week switcher. */
fun weekLabel(weekStart: LocalDate, today: LocalDate = LocalDate.now()): String {
    val current = startOfWeek(today)
    val weeksAgo = ChronoUnit.WEEKS.between(weekStart, current).toInt()
    return when (weeksAgo) {
        0 -> "This week"
        1 -> "Last week"
        else -> "$weeksAgo weeks ago"
    }
}

/**
 * Aggregates sessions into one Monday–Sunday report.
 *
 * [sessions] may contain older data: anything outside the week (and the week
 * before it, used for the comparison) is ignored.
 */
fun buildWeeklyReport(
    sessions: List<SessionEntity>,
    weekStart: LocalDate = startOfWeek(),
    today: LocalDate = LocalDate.now(),
    colorFor: (String) -> Int? = { null }
): WeeklyReport {
    val weekEnd = weekStart.plusDays(6)
    val previousStart = weekStart.minusWeeks(1)
    val previousEnd = weekStart.minusDays(1)

    val thisWeek = sessions.filter { it.weekDate() in weekStart..weekEnd }
    val lastWeek = sessions.filter { it.weekDate() in previousStart..previousEnd }

    val formatter = DateTimeFormatter.ofPattern("EEE", Locale.US)
    val days = (0L..6L).map { offset ->
        val date = weekStart.plusDays(offset)
        val daySessions = thisWeek.filter { it.weekDate() == date }
        WeekDay(
            date = date,
            label = date.format(formatter),
            seconds = daySessions.sumOf { it.durationSeconds.toLong() },
            sessions = daySessions.size,
            isToday = date == today
        )
    }

    val subjects = thisWeek
        .groupBy { it.tag.trim().ifBlank { "Untagged" } }
        .map { (tag, group) ->
            WeekSubject(
                tag = tag,
                seconds = group.sumOf { it.durationSeconds.toLong() },
                sessions = group.size,
                colorArgb = colorFor(tag) ?: DEFAULT_WEEK_COLOR
            )
        }
        .filter { it.seconds > 0L }
        .sortedByDescending { it.seconds }
        .take(5)

    return WeeklyReport(
        weekStart = weekStart,
        weekEnd = weekEnd,
        totalSeconds = thisWeek.sumOf { it.durationSeconds.toLong() },
        previousSeconds = lastWeek.sumOf { it.durationSeconds.toLong() },
        sessions = thisWeek.size,
        activeDays = thisWeek.map { it.weekDate() }.distinct().size,
        reflections = thisWeek.count { it.note.isNotBlank() },
        days = days,
        subjects = subjects
    )
}

/** Sessions are grouped by the day they ended, matching the diary. */
private fun SessionEntity.weekDate(): LocalDate = localDateOf(endedAt)

private const val DEFAULT_WEEK_COLOR = 0xFF7DD3FC.toInt()

/** "10–16 Nov" or "27 Oct – 2 Nov" when the week crosses a month boundary. */
fun weekRangeLabel(weekStart: LocalDate): String {
    val end = weekStart.plusDays(6)
    val day = DateTimeFormatter.ofPattern("d", Locale.US)
    val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.US)
    return if (weekStart.month == end.month) {
        "${weekStart.format(day)}–${end.format(dayMonth)}"
    } else {
        "${weekStart.format(dayMonth)} – ${end.format(dayMonth)}"
    }
}
