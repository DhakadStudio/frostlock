package com.frostlock.app.ui.util

import com.frostlock.app.data.local.db.SessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SessionStampsTest {

    private fun session(
        date: LocalDate = LocalDate.of(2026, 11, 13), // Friday
        hour: Int = 10,
        minutes: Int = 45,
        completed: Boolean = true
    ): SessionEntity {
        val start = date.atTime(hour, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return SessionEntity(
            id = 1,
            mode = "TIMER",
            plannedSeconds = minutes * 60,
            durationSeconds = minutes * 60,
            startedAt = start,
            endedAt = start + minutes * 60_000L,
            completed = completed
        )
    }

    private fun labels(session: SessionEntity) = stampsFor(session).map { it.label }

    @Test
    fun `early bird before eight`() {
        assertEquals(listOf("Early bird"), labels(session(hour = 7)))
    }

    @Test
    fun `night owl from ten pm`() {
        assertEquals(listOf("Night owl"), labels(session(hour = 22)))
    }

    @Test
    fun `weekend stamp applies on saturday and sunday`() {
        assertTrue("Weekend" in labels(session(date = LocalDate.of(2026, 11, 14))))
        assertTrue("Weekend" in labels(session(date = LocalDate.of(2026, 11, 15))))
        assertTrue("Weekend" !in labels(session(date = LocalDate.of(2026, 11, 13))))
    }

    @Test
    fun `length stamps use the longest matching bucket`() {
        assertEquals(listOf("Deep freeze"), labels(session(minutes = 90)))
        assertEquals(listOf("2-hour freeze"), labels(session(minutes = 120)))
        assertEquals(listOf("Quick sprint"), labels(session(minutes = 12)))
        assertTrue(labels(session(minutes = 45)).isEmpty())
    }

    @Test
    fun `incomplete sessions do not earn length stamps`() {
        assertEquals(emptyList<String>(), labels(session(minutes = 150, completed = false)))
        assertEquals(emptyList<String>(), labels(session(minutes = 5, completed = false)))
    }

    @Test
    fun `time and weekend stamps survive an early stop`() {
        val earlyWeekend = session(
            date = LocalDate.of(2026, 11, 14),
            hour = 7,
            minutes = 10,
            completed = false
        )

        assertEquals(listOf("Early bird", "Weekend"), labels(earlyWeekend))
    }

    @Test
    fun `stamps keep a stable order`() {
        val rich = session(
            date = LocalDate.of(2026, 11, 14),
            hour = 7,
            minutes = 130
        )

        assertEquals(
            listOf("Early bird", "Weekend", "2-hour freeze"),
            labels(rich)
        )
        assertEquals(listOf("early_bird", "weekend", "marathon"), stampsFor(rich).map { it.id })
    }
}
