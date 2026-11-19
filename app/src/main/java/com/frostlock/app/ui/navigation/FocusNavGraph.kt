package com.frostlock.app.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.frostlock.app.ui.screens.focus.FocusScreen
import com.frostlock.app.ui.screens.focus.SessionSummaryScreen

/**
 * Focus tab: the timer, the active session and the session summary.
 *
 * The running session itself is hosted above the navigation host (see
 * `AppNavHost`), so it never gets torn down on a tab switch. The routes here
 * are for the tab root and the post-session summary.
 */
fun NavGraphBuilder.focusNavGraph(navController: NavHostController) {
    navigation(
        route = Destinations.Focus.GRAPH,
        startDestination = Destinations.Focus.HOME
    ) {
        composable(Destinations.Focus.HOME) {
            FocusScreen(
                onOpenShieldApps = {
                    navController.navigateToTab(Destinations.Shield.GRAPH)
                    navController.navigateOnce(Destinations.Shield.APPS)
                }
            )
        }

        // The session is shown by the shell-level immersive host. This route
        // only exists so a deep link or a restored back stack lands on the tab
        // root instead of crashing; the current session (if any) is already
        // visible as the running-session bar.
        composable(Destinations.Focus.SESSION) {
            androidx.compose.runtime.LaunchedEffect(Unit) {
                if (!navController.popBackStack(Destinations.Focus.HOME, inclusive = false)) {
                    navController.navigate(Destinations.Focus.HOME) {
                        launchSingleTop = true
                    }
                }
            }
        }

        composable(
            route = Destinations.Focus.SUMMARY,
            arguments = listOf(
                navArgument(Destinations.Focus.SUMMARY_ARG) { type = NavType.LongType },
                navArgument(Destinations.Focus.SUMMARY_CONTINUES_ARG) {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { entry ->
            SessionSummaryScreen(
                continues = entry.arguments
                    ?.getBoolean(Destinations.Focus.SUMMARY_CONTINUES_ARG) == true,
                onDone = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Destinations.Focus.HOME) {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }
    }
}
