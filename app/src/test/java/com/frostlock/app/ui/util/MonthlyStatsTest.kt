package com.frostlock.app.ui.util

import com.frostlock.app.data.local.db.SessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class MonthlyStatsTest {

    private val today = LocalDate.of(2026, 11, 15)

    private fun session(id: Long, date: LocalDate, minutes: Int): SessionEntity {
        val start = date.atTime(9, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return SessionEntity(
            id = id,
            mode = "POMODORO",
            plannedSeconds = minutes * 60,
            durationSeconds = minutes * 60,
            startedAt = start,
            endedAt = start + minutes * 60_000L,
            completed = true
        )
    }

    @Test
    fun `empty history still shows six month buckets`() {
        val summary = buildMonthlySummary(emptyList(), today)

        assertEquals(6, summary.months.size)
        assertEquals(YearMonth.of(2026, 6), summary.months.first().month)
        assertEquals(YearMonth.of(2026, 11), summary.months.last().month)
        assertEquals(0L, summary.current.seconds)
        assertEquals(0, summary.current.activeDays)
        assertEquals(0L, summary.averagePerActiveDay)
    }

    @Test
    fun `current month totals only count its sessions`() {
        val sessions = listOf(
            session(1, LocalDate.of(2026, 11, 2), 25),
            session(2, LocalDate.of(2026, 11, 2), 35),
            session(3, LocalDate.of(2026, 11, 14), 60),
            session(4, LocalDate.of(2026, 10, 30), 90)
        )

        val summary = buildMonthlySummary(sessions, today)
        val november = summary.current

        assertEquals(YearMonth.of(2026, 11), november.month)
        assertEquals(120 * 60L, november.seconds)
        assertEquals(3, november.sessions)
        assertEquals(2, november.activeDays)
        assertEquals(60 * 60L, november.bestDaySeconds)
        assertEquals(60 * 60L, summary.averagePerActiveDay)
    }

    @Test
    fun `older months keep their own totals and labels`() {
        val sessions = listOf(
            session(1, LocalDate.of(2026, 9, 10), 30),
            session(2, LocalDate.of(2026, 10, 10), 45),
            session(3, LocalDate.of(2026, 11, 10), 20)
        )

        val summary = buildMonthlySummary(sessions, today)

        assertEquals(listOf("Jun", "Jul", "Aug", "Sep", "Oct", "Nov"), summary.months.map { it.label })
        assertEquals(30 * 60L, summary.months[3].seconds)
        assertEquals(45 * 60L, summary.months[4].seconds)
        assertEquals(20 * 60L, summary.months[5].seconds)
        assertFalse(summary.months[0].hasFocus)
        assertTrue(summary.months[5].hasFocus)
    }

    @Test
    fun `monthsBack is clamped to a useful range`() {
        assertEquals(1, buildMonthlySummary(emptyList(), today, monthsBack = 0).months.size)
        assertEquals(24, buildMonthlySummary(emptyList(), today, monthsBack = 99).months.size)
    }

    @Test
    fun `average divides by active days not calendar days`() {
        val sessions = listOf(
            session(1, LocalDate.of(2026, 11, 3), 20),
            session(2, LocalDate.of(2026, 11, 3), 20),
            session(3, LocalDate.of(2026, 11, 9), 40)
        )

        val summary = buildMonthlySummary(sessions, today)

        assertEquals(2, summary.current.activeDays)
        assertEquals(40 * 60L, summary.averagePerActiveDay)
    }
}
