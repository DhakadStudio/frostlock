package com.frostlock.app.ui.util

import com.frostlock.app.data.local.prefs.FocusSchedule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ScheduleStreakTest {

    // Thursday 19 November 2026.
    private val today = LocalDate.of(2026, 11, 19)

    private fun rule(
        id: Long = 1L,
        days: Set<Int> = setOf(1, 2, 3, 4, 5),
        dates: Set<String> = emptySet(),
        startMinutes: Int = 9 * 60,
        endMinutes: Int = 17 * 60,
        enabled: Boolean = true
    ) = FocusSchedule(
        id = id,
        label = "Focus",
        days = days,
        dates = dates,
        startMinutes = startMinutes,
        endMinutes = endMinutes,
        enabled = enabled
    )

    private fun start(date: LocalDate, hour: Int, minute: Int = 0): Long =
        date.atTime(hour, minute)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    @Test
    fun `no rules means no schedule streak`() {
        assertEquals(0, computeScheduleStreak(emptyList(), listOf(start(today, 10)), today))
    }

    @Test
    fun `consecutive scheduled days with sessions count`() {
        val sessions = listOf(
            start(LocalDate.of(2026, 11, 17), 10),
            start(LocalDate.of(2026, 11, 18), 10),
            start(LocalDate.of(2026, 11, 19), 10)
        )

        assertEquals(3, computeScheduleStreak(listOf(rule()), sessions, today))
    }

    @Test
    fun `a scheduled day without a session yet does not break the streak`() {
        val sessions = listOf(
            start(LocalDate.of(2026, 11, 17), 10),
            start(LocalDate.of(2026, 11, 18), 10)
        )

        assertEquals(2, computeScheduleStreak(listOf(rule()), sessions, today))
    }

    @Test
    fun `a missed scheduled day breaks the streak`() {
        val sessions = listOf(
            start(LocalDate.of(2026, 11, 16), 10),
            start(LocalDate.of(2026, 11, 17), 10),
            start(LocalDate.of(2026, 11, 19), 10)
        )

        assertEquals(1, computeScheduleStreak(listOf(rule()), sessions, today))
    }

    @Test
    fun `unscheduled weekends neither extend nor break the streak`() {
        // Friday 13 through Wednesday 18, then today (Thursday) is still open.
        // The weekend is skipped without resetting the run.
        val sessions = listOf(
            start(LocalDate.of(2026, 11, 13), 10),
            start(LocalDate.of(2026, 11, 16), 10),
            start(LocalDate.of(2026, 11, 17), 10),
            start(LocalDate.of(2026, 11, 18), 10)
        )

        assertEquals(4, computeScheduleStreak(listOf(rule()), sessions, today))
    }

    @Test
    fun `sessions outside the window do not count`() {
        val sessions = listOf(
            start(LocalDate.of(2026, 11, 17), 8),
            start(LocalDate.of(2026, 11, 18), 18)
        )

        assertEquals(0, computeScheduleStreak(listOf(rule()), sessions, today))
    }

    @Test
    fun `overnight windows count for the day the rule started`() {
        val night = rule(
            days = setOf(1, 2, 3, 4, 5),
            startMinutes = 22 * 60,
            endMinutes = 2 * 60
        )
        // Tuesday 01:00 belongs to Monday's 22:00–02:00 window.
        val tuesday = LocalDate.of(2026, 11, 17)
        val sessions = listOf(start(tuesday, 1))

        assertEquals(1, computeScheduleStreak(listOf(night), sessions, tuesday))
    }

    @Test
    fun `one-off date rules count on their date`() {
        val oneOff = rule(
            days = emptySet(),
            dates = setOf("2026-11-19")
        )

        assertEquals(
            1,
            computeScheduleStreak(listOf(oneOff), listOf(start(today, 10)), today)
        )
    }

    @Test
    fun `disabled rules are ignored`() {
        val sessions = listOf(start(today, 10))

        assertEquals(
            0,
            computeScheduleStreak(listOf(rule(enabled = false)), sessions, today)
        )
    }
}
