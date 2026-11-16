package com.frostlock.app.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YouTubeChannelDetectorTest {

    @Test
    fun `subscribe label carries the channel name`() {
        assertEquals(
            "rick astley",
            YouTubeChannelDetector.ownerChannelName("Subscribe to Rick Astley.")
        )
    }

    @Test
    fun `avatar label with artist badge carries the channel name`() {
        assertEquals(
            "rick astley",
            YouTubeChannelDetector.ownerChannelName(
                "Rick Astley, Official Artist Channel 4.56M subscribers"
            )
        )
    }

    @Test
    fun `plain subscriber count label carries the channel name`() {
        assertEquals(
            "worldwide records bhojpuri",
            YouTubeChannelDetector.ownerChannelName("Worldwide Records Bhojpuri 12.3M subscribers")
        )
        assertEquals(
            "small channel",
            YouTubeChannelDetector.ownerChannelName("Small Channel 847 subscribers")
        )
    }

    @Test
    fun `handles are stripped of the at sign`() {
        assertEquals(
            "rickastleyyt",
            YouTubeChannelDetector.ownerChannelName("Subscribe to @RickAstleyYT")
        )
    }

    @Test
    fun `recommendation chips are not owners`() {
        assertNull(
            YouTubeChannelDetector.ownerChannelName("Go to channel Worldwide Records Bhojpuri")
        )
    }

    @Test
    fun `video titles and actions are not owners`() {
        assertNull(YouTubeChannelDetector.ownerChannelName("Never Gonna Give You Up"))
        assertNull(YouTubeChannelDetector.ownerChannelName("More actions"))
        assertNull(YouTubeChannelDetector.ownerChannelName("19M likes"))
        assertNull(YouTubeChannelDetector.ownerChannelName(""))
    }

    @Test
    fun `normalize collapses case whitespace and handles`() {
        assertEquals("karea studio", YouTubeChannelDetector.normalize(" @Karea  Studio "))
    }

    @Test
    fun `block mode walls a listed channel`() {
        val owner = setOf("rick astley")
        assertEquals(
            "rick astley",
            YouTubeChannelDetector.violatingOwner(
                allowOnly = false,
                ownerNames = owner,
                blocked = setOf("Rick Astley"),
                allowed = emptySet()
            )
        )
    }

    @Test
    fun `block mode passes an unlisted channel`() {
        assertNull(
            YouTubeChannelDetector.violatingOwner(
                allowOnly = false,
                ownerNames = setOf("karea studio"),
                blocked = setOf("Rick Astley"),
                allowed = emptySet()
            )
        )
    }

    @Test
    fun `study mode walls channels outside the allowlist`() {
        assertEquals(
            "rick astley",
            YouTubeChannelDetector.violatingOwner(
                allowOnly = true,
                ownerNames = setOf("rick astley"),
                blocked = emptySet(),
                allowed = setOf("Karea Studio", "@studytee")
            )
        )
    }

    @Test
    fun `study mode passes an allowlisted channel`() {
        assertNull(
            YouTubeChannelDetector.violatingOwner(
                allowOnly = true,
                ownerNames = setOf("@StudyTee"),
                blocked = emptySet(),
                allowed = setOf("studytee")
            )
        )
    }

    @Test
    fun `empty study list never walls everything`() {
        assertNull(
            YouTubeChannelDetector.violatingOwner(
                allowOnly = true,
                ownerNames = setOf("rick astley"),
                blocked = emptySet(),
                allowed = emptySet()
            )
        )
    }

    @Test
    fun `no owners means no violation`() {
        assertNull(
            YouTubeChannelDetector.violatingOwner(
                allowOnly = false,
                ownerNames = emptySet(),
                blocked = setOf("rick astley"),
                allowed = emptySet()
            )
        )
    }
}
