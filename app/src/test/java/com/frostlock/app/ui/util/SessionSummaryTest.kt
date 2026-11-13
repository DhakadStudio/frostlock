package com.frostlock.app.ui.util

import com.frostlock.app.data.local.db.SessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionSummaryTest {

    private fun session(
        plannedSeconds: Int = 1_500,
        durationSeconds: Int = 1_500,
        completed: Boolean = true,
        tag: String = "",
        mode: String = "TIMER"
    ) = SessionEntity(
        id = 1,
        mode = mode,
        plannedSeconds = plannedSeconds,
        durationSeconds = durationSeconds,
        startedAt = 0L,
        endedAt = durationSeconds * 1_000L,
        completed = completed,
        tag = tag
    )

    @Test
    fun `completed timer reaches full progress and labels the outcome`() {
        val summary = buildSessionSummary(session(tag = "Physics"), streakDays = 13)

        assertEquals("Physics", summary.subjectLabel)
        assertEquals("Timer", summary.modeLabel)
        assertEquals(1f, summary.progress)
        assertEquals("Completed", summary.outcomeLabel)
        assertEquals("25m", summary.plannedLabel)
        assertTrue(summary.completed)
        assertEquals(13, summary.streakDays)
    }

    @Test
    fun `stopped session keeps its real progress and early outcome`() {
        val summary = buildSessionSummary(
            session(plannedSeconds = 1_500, durationSeconds = 600, completed = false),
            streakDays = 4
        )

        assertEquals(0.4f, summary.progress)
        assertEquals("Stopped early", summary.outcomeLabel)
        assertEquals("Untagged", summary.subjectLabel)
        assertFalse(summary.completed)
    }

    @Test
    fun `stopwatch sessions have no planned target`() {
        val summary = buildSessionSummary(
            session(
                plannedSeconds = 0,
                durationSeconds = 540,
                completed = false,
                mode = "STOPWATCH"
            ),
            streakDays = 0
        )

        assertNull(summary.progress)
        assertEquals("Open-ended", summary.plannedLabel)
        assertEquals("Stopwatch", summary.modeLabel)
        assertEquals("9m", formatDuration(summary.focusedSeconds.toLong()))
    }

    @Test
    fun `progress is clamped when a session overshoots its target`() {
        val summary = buildSessionSummary(
            session(plannedSeconds = 60, durationSeconds = 120),
            streakDays = 1
        )

        assertEquals(1f, summary.progress)
    }
}
