package com.frostlock.app.ui.util

/**
 * Pure helpers for the accessibility Shorts/Reels detector.
 *
 * The detector looks for surface labels that the apps expose to accessibility.
 * Keeping the mapping here makes the behaviour unit-testable without an
 * emulator or a live accessibility tree.
 */
object ShortFormDetector {

    private val KEYWORDS_BY_PACKAGE = mapOf(
        "com.google.android.youtube" to setOf("shorts"),
        "com.instagram.android" to setOf("reels"),
        "com.facebook.katana" to setOf("reels"),
        "com.snapchat.android" to setOf("spotlight")
    )

    fun keywordsFor(packageName: String): Set<String> =
        KEYWORDS_BY_PACKAGE[packageName].orEmpty()

    fun isShortFormPackage(packageName: String): Boolean =
        packageName in KEYWORDS_BY_PACKAGE

    fun matches(packageName: String, text: String): Boolean =
        keywordsFor(packageName).any { text.contains(it, ignoreCase = true) }
}
