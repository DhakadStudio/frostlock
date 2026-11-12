package com.frostlock.app.ui.util

import com.frostlock.app.data.InstalledApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestedLimitsTest {

    private fun app(packageName: String, label: String) =
        InstalledApp(packageName = packageName, label = label)

    @Test
    fun `suggestions keep priority order and skip already limited apps`() {
        val apps = listOf(
            app("com.spotify.music", "Spotify"),
            app("com.instagram.android", "Instagram"),
            app("com.google.android.youtube", "YouTube"),
            app("com.android.chrome", "Chrome"),
            app("com.example.notes", "Notes")
        )

        val suggested = suggestedLimitApps(
            apps = apps,
            existingLimits = mapOf("com.instagram.android" to 20)
        )

        assertEquals(
            listOf("com.google.android.youtube", "com.android.chrome", "com.spotify.music"),
            suggested.map { it.packageName }
        )
    }

    @Test
    fun `regional variants match their base distractor package`() {
        val apps = listOf(
            app("com.google.android.youtube.music", "YouTube Music"),
            app("com.android.chrome.beta", "Chrome Beta")
        )

        val suggested = suggestedLimitApps(apps, existingLimits = emptyMap())

        assertEquals(2, suggested.size)
        assertTrue(suggested.any { it.packageName == "com.google.android.youtube.music" })
    }

    @Test
    fun `limit parameter caps the number of suggestions`() {
        val apps = (1..6).map { index ->
            app("com.instagram.android.v$index", "Instagram $index")
        }

        assertEquals(2, suggestedLimitApps(apps, emptyMap(), limit = 2).size)
    }

    @Test
    fun `no match yields no suggestions`() {
        val apps = listOf(app("com.example.notes", "Notes"))

        assertTrue(suggestedLimitApps(apps, emptyMap()).isEmpty())
    }
}
