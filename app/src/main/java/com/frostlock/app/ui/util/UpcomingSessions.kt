package com.frostlock.app.ui.util

import com.frostlock.app.data.local.prefs.FocusSchedule
import com.frostlock.app.data.local.prefs.appliesOn
import com.frostlock.app.data.local.prefs.endsAt
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One future run of a blocking rule, used by the "Coming up" card. */
data class UpcomingSession(
    val ruleId: Long,
    val label: String,
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val packageCount: Int
) {
    fun minutesUntil(now: ZonedDateTime): Long =
        Duration.between(now, start).toMinutes().coerceAtLeast(0L)
}

/**
 * The next runs of every enabled rule inside [horizonDays], earliest first.
 *
 * Weekly rules repeat on their chosen weekdays; date-scoped rules run once on
 * each saved date. Overnight windows keep their end on the following morning.
 * Rules that are already running are reported by their next start, since the
 * rules list already shows an "Active now" badge.
 */
fun upcomingSessions(
    schedules: List<FocusSchedule>,
    now: ZonedDateTime = ZonedDateTime.now(),
    horizonDays: Long = 7,
    limit: Int = 4
): List<UpcomingSession> {
    val horizon = now.toLocalDate().plusDays(horizonDays).atTime(23, 59).atZone(now.zone)
    return schedules.asSequence()
        .filter { it.enabled && it.startMinutes != it.endMinutes }
        .flatMap { schedule ->
            startsFor(schedule, now, horizon).map { start ->
                UpcomingSession(
                    ruleId = schedule.id,
                    label = schedule.label,
                    start = start,
                    end = schedule.endsAt(start),
                    packageCount = schedule.packages.size
                )
            }
        }
        .sortedBy { it.start }
        .take(limit)
        .toList()
}

private fun startsFor(
    schedule: FocusSchedule,
    now: ZonedDateTime,
    horizon: ZonedDateTime
): Sequence<ZonedDateTime> {
    val candidates: Sequence<LocalDate> = if (schedule.dates.isNotEmpty()) {
        schedule.dates.asSequence()
            .mapNotNull { iso -> runCatching { LocalDate.parse(iso) }.getOrNull() }
            .sorted()
    } else {
        (0L..7L).asSequence()
            .map { now.toLocalDate().plusDays(it) }
            .filter { schedule.appliesOn(it) }
    }

    return candidates
        .map { date ->
            date.atTime(schedule.startMinutes / 60, schedule.startMinutes % 60)
                .atZone(now.zone)
        }
        .filter { it.isAfter(now) && !it.isAfter(horizon) }
}

/** "Today 09:00", "Tomorrow 09:00", "Mon 09:00" or "20 Nov 09:00". */
fun upcomingWhen(start: ZonedDateTime, now: ZonedDateTime = ZonedDateTime.now()): String {
    val time = start.format(DateTimeFormatter.ofPattern("HH:mm", Locale.US))
    val date = start.toLocalDate()
    val today = now.toLocalDate()
    return when (date) {
        today -> "Today $time"
        today.plusDays(1) -> "Tomorrow $time"
        else -> {
            val withinWeek = date.isBefore(today.plusDays(7))
            val day = if (withinWeek) {
                date.format(DateTimeFormatter.ofPattern("EEE", Locale.US))
            } else {
                date.format(DateTimeFormatter.ofPattern("d MMM", Locale.US))
            }
            "$day $time"
        }
    }
}

/** Compact countdown such as "in 25m", "in 3h 10m" or "in 2d 4h". */
fun upcomingCountdown(start: ZonedDateTime, now: ZonedDateTime = ZonedDateTime.now()): String {
    val minutes = Duration.between(now, start).toMinutes()
    return when {
        minutes <= 0L -> "starting now"
        minutes < 60L -> "in ${minutes}m"
        minutes < 24 * 60L -> {
            val hours = minutes / 60
            val rest = minutes % 60
            if (rest == 0L) "in ${hours}h" else "in ${hours}h ${rest}m"
        }
        else -> {
            val days = minutes / (24 * 60)
            val hours = (minutes % (24 * 60)) / 60
            if (hours == 0L) "in ${days}d" else "in ${days}d ${hours}h"
        }
    }
}
