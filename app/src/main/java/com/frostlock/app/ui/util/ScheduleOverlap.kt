package com.frostlock.app.ui.util

import com.frostlock.app.data.local.prefs.FocusSchedule
import com.frostlock.app.data.local.prefs.appliesOn
import java.time.LocalDate

/** Another rule whose blocking window collides with a candidate rule. */
data class RuleOverlap(
    val other: FocusSchedule,
    /** Weekdays (1 = Monday … 7 = Sunday) that collide, for repeating rules. */
    val days: Set<Int> = emptySet(),
    /** ISO dates that collide, when at least one rule is a one-off. */
    val dates: Set<String> = emptySet()
)

private data class MinuteSpan(val start: Int, val end: Int)

private const val MINUTES_PER_DAY = 24 * 60

/**
 * The minutes of [date] during which [schedule] blocks.
 *
 * Overnight rules (22:00–02:00) are split across two days: the evening part
 * belongs to the rule's own date, while the early-morning part belongs to the
 * day after a selected day. This mirrors [com.frostlock.app.data.local.prefs.isActiveAt].
 */
private fun blockedSpans(date: LocalDate, schedule: FocusSchedule): List<MinuteSpan> {
    if (!schedule.enabled || schedule.startMinutes == schedule.endMinutes) return emptyList()
    val overnight = schedule.startMinutes > schedule.endMinutes
    val spans = mutableListOf<MinuteSpan>()

    if (schedule.appliesOn(date)) {
        spans += if (overnight) {
            MinuteSpan(schedule.startMinutes, MINUTES_PER_DAY)
        } else {
            MinuteSpan(schedule.startMinutes, schedule.endMinutes)
        }
    }
    if (overnight && schedule.appliesOn(date.minusDays(1))) {
        spans += MinuteSpan(0, schedule.endMinutes)
    }
    return spans
}

private fun spansCollide(first: List<MinuteSpan>, second: List<MinuteSpan>): Boolean =
    first.any { a -> second.any { b -> a.start < b.end && b.start < a.end } }

/**
 * Dates on which the two rules are blocking at the same time.
 *
 * Repeating rules are compared across one representative Monday–Monday week
 * (the extra Monday catches a Sunday-night overnight tail). One-off rules are
 * compared on their saved dates plus the following morning.
 */
private fun collisionDates(first: FocusSchedule, second: FocusSchedule): Set<LocalDate> {
    val candidates = mutableSetOf<LocalDate>()

    if (first.dates.isEmpty() && second.dates.isEmpty()) {
        // Any fixed Monday works because weekday rules repeat.
        val monday = LocalDate.of(2026, 11, 16)
        (0..7).forEach { candidates += monday.plusDays(it.toLong()) }
    } else {
        (first.dates + second.dates)
            .mapNotNull { iso -> runCatching { LocalDate.parse(iso) }.getOrNull() }
            .forEach { date ->
                candidates += date
                // Overnight rules spill into the following morning.
                candidates += date.plusDays(1)
            }
    }

    return candidates.filterTo(mutableSetOf()) { date ->
        spansCollide(blockedSpans(date, first), blockedSpans(date, second))
    }
}

/** True when both enabled rules block at the same moment. */
fun rulesOverlap(first: FocusSchedule, second: FocusSchedule): Boolean {
    if (first.id == second.id) return false
    if (!first.enabled || !second.enabled) return false
    return collisionDates(first, second).isNotEmpty()
}

/**
 * Every other rule that collides with [candidate], with the shared weekdays or
 * one-off dates. Disabled rules and the candidate itself are skipped, and the
 * result is sorted by label so the warning list stays stable.
 */
fun ruleOverlaps(
    candidate: FocusSchedule,
    existing: List<FocusSchedule>
): List<RuleOverlap> = existing.asSequence()
    .filter { it.id != candidate.id }
    .mapNotNull { other ->
        val collisions = collisionDates(candidate, other)
        if (collisions.isEmpty()) {
            null
        } else if (candidate.dates.isEmpty() && other.dates.isEmpty()) {
            RuleOverlap(
                other = other,
                days = collisions.mapTo(mutableSetOf()) { it.dayOfWeek.value }
            )
        } else {
            RuleOverlap(
                other = other,
                dates = collisions.mapTo(mutableSetOf()) { it.toString() }
            )
        }
    }
    .sortedBy { it.other.label.lowercase() }
    .toList()

/** Quick check used by the rules list to badge a colliding rule. */
fun hasOverlap(schedule: FocusSchedule, all: List<FocusSchedule>): Boolean =
    ruleOverlaps(schedule, all).isNotEmpty()
