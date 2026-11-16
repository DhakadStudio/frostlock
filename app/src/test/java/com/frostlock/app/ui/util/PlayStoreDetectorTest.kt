package com.frostlock.app.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayStoreDetectorTest {

    private val blocked = listOf("YouTube Music", "Instagram", "X")

    @Test
    fun `exact app name matches`() {
        assertEquals("YouTube Music", PlayStoreDetector.matchedLabel("youtube music", blocked))
    }

    @Test
    fun `extra words around the name still match`() {
        assertEquals(
            "YouTube Music",
            PlayStoreDetector.matchedLabel("best youtube music apps 2024", blocked)
        )
    }

    @Test
    fun `case and punctuation are ignored`() {
        assertEquals(
            "Instagram",
            PlayStoreDetector.matchedLabel("Instagram!!", blocked)
        )
    }

    @Test
    fun `partial word does not match`() {
        assertNull(PlayStoreDetector.matchedLabel("xbox games", blocked))
    }

    @Test
    fun `different word order does not match`() {
        assertNull(PlayStoreDetector.matchedLabel("music youtube", blocked))
    }

    @Test
    fun `unrelated query does not match`() {
        assertNull(PlayStoreDetector.matchedLabel("note taking app", blocked))
    }

    @Test
    fun `empty query does not match`() {
        assertNull(PlayStoreDetector.matchedLabel("", blocked))
        assertNull(PlayStoreDetector.matchedLabel("   ", blocked))
    }

    @Test
    fun `label with no word tokens does not match`() {
        assertNull(PlayStoreDetector.matchedLabel("what is this", listOf("***")))
    }

    @Test
    fun `short label matches as a whole word`() {
        assertEquals("X", PlayStoreDetector.matchedLabel("open x now", blocked))
    }

    @Test
    fun `numeric app name matches`() {
        assertEquals("2048", PlayStoreDetector.matchedLabel("play 2048", listOf("2048")))
    }

    @Test
    fun `longer label spanning query words matches`() {
        assertEquals(
            "YT Music",
            PlayStoreDetector.matchedLabel("download yt music songs", listOf("YT Music"))
        )
    }

    @Test
    fun `empty blocked list never matches`() {
        assertNull(PlayStoreDetector.matchedLabel("youtube music", emptyList()))
    }
}
