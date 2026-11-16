package com.frostlock.app.ui.util

/** One bottom-navigation tab reported by accessibility. */
data class NavTab(
    val label: String,
    val isSelected: Boolean
)

/**
 * Pure rules for YouTube surface filtering.
 *
 * YouTube is often needed for study videos, so instead of blocking the whole app
 * the user can block just the recommendation home feed. The selected bottom
 * navigation tab is the reliable signal: search, subscriptions and a playing
 * video either select another tab or hide the bar entirely.
 */
object YouTubeSurfaceDetector {

    const val PACKAGE = "com.google.android.youtube"

    /** Labels YouTube uses for its bottom navigation, lower-cased. */
    val tabLabels = setOf("home", "shorts", "subscriptions", "you", "library")

    /** True when the Home tab is the selected bottom-navigation tab. */
    fun isHomeFeed(tabs: List<NavTab>): Boolean =
        tabs.any { it.isSelected && it.label.trim().lowercase() == "home" }
}
