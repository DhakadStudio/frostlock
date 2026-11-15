package com.frostlock.app.ui.util

/**
 * One window reported by [android.accessibilityservice.AccessibilityService.getWindows].
 *
 * Only the fields needed for split-screen/floating-window blocking are kept so
 * the matching rules can be unit-tested without an accessibility tree.
 */
data class VisibleWindow(
    val packageName: String,
    val isActive: Boolean,
    val isApplication: Boolean
)

/**
 * Pure rules for blocking a blocked app that stays visible in a second window.
 *
 * Opening a blocked app is handled by the foreground path. The gap is split
 * screen, freeform windows, and picture-in-picture: the user opens an allowed
 * app, so no foreground block fires, yet the blocked app is still usable in the
 * other window. These helpers flag that case.
 */
object MultiWindowDetector {

    /**
     * Returns a blocked package that is visible in a non-active application
     * window, or null when there is nothing to block.
     *
     * The check is skipped when FrostLock itself is the active window so an open
     * block wall does not keep re-triggering. A single visible app is normal
     * full-screen use and is left to the foreground path.
     */
    fun blockedSideWindow(
        windows: List<VisibleWindow>,
        blockedPackages: Set<String>,
        ownPackage: String
    ): String? {
        if (windows.any { it.isActive && it.packageName == ownPackage }) return null

        val apps = windows.filter { it.isApplication && it.packageName != ownPackage }
        if (apps.size < 2) return null

        return apps.firstOrNull { !it.isActive && it.packageName in blockedPackages }?.packageName
    }
}
