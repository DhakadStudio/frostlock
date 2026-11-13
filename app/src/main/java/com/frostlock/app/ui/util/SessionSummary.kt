package com.frostlock.app.ui.util

import com.frostlock.app.data.local.db.SessionEntity

/**
 * Presentation-ready numbers for the dialog shown right after a session ends.
 *
 * Stopwatch sessions have no planned total, so [progress] and [plannedLabel]
 * become null / "Open-ended" instead of pretending a target existed.
 */
data class SessionSummary(
    val subjectLabel: String,
    val modeLabel: String,
    val focusedSeconds: Int,
    val plannedSeconds: Int,
    val progress: Float?,
    val completed: Boolean,
    val outcomeLabel: String,
    val plannedLabel: String,
    val streakDays: Int
)

fun buildSessionSummary(session: SessionEntity, streakDays: Int): SessionSummary {
    val planned = session.plannedSeconds.coerceAtLeast(0)
    val focused = session.durationSeconds.coerceAtLeast(0)
    return SessionSummary(
        subjectLabel = session.tag.trim().ifBlank { "Untagged" },
        modeLabel = modeLabel(session.mode),
        focusedSeconds = focused,
        plannedSeconds = planned,
        progress = if (planned > 0) {
            (focused.toFloat() / planned.toFloat()).coerceIn(0f, 1f)
        } else {
            null
        },
        completed = session.completed,
        outcomeLabel = if (session.completed) "Completed" else "Stopped early",
        plannedLabel = if (planned > 0) formatDuration(planned.toLong()) else "Open-ended",
        streakDays = streakDays.coerceAtLeast(0)
    )
}
