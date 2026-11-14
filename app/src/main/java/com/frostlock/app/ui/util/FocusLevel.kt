package com.frostlock.app.ui.util

/**
 * A local, offline "freeze level" derived from lifetime focus minutes.
 *
 * Levels get further apart as they go: reaching level 2 takes one hour, level 3
 * another two, level 4 another three, and so on. Nothing is stored — the level
 * is recomputed from the same session totals the stats screen already shows, so
 * it can never drift out of sync with the history.
 */
data class FocusLevel(
    val level: Int,
    val title: String,
    val totalMinutes: Long,
    /** Minutes already focused inside the current level. */
    val minutesIntoLevel: Long,
    /** Minutes this level requires in total. */
    val minutesForNextLevel: Long,
    val minutesToNextLevel: Long,
    val progress: Float
)

private val LEVEL_TITLES = listOf(
    "Frostbite",
    "Snowflake",
    "Ice shard",
    "Glacier",
    "Iceberg",
    "Permafrost",
    "Absolute zero"
)

private const val MINUTES_PER_STEP = 60L

/** Total minutes needed to reach [level] (level 1 starts at zero). */
internal fun minutesForLevel(level: Int): Long {
    if (level <= 1) return 0L
    val steps = level - 1L
    return MINUTES_PER_STEP * steps * (steps + 1L) / 2L
}

fun focusLevelTitle(level: Int): String =
    LEVEL_TITLES.getOrNull(level - 1) ?: "Cryomaster"

fun focusLevelFor(totalSeconds: Long): FocusLevel {
    val totalMinutes = (totalSeconds.coerceAtLeast(0L)) / 60L

    var level = 1
    while (totalMinutes >= minutesForLevel(level + 1)) {
        level++
    }

    val levelStart = minutesForLevel(level)
    val nextStart = minutesForLevel(level + 1)
    val span = (nextStart - levelStart).coerceAtLeast(1L)
    val into = totalMinutes - levelStart

    return FocusLevel(
        level = level,
        title = focusLevelTitle(level),
        totalMinutes = totalMinutes,
        minutesIntoLevel = into,
        minutesForNextLevel = span,
        minutesToNextLevel = (span - into).coerceAtLeast(0L),
        progress = (into.toFloat() / span.toFloat()).coerceIn(0f, 1f)
    )
}
