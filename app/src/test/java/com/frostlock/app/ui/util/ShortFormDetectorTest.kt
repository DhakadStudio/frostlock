package com.frostlock.app.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortFormDetectorTest {

    @Test
    fun `youtube detects shorts surface`() {
        assertTrue(ShortFormDetector.matches("com.google.android.youtube", "Shorts"))
        assertTrue(ShortFormDetector.matches("com.google.android.youtube", "SHORTS tab"))
    }

    @Test
    fun `instagram and facebook detect reels`() {
        assertTrue(ShortFormDetector.matches("com.instagram.android", "Reels"))
        assertTrue(ShortFormDetector.matches("com.facebook.katana", "Reels"))
    }

    @Test
    fun `snapchat detects spotlight`() {
        assertTrue(ShortFormDetector.matches("com.snapchat.android", "Spotlight"))
    }

    @Test
    fun `unrelated text does not match`() {
        assertFalse(ShortFormDetector.matches("com.google.android.youtube", "Subscriptions"))
        assertFalse(ShortFormDetector.matches("com.instagram.android", "Direct"))
    }

    @Test
    fun `unknown package has no keywords`() {
        assertEquals(emptySet<String>(), ShortFormDetector.keywordsFor("com.example.app"))
        assertFalse(ShortFormDetector.isShortFormPackage("com.example.app"))
    }
}
