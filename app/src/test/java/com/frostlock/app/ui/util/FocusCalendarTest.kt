package com.frostlock.app.ui.util

import com.frostlock.app.data.local.db.SessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class FocusCalendarTest {

    private val month = YearMonth.of(2026, 11)
    private val today = LocalDate.of(2026, 11, 13) // Friday

    private fun session(
        id: Long,
        date: LocalDate,
        minutes: Int,
        hour: Int = 9,
        completed: Boolean = true
    ): SessionEntity {
        val start = date.atTime(hour, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return SessionEntity(
            id = id,
            mode = "TIMER",
            plannedSeconds = minutes * 60,
            durationSeconds = minutes * 60,
            startedAt = start,
            endedAt = start + minutes * 60_000L,
            completed = completed
        )
    }

    @Test
    fun `grid is six monday-first weeks`() {
        val focusMonth = buildFocusMonth(month, emptyList(), today)

        assertEquals(42, focusMonth.days.size)
        // 1 November 2026 is a Sunday, so the grid starts on Monday 26 October.
        assertEquals(LocalDate.of(2026, 10, 26), focusMonth.days.first().date)
        assertEquals(LocalDate.of(2026, 12, 6), focusMonth.days.last().date)
        assertFalse(focusMonth.days.first().inMonth)
        assertTrue(focusMonth.days[6].inMonth)
    }

    @Test
    fun `month totals only count days inside the month`() {
        val focusMonth = buildFocusMonth(
            month,
            listOf(
                session(1, LocalDate.of(2026, 11, 2), 30),
                session(2, LocalDate.of(2026, 11, 2), 15),
                session(3, LocalDate.of(2026, 11, 20), 90),
                session(4, LocalDate.of(2026, 10, 30), 120) // leading cell only
            ),
            today
        )

        assertEquals(135L * 60L, focusMonth.totalSeconds)
        assertEquals(3, focusMonth.sessions)
        assertEquals(2, focusMonth.activeDays)
        assertEquals(LocalDate.of(2026, 11, 20), focusMonth.bestDay?.date)
    }

    @Test
    fun `leading and trailing cells still show their sessions`() {
        val focusMonth = buildFocusMonth(
            month,
            listOf(session(1, LocalDate.of(2026, 10, 30), 45)),
            today
        )

        val leading = focusMonth.days.first { it.date == LocalDate.of(2026, 10, 30) }
        assertFalse(leading.inMonth)
        assertEquals(45L * 60L, leading.seconds)
        assertEquals(1, leading.sessions)
    }

    @Test
    fun `today is marked once`() {
        val focusMonth = buildFocusMonth(month, emptyList(), today)

        assertEquals(1, focusMonth.days.count { it.isToday })
        assertTrue(focusMonth.days.first { it.isToday }.inMonth)
    }

    @Test
    fun `intensity buckets grow with focus time`() {
        assertEquals(0, focusIntensity(0))
        assertEquals(1, focusIntensity(29 * 60L))
        assertEquals(2, focusIntensity(30 * 60L))
        assertEquals(3, focusIntensity(60 * 60L))
        assertEquals(4, focusIntensity(120 * 60L))
        assertEquals(4, focusIntensity(300 * 60L))
    }

    @Test
    fun `sessions on a day are newest first`() {
        val date = LocalDate.of(2026, 11, 13)
        val early = session(1, date, 25, hour = 9)
        val late = session(2, date, 25, hour = 14)

        assertEquals(listOf(2L, 1L), sessionsOn(date, listOf(early, late)).map { it.id })
    }

    @Test
    fun `empty month has no best day`() {
        val focusMonth = buildFocusMonth(month, emptyList(), today)

        assertEquals(0L, focusMonth.totalSeconds)
        assertEquals(0, focusMonth.activeDays)
        assertEquals(null, focusMonth.bestDay)
        assertEquals(0L, focusMonth.averageSeconds)
    }
}
