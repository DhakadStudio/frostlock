package com.frostlock.app.ui.util

import com.frostlock.app.data.local.db.SessionEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

/**
 * A small, earned badge for one finished session. Stamps are derived from the
 * session itself (start time, weekday, length) so nothing extra is stored and
 * the same session always earns the same stamps.
 */
data class SessionStamp(
    val id: String,
    val label: String
)

/**
 * Every stamp this session earns, in a stable display order:
 * time-of-day, weekend, then length.
 *
 * Length stamps only apply to completed sessions — stopping early should not
 * earn a "Deep freeze". Time and weekend stamps apply regardless of outcome,
 * because they describe when the user showed up.
 */
fun stampsFor(session: SessionEntity): List<SessionStamp> {
    val start = Instant.ofEpochMilli(session.startedAt)
        .atZone(ZoneId.systemDefault())
    val minutes = session.durationSeconds / 60
    val stamps = mutableListOf<SessionStamp>()

    if (start.hour < 8) stamps += SessionStamp("early_bird", "Early bird")
    if (start.hour >= 22) stamps += SessionStamp("night_owl", "Night owl")
    if (start.dayOfWeek == DayOfWeek.SATURDAY || start.dayOfWeek == DayOfWeek.SUNDAY) {
        stamps += SessionStamp("weekend", "Weekend")
    }

    if (session.completed) {
        when {
            minutes >= 120 -> stamps += SessionStamp("marathon", "2-hour freeze")
            minutes >= 90 -> stamps += SessionStamp("deep", "Deep freeze")
            minutes in 1..15 -> stamps += SessionStamp("sprint", "Quick sprint")
        }
    }

    return stamps
}
