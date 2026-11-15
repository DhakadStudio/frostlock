package com.frostlock.app.ui.util

import com.frostlock.app.data.local.db.SessionEntity
import com.frostlock.app.data.local.prefs.FocusGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class FocusGoalsTest {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val today = LocalDate.of(2026, 11, 15)

    private fun at(date: LocalDate, hour: Int = 10): Long =
        LocalDateTime.of(date, java.time.LocalTime.of(hour, 0))
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    private fun goal(
        id: Long = 1L,
        name: String = "Final exams",
        targetDate: String = "2026-11-20",
        targetMinutes: Int = 600,
        createdAt: Long = at(LocalDate.of(2026, 11, 10))
    ) = FocusGoal(id, name, targetDate, targetMinutes, createdAt)

    private fun session(id: Long, date: LocalDate, durationSeconds: Int, hour: Int = 10) =
        SessionEntity(
            id = id,
            mode = "TIMER",
            plannedSeconds = durationSeconds,
            durationSeconds = durationSeconds,
            startedAt = at(date, hour),
            endedAt = at(date, hour) + durationSeconds * 1000L,
            completed = true
        )

    @Test
    fun `sessions count only inside the goal window`() {
        val progress = goalProgress(
            goal = goal(),
            sessions = listOf(
                session(1, LocalDate.of(2026, 11, 9), 3600),   // before creation
                session(2, LocalDate.of(2026, 11, 11), 3600),   // 60 min
                session(3, LocalDate.of(2026, 11, 15), 7200),   // 120 min
                session(4, LocalDate.of(2026, 11, 21), 3600)    // after target day
            ),
            today = today
        )

        assertEquals(180L, progress.focusedMinutes)
        assertEquals(0.3f, progress.percent, 0.001f)
        assertEquals(5L, progress.daysLeft)
        assertEquals(0.5f, progress.expectedPercent, 0.001f)
        assertFalse(progress.reached)
        assertFalse(progress.expired)
        assertFalse(progress.onTrack)
    }

    @Test
    fun `reaching the target caps progress and marks the goal complete`() {
        val progress = goalProgress(
            goal = goal(targetMinutes = 120),
            sessions = listOf(session(1, LocalDate.of(2026, 11, 12), 7200)),
            today = today
        )

        assertTrue(progress.reached)
        assertTrue(progress.onTrack)
        assertEquals(1f, progress.percent, 0.001f)
        assertEquals("Complete", goalDaysLabel(progress))
        assertEquals("Goal reached", goalStatusLabel(progress))
    }

    @Test
    fun `a passed date is expired but keeps its focused time`() {
        val progress = goalProgress(
            goal = goal(targetDate = "2026-11-14", targetMinutes = 600),
            sessions = listOf(session(1, LocalDate.of(2026, 11, 12), 3600)),
            today = today
        )

        assertTrue(progress.expired)
        assertEquals(0L, progress.daysLeft)
        assertEquals(1f, progress.expectedPercent, 0.001f)
        assertEquals("Target date passed", goalStatusLabel(progress))
        assertEquals("Target date passed", goalDaysLabel(progress))
    }

    @Test
    fun `an on-track goal is ahead of its expected pace`() {
        val progress = goalProgress(
            goal = goal(targetMinutes = 120),
            sessions = listOf(session(1, LocalDate.of(2026, 11, 12), 3600)),
            today = today
        )

        assertTrue(progress.onTrack)
        assertEquals("On track", goalStatusLabel(progress))
        assertEquals("5 days left", goalDaysLabel(progress))
    }

    @Test
    fun `goals sort active first then by target date`() {
        val active = goalProgress(goal(id = 1, name = "Later", targetDate = "2026-12-01"), emptyList(), today)
        val soon = goalProgress(goal(id = 2, name = "Sooner", targetDate = "2026-11-18"), emptyList(), today)
        val done = goalProgress(
            goal(id = 3, name = "Done", targetDate = "2026-11-16", targetMinutes = 30),
            listOf(session(1, LocalDate.of(2026, 11, 12), 3600)),
            today
        )

        val sorted = sortGoalProgress(listOf(done, active, soon))

        assertEquals(listOf("Sooner", "Later", "Done"), sorted.map { it.goal.name })
    }

    @Test
    fun `labels use days, hours and minutes`() {
        assertEquals("45m", goalMinutesLabel(45))
        assertEquals("10h", goalMinutesLabel(600))
        assertEquals("2h 30m", goalMinutesLabel(150))
        assertEquals("12 days left", goalTargetLabel("2026-11-27", today))
        assertEquals("1 day left", goalTargetLabel("2026-11-16", today))
        assertEquals("Target is today", goalTargetLabel("2026-11-15", today))
        assertEquals("Target date passed", goalTargetLabel("2026-11-14", today))
    }
}
