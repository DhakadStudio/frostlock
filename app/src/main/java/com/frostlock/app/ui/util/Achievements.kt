package com.frostlock.app.ui.util

import java.time.LocalDate

/** The three kinds of progress FrostLock celebrates. */
enum class AchievementCategory { FOCUS, STREAK, SESSIONS }

/** One local, offline badge earned from focus history. */
data class Achievement(
    val id: String,
    val badge: String,
    val title: String,
    val description: String,
    val current: Long,
    val target: Long,
    val category: AchievementCategory
) {
    val unlocked: Boolean get() = current >= target

    val progress: Float
        get() = if (target <= 0L) 0f else (current.toFloat() / target).coerceIn(0f, 1f)

    val progressLabel: String
        get() {
            val shown = current.coerceAtMost(target)
            return when (category) {
                AchievementCategory.FOCUS ->
                    "${formatDuration(shown)} / ${formatDuration(target)}"
                AchievementCategory.STREAK -> "$shown / $target days"
                AchievementCategory.SESSIONS -> "$shown / $target sessions"
            }
        }
}

private val FOCUS_TARGETS_HOURS = listOf(1L, 3L, 5L, 10L, 25L, 50L, 100L)
private val STREAK_TARGET_DAYS = listOf(3, 7, 14, 30, 100)
private val SESSION_TARGETS = listOf(1, 10, 50, 100)

/**
 * Builds the complete badge set from data FrostLock already stores locally.
 * Focus totals use every saved session, while the streak is the best run found
 * in the recent session history.
 */
fun buildAchievements(
    totalSeconds: Long,
    totalSessions: Int,
    startTimes: List<Long>
): List<Achievement> {
    val bestStreak = longestStreak(startTimes).toLong()

    val focus = FOCUS_TARGETS_HOURS.map { hours ->
        Achievement(
            id = "focus_${hours}h",
            badge = "${hours}H",
            title = "Focus ${hours}h",
            description = "Focus for ${hours}h in total",
            current = totalSeconds,
            target = hours * 3600L,
            category = AchievementCategory.FOCUS
        )
    }

    val streaks = STREAK_TARGET_DAYS.map { days ->
        Achievement(
            id = "streak_${days}d",
            badge = "${days}D",
            title = "Streak ${days}d",
            description = "Focus on $days days in a row",
            current = bestStreak,
            target = days.toLong(),
            category = AchievementCategory.STREAK
        )
    }

    val sessions = SESSION_TARGETS.map { count ->
        Achievement(
            id = "sessions_$count",
            badge = "$count",
            title = if (count == 1) "First session" else "$count sessions",
            description = if (count == 1) {
                "Complete your first focus session"
            } else {
                "Complete $count focus sessions"
            },
            current = totalSessions.toLong(),
            target = count.toLong(),
            category = AchievementCategory.SESSIONS
        )
    }

    return focus + streaks + sessions
}

/** Longest run of consecutive days with at least one session. */
fun longestStreak(startTimes: List<Long>): Int {
    if (startTimes.isEmpty()) return 0
    val dates: List<LocalDate> = startTimes
        .map { localDateOf(it) }
        .distinct()
        .sorted()

    var best = 1
    var run = 1
    for (index in 1 until dates.size) {
        if (dates[index] == dates[index - 1].plusDays(1)) {
            run++
            if (run > best) best = run
        } else {
            run = 1
        }
    }
    return best
}

/** Copy used by the local share sheet. No image or network is needed. */
fun achievementShareText(achievement: Achievement): String =
    "I unlocked \"${achievement.title}\" in FrostLock — " +
        "${achievement.description} (${achievement.progressLabel}).\n\n" +
        "FrostLock: offline focus timer and app blocker."

fun achievementCategoryLabel(category: AchievementCategory): String = when (category) {
    AchievementCategory.FOCUS -> "Focus milestones"
    AchievementCategory.STREAK -> "Streak milestones"
    AchievementCategory.SESSIONS -> "Session milestones"
}
