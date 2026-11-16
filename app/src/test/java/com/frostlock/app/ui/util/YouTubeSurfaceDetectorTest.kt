package com.frostlock.app.ui.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeSurfaceDetectorTest {

    @Test
    fun `selected home tab is the home feed`() {
        val tabs = listOf(
            NavTab("Home", isSelected = true),
            NavTab("Shorts", isSelected = false),
            NavTab("Subscriptions", isSelected = false),
            NavTab("You", isSelected = false)
        )
        assertTrue(YouTubeSurfaceDetector.isHomeFeed(tabs))
    }

    @Test
    fun `subscriptions tab is not the home feed`() {
        val tabs = listOf(
            NavTab("Home", isSelected = false),
            NavTab("Subscriptions", isSelected = true)
        )
        assertFalse(YouTubeSurfaceDetector.isHomeFeed(tabs))
    }

    @Test
    fun `search screen without a nav bar is not the home feed`() {
        assertFalse(YouTubeSurfaceDetector.isHomeFeed(emptyList()))
    }

    @Test
    fun `unselected home label does not match`() {
        val tabs = listOf(NavTab("Home", isSelected = false))
        assertFalse(YouTubeSurfaceDetector.isHomeFeed(tabs))
    }

    @Test
    fun `label matching ignores case and whitespace`() {
        assertTrue(YouTubeSurfaceDetector.isHomeFeed(listOf(NavTab(" home ", isSelected = true))))
    }
}
