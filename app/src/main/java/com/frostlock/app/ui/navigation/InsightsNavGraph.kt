package com.frostlock.app.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.frostlock.app.ui.screens.insights.HistoryScreen
import com.frostlock.app.ui.screens.insights.InsightsScreen
import com.frostlock.app.ui.screens.insights.ReflectionsScreen
import com.frostlock.app.ui.screens.insights.TrendsScreen
import com.frostlock.app.ui.screens.report.WeeklyReviewScreen
import com.frostlock.app.ui.screens.stats.BadgesScreen
import com.frostlock.app.ui.screens.stats.LevelsScreen

/**
 * Insights tab: progress, trends, history, reviews, reflections, badges, levels.
 *
 * The dashboard merges the old Home and Stats pages. Every drill-down owns one
 * period or one archive, so no metric has two homes.
 */
fun NavGraphBuilder.insightsNavGraph(navController: NavHostController) {
    navigation(
        route = Destinations.Insights.GRAPH,
        startDestination = Destinations.Insights.HOME
    ) {
        composable(Destinations.Insights.HOME) {
            InsightsScreen(
                onOpenTrends = { navController.navigateOnce(Destinations.Insights.TRENDS) },
                onOpenHistory = { navController.navigateOnce(Destinations.Insights.HISTORY) },
                onOpenWeekly = { navController.navigateOnce(Destinations.Insights.WEEKLY) },
                onOpenReflections = {
                    navController.navigateOnce(Destinations.Insights.REFLECTIONS)
                },
                onOpenBadges = { navController.navigateOnce(Destinations.Insights.BADGES) },
                onOpenLevels = { navController.navigateOnce(Destinations.Insights.LEVELS) },
                onStartFocus = {
                    navController.navigateToTab(Destinations.Focus.HOME)
                }
            )
        }

        composable(Destinations.Insights.TRENDS) {
            TrendsScreen(onBack = { navController.popBackStack() })
        }
        composable(Destinations.Insights.HISTORY) {
            HistoryScreen(onBack = { navController.popBackStack() })
        }
        composable(Destinations.Insights.WEEKLY) {
            WeeklyReviewScreen(
                onBack = { navController.popBackStack() },
                onOpenReflections = {
                    navController.navigateOnce(Destinations.Insights.REFLECTIONS)
                },
                onStartFocus = {
                    navController.navigateToTab(Destinations.Focus.HOME)
                }
            )
        }
        composable(Destinations.Insights.REFLECTIONS) {
            ReflectionsScreen(
                onBack = { navController.popBackStack() },
                onStartFocus = {
                    navController.navigateToTab(Destinations.Focus.HOME)
                }
            )
        }
        composable(Destinations.Insights.BADGES) {
            BadgesScreen(onBack = { navController.popBackStack() })
        }
        composable(Destinations.Insights.LEVELS) {
            LevelsScreen(onBack = { navController.popBackStack() })
        }
    }
}
