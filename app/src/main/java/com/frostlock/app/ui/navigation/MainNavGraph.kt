package com.frostlock.app.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController

/**
 * The main graph: four sibling tab graphs under one shared scaffold.
 *
 * Tab state is saved and restored by [navigateToTab], so switching tabs never
 * loses scroll position or a half-filled form.
 */
fun NavGraphBuilder.mainNavGraph(navController: NavHostController) {
    focusNavGraph(navController)
    shieldNavGraph(navController)
    insightsNavGraph(navController)
    youNavGraph(navController)
}
