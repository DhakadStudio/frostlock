package com.frostlock.app.data.local.prefs

import org.junit.Assert.assertEquals
import org.junit.Test

class UnlockStoreTest {

    @Test
    fun `first unlock of the day starts at one`() {
        assertEquals(1, UnlockStore.nextCount(null, 0, "2026-11-16"))
    }

    @Test
    fun `same day increments the count`() {
        assertEquals(4, UnlockStore.nextCount("2026-11-16", 3, "2026-11-16"))
    }

    @Test
    fun `new day resets the count`() {
        assertEquals(1, UnlockStore.nextCount("2026-11-15", 12, "2026-11-16"))
    }
}
