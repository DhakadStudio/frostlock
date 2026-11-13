package com.frostlock.app.ui.navigation

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
import com.frostlock.app.ui.components.AppBottomBar
import com.frostlock.app.ui.screens.block.BlockScreen
import com.frostlock.app.ui.screens.diary.DiaryScreen
import com.frostlock.app.ui.screens.focus.FocusScreen
import com.frostlock.app.ui.screens.home.HomeScreen
import com.frostlock.app.ui.screens.notifications.NotificationsScreen
import com.frostlock.app.ui.screens.onboarding.OnboardingScreen
import com.frostlock.app.ui.screens.onboarding.OnboardingViewModel
import com.frostlock.app.ui.screens.preview.DesignPreviewScreen
import com.frostlock.app.ui.screens.profile.ProfileScreen
import com.frostlock.app.ui.screens.report.WeeklyReportScreen
import com.frostlock.app.ui.screens.schedule.ScheduleScreen
import com.frostlock.app.ui.screens.stats.AchievementsScreen
import com.frostlock.app.ui.screens.stats.StatsScreen

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
        currentRoute != Screen.Schedules.route &&
        currentRoute != Screen.Achievements.route &&
        currentRoute != Screen.Diary.route &&
        currentRoute != Screen.WeeklyReport.route &&
        currentRoute != Screen.Notifications.route

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
                        },
                        onOpenSchedules = {
                            navController.navigate(Screen.Schedules.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(Screen.Block.route) {
                    BlockScreen(
                        onOpenSchedules = { navController.navigate(Screen.Schedules.route) },
                        onOpenNotifications = {
                            navController.navigate(Screen.Notifications.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(Screen.Focus.route) { FocusScreen() }
                composable(Screen.Stats.route) {
                    StatsScreen(
                        onOpenAchievements = {
                            navController.navigate(Screen.Achievements.route) {
                                launchSingleTop = true
                            }
                        },
                        onOpenDiary = {
                            navController.navigate(Screen.Diary.route) {
                                launchSingleTop = true
                            }
                        },
                        onOpenWeeklyReport = {
                            navController.navigate(Screen.WeeklyReport.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(Screen.Achievements.route) {
                    AchievementsScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.Diary.route) {
                    DiaryScreen(
                        onBack = { navController.popBackStack() },
                        onStartFocus = {
                            navController.navigate(Screen.Focus.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                composable(Screen.WeeklyReport.route) {
                    WeeklyReportScreen(
                        onBack = { navController.popBackStack() },
                        onOpenDiary = {
                            navController.navigate(Screen.Diary.route) {
                                launchSingleTop = true
                            }
                        },
                        onStartFocus = {
                            navController.navigate(Screen.Focus.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                composable(Screen.Schedules.route) {
                    ScheduleScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.Notifications.route) {
                    NotificationsScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.Profile.route) {
                    ProfileScreen(
                        onNavigateToDesignPreview = {
                            navController.navigate(Screen.DesignPreview.route)
                        },
                        onOpenDiary = {
                            navController.navigate(Screen.Diary.route) {
                                launchSingleTop = true
                            }
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
