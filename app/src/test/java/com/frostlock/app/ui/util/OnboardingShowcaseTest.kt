package com.frostlock.app.ui.util

import com.frostlock.app.data.InstalledApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingShowcaseTest {

    private fun app(packageName: String, label: String) =
        InstalledApp(packageName = packageName, label = label)

    @Test
    fun `installed favourites are picked in priority order`() {
        val apps = listOf(
            app("com.spotify.music", "Spotify"),
            app("com.android.chrome", "Chrome"),
            app("com.google.android.youtube", "YouTube"),
            app("com.example.notes", "Notes")
        )

        val picked = onboardingShowcaseApps(apps)

        assertEquals(
            listOf(
                "com.google.android.youtube",
                "com.android.chrome",
                "com.spotify.music"
            ),
            picked.map { it.packageName }
        )
    }

    @Test
    fun `unrecognised apps fill the remaining slots`() {
        val apps = listOf(
            app("com.google.android.youtube", "YouTube"),
            app("com.example.notes", "Notes"),
            app("com.example.maps", "Maps")
        )

        val picked = onboardingShowcaseApps(apps)

        assertEquals(
            listOf(
                "com.google.android.youtube",
                "com.example.notes",
                "com.example.maps"
            ),
            picked.map { it.packageName }
        )
    }

    @Test
    fun `youtube does not shadow youtube music`() {
        val apps = listOf(
            app("com.google.android.youtube.music", "YouTube Music"),
            app("com.google.android.youtube", "YouTube")
        )

        val picked = onboardingShowcaseApps(apps)

        assertEquals(
            listOf("com.google.android.youtube", "com.google.android.youtube.music"),
            picked.map { it.packageName }
        )
    }

    @Test
    fun `count caps the result`() {
        val apps = listOf(
            app("com.google.android.youtube", "YouTube"),
            app("com.instagram.android", "Instagram"),
            app("com.android.chrome", "Chrome"),
            app("com.spotify.music", "Spotify")
        )

        assertEquals(2, onboardingShowcaseApps(apps, count = 2).size)
    }

    @Test
    fun `empty device yields nothing`() {
        assertTrue(onboardingShowcaseApps(emptyList()).isEmpty())
    }
}
