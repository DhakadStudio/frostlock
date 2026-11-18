package com.frostlock.app.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.frostlock.app.ui.screens.focus.FocusScreen

/**
 * Focus tab: the timer and the active session.
 *
 * The session and summary routes arrive in Phase 6. Until then the tab root is
 * the existing Focus screen, so no behaviour changes while the shell is in
 * place.
 */
fun NavGraphBuilder.focusNavGraph(navController: NavHostController) {
    navigation(
        route = Destinations.Focus.GRAPH,
        startDestination = Destinations.Focus.HOME
    ) {
        composable(Destinations.Focus.HOME) {
            FocusScreen()
        }
    }
}
