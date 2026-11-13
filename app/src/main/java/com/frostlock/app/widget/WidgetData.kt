package com.frostlock.app.widget

import com.frostlock.app.data.local.prefs.FocusSettings
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.ceil

/** Everything the home-screen widgets render. Cached so the first frame isn't blank. */
data class WidgetData(
    val todaySeconds: Long = 0L,
    val goalMinutes: Int = FocusSettings.DEFAULT_GOAL_MINUTES,
    val streak: Int = 0,
    /** Focus seconds for the current week, Monday first. */
    val weekSeconds: List<Long> = List(DAYS_IN_WEEK) { 0L }
) {
    companion object {
        const val DAYS_IN_WEEK = 7
    }
}

/** One column in the week widget. */
data class WeekBar(
    val label: String,
    val seconds: Long,
    val isToday: Boolean
)

data class WeekSummary(
    val bars: List<WeekBar>,
    val totalSeconds: Long,
    val averageSeconds: Long,
    /** Short day name of the best day, or null when the week is empty. */
    val bestDay: String?
)

/**
 * Builds the Monday-first week strip and its totals. Kept free of Android types so
 * the widget maths can be unit tested.
 */
fun summarizeWeek(weekSeconds: List<Long>, today: DayOfWeek): WeekSummary {
    val seconds = (0 until WidgetData.DAYS_IN_WEEK).map { index ->
        weekSeconds.getOrElse(index) { 0L }.coerceAtLeast(0L)
    }
    val todayIndex = today.value - 1
    val bars = seconds.mapIndexed { index, value ->
        WeekBar(
            label = dayName(index + 1, TextStyle.NARROW),
            seconds = value,
            isToday = index == todayIndex
        )
    }
    val bestIndex = seconds.indices.maxByOrNull { seconds[it] }
    return WeekSummary(
        bars = bars,
        totalSeconds = seconds.sum(),
        averageSeconds = seconds.sum() / WidgetData.DAYS_IN_WEEK,
        bestDay = bestIndex
            ?.takeIf { seconds[it] > 0 }
            ?.let { dayName(it + 1, TextStyle.SHORT) }
    )
}

/** Daily-goal completion as 0…100. */
fun goalPercent(todaySeconds: Long, goalMinutes: Int): Int {
    if (goalMinutes <= 0) return 0
    val percent = (todaySeconds / 60.0) / goalMinutes * 100
    return percent.toInt().coerceIn(0, 100)
}

/** "28m left of 1h 30m" or "Goal reached today". */
fun goalRemainingLabel(todaySeconds: Long, goalMinutes: Int): String {
    if (goalMinutes <= 0) return "No daily goal set"
    val goalSeconds = goalMinutes * 60L
    if (todaySeconds >= goalSeconds) return "Goal reached today"
    val remainingMinutes = ceil((goalSeconds - todaySeconds) / 60.0).toInt().coerceAtLeast(1)
    return "${formatWidgetDuration(remainingMinutes * 60L)} left of ${formatWidgetDuration(goalSeconds)}"
}

/** Compact duration used by all three widgets: "2h 15m", "45m", "0m". */
fun formatWidgetDuration(totalSeconds: Long): String {
    val minutes = (totalSeconds / 60L).toInt()
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours > 0 && rest > 0 -> "${hours}h ${rest}m"
        hours > 0 -> "${hours}h"
        else -> "${rest}m"
    }
}

private fun dayName(dayOfWeek: Int, style: TextStyle): String =
    DayOfWeek.of(dayOfWeek).getDisplayName(style, Locale.ENGLISH)
