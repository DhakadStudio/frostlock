package com.frostlock.app.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerAutoSizeTest {

    @Test
    fun `configuration is a valid step range`() {
        assertTrue(TimerAutoSize.MIN_FONT_SP < TimerAutoSize.MAX_FONT_SP)
        assertTrue(TimerAutoSize.STEP_SP > 0f)
        assertEquals(
            true,
            (TimerAutoSize.MAX_FONT_SP - TimerAutoSize.MIN_FONT_SP) % TimerAutoSize.STEP_SP == 0f
        )
        assertTrue(TimerAutoSize.stepCount() >= 2)
    }

    @Test
    fun `max matches the displayTimer token so default rendering is unchanged`() {
        // FrostTypography.displayTimer is 64sp; the autosize ceiling must match it.
        assertEquals(64f, TimerAutoSize.MAX_FONT_SP, 0f)
    }

    @Test
    fun `longest readout fits the dial at every supported font scale`() {
        for (scale in listOf(1.0f, 1.3f, 1.6f, 2.0f)) {
            assertTrue(
                "expected fit at fontScale=$scale",
                TimerAutoSize.fits(scale)
            )
        }
    }

    @Test
    fun `fits allows reasonable shortfall at 2_0 scale`() {
        // Sanity check the width model itself: 8 tabular glyphs at 24sp and 2.0 scale.
        assertEquals(238.08f, TimerAutoSize.widthDp(24f, 2.0f), 0.01f)
        assertEquals(240f / TimerAutoSize.widthDp(24f, 2.0f), 1.008f, 0.01f)
    }

    @Test
    fun `reading fits a normal five glyph timer even at max size`() {
        // "25:00" at the 64sp ceiling and fontScale 1.0 fits comfortably.
        assertTrue(TimerAutoSize.fits(1.0f, 64f, 5))
    }
}
