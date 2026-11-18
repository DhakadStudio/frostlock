package com.frostlock.app.data.local.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class LimitMidnightSplitTest {

    private val zone = ZoneId.of("Europe/London")

    private fun at(date: String, time: String): Long =
        LocalDateTime.parse("${date}T$time").atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `usage inside one day is one slice`() {
        val slices = AppLimitStore.splitUsageAtMidnight(
            fromMillis = at("2026-11-18", "09:00"),
            toMillis = at("2026-11-18", "09:30"),
            zone = zone
        )

        assertEquals(1, slices.size)
        assertEquals(LocalDate.of(2026, 11, 18), slices.single().first)
        assertEquals(30 * 60_000L, slices.single().second)
    }

    @Test
    fun `usage across midnight gives each day its own share`() {
        val slices = AppLimitStore.splitUsageAtMidnight(
            fromMillis = at("2026-11-18", "23:40"),
            toMillis = at("2026-11-19", "00:20"),
            zone = zone
        )

        assertEquals(2, slices.size)
        assertEquals(LocalDate.of(2026, 11, 18), slices[0].first)
        assertEquals(20 * 60_000L, slices[0].second)
        assertEquals(LocalDate.of(2026, 11, 19), slices[1].first)
        assertEquals(20 * 60_000L, slices[1].second)
    }

    @Test
    fun `span ending exactly at midnight stays on the day that finished`() {
        val slices = AppLimitStore.splitUsageAtMidnight(
            fromMillis = at("2026-11-18", "23:30"),
            toMillis = at("2026-11-19", "00:00"),
            zone = zone
        )

        assertEquals(1, slices.size)
        assertEquals(LocalDate.of(2026, 11, 18), slices.single().first)
        assertEquals(30 * 60_000L, slices.single().second)
    }

    @Test
    fun `span crossing several days is split per day`() {
        val slices = AppLimitStore.splitUsageAtMidnight(
            fromMillis = at("2026-11-18", "23:00"),
            toMillis = at("2026-11-20", "01:00"),
            zone = zone
        )

        assertEquals(3, slices.size)
        assertEquals(LocalDate.of(2026, 11, 18), slices[0].first)
        assertEquals(60 * 60_000L, slices[0].second)
        assertEquals(LocalDate.of(2026, 11, 19), slices[1].first)
        assertEquals(24 * 60 * 60_000L, slices[1].second)
        assertEquals(LocalDate.of(2026, 11, 20), slices[2].first)
        assertEquals(60 * 60_000L, slices[2].second)
    }

    @Test
    fun `dst spring forward keeps the wall-clock split exact`() {
        // Europe/London skips 01:00–02:00 on 29 March 2026.
        val slices = AppLimitStore.splitUsageAtMidnight(
            fromMillis = at("2026-03-28", "23:30"),
            toMillis = at("2026-03-29", "03:30"),
            zone = zone
        )

        assertEquals(2, slices.size)
        assertEquals(LocalDate.of(2026, 3, 28), slices[0].first)
        assertEquals(30 * 60_000L, slices[0].second)
        assertEquals(LocalDate.of(2026, 3, 29), slices[1].first)
        // 29 March skips 01:00–02:00, so 00:00–03:30 is only 2.5 real hours.
        assertEquals(2 * 60 * 60_000L + 30 * 60_000L, slices[1].second)
    }

    @Test
    fun `empty or backwards spans produce no slices`() {
        val now = at("2026-11-18", "09:00")

        assertTrue(AppLimitStore.splitUsageAtMidnight(now, now, zone).isEmpty())
        assertTrue(AppLimitStore.splitUsageAtMidnight(now, now - 1_000L, zone).isEmpty())
    }
}
