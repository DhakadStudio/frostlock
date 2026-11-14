package com.frostlock.app.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusPausesTest {

    @Test
    fun `default allowance permits two pauses then stops`() {
        val fresh = pauseAllowance(used = 0, max = PausePolicy.DEFAULT)
        assertTrue(fresh.canPause)
        assertEquals(2, fresh.remaining)

        val usedOnce = pauseAllowance(used = 1, max = PausePolicy.DEFAULT)
        assertTrue(usedOnce.canPause)
        assertEquals(1, usedOnce.remaining)

        val usedTwice = pauseAllowance(used = 2, max = PausePolicy.DEFAULT)
        assertFalse(usedTwice.canPause)
        assertEquals(0, usedTwice.remaining)
    }

    @Test
    fun `zero pauses disables the pause button immediately`() {
        val allowance = pauseAllowance(used = 0, max = 0)
        assertFalse(allowance.canPause)
        assertEquals(0, allowance.remaining)
        assertEquals("Pauses off", allowance.usageLabel)
    }

    @Test
    fun `unlimited pauses never run out`() {
        val allowance = pauseAllowance(used = 99, max = PausePolicy.UNLIMITED)
        assertTrue(allowance.canPause)
        assertTrue(allowance.unlimited)
        assertNull(allowance.remaining)
        assertEquals("No pause limit", allowance.usageLabel)
    }

    @Test
    fun `used count cannot go negative`() {
        val allowance = pauseAllowance(used = -3, max = 1)
        assertEquals(0, allowance.used)
        assertTrue(allowance.canPause)
    }

    @Test
    fun `out of range caps are clamped`() {
        assertEquals(PausePolicy.MAX, pauseAllowance(used = 0, max = 50).max)
        assertEquals(PausePolicy.UNLIMITED, pauseAllowance(used = 0, max = -8).max)
        assertEquals(0, pauseAllowance(used = 0, max = -0).max)
    }

    @Test
    fun `usage label shows progress after the first pause`() {
        assertEquals("Pauses 3", pauseAllowance(used = 0, max = 3).usageLabel)
        assertEquals("Pauses 1/3", pauseAllowance(used = 1, max = 3).usageLabel)
        assertEquals("Pauses 3/3", pauseAllowance(used = 3, max = 3).usageLabel)
    }

    @Test
    fun `option labels are short enough for chips`() {
        assertEquals("Off", pauseOptionLabel(0))
        assertEquals("1", pauseOptionLabel(1))
        assertEquals("5", pauseOptionLabel(5))
        assertEquals("No limit", pauseOptionLabel(PausePolicy.UNLIMITED))
    }
}
