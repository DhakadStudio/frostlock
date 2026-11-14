package com.frostlock.app.ui.util

import com.frostlock.app.data.local.prefs.FocusSchedule
import com.frostlock.app.data.local.prefs.appliesOn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Consecutive scheduled days on which at least one completed session ran inside
 * one of the enabled rules.
 *
 * Only days the user actually scheduled are counted. Unscheduled days (for
 * example a weekend when rules only run Monday–Friday) neither extend nor break
 * the streak. Today is forgiving: if a rule is scheduled today but no session
 * has happened yet, the streak still counts from yesterday.
 *
 * Overnight windows are attributed to the day the rule started, so a 01:00
 * session inside a 22:00–02:00 Monday rule counts for Monday.
 */
fun computeScheduleStreak(
    schedules: List<FocusSchedule>,
    sessionStarts: List<Long>,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault()
): Int {
    val rules = schedules.filter { it.enabled && it.startMinutes != it.endMinutes }
    if (rules.isEmpty()) return 0

    val kept = sessionStarts.asSequence()
        .map { Instant.ofEpochMilli(it).atZone(zone) }
        .mapNotNull { moment -> anchorDate(rules, moment) }
        .toSet()

    var cursor = today
    if (isScheduled(today, rules) && today !in kept) {
        // Give today a chance: a scheduled day without a session yet is not a miss.
        cursor = today.minusDays(1)
    }

    var streak = 0
    var scanned = 0
    while (scanned < 400) {
        if (isScheduled(cursor, rules)) {
            if (cursor in kept) streak++ else break
        }
        cursor = cursor.minusDays(1)
        scanned++
    }
    return streak
}

private fun isScheduled(date: LocalDate, rules: List<FocusSchedule>): Boolean =
    rules.any { it.appliesOn(date) }

/**
 * The date whose window contains [moment], or null when no rule is running.
 * Mirrors [com.frostlock.app.data.local.prefs.isActiveAt].
 */
private fun anchorDate(rules: List<FocusSchedule>, moment: ZonedDateTime): LocalDate? {
    val minute = moment.hour * 60 + moment.minute
    val date = moment.toLocalDate()

    rules.forEach { rule ->
        val overnight = rule.startMinutes > rule.endMinutes
        if (!overnight) {
            if (rule.appliesOn(date) && minute >= rule.startMinutes && minute < rule.endMinutes) {
                return date
            }
        } else {
            if (rule.appliesOn(date) && minute >= rule.startMinutes) return date
            if (rule.appliesOn(date.minusDays(1)) && minute < rule.endMinutes) {
                return date.minusDays(1)
            }
        }
    }
    return null
}
