package com.frostlock.app.ui.util

import com.frostlock.app.data.local.db.SessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class WeeklyReportTest {

    private val today = LocalDate.of(2026, 11, 12) // Thursday
    private val thisWeek = startOfWeek(today)      // Monday 9 Nov

    private fun session(
        id: Long,
        date: LocalDate,
        hour: Int,
        minutes: Int,
        tag: String = "",
        note: String = "",
        completed: Boolean = true
    ): SessionEntity {
        val start = date.atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return SessionEntity(
            id = id,
            mode = "TIMER",
            plannedSeconds = minutes * 60,
            durationSeconds = minutes * 60,
            startedAt = start,
            endedAt = start + minutes * 60_000L,
            completed = completed,
            tag = tag,
            note = note
        )
    }

    @Test
    fun `week starts on monday and labels relative weeks`() {
        assertEquals(LocalDate.of(2026, 11, 9), thisWeek)
        assertEquals("This week", weekLabel(thisWeek, today))
        assertEquals("Last week", weekLabel(thisWeek.minusWeeks(1), today))
        assertEquals("2 weeks ago", weekLabel(thisWeek.minusWeeks(2), today))
    }

    @Test
    fun `report only counts sessions inside the week`() {
        val report = buildWeeklyReport(
            sessions = listOf(
                session(1, thisWeek, 9, 60, tag = "Maths"),
                session(2, thisWeek.plusDays(2), 14, 30, tag = "Physics"),
                session(3, thisWeek.plusDays(6), 18, 20, tag = "Maths"),
                session(4, thisWeek.minusWeeks(1), 10, 90, tag = "Maths"),
                session(5, thisWeek.plusWeeks(1), 10, 45, tag = "Work")
            ),
            weekStart = thisWeek,
            today = today
        )

        assertEquals(110L * 60L, report.totalSeconds)
        assertEquals(3, report.sessions)
        assertEquals(3, report.activeDays)
        assertEquals(90L * 60L, report.previousSeconds)
        assertEquals(60L * 60L, report.days[0].seconds) // Monday: first session
        assertEquals(0L, report.days[1].seconds)        // Tuesday: empty
    }

    @Test
    fun `change percent compares against the previous week`() {
        val report = buildWeeklyReport(
            sessions = listOf(
                session(1, thisWeek, 9, 90),
                session(2, thisWeek.minusWeeks(1), 9, 60)
            ),
            weekStart = thisWeek,
            today = today
        )

        assertEquals(50, report.changePercent)
    }

    @Test
    fun `no baseline means no percentage`() {
        val report = buildWeeklyReport(
            sessions = listOf(session(1, thisWeek, 9, 30)),
            weekStart = thisWeek,
            today = today
        )

        assertNull(report.changePercent)
    }

    @Test
    fun `best day and subject breakdown pick the biggest entries`() {
        val report = buildWeeklyReport(
            sessions = listOf(
                session(1, thisWeek, 9, 30, tag = "Physics"),
                session(2, thisWeek.plusDays(1), 9, 120, tag = "Maths"),
                session(3, thisWeek.plusDays(1), 15, 30, tag = "Maths", note = "Finished the set"),
                session(4, thisWeek.plusDays(3), 9, 45, tag = "Reading")
            ),
            weekStart = thisWeek,
            today = today
        )

        assertEquals(thisWeek.plusDays(1), report.bestDay?.date)
        assertEquals(150L * 60L, report.bestDay?.seconds)
        assertEquals(listOf("Maths", "Reading", "Physics"), report.subjects.map { it.tag })
        assertEquals(1, report.reflections)
        assertEquals(225L * 60L / 4L, report.averageSessionSeconds)
    }

    @Test
    fun `week range stays compact inside one month and expands across months`() {
        assertEquals("9–15 Nov", weekRangeLabel(LocalDate.of(2026, 11, 9)))
        assertEquals("26 Oct – 1 Nov", weekRangeLabel(LocalDate.of(2026, 10, 26)))
    }
}
