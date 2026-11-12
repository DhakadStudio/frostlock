package com.frostlock.app.ui.util

import com.frostlock.app.data.local.db.SessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DiaryTest {

    private fun session(
        id: Long,
        endedAt: LocalDate,
        note: String = "",
        tag: String = "",
        durationSeconds: Int = 1_500
    ) = SessionEntity(
        id = id,
        mode = "TIMER",
        plannedSeconds = 1_500,
        durationSeconds = durationSeconds,
        startedAt = endedAt.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        endedAt = endedAt.atTime(14, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        completed = true,
        tag = tag,
        note = note
    )

    @Test
    fun `groups entries by the day they ended, newest day first`() {
        val today = LocalDate.now()
        val days = buildDiaryDays(
            listOf(
                session(1, today.minusDays(2)),
                session(2, today),
                session(3, today)
            )
        )

        assertEquals(2, days.size)
        assertEquals(today, days.first().date)
        assertEquals(2, days.first().sessions.size)
    }

    @Test
    fun `keeps the newest session at the top of a day`() {
        val today = LocalDate.now()
        val days = buildDiaryDays(
            listOf(
                session(1, today, durationSeconds = 600).copy(endedAt = today.atTime(9, 0).toEpochMillis()),
                session(2, today, durationSeconds = 1_200).copy(endedAt = today.atTime(18, 0).toEpochMillis())
            )
        )

        assertEquals(listOf(2L, 1L), days.first().sessions.map { it.id })
    }

    @Test
    fun `summary counts only sessions that carry a note`() {
        val today = LocalDate.now()
        val summary = diarySummary(
            listOf(
                session(1, today, note = "Learned integrals", durationSeconds = 1_500),
                session(2, today, note = "", durationSeconds = 900),
                session(3, today.minusDays(1), note = "Read chapter 4", durationSeconds = 600)
            )
        )

        assertEquals(2, summary.noteCount)
        assertEquals(2, summary.dayCount)
        assertEquals(2_100L, summary.focusedSeconds)
    }

    @Test
    fun `search matches notes subjects and timer mode`() {
        val today = LocalDate.now()
        val entry = session(1, today, note = "Finished the physics lab", tag = "Physics")

        assertTrue(entry.matchesDiaryQuery("physics"))
        assertTrue(entry.matchesDiaryQuery("LAB"))
        assertTrue(entry.matchesDiaryQuery("timer"))
        assertFalse(entry.matchesDiaryQuery("history"))
        assertTrue(entry.matchesDiaryQuery("  "))
    }
}

private fun java.time.LocalDateTime.toEpochMillis(): Long =
    atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
