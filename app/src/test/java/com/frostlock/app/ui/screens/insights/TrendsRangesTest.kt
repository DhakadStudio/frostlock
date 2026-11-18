package com.frostlock.app.ui.screens.insights

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TrendsRangesTest {

    private val today = LocalDate.of(2026, 11, 18)

    @Test
    fun `seven days ends today and starts six days back`() {
        val (start, end) = TrendsRanges.range(TrendsPeriod.WEEK, today)
        assertEquals(today.minusDays(6), start)
        assertEquals(today, end)
    }

    @Test
    fun `thirty days covers exactly thirty dates`() {
        val (start, end) = TrendsRanges.range(TrendsPeriod.MONTH, today)
        assertEquals(today.minusDays(29), start)
        assertEquals(30L, start.datesUntil(end.plusDays(1)).count())
    }

    @Test
    fun `six months starts on the first of the month five months back`() {
        val (start, end) = TrendsRanges.range(TrendsPeriod.SIX_MONTHS, today)
        assertEquals(LocalDate.of(2026, 6, 1), start)
        assertEquals(today, end)
    }

    @Test
    fun `weekly labels use weekday initials`() {
        // 2026-11-18 is a Wednesday.
        assertEquals("W", TrendsRanges.label(TrendsPeriod.WEEK, today))
    }

    @Test
    fun `month bars carry no per-bar labels`() {
        assertEquals("", TrendsRanges.label(TrendsPeriod.MONTH, today))
    }
}
