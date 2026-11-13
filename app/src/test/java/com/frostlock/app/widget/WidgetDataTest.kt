package com.frostlock.app.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek

class WidgetDataTest {

    private val minutes = 60L

    @Test
    fun `week starts on Monday and marks today`() {
        val summary = summarizeWeek(
            listOf(10 * minutes, 20 * minutes, 30 * minutes, 40 * minutes, 50 * minutes, 60 * minutes, 70 * minutes),
            DayOfWeek.THURSDAY
        )

        assertEquals(listOf("M", "T", "W", "T", "F", "S", "S"), summary.bars.map { it.label })
        assertEquals(3, summary.bars.indexOfFirst { it.isToday })
        assertEquals(1, summary.bars.count { it.isToday })
        assertEquals(40 * minutes, summary.bars[3].seconds)
    }

    @Test
    fun `week totals average and best day`() {
        val summary = summarizeWeek(
            listOf(30 * minutes, 0, 90 * minutes, 0, 60 * minutes, 0, 0),
            DayOfWeek.MONDAY
        )

        assertEquals(180 * minutes, summary.totalSeconds)
        assertEquals((180 * minutes) / 7, summary.averageSeconds)
        assertEquals("Wed", summary.bestDay)
    }

    @Test
    fun `empty week has no best day and renders seven zeros`() {
        val summary = summarizeWeek(emptyList(), DayOfWeek.SUNDAY)

        assertEquals(7, summary.bars.size)
        assertEquals(0L, summary.totalSeconds)
        assertNull(summary.bestDay)
        assertEquals(6, summary.bars.indexOfFirst { it.isToday })
    }

    @Test
    fun `goal percent clamps between zero and one hundred`() {
        assertEquals(0, goalPercent(0, 120))
        assertEquals(50, goalPercent(60 * minutes, 120))
        assertEquals(100, goalPercent(180 * minutes, 120))
        assertEquals(0, goalPercent(60 * minutes, 0))
    }

    @Test
    fun `goal label reports time left and completion`() {
        assertEquals("28m left of 1h 30m", goalRemainingLabel(62 * minutes, 90))
        assertEquals("Goal reached today", goalRemainingLabel(95 * minutes, 90))
    }

    @Test
    fun `duration formatter stays compact`() {
        assertEquals("0m", formatWidgetDuration(0))
        assertEquals("45m", formatWidgetDuration(45 * minutes))
        assertEquals("2h", formatWidgetDuration(120 * minutes))
        assertEquals("2h 15m", formatWidgetDuration(135 * minutes))
    }
}
