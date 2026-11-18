package com.frostlock.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.frostlock.app.ui.components.navigation.FrostNavigationBar
import com.frostlock.app.ui.screens.onboarding.OnboardingScreen
import com.frostlock.app.ui.screens.onboarding.OnboardingViewModel
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Root host: onboarding, then the four-tab main graph.
 *
 * One [NavHostController] owns the whole back stack, including the nested tab
 * graphs, so system back and predictive back behave like a single app rather
 * than two hosts fighting over the dispatcher.
 */
@Composable
fun AppNavHost(
    startDestination: String,
    onFinishApp: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentTab = TopLevelDestination.fromRoute(currentRoute)
    val showNavigation = currentTab != null && currentRoute !in Destinations.immersiveRoutes

    Scaffold(
        containerColor = FrostTheme.colors.background,
        bottomBar = {
            if (showNavigation && currentTab != null) {
                FrostNavigationBar(
                    items = TopLevelDestination.entries.map { it.navItem },
                    selectedRoute = currentTab.route,
                    onSelect = navController::navigateToTab
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding)
        ) {
            composable(Destinations.ONBOARDING) {
                val viewModel: OnboardingViewModel = hiltViewModel()
                OnboardingScreen(
                    viewModel = viewModel,
                    onFinished = {
                        navController.navigate(Destinations.MAIN) {
                            popUpTo(Destinations.ONBOARDING) { inclusive = true }
                        }
                    },
                    onExit = onFinishApp
                )
            }

            navigation(
                route = Destinations.MAIN,
                startDestination = Destinations.Focus.GRAPH
            ) {
                mainNavGraph(navController)
            }
        }
    }
}
