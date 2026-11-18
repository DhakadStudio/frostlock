package com.frostlock.app.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.frostlock.app.ui.screens.calendar.CalendarScreen
import com.frostlock.app.ui.screens.diary.DiaryScreen
import com.frostlock.app.ui.screens.report.WeeklyReportScreen
import com.frostlock.app.ui.screens.stats.AchievementsScreen
import com.frostlock.app.ui.screens.stats.LevelsScreen
import com.frostlock.app.ui.screens.stats.StatsScreen

/**
 * Insights tab: progress, reviews, reflections, badges and levels.
 *
 * The old Stats screen stands in for the consolidated dashboard until Phase 4.
 * Each destination already has its final route, so Phase 4 replaces screens
 * without moving links again.
 */
fun NavGraphBuilder.insightsNavGraph(navController: NavHostController) {
    navigation(
        route = Destinations.Insights.GRAPH,
        startDestination = Destinations.Insights.HOME
    ) {
        composable(Destinations.Insights.HOME) {
            StatsScreen(
                onOpenAchievements = {
                    navController.navigateOnce(Destinations.Insights.BADGES)
                },
                onOpenDiary = {
                    navController.navigateOnce(Destinations.Insights.REFLECTIONS)
                },
                onOpenWeeklyReport = {
                    navController.navigateOnce(Destinations.Insights.WEEKLY)
                },
                onOpenCalendar = {
                    navController.navigateOnce(Destinations.Insights.HISTORY)
                },
                onOpenLevels = {
                    navController.navigateOnce(Destinations.Insights.LEVELS)
                },
                onStartFocus = {
                    navController.navigateToTab(Destinations.Focus.HOME)
                }
            )
        }

        composable(Destinations.Insights.HISTORY) {
            CalendarScreen(onBack = { navController.popBackStack() })
        }
        composable(Destinations.Insights.WEEKLY) {
            WeeklyReportScreen(
                onBack = { navController.popBackStack() },
                onOpenDiary = {
                    navController.navigateOnce(Destinations.Insights.REFLECTIONS)
                },
                onStartFocus = {
                    navController.navigateToTab(Destinations.Focus.HOME)
                }
            )
        }
        composable(Destinations.Insights.REFLECTIONS) {
            DiaryScreen(
                onBack = { navController.popBackStack() },
                onStartFocus = {
                    navController.navigateToTab(Destinations.Focus.HOME)
                }
            )
        }
        composable(Destinations.Insights.BADGES) {
            AchievementsScreen(onBack = { navController.popBackStack() })
        }
        composable(Destinations.Insights.LEVELS) {
            LevelsScreen(onBack = { navController.popBackStack() })
        }
    }
}
