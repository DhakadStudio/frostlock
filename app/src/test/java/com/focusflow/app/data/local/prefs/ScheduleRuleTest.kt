package com.focusflow.app.data.local.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Pure tests for rule matching. 2026-11-11 is a Wednesday (dayOfWeek = 3),
 * 2026-11-12 is a Thursday (4) and 2026-11-20 is a Friday (5).
 */
class ScheduleRuleTest {

    private val zone = ZoneId.of("UTC")

    private fun at(day: Int, hour: Int, minute: Int = 0): ZonedDateTime =
        ZonedDateTime.of(2026, 11, day, hour, minute, 0, 0, zone)

    private fun rule(
        days: Set<Int> = setOf(3),
        dates: Set<String> = emptySet(),
        start: Int = 9 * 60,
        end: Int = 17 * 60,
        packages: Set<String> = setOf("a"),
        enabled: Boolean = true
    ) = FocusSchedule(
        id = 1L,
        label = "Test",
        days = days,
        dates = dates,
        startMinutes = start,
        endMinutes = end,
        packages = packages,
        enabled = enabled
    )

    @Test
    fun weeklyRuleOnlyRunsOnSelectedDays() {
        val weekly = rule(days = setOf(3))
        assertTrue(weekly.isActiveAt(at(11, 10)))
        assertFalse(weekly.isActiveAt(at(12, 10)))
    }

    @Test
    fun weeklyRuleRespectsWindowEdges() {
        val weekly = rule(days = setOf(3), start = 9 * 60, end = 17 * 60)
        assertTrue(weekly.isActiveAt(at(11, 9, 0)))
        assertFalse(weekly.isActiveAt(at(11, 8, 59)))
        assertFalse(weekly.isActiveAt(at(11, 17, 0)))
    }

    @Test
    fun overnightRuleSpillsIntoTheNextDay() {
        val overnight = rule(days = setOf(3), start = 22 * 60, end = 2 * 60)
        assertTrue(overnight.isActiveAt(at(11, 23)))
        assertTrue(overnight.isActiveAt(at(12, 1)))
        assertFalse(overnight.isActiveAt(at(12, 3)))
        assertFalse(overnight.isActiveAt(at(11, 12)))
    }

    @Test
    fun dateRuleRunsOnlyOnThatDate() {
        val oneOff = rule(days = emptySet(), dates = setOf("2026-11-20"))
        assertTrue(oneOff.isActiveAt(at(20, 10)))
        assertFalse(oneOff.isActiveAt(at(19, 10)))
        assertFalse(oneOff.isActiveAt(at(21, 10)))
    }

    @Test
    fun dateRuleIgnoresWeekdays() {
        // Same weekday as the rule, but a date rule must not fire on other Wednesdays.
        val oneOff = rule(days = setOf(3), dates = setOf("2026-11-20"))
        assertFalse(oneOff.isActiveAt(at(11, 10)))
        assertTrue(oneOff.isActiveAt(at(20, 10)))
    }

    @Test
    fun overnightDateRuleSpillsIntoTheNextDay() {
        val oneOff = rule(days = emptySet(), dates = setOf("2026-11-20"), start = 22 * 60, end = 2 * 60)
        assertTrue(oneOff.isActiveAt(at(20, 23)))
        assertTrue(oneOff.isActiveAt(at(21, 1)))
        assertFalse(oneOff.isActiveAt(at(21, 3)))
    }

    @Test
    fun disabledOrEmptyRulesNeverRun() {
        assertFalse(rule(enabled = false).isActiveAt(at(11, 10)))
        assertFalse(rule(days = emptySet()).isActiveAt(at(11, 10)))
        assertFalse(rule(start = 600, end = 600).isActiveAt(at(11, 10)))
    }

    @Test
    fun endsAtReturnsClosingTime() {
        val daytime = rule(start = 9 * 60, end = 17 * 60)
        assertEquals(at(11, 17), daytime.endsAt(at(11, 10)))

        val overnight = rule(start = 22 * 60, end = 2 * 60)
        assertEquals(at(12, 2), overnight.endsAt(at(11, 23)))
    }

    @Test
    fun rulesBlockOnlyTheirOwnApps() {
        val study = rule(packages = setOf("instagram", "youtube"))
        val work = rule(packages = setOf("slack"))
        val globals = setOf("instagram", "youtube", "slack")

        assertTrue(study.blocks("instagram", globals))
        assertTrue(study.blocks("youtube", globals))
        assertFalse(study.blocks("slack", globals))

        assertTrue(work.blocks("slack", globals))
        assertFalse(work.blocks("youtube", globals))
    }

    @Test
    fun emptyRuleAppsFallBackToFocusSessionList() {
        val legacy = rule(packages = emptySet())
        val globals = setOf("instagram")

        assertTrue(legacy.blocks("instagram", globals))
        assertFalse(legacy.blocks("slack", globals))
    }

    @Test
    fun twoRulesCanRunAtTheSameTimeOnDifferentApps() {
        val study = rule(days = setOf(3), start = 9 * 60, end = 17 * 60, packages = setOf("instagram"))
        val work = rule(days = setOf(3), start = 9 * 60, end = 17 * 60, packages = setOf("slack"))
        val now = at(11, 10)

        assertTrue(study.isActiveAt(now))
        assertTrue(work.isActiveAt(now))
        assertNotEquals(study.packages, work.packages)
    }
}
