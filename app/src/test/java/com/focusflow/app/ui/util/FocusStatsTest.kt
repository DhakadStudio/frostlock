package com.focusflow.app.ui.util

import com.focusflow.app.data.local.db.SessionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class FocusStatsTest {

    private fun session(tag: String, seconds: Int) = SessionEntity(
        mode = "POMODORO",
        plannedSeconds = seconds,
        durationSeconds = seconds,
        startedAt = 0L,
        endedAt = seconds * 1000L,
        completed = true,
        tag = tag
    )

    @Test
    fun `groups sessions by tag and sorts by total time`() {
        val breakdown = tagBreakdown(
            listOf(
                session("Physics", 600),
                session("Math", 900),
                session("Physics", 1200),
                session("", 300)
            )
        )

        assertEquals(listOf("Physics", "Math", "Untagged"), breakdown.map { it.tag })
        assertEquals(listOf(1800L, 900L, 300L), breakdown.map { it.seconds })
    }

    @Test
    fun `ignores zero length sessions and respects the limit`() {
        val breakdown = tagBreakdown(
            listOf(
                session("Physics", 0),
                session("Math", 500),
                session("Work", 400),
                session("Reading", 300)
            ),
            limit = 2
        )

        assertEquals(listOf("Math", "Work"), breakdown.map { it.tag })
    }

    @Test
    fun `blank tags collapse into one untagged row`() {
        val breakdown = tagBreakdown(
            listOf(session("", 100), session("  ", 200))
        )

        assertEquals(1, breakdown.size)
        assertEquals("Untagged", breakdown.first().tag)
        assertEquals(300L, breakdown.first().seconds)
    }
}
