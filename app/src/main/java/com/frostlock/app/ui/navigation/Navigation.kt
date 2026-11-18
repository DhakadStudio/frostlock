package com.frostlock.app.ui.navigation

import androidx.navigation.NavHostController

/**
 * Switches to a primary tab.
 *
 * Focus is the anchor: it is the main graph's start destination and is never
 * popped. Switching tabs saves the stack above it, so returning to a tab
 * restores its whole stack — including an open detail — and system back from any
 * other tab root returns to Focus instead of closing the app.
 *
 * Navigation targets the tab *graph* rather than its root leaf, which is what
 * lets `restoreState` bring back a multi-screen stack.
 */
fun NavHostController.navigateToTab(route: String) {
    val destination = TopLevelDestination.fromRoute(route)
    navigate(destination?.graphRoute ?: route) {
        popUpTo(Destinations.Focus.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Opens a detail inside the current tab without stacking duplicates. */
fun NavHostController.navigateOnce(route: String) {
    navigate(route) { launchSingleTop = true }
}
