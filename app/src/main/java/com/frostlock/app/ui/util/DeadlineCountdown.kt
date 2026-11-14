package com.frostlock.app.ui.util

import com.frostlock.app.data.local.prefs.FocusDeadline
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** The parsed date, or null if the saved value is malformed. */
fun deadlineDate(deadline: FocusDeadline): LocalDate? =
    runCatching { LocalDate.parse(deadline.date) }.getOrNull()

/**
 * Whole days from [today] to the deadline. 0 means today, negative means the
 * date has passed. Null when the stored date cannot be parsed.
 */
fun daysUntilDeadline(
    deadline: FocusDeadline,
    today: LocalDate = LocalDate.now()
): Long? = deadlineDate(deadline)?.let { ChronoUnit.DAYS.between(today, it) }

/**
 * "23 days left", "1 day left", "Today" or "Passed". Null for a bad date.
 */
fun deadlineCountdown(
    deadline: FocusDeadline,
    today: LocalDate = LocalDate.now()
): String? {
    val days = daysUntilDeadline(deadline, today) ?: return null
    return when {
        days > 1L -> "$days days left"
        days == 1L -> "1 day left"
        days == 0L -> "Today"
        else -> "Passed"
    }
}

/**
 * True while the target date is today or still ahead. Used to decide whether
 * the home card is worth showing.
 */
fun deadlineIsUpcoming(
    deadline: FocusDeadline,
    today: LocalDate = LocalDate.now()
): Boolean {
    val days = daysUntilDeadline(deadline, today) ?: return false
    return days >= 0L
}

/** Amber when three days or fewer remain, otherwise the frost accent. */
fun deadlineIsUrgent(
    deadline: FocusDeadline,
    today: LocalDate = LocalDate.now()
): Boolean {
    val days = daysUntilDeadline(deadline, today) ?: return false
    return days in 0L..3L
}
