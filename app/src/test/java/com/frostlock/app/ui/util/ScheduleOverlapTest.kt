package com.frostlock.app.ui.util

import com.frostlock.app.data.local.prefs.FocusSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleOverlapTest {

    private fun rule(
        id: Long = 1L,
        label: String = "Focus",
        days: Set<Int> = setOf(1, 2, 3, 4, 5),
        dates: Set<String> = emptySet(),
        startMinutes: Int = 9 * 60,
        endMinutes: Int = 17 * 60,
        enabled: Boolean = true
    ) = FocusSchedule(
        id = id,
        label = label,
        days = days,
        dates = dates,
        startMinutes = startMinutes,
        endMinutes = endMinutes,
        enabled = enabled
    )

    @Test
    fun `identical weekday windows collide`() {
        val overlap = ruleOverlaps(rule(), listOf(rule(id = 2, label = "Deep work")))

        assertEquals(1, overlap.size)
        assertEquals("Deep work", overlap[0].other.label)
        assertEquals(setOf(1, 2, 3, 4, 5), overlap[0].days)
        assertTrue(overlap[0].dates.isEmpty())
    }

    @Test
    fun `partial weekday windows report only the shared days`() {
        val overlap = ruleOverlaps(
            rule(days = setOf(1, 2, 3)),
            listOf(rule(id = 2, label = "Study", days = setOf(3, 4, 5)))
        )

        assertEquals(setOf(3), overlap[0].days)
    }

    @Test
    fun `disjoint windows do not collide`() {
        val overlap = ruleOverlaps(
            rule(startMinutes = 9 * 60, endMinutes = 12 * 60),
            listOf(rule(id = 2, startMinutes = 13 * 60, endMinutes = 17 * 60))
        )

        assertTrue(overlap.isEmpty())
    }

    @Test
    fun `touching windows are not an overlap`() {
        val overlap = ruleOverlaps(
            rule(startMinutes = 9 * 60, endMinutes = 12 * 60),
            listOf(rule(id = 2, startMinutes = 12 * 60, endMinutes = 17 * 60))
        )

        assertTrue(overlap.isEmpty())
    }

    @Test
    fun `overnight tail collides with the next morning`() {
        val night = rule(
            id = 1,
            label = "Night",
            days = setOf(1, 2, 3, 4, 5),
            startMinutes = 22 * 60,
            endMinutes = 2 * 60
        )
        val morning = rule(
            id = 2,
            label = "Early",
            days = setOf(2, 3, 4, 5, 6),
            startMinutes = 60,
            endMinutes = 3 * 60
        )

        val overlap = ruleOverlaps(night, listOf(morning))

        assertEquals(1, overlap.size)
        assertEquals(setOf("Early"), overlap.map { it.other.label }.toSet())
        assertTrue(overlap[0].days.contains(2))
    }

    @Test
    fun `one-off rule colliding with a weekday rule reports the date`() {
        // Friday 20 November 2026.
        val dated = rule(
            id = 1,
            label = "Exam prep",
            days = emptySet(),
            dates = setOf("2026-11-20"),
            startMinutes = 10 * 60,
            endMinutes = 12 * 60
        )
        val weekly = rule(id = 2, label = "Workday")

        val overlap = ruleOverlaps(dated, listOf(weekly))

        assertEquals(1, overlap.size)
        assertEquals(setOf("2026-11-20"), overlap[0].dates)
        assertTrue(overlap[0].days.isEmpty())
    }

    @Test
    fun `one-off rules on different dates do not collide`() {
        val first = rule(
            id = 1,
            days = emptySet(),
            dates = setOf("2026-11-20")
        )
        val second = rule(
            id = 2,
            days = emptySet(),
            dates = setOf("2026-11-21")
        )

        assertFalse(rulesOverlap(first, second))
        assertTrue(ruleOverlaps(first, listOf(second)).isEmpty())
    }

    @Test
    fun `disabled rules and the rule itself are skipped`() {
        val focus = rule()

        assertTrue(ruleOverlaps(focus, listOf(focus)).isEmpty())
        assertTrue(ruleOverlaps(focus, listOf(rule(id = 2, enabled = false))).isEmpty())
        assertFalse(rulesOverlap(focus, rule(id = 2, enabled = false)))
    }

    @Test
    fun `zero-length rules never collide`() {
        val broken = rule(startMinutes = 9 * 60, endMinutes = 9 * 60)

        assertTrue(ruleOverlaps(broken, listOf(rule(id = 2))).isEmpty())
    }

    @Test
    fun `overlaps are sorted by label and deduplicated per rule`() {
        val candidate = rule(id = 10, label = "Mine")
        val overlap = ruleOverlaps(
            candidate,
            listOf(
                rule(id = 2, label = "Zeta"),
                rule(id = 3, label = "Alpha"),
                rule(id = 4, label = "Alpha", startMinutes = 18 * 60, endMinutes = 19 * 60)
            )
        )

        assertEquals(listOf("Alpha", "Zeta"), overlap.map { it.other.label })
    }

    @Test
    fun `has overlap mirrors the list`() {
        val focus = rule()

        assertTrue(hasOverlap(focus, listOf(focus, rule(id = 2))))
        assertFalse(hasOverlap(focus, listOf(focus)))
        assertFalse(hasOverlap(focus, listOf(rule(id = 2, startMinutes = 20 * 60, endMinutes = 21 * 60))))
    }
}
