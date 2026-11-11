package com.focusflow.app.data.local.prefs

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LimitWarningTest {

    private val thirtyMinutes = 30 * 60_000L

    @Test
    fun `no warning before eighty percent`() {
        assertFalse(AppLimitStore.shouldWarn(0L, 30))
        assertFalse(AppLimitStore.shouldWarn(thirtyMinutes / 2, 30))
        assertFalse(AppLimitStore.shouldWarn(thirtyMinutes * 8 / 10 - 1, 30))
    }

    @Test
    fun `warning starts at eighty percent`() {
        assertTrue(AppLimitStore.shouldWarn(thirtyMinutes * 8 / 10, 30))
        assertTrue(AppLimitStore.shouldWarn(thirtyMinutes - 1, 30))
    }

    @Test
    fun `no warning once the cap is reached or no limit is set`() {
        assertFalse(AppLimitStore.shouldWarn(thirtyMinutes, 30))
        assertFalse(AppLimitStore.shouldWarn(thirtyMinutes * 2, 30))
        assertFalse(AppLimitStore.shouldWarn(thirtyMinutes, 0))
    }
}
