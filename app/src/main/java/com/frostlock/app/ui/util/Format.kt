package com.frostlock.app.ui.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.vector.ImageVector

/** Formats seconds as a clock: MM:SS, or H:MM:SS when at least an hour. */
fun formatClock(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) {
        String.format(Locale.US, "%d:%02d:%02d", h, m, sec)
    } else {
        String.format(Locale.US, "%02d:%02d", m, sec)
    }
}

/** Formats seconds as a compact human duration, e.g. "2h 15m", "45m", "30s". */
fun formatDuration(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        m > 0 -> "${m}m"
        else -> "${s}s"
    }
}

/** Like [formatDuration], but a true zero reads "0m" instead of "0s". */
fun formatDurationOrZero(totalSeconds: Long): String =
    if (totalSeconds <= 0L) "0m" else formatDuration(totalSeconds)

fun modeLabel(mode: String): String = when (mode) {
    "TIMER" -> "Timer"
    "POMODORO" -> "Pomodoro"
    "STOPWATCH" -> "Stopwatch"
    else -> mode
}

/** Icon shown next to each session type in lists. */
fun modeIcon(mode: String): ImageVector = when (mode) {
    "TIMER" -> Icons.Default.HourglassTop
    "STOPWATCH" -> Icons.Default.Speed
    else -> Icons.Default.Timer
}

fun localDateOf(epochMillis: Long): LocalDate =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()

/** "Today 14:05", "Yesterday 09:12", or "12 Nov 14:05". */
fun formatSessionTime(epochMillis: Long): String {
    val date = localDateOf(epochMillis)
    val time = Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm", Locale.US))
    val today = LocalDate.now()
    return when (date) {
        today -> "Today $time"
        today.minusDays(1) -> "Yesterday $time"
        else -> date.format(DateTimeFormatter.ofPattern("d MMM", Locale.US)) + " $time"
    }
}

/** Consecutive-day streak from a list of session start timestamps. */
fun computeStreak(startTimes: List<Long>): Int {
    if (startTimes.isEmpty()) return 0
    val dates = startTimes
        .map { localDateOf(it) }
        .distinct()
        .sortedDescending()
    if (dates.isEmpty()) return 0

    val today = LocalDate.now()
    var cursor = when (dates.first()) {
        today -> today
        today.minusDays(1) -> today.minusDays(1)
        else -> return 0
    }
    var streak = 0
    for (d in dates) {
        if (d == cursor) {
            streak++
            cursor = cursor.minusDays(1)
        } else if (d.isBefore(cursor)) {
            break
        }
    }
    return streak
}
