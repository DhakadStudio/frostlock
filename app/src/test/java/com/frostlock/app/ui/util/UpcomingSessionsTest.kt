package com.frostlock.app.ui.util

import com.frostlock.app.data.local.prefs.FocusSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class UpcomingSessionsTest {

    private val zone = ZoneId.of("UTC")
    private val now = ZonedDateTime.of(2026, 11, 13, 8, 0, 0, 0, zone) // Friday

    private fun rule(
        id: Long = 1L,
        label: String = "Study",
        days: Set<Int> = setOf(1, 2, 3, 4, 5),
        dates: Set<String> = emptySet(),
        startMinutes: Int = 9 * 60,
        endMinutes: Int = 17 * 60,
        packages: Set<String> = setOf("a", "b"),
        enabled: Boolean = true
    ) = FocusSchedule(
        id = id,
        label = label,
        days = days,
        dates = dates,
        startMinutes = startMinutes,
        endMinutes = endMinutes,
        packages = packages,
        enabled = enabled
    )

    @Test
    fun `weekly rule reports its next start and end`() {
        val upcoming = upcomingSessions(listOf(rule()), now, limit = 1)

        assertEquals(1, upcoming.size)
        assertEquals(ZonedDateTime.of(2026, 11, 13, 9, 0, 0, 0, zone), upcoming[0].start)
        assertEquals(ZonedDateTime.of(2026, 11, 13, 17, 0, 0, 0, zone), upcoming[0].end)
        assertEquals(2, upcoming[0].packageCount)
    }

    @Test
    fun `weekend is skipped for weekday rules`() {
        val saturday = ZonedDateTime.of(2026, 11, 14, 8, 0, 0, 0, zone)

        val upcoming = upcomingSessions(listOf(rule()), saturday)

        assertEquals(ZonedDateTime.of(2026, 11, 16, 9, 0, 0, 0, zone), upcoming[0].start)
    }

    @Test
    fun `one-off date rule appears once`() {
        val upcoming = upcomingSessions(
            listOf(
                rule(
                    days = emptySet(),
                    dates = setOf("2026-11-20"),
                    startMinutes = 14 * 60,
                    endMinutes = 15 * 60
                )
            ),
            now
        )

        assertEquals(1, upcoming.size)
        assertEquals(ZonedDateTime.of(2026, 11, 20, 14, 0, 0, 0, zone), upcoming[0].start)
        assertEquals(ZonedDateTime.of(2026, 11, 20, 15, 0, 0, 0, zone), upcoming[0].end)
    }

    @Test
    fun `past dates and disabled rules are ignored`() {
        val upcoming = upcomingSessions(
            listOf(
                rule(id = 1, days = emptySet(), dates = setOf("2026-11-01")),
                rule(id = 2, enabled = false)
            ),
            now
        )

        assertTrue(upcoming.isEmpty())
    }

    @Test
    fun `overnight window ends the following morning`() {
        val upcoming = upcomingSessions(
            listOf(
                rule(
                    days = setOf(1, 2, 3, 4, 5, 6, 7),
                    startMinutes = 22 * 60,
                    endMinutes = 2 * 60
                )
            ),
            now
        )

        assertEquals(ZonedDateTime.of(2026, 11, 13, 22, 0, 0, 0, zone), upcoming[0].start)
        assertEquals(ZonedDateTime.of(2026, 11, 14, 2, 0, 0, 0, zone), upcoming[0].end)
    }

    @Test
    fun `occurrences are sorted and limited`() {
        val upcoming = upcomingSessions(
            listOf(
                rule(id = 1, label = "Study", startMinutes = 9 * 60, endMinutes = 17 * 60),
                rule(id = 2, label = "Early", startMinutes = 8 * 60 + 30, endMinutes = 9 * 60 + 30),
                rule(id = 3, label = "Late", startMinutes = 20 * 60, endMinutes = 21 * 60)
            ),
            now,
            limit = 2
        )

        assertEquals(listOf("Early", "Study"), upcoming.map { it.label })
    }

    @Test
    fun `labels read naturally around today`() {
        assertEquals(
            "Today 09:00",
            upcomingWhen(ZonedDateTime.of(2026, 11, 13, 9, 0, 0, 0, zone), now)
        )
        assertEquals(
            "Tomorrow 09:00",
            upcomingWhen(ZonedDateTime.of(2026, 11, 14, 9, 0, 0, 0, zone), now)
        )
        assertEquals(
            "Mon 09:00",
            upcomingWhen(ZonedDateTime.of(2026, 11, 16, 9, 0, 0, 0, zone), now)
        )
    }

    @Test
    fun `countdown uses minutes, hours and days`() {
        assertEquals(
            "in 25m",
            upcomingCountdown(ZonedDateTime.of(2026, 11, 13, 8, 25, 0, 0, zone), now)
        )
        assertEquals(
            "in 3h 10m",
            upcomingCountdown(ZonedDateTime.of(2026, 11, 13, 11, 10, 0, 0, zone), now)
        )
        assertEquals(
            "in 2d 4h",
            upcomingCountdown(ZonedDateTime.of(2026, 11, 15, 12, 0, 0, 0, zone), now)
        )
        assertEquals(
            "starting now",
            upcomingCountdown(ZonedDateTime.of(2026, 11, 13, 7, 0, 0, 0, zone), now)
        )
    }
}
