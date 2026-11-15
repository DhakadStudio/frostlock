package com.frostlock.app.ui.util

import com.frostlock.app.data.local.db.SessionEntity
import com.frostlock.app.data.local.prefs.FocusGoal
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** A goal plus the local progress derived from saved focus sessions. */
data class GoalProgress(
    val goal: FocusGoal,
    val focusedMinutes: Long,
    val percent: Float,
    val daysLeft: Long,
    /** How much of the goal's window has elapsed, from 0 to 1. */
    val expectedPercent: Float,
    val onTrack: Boolean,
    val reached: Boolean,
    val expired: Boolean
)

/** Sessions started at or after this goal's creation and before its target day ends. */
private fun goalWindow(goal: FocusGoal): Pair<Long, Long> {
    val targetEnd = runCatching {
        LocalDate.parse(goal.targetDate)
            .plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }.getOrDefault(Long.MAX_VALUE)
    return goal.createdAt to targetEnd
}

/**
 * Computes a goal's progress without storing anything extra. Sessions only count
 * when they started after the goal was created and before the target day ended,
 * so editing a goal can never rewrite old history.
 */
fun goalProgress(
    goal: FocusGoal,
    sessions: List<SessionEntity>,
    today: LocalDate = LocalDate.now()
): GoalProgress {
    val (windowStart, windowEnd) = goalWindow(goal)
    val focusedSeconds = sessions
        .asSequence()
        .filter { it.startedAt >= windowStart && it.startedAt < windowEnd }
        .sumOf { it.durationSeconds.toLong() }
    val focusedMinutes = focusedSeconds / 60L
    val target = goal.targetMinutes.coerceAtLeast(1)
    val percent = (focusedMinutes.toFloat() / target.toFloat()).coerceIn(0f, 1f)

    val targetDate = runCatching { LocalDate.parse(goal.targetDate) }.getOrNull()
    val createdDate = localDateOf(goal.createdAt)
    val totalDays = targetDate
        ?.let { ChronoUnit.DAYS.between(createdDate, it).coerceAtLeast(1L) }
        ?: 1L
    val elapsedDays = targetDate
        ?.let { ChronoUnit.DAYS.between(createdDate, today).coerceIn(0L, totalDays) }
        ?: 0L
    val expected = (elapsedDays.toFloat() / totalDays.toFloat()).coerceIn(0f, 1f)
    val daysLeft = targetDate
        ?.let { ChronoUnit.DAYS.between(today, it).coerceAtLeast(0L) }
        ?: 0L
    val expired = targetDate?.let { today.isAfter(it) } ?: false
    val reached = focusedMinutes >= target

    return GoalProgress(
        goal = goal,
        focusedMinutes = focusedMinutes,
        percent = percent,
        daysLeft = daysLeft,
        expectedPercent = expected,
        onTrack = reached || percent + 0.02f >= expected,
        reached = reached,
        expired = expired
    )
}

/** "Goal reached", "Target date passed", "On track" or "Behind schedule". */
fun goalStatusLabel(progress: GoalProgress): String = when {
    progress.reached -> "Goal reached"
    progress.expired -> "Target date passed"
    progress.onTrack -> "On track"
    else -> "Behind schedule"
}

/** "12 days left", "1 day left", "Target is today" or "Complete". */
fun goalDaysLabel(progress: GoalProgress): String = when {
    progress.reached -> "Complete"
    progress.daysLeft > 1L -> "${progress.daysLeft} days left"
    progress.daysLeft == 1L -> "1 day left"
    progress.expired -> "Target date passed"
    else -> "Target is today"
}

/** Active goals first, then reached or expired ones; nearest target date first. */
fun sortGoalProgress(progress: List<GoalProgress>): List<GoalProgress> =
    progress.sortedWith(
        compareBy<GoalProgress> { it.reached || it.expired }
            .thenBy { it.goal.targetDate }
            .thenBy { it.goal.name }
    )

/** "45m", "10h" or "2h 30m" for a minute count. */
fun goalMinutesLabel(minutes: Long): String {
    val safe = minutes.coerceAtLeast(0L)
    return when {
        safe < 60L -> "${safe}m"
        safe % 60L == 0L -> "${safe / 60L}h"
        else -> "${safe / 60L}h ${safe % 60L}m"
    }
}

/** "12 days left", "Target is today" or "Target date passed" for an ISO date. */
fun goalTargetLabel(isoDate: String, today: LocalDate = LocalDate.now()): String {
    val target = runCatching { LocalDate.parse(isoDate) }.getOrNull() ?: return "No date"
    val days = ChronoUnit.DAYS.between(today, target)
    return when {
        days > 1L -> "$days days left"
        days == 1L -> "1 day left"
        days == 0L -> "Target is today"
        else -> "Target date passed"
    }
}
