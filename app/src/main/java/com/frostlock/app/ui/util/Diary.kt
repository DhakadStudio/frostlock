package com.frostlock.app.ui.util

import com.frostlock.app.data.local.db.SessionEntity
import java.time.LocalDate

/** One date's worth of diary entries, newest first. */
data class DiaryDay(
    val date: LocalDate,
    val sessions: List<SessionEntity>,
    val totalSeconds: Long
)

/** Headline numbers for the diary hero card. */
data class DiarySummary(
    val noteCount: Int,
    val dayCount: Int,
    val focusedSeconds: Long
)

/**
 * Groups sessions into calendar days using the time the session ended, so a
 * session that crosses midnight lands on the day it finished.
 */
fun buildDiaryDays(sessions: List<SessionEntity>): List<DiaryDay> =
    sessions
        .groupBy { localDateOf(it.endedAt) }
        .map { (date, group) ->
            DiaryDay(
                date = date,
                sessions = group.sortedByDescending { it.endedAt },
                totalSeconds = group.sumOf { it.durationSeconds.toLong() }
            )
        }
        .sortedByDescending { it.date }

/**
 * Counts only sessions that actually carry a reflection. The diary is about
 * the notes, so silent sessions must not inflate the "entries" number.
 */
fun diarySummary(sessions: List<SessionEntity>): DiarySummary {
    val withNotes = sessions.filter { it.note.isNotBlank() }
    return DiarySummary(
        noteCount = withNotes.size,
        dayCount = withNotes.map { localDateOf(it.endedAt) }.distinct().size,
        focusedSeconds = withNotes.sumOf { it.durationSeconds.toLong() }
    )
}

/** Case-insensitive search across the note, subject and timer mode. */
fun SessionEntity.matchesDiaryQuery(query: String): Boolean {
    val needle = query.trim()
    if (needle.isEmpty()) return true
    return note.contains(needle, ignoreCase = true) ||
        tag.contains(needle, ignoreCase = true) ||
        modeLabel(mode).contains(needle, ignoreCase = true)
}
