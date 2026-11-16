package com.frostlock.app.ui.util

/**
 * Pure helpers for the accessibility Shorts/Reels detector.
 *
 * A bare text match is not enough: the home feed of a short-form app also
 * carries the surface label (a nav tab or a shelf), so matching any occurrence
 * of "Shorts" blocks the app everywhere. Apps with a known bottom-navigation
 * tab are only blocked when that tab is selected, or when the app's short-form
 * player surface is on screen, which its view ids reveal. Apps without a known
 * tab keep the looser text heuristic.
 */
object ShortFormDetector {

    private val KEYWORDS_BY_PACKAGE = mapOf(
        "com.google.android.youtube" to setOf("shorts"),
        "com.instagram.android" to setOf("reels"),
        "com.facebook.katana" to setOf("reels"),
        "com.snapchat.android" to setOf("spotlight")
    )

    /** Bottom-navigation tab that selects the short-form surface, lower-cased. */
    private val TAB_BY_PACKAGE = mapOf(
        "com.google.android.youtube" to "shorts",
        "com.instagram.android" to "reels",
        "com.facebook.katana" to "reels"
    )

    /** View-id prefixes that only appear on the app's short-form player. */
    private val PLAYER_ID_PREFIXES = mapOf(
        "com.google.android.youtube" to "com.google.android.youtube:id/reel_"
    )

    fun keywordsFor(packageName: String): Set<String> =
        KEYWORDS_BY_PACKAGE[packageName].orEmpty()

    fun isShortFormPackage(packageName: String): Boolean =
        packageName in KEYWORDS_BY_PACKAGE

    /** Non-null when this app has a known short-form navigation tab. */
    fun selectedTabLabel(packageName: String): String? = TAB_BY_PACKAGE[packageName]

    /** True when [selectedTabLabel] is the currently selected bottom-nav tab. */
    fun matchesSelectedTab(packageName: String, selectedTab: String?): Boolean {
        val expected = TAB_BY_PACKAGE[packageName] ?: return false
        return selectedTab?.trim()?.lowercase() == expected
    }

    /** True when a node's view id belongs to this app's short-form player. */
    fun matchesPlayerSurface(packageName: String, viewId: String?): Boolean {
        val prefix = PLAYER_ID_PREFIXES[packageName] ?: return false
        return viewId?.startsWith(prefix) == true
    }

    fun matches(packageName: String, text: String): Boolean =
        keywordsFor(packageName).any { text.contains(it, ignoreCase = true) }
}
