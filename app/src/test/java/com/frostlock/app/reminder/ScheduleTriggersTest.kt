package com.frostlock.app.reminder

import com.frostlock.app.data.local.prefs.FocusSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZonedDateTime

class ScheduleTriggersTest {

    private val zone = java.time.ZoneId.of("UTC")
    private val now = ZonedDateTime.of(2026, 11, 13, 8, 0, 0, 0, zone) // Friday

    private fun schedule(
        days: Set<Int> = setOf(1, 2, 3, 4, 5),
        dates: Set<String> = emptySet(),
        startMinutes: Int = 9 * 60,
        endMinutes: Int = 17 * 60,
        enabled: Boolean = true
    ) = FocusSchedule(
        id = 42L,
        label = "Study",
        days = days,
        dates = dates,
        startMinutes = startMinutes,
        endMinutes = endMinutes,
        enabled = enabled
    )

    @Test
    fun `weekly rule triggers at its start and end on the same day`() {
        val rule = schedule()
        val start = nextScheduleStart(rule, now)
        val end = nextScheduleEnd(rule, now)

        assertEquals(ZonedDateTime.of(2026, 11, 13, 9, 0, 0, 0, zone), start)
        assertEquals(ZonedDateTime.of(2026, 11, 13, 17, 0, 0, 0, zone), end)
    }

    @Test
    fun `running rule ends today and starts again next eligible day`() {
        val rule = schedule()
        val during = ZonedDateTime.of(2026, 11, 13, 12, 0, 0, 0, zone)

        assertEquals(
            ZonedDateTime.of(2026, 11, 13, 17, 0, 0, 0, zone),
            nextScheduleEnd(rule, during)
        )
        assertEquals(
            ZonedDateTime.of(2026, 11, 16, 9, 0, 0, 0, zone),
            nextScheduleStart(rule, during) // weekend skipped
        )
    }

    @Test
    fun `overnight rule closes the following morning`() {
        val rule = schedule(
            days = setOf(1, 2, 3, 4, 5, 6, 7),
            startMinutes = 22 * 60,
            endMinutes = 2 * 60
        )
        val late = ZonedDateTime.of(2026, 11, 13, 23, 0, 0, 0, zone)

        assertEquals(
            ZonedDateTime.of(2026, 11, 14, 2, 0, 0, 0, zone),
            nextScheduleEnd(rule, late)
        )
    }

    @Test
    fun `one-off date rule uses its saved date`() {
        val rule = schedule(
            days = emptySet(),
            dates = setOf("2026-11-20"),
            startMinutes = 14 * 60,
            endMinutes = 15 * 60
        )

        assertEquals(
            ZonedDateTime.of(2026, 11, 20, 14, 0, 0, 0, zone),
            nextScheduleStart(rule, now)
        )
        assertEquals(
            ZonedDateTime.of(2026, 11, 20, 15, 0, 0, 0, zone),
            nextScheduleEnd(rule, now)
        )
    }

    @Test
    fun `past dates and disabled rules have no next trigger`() {
        val past = schedule(days = emptySet(), dates = setOf("2026-11-01"))
        val disabled = schedule(enabled = false)

        assertNull(nextScheduleStart(past, now))
        assertNull(nextScheduleEnd(past, now))
        assertNull(nextScheduleStart(disabled, now))
        assertNull(nextScheduleEnd(disabled, now))
    }

    @Test
    fun `triggers are chronological and include the close reminder first while running`() {
        val rule = schedule()
        val during = ZonedDateTime.of(2026, 11, 13, 12, 0, 0, 0, zone)

        val triggers = nextScheduleTriggers(rule, during)

        assertEquals(
            listOf(ScheduleReminderKind.END, ScheduleReminderKind.START),
            triggers.map { it.kind }
        )
        assertEquals(LocalDate.of(2026, 11, 16), triggers.last().at.toLocalDate())
    }
}
