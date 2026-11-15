package com.frostlock.app.data.local.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeBlockingTest {

    @Test
    fun `home blocking key exists`() {
        assertEquals("home_blocking_enabled", BlockingPreferences.KEY_HOME_BLOCKING_ENABLED)
    }

    @Test
    fun `preference name unchanged`() {
        assertEquals("frostlock_blocking", BlockingPreferences.PREFS_NAME)
    }
}
