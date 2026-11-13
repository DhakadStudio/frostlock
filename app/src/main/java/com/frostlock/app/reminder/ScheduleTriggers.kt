package com.frostlock.app.reminder

import com.frostlock.app.data.local.prefs.FocusSchedule
import com.frostlock.app.data.local.prefs.appliesOn
import com.frostlock.app.data.local.prefs.endsAt
import com.frostlock.app.data.local.prefs.isActiveAt
import java.time.LocalDate
import java.time.ZonedDateTime

/** The two moments a blocking rule should notify the user about. */
enum class ScheduleReminderKind { START, END }

data class ScheduleReminderTrigger(
    val scheduleId: Long,
    val kind: ScheduleReminderKind,
    val at: ZonedDateTime
)

/**
 * The next time this rule begins after [now].
 *
 * Date-scoped rules can only fire once per saved date, while weekly rules repeat
 * on their selected days. A rule that is currently running still reports its
 * *next* start (not the one already in progress).
 */
fun nextScheduleStart(
    schedule: FocusSchedule,
    now: ZonedDateTime = ZonedDateTime.now()
): ZonedDateTime? {
    if (!schedule.enabled || schedule.startMinutes == schedule.endMinutes) return null

    val candidates: Sequence<LocalDate> = if (schedule.dates.isNotEmpty()) {
        schedule.dates.asSequence()
            .mapNotNull { iso -> runCatching { LocalDate.parse(iso) }.getOrNull() }
            .sorted()
    } else {
        // At most seven days ahead is enough for any weekly rule.
        (0L..7L).asSequence().map { now.toLocalDate().plusDays(it) }
            .filter { schedule.appliesOn(it) }
    }

    return candidates
        .map { date ->
            date.atTime(schedule.startMinutes / 60, schedule.startMinutes % 60)
                .atZone(now.zone)
        }
        .firstOrNull { it.isAfter(now) }
}

/**
 * The next time this rule closes after [now]. If the window is already open the
 * current close time wins; otherwise it closes at the end of the next window.
 */
fun nextScheduleEnd(
    schedule: FocusSchedule,
    now: ZonedDateTime = ZonedDateTime.now()
): ZonedDateTime? {
    if (!schedule.enabled || schedule.startMinutes == schedule.endMinutes) return null
    if (schedule.isActiveAt(now)) return schedule.endsAt(now)

    val start = nextScheduleStart(schedule, now) ?: return null
    return schedule.endsAt(start)
}

/**
 * Both upcoming alarms for a rule, chronological. A rule that is running now
 * yields its close reminder before the next start reminder.
 */
fun nextScheduleTriggers(
    schedule: FocusSchedule,
    now: ZonedDateTime = ZonedDateTime.now()
): List<ScheduleReminderTrigger> {
    val start = nextScheduleStart(schedule, now)
    val end = nextScheduleEnd(schedule, now)
    return listOfNotNull(
        start?.let { ScheduleReminderTrigger(schedule.id, ScheduleReminderKind.START, it) },
        end?.let { ScheduleReminderTrigger(schedule.id, ScheduleReminderKind.END, it) }
    ).sortedBy { it.at }
}
