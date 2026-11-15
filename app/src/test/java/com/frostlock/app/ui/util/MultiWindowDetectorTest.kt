package com.frostlock.app.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MultiWindowDetectorTest {

    private val blocked = setOf("com.google.android.youtube", "com.instagram.android")

    @Test
    fun `single full-screen app is not treated as split screen`() {
        val windows = listOf(
            VisibleWindow("com.google.android.youtube", isActive = true, isApplication = true)
        )
        assertNull(MultiWindowDetector.blockedSideWindow(windows, blocked, "com.frostlock.app"))
    }

    @Test
    fun `blocked app beside an allowed app is detected`() {
        val windows = listOf(
            VisibleWindow("com.android.chrome", isActive = true, isApplication = true),
            VisibleWindow("com.google.android.youtube", isActive = false, isApplication = true)
        )
        assertEquals(
            "com.google.android.youtube",
            MultiWindowDetector.blockedSideWindow(windows, blocked, "com.frostlock.app")
        )
    }

    @Test
    fun `picture-in-picture window counts as a second app`() {
        val windows = listOf(
            VisibleWindow("com.android.chrome", isActive = true, isApplication = true),
            VisibleWindow("com.instagram.android", isActive = false, isApplication = true)
        )
        assertEquals(
            "com.instagram.android",
            MultiWindowDetector.blockedSideWindow(windows, blocked, "com.frostlock.app")
        )
    }

    @Test
    fun `two allowed apps are left alone`() {
        val windows = listOf(
            VisibleWindow("com.android.chrome", isActive = true, isApplication = true),
            VisibleWindow("com.android.calendar", isActive = false, isApplication = true)
        )
        assertNull(MultiWindowDetector.blockedSideWindow(windows, blocked, "com.frostlock.app"))
    }

    @Test
    fun `system windows do not count as a second app`() {
        val windows = listOf(
            VisibleWindow("com.android.chrome", isActive = true, isApplication = true),
            VisibleWindow("com.google.android.youtube", isActive = false, isApplication = false)
        )
        assertNull(MultiWindowDetector.blockedSideWindow(windows, blocked, "com.frostlock.app"))
    }

    @Test
    fun `open block wall suppresses further detection`() {
        val windows = listOf(
            VisibleWindow("com.frostlock.app", isActive = true, isApplication = true),
            VisibleWindow("com.android.chrome", isActive = false, isApplication = true),
            VisibleWindow("com.google.android.youtube", isActive = false, isApplication = true)
        )
        assertNull(MultiWindowDetector.blockedSideWindow(windows, blocked, "com.frostlock.app"))
    }

    @Test
    fun `active blocked app is left to the foreground path`() {
        val windows = listOf(
            VisibleWindow("com.google.android.youtube", isActive = true, isApplication = true),
            VisibleWindow("com.android.chrome", isActive = false, isApplication = true)
        )
        assertNull(MultiWindowDetector.blockedSideWindow(windows, blocked, "com.frostlock.app"))
    }
}
