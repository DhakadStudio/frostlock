package com.frostlock.app.ui.util

import com.frostlock.app.data.local.db.SessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ImprovementReportTest {

    private fun session(
        id: Long,
        date: LocalDate,
        minutes: Int,
        completed: Boolean = true
    ): SessionEntity {
        val start = date.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
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
    fun `new user has no history and starts a baseline today`() {
        val report = buildImprovementReport(
            sessions = emptyList(),
            dailyGoalMinutes = 60,
            today = LocalDate.of(2026, 11, 1)
        )

        assertTrue(report.isEmpty)
        assertFalse(report.hasHistory)
        assertFalse(report.hasEnoughHistory)
        assertEquals(0, report.baselineDaysCollected)
        assertTrue(report.metrics.isEmpty())
        assertNull(report.baseline)
    }

    @Test
    fun `baseline counts days collected during the first week`() {
        val firstDay = LocalDate.of(2026, 11, 1)
        val report = buildImprovementReport(
            sessions = listOf(session(1, firstDay, 45)),
            dailyGoalMinutes = 60,
            today = firstDay.plusDays(2)
        )

        assertTrue(report.isBuildingBaseline)
        assertEquals(3, report.baselineDaysCollected)
        assertEquals(7, report.baselineTargetDays)
        assertTrue(report.metrics.isEmpty())
    }

    @Test
    fun `baseline stops at seven days but waits for a full comparison week`() {
        val firstDay = LocalDate.of(2026, 11, 1)
        val report = buildImprovementReport(
            sessions = listOf(session(1, firstDay, 45)),
            dailyGoalMinutes = 60,
            today = firstDay.plusDays(9)
        )

        assertTrue(report.isBuildingBaseline)
        assertEquals(7, report.baselineDaysCollected)
        // First comparable week ends on day 13 after the first session.
        assertEquals(4, report.daysUntilComparison)
        assertTrue(report.metrics.isEmpty())
    }

    @Test
    fun `comparison appears once the windows no longer overlap`() {
        val firstDay = LocalDate.of(2026, 11, 1)
        val report = buildImprovementReport(
            sessions = listOf(
                session(1, firstDay, 60),
                session(2, firstDay.plusDays(1), 30),
                session(3, firstDay.plusDays(23), 120),
                session(4, firstDay.plusDays(24), 60),
                session(5, firstDay.plusDays(25), 60)
            ),
            dailyGoalMinutes = 60,
            today = firstDay.plusDays(29)
        )

        assertTrue(report.hasEnoughHistory)
        assertFalse(report.isBuildingBaseline)
        assertEquals(0, report.daysUntilComparison)
        assertEquals(90L * 60L, report.baseline?.seconds)
        assertEquals(240L * 60L, report.current?.seconds)

        val focus = report.metrics.first { it.label == "Focus time" }
        assertEquals("+167%", focus.deltaText)
        assertEquals(ImprovementTrend.UP, focus.trend)

        val sessions = report.metrics.first { it.label == "Sessions" }
        assertEquals("+1 session", sessions.deltaText)

        val activeDays = report.metrics.first { it.label == "Active days" }
        assertEquals("+1 day", activeDays.deltaText)

        val goals = report.metrics.first { it.label == "Goal hit" }
        assertEquals("+2 days", goals.deltaText)

        val streak = report.metrics.first { it.label == "Best streak" }
        assertEquals("+1 day", streak.deltaText)
    }

    @Test
    fun `goal days require the full daily goal`() {
        val firstDay = LocalDate.of(2026, 11, 1)
        val report = buildImprovementReport(
            sessions = listOf(
                session(1, firstDay, 30),
                session(2, firstDay.plusDays(1), 60),
                session(3, firstDay.plusDays(2), 90)
            ),
            dailyGoalMinutes = 60,
            today = firstDay.plusDays(2)
        )

        assertEquals(2, report.baseline?.goalDays)
    }

    @Test
    fun `best streak finds the longest consecutive run`() {
        val firstDay = LocalDate.of(2026, 11, 1)
        val report = buildImprovementReport(
            sessions = listOf(
                session(1, firstDay, 30),
                session(2, firstDay.plusDays(1), 30),
                session(3, firstDay.plusDays(3), 30),
                session(4, firstDay.plusDays(4), 30),
                session(5, firstDay.plusDays(5), 30)
            ),
            dailyGoalMinutes = 60,
            today = firstDay.plusDays(5)
        )

        assertEquals(3, report.baseline?.bestStreak)
    }

    @Test
    fun `flat metrics say no change`() {
        val firstDay = LocalDate.of(2026, 11, 1)
        val report = buildImprovementReport(
            sessions = listOf(
                session(1, firstDay, 60),
                session(2, firstDay.plusDays(23), 60)
            ),
            dailyGoalMinutes = 60,
            today = firstDay.plusDays(29)
        )

        val focus = report.metrics.first { it.label == "Focus time" }
        assertEquals("0%", focus.deltaText)
        assertEquals(ImprovementTrend.FLAT, focus.trend)
    }
}
