package com.focusflow.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.focusflow.app.ui.components.AppBottomBar
import com.focusflow.app.ui.screens.block.BlockScreen
import com.focusflow.app.ui.screens.focus.FocusScreen
import com.focusflow.app.ui.screens.home.HomeScreen
import com.focusflow.app.ui.screens.onboarding.OnboardingScreen
import com.focusflow.app.ui.screens.onboarding.OnboardingViewModel
import com.focusflow.app.ui.screens.preview.DesignPreviewScreen
import com.focusflow.app.ui.screens.profile.ProfileScreen
import com.focusflow.app.ui.screens.schedule.ScheduleScreen
import com.focusflow.app.ui.screens.stats.StatsScreen

@Composable
fun AppNavGraph(
    startDestination: String,
    onFinishApp: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute != Screen.DesignPreview.route &&
        currentRoute != Screen.Onboarding.route &&
        currentRoute != Screen.Schedules.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    currentRoute = currentRoute,
                    onTabSelected = { route ->
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable(Screen.Onboarding.route) {
                    val viewModel: OnboardingViewModel = hiltViewModel()
                    OnboardingScreen(
                        viewModel = viewModel,
                        onFinished = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        },
                        onExit = onFinishApp
                    )
                }
                composable(Screen.Home.route) {
                    HomeScreen(
                        onStartFocus = {
                            navController.navigate(Screen.Focus.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onOpenBlock = {
                            navController.navigate(Screen.Block.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                composable(Screen.Block.route) {
                    BlockScreen(
                        onOpenSchedules = { navController.navigate(Screen.Schedules.route) }
                    )
                }
                composable(Screen.Focus.route) { FocusScreen() }
                composable(Screen.Stats.route) { StatsScreen() }
                composable(Screen.Schedules.route) {
                    ScheduleScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.Profile.route) {
                    ProfileScreen(
                        onNavigateToDesignPreview = {
                            navController.navigate(Screen.DesignPreview.route)
                        }
                    )
                }
                composable(Screen.DesignPreview.route) {
                    DesignPreviewScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
