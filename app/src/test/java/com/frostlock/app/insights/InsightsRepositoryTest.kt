package com.frostlock.app.insights

import com.frostlock.app.data.local.db.SessionEntity
import com.frostlock.app.data.local.prefs.FocusSubject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class InsightsRepositoryTest {

    private val zone = ZoneId.of("Europe/London")
    private val today = LocalDate.of(2026, 11, 18)
    private val now = Instant.parse("2026-11-18T12:00:00Z")

    private fun at(date: String, time: String): Long =
        LocalDateTime.parse("${date}T$time").atZone(zone).toInstant().toEpochMilli()

    private fun session(
        start: Long,
        end: Long,
        durationSeconds: Int = ((end - start) / 1000L).toInt(),
        tag: String = "",
        note: String = "",
        completed: Boolean = true
    ) = SessionEntity(
        mode = "TIMER",
        plannedSeconds = durationSeconds,
        durationSeconds = durationSeconds,
        startedAt = start,
        endedAt = end,
        completed = completed,
        tag = tag,
        note = note
    )

    private fun snapshot(
        sessions: List<SessionEntity>,
        goalMinutes: Int = 120,
        subjects: List<FocusSubject> = emptyList()
    ) = buildInsightsSnapshot(
        sessions = sessions,
        dailyTargetMinutes = goalMinutes,
        subjects = subjects,
        today = today,
        zone = zone,
        now = now
    )

    @Test
    fun `same session produces identical totals across periods`() {
        val result = snapshot(
            listOf(session(at("2026-11-17", "23:30"), at("2026-11-18", "00:30")))
        )

        assertEquals(3600L, result.week.seconds)
        assertEquals(3600L, result.last7.seconds)
        assertEquals(3600L, result.months.last().seconds)
        assertEquals(1800L, result.today.seconds)
        assertEquals(1, result.today.sessions)
        assertEquals(2, result.currentStreak)
        assertEquals(2, result.bestStreak)
    }

    @Test
    fun `week includes both halves of a cross-midnight session`() {
        val result = snapshot(
            listOf(session(at("2026-11-17", "23:30"), at("2026-11-18", "00:30"), tag = "Physics"))
        )

        assertEquals(1, result.subjects.size)
        assertEquals("Physics", result.subjects.first().tag)
        assertEquals(3600L, result.subjects.first().seconds)
        assertEquals(1f, result.subjects.first().share, 0.001f)
    }

    @Test
    fun `subject breakdown keeps top five and collapses the rest`() {
        val sessions = (1..7).map { index ->
            session(
                start = at("2026-11-16", "09:00"),
                end = at("2026-11-16", "10:00"),
                durationSeconds = index * 60,
                tag = "Subject $index"
            )
        }

        val result = snapshot(sessions)

        assertEquals(6, result.subjects.size)
        assertEquals("Subject 7", result.subjects.first().tag)
        assertEquals(OTHER_SUBJECT, result.subjects.last().tag)
        assertEquals(60L + 120L, result.subjects.last().seconds)
    }

    @Test
    fun `reflections are counted overall and for the week`() {
        val result = snapshot(
            listOf(
                session(
                    at("2026-11-16", "09:00"),
                    at("2026-11-16", "10:00"),
                    note = "Good session"
                ),
                session(
                    at("2026-11-10", "09:00"),
                    at("2026-11-10", "10:00"),
                    note = "Last week"
                ),
                session(at("2026-11-17", "09:00"), at("2026-11-17", "10:00"))
            )
        )

        assertEquals(2, result.reflections)
        assertEquals(1, result.weekReflections)
    }

    @Test
    fun `goal days use the daily target`() {
        val result = snapshot(
            sessions = listOf(
                session(at("2026-11-16", "09:00"), at("2026-11-16", "11:00")),
                session(at("2026-11-17", "09:00"), at("2026-11-17", "09:30"))
            ),
            goalMinutes = 60
        )

        assertEquals(1, result.week.goalDays)
        assertEquals(2, result.week.activeDays)
    }

    @Test
    fun `recent sessions are newest first and capped`() {
        val sessions = (0 until 12).map { index ->
            session(
                start = at("2026-11-01", "09:00") + index * 60_000L,
                end = at("2026-11-01", "09:30") + index * 60_000L,
                durationSeconds = 60
            )
        }

        val result = snapshot(sessions)

        assertEquals(10, result.recent.size)
        assertEquals(sessions.last().endedAt, result.recent.first().endedAt)
        assertEquals(12, result.totalSessions)
    }

    @Test
    fun `zero-duration sessions never appear in insights`() {
        val result = snapshot(
            listOf(
                session(at("2026-11-18", "09:00"), at("2026-11-18", "09:01"), durationSeconds = 0)
            )
        )

        assertEquals(0L, result.totalSeconds)
        assertEquals(0, result.totalSessions)
        assertTrue(result.recent.isEmpty())
    }

    @Test
    fun `a session ending exactly at midnight counts on the day that finished`() {
        val result = snapshot(
            listOf(
                session(
                    at("2026-11-15", "23:30"),
                    at("2026-11-16", "00:00"),
                    tag = "Physics",
                    note = "Late"
                )
            )
        )

        // 16 Nov is the Monday that starts the current week, so a 23:30–00:00
        // session belongs to the previous week.
        assertEquals(0, result.week.sessions)
        assertEquals(0, result.weekReflections)
        assertEquals(1800L, result.days.getValue(LocalDate.of(2026, 11, 15)).seconds)
        assertEquals(1, result.days.getValue(LocalDate.of(2026, 11, 15)).sessions)
    }

    @Test
    fun `subject colours come from the saved subject list`() {
        val colour = 0xFF123456.toInt()
        val result = snapshot(
            sessions = listOf(
                session(at("2026-11-18", "09:00"), at("2026-11-18", "10:00"), tag = "Physics")
            ),
            subjects = listOf(FocusSubject(id = 1, name = "Physics", colorArgb = colour))
        )

        assertEquals(colour, result.subjects.first().colorArgb)
    }
}
