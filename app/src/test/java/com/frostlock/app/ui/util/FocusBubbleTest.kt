package com.frostlock.app.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FocusBubbleTest {

    @Test
    fun `countdown uses the shared clock format`() {
        assertEquals("\u2744 10:00", focusBubbleLabel(openEnded = false, remainingMs = 600_000L))
        assertEquals("\u2744 00:05", focusBubbleLabel(openEnded = false, remainingMs = 5_000L))
    }

    @Test
    fun `open ended sessions show a plain label`() {
        assertEquals("\u2744 Focus", focusBubbleLabel(openEnded = true, remainingMs = 0L))
        assertEquals("\u2744 Focus", focusBubbleLabel(openEnded = true, remainingMs = 999_000L))
    }

    @Test
    fun `negative time is clamped`() {
        assertEquals("\u2744 00:00", focusBubbleLabel(openEnded = false, remainingMs = -4_000L))
    }
}
