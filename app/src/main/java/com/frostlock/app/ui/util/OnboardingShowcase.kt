package com.frostlock.app.ui.util

import com.frostlock.app.data.InstalledApp

/**
 * Apps shown in the onboarding "App control" illustration, most recognisable first.
 * Anything that is not installed is skipped, so the slide only ever shows real
 * launcher icons that exist on this device.
 */
private val SHOWCASE_PRIORITY = listOf(
    "com.google.android.youtube",
    "com.instagram.android",
    "com.android.chrome",
    "com.google.android.apps.youtube.music",
    "com.zhiliaoapp.musically",
    "com.facebook.katana",
    "com.twitter.android",
    "com.reddit.frontpage",
    "com.snapchat.android",
    "com.netflix.mediaclient",
    "com.spotify.music"
)

/**
 * Picks up to [count] apps to illustrate app limits during onboarding.
 *
 * Well-known distractors win, then any other launchable app fills the remaining
 * slots so a fresh device still shows a full card.
 */
fun onboardingShowcaseApps(
    apps: List<InstalledApp>,
    count: Int = 3
): List<InstalledApp> {
    val picked = LinkedHashMap<String, InstalledApp>()

    // Exact matches first so a YouTube package can never shadow YouTube Music.
    SHOWCASE_PRIORITY.forEach { packageName ->
        if (picked.size >= count) return@forEach
        apps.firstOrNull { it.packageName == packageName }?.let {
            picked[it.packageName] = it
        }
    }

    SHOWCASE_PRIORITY.forEach { prefix ->
        if (picked.size >= count) return@forEach
        apps.firstOrNull { app ->
            app.packageName !in picked && app.packageName.startsWith("$prefix.")
        }?.let { picked[it.packageName] = it }
    }

    if (picked.size < count) {
        apps.filter { it.packageName !in picked }
            .take(count - picked.size)
            .forEach { picked[it.packageName] = it }
    }

    return picked.values.toList()
}
