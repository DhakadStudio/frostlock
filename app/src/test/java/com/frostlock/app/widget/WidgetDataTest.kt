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

    @Test
    fun `calendar widget defaults to a full six-week grid`() {
        assertEquals(42, WidgetData().monthSeconds.size)
        assertEquals(42, WidgetData.DAYS_IN_MONTH_GRID)
    }

    @Test
    fun `calendar meta labels time and active days`() {
        assertEquals(
            "10h 31m · 12 active days",
            monthMetaLabel(10 * 60 * minutes + 31 * minutes, 12)
        )
        assertEquals("45m · 1 active day", monthMetaLabel(45 * minutes, 1))
    }

    @Test
    fun `week total sums the strip and ignores negatives`() {
        assertEquals(
            "4h 20m",
            weekTotalLabel(listOf(60 * minutes, 120 * minutes, 0, 80 * minutes, 0, 0, 0))
        )
        assertEquals("0m", weekTotalLabel(emptyList()))
        assertEquals("10m", weekTotalLabel(listOf(10 * minutes, -30 * minutes)))
    }

    @Test
    fun `unlock count label handles zero one and many`() {
        assertEquals("No unlocks yet", unlockCountLabel(0))
        assertEquals("1 unlock", unlockCountLabel(1))
        assertEquals("12 unlocks", unlockCountLabel(12))
    }

    @Test
    fun `unlock footer combines focus time and streak`() {
        assertEquals("Focus 1h 25m · \uD83D\uDD25 3-day streak", unlockFooterLabel(85 * minutes, 3))
        assertEquals("Focus 0m · no streak yet", unlockFooterLabel(0, 0))
    }

    @Test
    fun `usage widget defaults to empty local data`() {
        val data = WidgetData()
        assertEquals("0m", weekTotalLabel(data.weekSeconds))
        assertEquals(0L, data.monthTotalSeconds)
        assertEquals("No unlocks yet", unlockCountLabel(data.unlocksToday))
    }
}
