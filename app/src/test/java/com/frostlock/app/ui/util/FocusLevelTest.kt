package com.frostlock.app.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusLevelTest {

    private fun levelAt(minutes: Long) = focusLevelFor(minutes * 60L)

    @Test
    fun `starts at level one with no focus time`() {
        val level = levelAt(0)

        assertEquals(1, level.level)
        assertEquals("Frostbite", level.title)
        assertEquals(0L, level.minutesIntoLevel)
        assertEquals(60L, level.minutesForNextLevel)
        assertEquals(60L, level.minutesToNextLevel)
        assertEquals(0f, level.progress, 0.001f)
    }

    @Test
    fun `one hour reaches level two`() {
        val level = levelAt(60)

        assertEquals(2, level.level)
        assertEquals("Snowflake", level.title)
        assertEquals(0L, level.minutesIntoLevel)
        assertEquals(120L, level.minutesForNextLevel)
    }

    @Test
    fun `three hours reaches level three`() {
        assertEquals(3, levelAt(180).level)
        assertEquals(4, levelAt(360).level)
        assertEquals(5, levelAt(600).level)
    }

    @Test
    fun `progress and remaining minutes use the current step`() {
        // Level 3 starts at 180 minutes and needs 180 more to reach level 4.
        val level = levelAt(270)

        assertEquals(3, level.level)
        assertEquals(90L, level.minutesIntoLevel)
        assertEquals(180L, level.minutesForNextLevel)
        assertEquals(90L, level.minutesToNextLevel)
        assertEquals(0.5f, level.progress, 0.001f)
    }

    @Test
    fun `partial minutes do not round up`() {
        val level = focusLevelFor(59 * 60L + 59L)

        assertEquals(1, level.level)
        assertEquals(59L, level.totalMinutes)
        assertEquals(1L, level.minutesToNextLevel)
    }

    @Test
    fun `titles run out gracefully`() {
        assertEquals("Absolute zero", focusLevelTitle(7))
        assertEquals("Cryomaster", focusLevelTitle(8))
        assertTrue(levelAt(10_000).level > 7)
    }

    @Test
    fun `negative time is clamped`() {
        val level = focusLevelFor(-500L)

        assertEquals(1, level.level)
        assertEquals(0L, level.totalMinutes)
    }
}
