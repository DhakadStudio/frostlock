package com.frostlock.app.ui.util

import com.frostlock.app.data.InstalledApp

/**
 * Apps that most often steal focus time. Matched by package prefix so regional
 * variants (for example YouTube Music) still resolve to the right suggestion.
 */
private val DISTRACTOR_PREFIXES = listOf(
    "com.google.android.youtube",
    "com.google.android.apps.youtube.music",
    "com.instagram.android",
    "com.zhiliaoapp.musically",
    "com.android.chrome",
    "com.facebook.katana",
    "com.twitter.android",
    "com.reddit.frontpage",
    "com.snapchat.android",
    "com.netflix.mediaclient",
    "com.spotify.music"
)

/** Quick limit offered by the suggestion row. */
const val SUGGESTED_LIMIT_MINUTES = 20

/**
 * Picks installed distractor apps that do not already have a daily limit, in a
 * stable priority order. Everything comes from the local package list, so the
 * suggestions work offline and never leave the device.
 */
fun suggestedLimitApps(
    apps: List<InstalledApp>,
    existingLimits: Map<String, Int>,
    limit: Int = 4
): List<InstalledApp> =
    apps
        .mapNotNull { app ->
            val rank = DISTRACTOR_PREFIXES.indexOfFirst { prefix ->
                app.packageName == prefix || app.packageName.startsWith("$prefix.")
            }
            if (rank < 0 || (existingLimits[app.packageName] ?: 0) > 0) null else rank to app
        }
        .sortedBy { it.first }
        .map { it.second }
        .take(limit)
