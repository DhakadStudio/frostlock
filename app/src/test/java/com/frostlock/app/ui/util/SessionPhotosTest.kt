package com.frostlock.app.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionPhotosTest {

    @Test
    fun `file names are stable per session`() {
        assertEquals("session_12.jpg", SessionPhotos.fileName(12L))
        assertEquals("session_0.jpg", SessionPhotos.fileName(0L))
    }

    @Test
    fun `only generated names are managed`() {
        assertTrue(SessionPhotos.isManagedName("session_7.jpg"))
        assertFalse(SessionPhotos.isManagedName(""))
        assertFalse(SessionPhotos.isManagedName("../session_7.jpg"))
        assertFalse(SessionPhotos.isManagedName("session_7.png"))
        assertFalse(SessionPhotos.isManagedName("holiday.jpg"))
        assertFalse(SessionPhotos.isManagedName("session_.jpg"))
    }

    @Test
    fun `small images are not downsampled`() {
        assertEquals(1, SessionPhotos.sampleSizeFor(800, 600))
        assertEquals(1, SessionPhotos.sampleSizeFor(1280, 720))
    }

    @Test
    fun `large images halve until they fit`() {
        assertEquals(4, SessionPhotos.sampleSizeFor(4000, 3000))
        assertEquals(2, SessionPhotos.sampleSizeFor(2000, 1500))
        assertEquals(8, SessionPhotos.sampleSizeFor(8000, 6000))
    }

    @Test
    fun `portrait and square images use the longest edge`() {
        assertEquals(4, SessionPhotos.sampleSizeFor(3000, 4000))
        assertEquals(4, SessionPhotos.sampleSizeFor(4000, 4000))
    }

    @Test
    fun `invalid dimensions fall back to no sampling`() {
        assertEquals(1, SessionPhotos.sampleSizeFor(0, 4000))
        assertEquals(1, SessionPhotos.sampleSizeFor(-10, -10))
        assertEquals(1, SessionPhotos.sampleSizeFor(4000, 3000, maxEdge = 0))
    }
}
