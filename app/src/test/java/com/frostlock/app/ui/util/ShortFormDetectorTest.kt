package com.frostlock.app.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortFormDetectorTest {

    @Test
    fun `youtube detects shorts player surface`() {
        assertTrue(
            ShortFormDetector.matchesPlayerSurface(
                "com.google.android.youtube",
                "com.google.android.youtube:id/reel_watch_player"
            )
        )
    }

    @Test
    fun `regular video page ids are not a shorts player`() {
        assertFalse(
            ShortFormDetector.matchesPlayerSurface(
                "com.google.android.youtube",
                "com.google.android.youtube:id/watch_player"
            )
        )
        assertFalse(ShortFormDetector.matchesPlayerSurface("com.google.android.youtube", null))
    }

    @Test
    fun `youtube detects selected shorts tab`() {
        assertTrue(ShortFormDetector.matchesSelectedTab("com.google.android.youtube", "Shorts"))
        assertTrue(ShortFormDetector.matchesSelectedTab("com.google.android.youtube", " shorts "))
    }

    @Test
    fun `unselected or other tabs are not short form`() {
        assertFalse(ShortFormDetector.matchesSelectedTab("com.google.android.youtube", "Home"))
        assertFalse(ShortFormDetector.matchesSelectedTab("com.google.android.youtube", null))
    }

    @Test
    fun `snapchat keeps the text heuristic`() {
        assertEquals(setOf("spotlight"), ShortFormDetector.keywordsFor("com.snapchat.android"))
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
