package com.frostlock.app.insights

import com.frostlock.app.data.local.db.SessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class FocusDayBucketsTest {

    private val zone = ZoneId.of("Europe/London")

    private fun at(date: String, time: String): Long =
        LocalDateTime.parse("${date}T$time").atZone(zone).toInstant().toEpochMilli()

    private fun session(
        start: Long,
        end: Long,
        durationSeconds: Int = ((end - start) / 1000L).toInt(),
        tag: String = ""
    ) = SessionEntity(
        mode = "TIMER",
        plannedSeconds = durationSeconds,
        durationSeconds = durationSeconds,
        startedAt = start,
        endedAt = end,
        completed = true,
        tag = tag
    )

    @Test
    fun `session inside one day lands on that day`() {
        val buckets = FocusDayBuckets.build(
            listOf(session(at("2026-11-18", "09:00"), at("2026-11-18", "10:00"))),
            zone
        )

        val day = buckets.getValue(LocalDate.of(2026, 11, 18))
        assertEquals(3600L, day.seconds)
        assertEquals(1, day.sessions)
        assertEquals(1, buckets.size)
    }

    @Test
    fun `session across midnight splits duration and counts on the end day`() {
        val buckets = FocusDayBuckets.build(
            listOf(session(at("2026-11-18", "23:30"), at("2026-11-19", "00:30"))),
            zone
        )

        assertEquals(1800L, buckets.getValue(LocalDate.of(2026, 11, 18)).seconds)
        assertEquals(0, buckets.getValue(LocalDate.of(2026, 11, 18)).sessions)
        assertEquals(1800L, buckets.getValue(LocalDate.of(2026, 11, 19)).seconds)
        assertEquals(1, buckets.getValue(LocalDate.of(2026, 11, 19)).sessions)
    }

    @Test
    fun `session ending exactly at midnight belongs to the day that finished`() {
        val buckets = FocusDayBuckets.build(
            listOf(session(at("2026-11-18", "23:30"), at("2026-11-19", "00:00"))),
            zone
        )

        assertEquals(1800L, buckets.getValue(LocalDate.of(2026, 11, 18)).seconds)
        assertEquals(1, buckets.getValue(LocalDate.of(2026, 11, 18)).sessions)
        assertTrue(LocalDate.of(2026, 11, 19) !in buckets)
    }

    @Test
    fun `paused session keeps its recorded duration when split`() {
        val buckets = FocusDayBuckets.build(
            listOf(
                session(
                    start = at("2026-11-18", "23:00"),
                    end = at("2026-11-19", "01:00"),
                    durationSeconds = 5400
                )
            ),
            zone
        )

        val first = buckets.getValue(LocalDate.of(2026, 11, 18))
        val second = buckets.getValue(LocalDate.of(2026, 11, 19))
        assertEquals(2700L, first.seconds)
        assertEquals(2700L, second.seconds)
        assertEquals(5400L, first.seconds + second.seconds)
    }

    @Test
    fun `zero duration session is ignored`() {
        val buckets = FocusDayBuckets.build(
            listOf(session(at("2026-11-18", "09:00"), at("2026-11-18", "09:01"), durationSeconds = 0)),
            zone
        )

        assertTrue(buckets.isEmpty())
    }

    @Test
    fun `current streak survives an empty today`() {
        val buckets = FocusDayBuckets.build(
            listOf(
                session(at("2026-11-16", "09:00"), at("2026-11-16", "10:00")),
                session(at("2026-11-17", "09:00"), at("2026-11-17", "10:00")),
                session(at("2026-11-18", "09:00"), at("2026-11-18", "10:00"))
            ),
            zone
        )

        assertEquals(3, FocusDayBuckets.currentStreak(buckets, LocalDate.of(2026, 11, 18)))
        assertEquals(3, FocusDayBuckets.currentStreak(buckets, LocalDate.of(2026, 11, 19)))
        assertEquals(0, FocusDayBuckets.currentStreak(buckets, LocalDate.of(2026, 11, 20)))
    }

    @Test
    fun `best streak is the longest run`() {
        val buckets = FocusDayBuckets.build(
            listOf(
                session(at("2026-11-01", "09:00"), at("2026-11-01", "10:00")),
                session(at("2026-11-02", "09:00"), at("2026-11-02", "10:00")),
                session(at("2026-11-05", "09:00"), at("2026-11-05", "10:00")),
                session(at("2026-11-06", "09:00"), at("2026-11-06", "10:00")),
                session(at("2026-11-07", "09:00"), at("2026-11-07", "10:00"))
            ),
            zone
        )

        assertEquals(3, FocusDayBuckets.bestStreak(buckets))
    }

    @Test
    fun `period includes future zero days and counts goal days`() {
        val buckets = FocusDayBuckets.build(
            listOf(
                session(at("2026-11-16", "09:00"), at("2026-11-16", "11:00")),
                session(at("2026-11-17", "09:00"), at("2026-11-17", "09:30"))
            ),
            zone
        )

        val week = FocusDayBuckets.period(
            buckets = buckets,
            start = LocalDate.of(2026, 11, 16),
            end = LocalDate.of(2026, 11, 22),
            goalSeconds = 3600
        )

        assertEquals(7, week.days.size)
        assertEquals(9000L, week.seconds)
        assertEquals(2, week.sessions)
        assertEquals(2, week.activeDays)
        assertEquals(1, week.goalDays)
        assertEquals(LocalDate.of(2026, 11, 22), week.days.last().date)
        assertEquals(0L, week.days.last().seconds)
    }

    @Test
    fun `month rollup uses canonical buckets`() {
        val buckets = FocusDayBuckets.build(
            listOf(
                session(at("2026-11-01", "23:30"), at("2026-11-02", "00:30")),
                session(at("2026-11-02", "09:00"), at("2026-11-02", "10:00"))
            ),
            zone
        )

        val month = FocusDayBuckets.month(buckets, YearMonth.of(2026, 11))

        assertEquals(7200L, month.seconds)
        assertEquals(2, month.sessions)
        assertEquals(2, month.activeDays)
        assertEquals(5400L, month.bestDaySeconds)
    }

    @Test
    fun `dst spring forward does not lose or duplicate time`() {
        // Europe/London skips 01:00–02:00 on 29 March 2026.
        val buckets = FocusDayBuckets.build(
            listOf(
                session(
                    start = at("2026-03-29", "00:30"),
                    end = at("2026-03-29", "03:30"),
                    durationSeconds = 7200
                )
            ),
            zone
        )

        val day = buckets.getValue(LocalDate.of(2026, 3, 29))
        assertEquals(7200L, day.seconds)
        assertEquals(1, day.sessions)
    }

    @Test
    fun `dst cross-midnight session splits at local midnight`() {
        val buckets = FocusDayBuckets.build(
            listOf(session(at("2026-03-28", "23:30"), at("2026-03-29", "00:30"))),
            zone
        )

        assertEquals(1800L, buckets.getValue(LocalDate.of(2026, 3, 28)).seconds)
        assertEquals(1800L, buckets.getValue(LocalDate.of(2026, 3, 29)).seconds)
    }
}
