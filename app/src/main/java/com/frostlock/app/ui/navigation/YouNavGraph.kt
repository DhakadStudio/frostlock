package com.frostlock.app.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.frostlock.app.ui.screens.deadline.DeadlineScreen
import com.frostlock.app.ui.screens.goals.GoalEditorScreen
import com.frostlock.app.ui.screens.goals.GoalEditorViewModel
import com.frostlock.app.ui.screens.goals.GoalsScreen
import com.frostlock.app.ui.screens.preview.DesignPreviewScreen
import com.frostlock.app.ui.screens.profile.ProfileScreen

/**
 * You tab: preferences, planning, appearance and widgets.
 *
 * Profile still carries a few links that Phase 5 removes (Reflections, Weekly
 * review, Channels). They point at their new homes now so the routes do not
 * change when the page is cleaned up.
 */
fun NavGraphBuilder.youNavGraph(navController: NavHostController) {
    navigation(
        route = Destinations.You.GRAPH,
        startDestination = Destinations.You.HOME
    ) {
        composable(Destinations.You.HOME) {
            ProfileScreen(
                onNavigateToDesignPreview = {
                    navController.navigateOnce(Destinations.You.DESIGN_PREVIEW)
                },
                onOpenDiary = {
                    navController.navigateOnce(Destinations.Insights.REFLECTIONS)
                },
                onOpenWeeklyReport = {
                    navController.navigateOnce(Destinations.Insights.WEEKLY)
                },
                onOpenDeadline = {
                    navController.navigateOnce(Destinations.You.COUNTDOWN)
                },
                onOpenGoals = {
                    navController.navigateOnce(Destinations.You.GOALS)
                },
                onOpenChannels = {
                    navController.navigateOnce(Destinations.Shield.CHANNELS)
                }
            )
        }

        composable(Destinations.You.GOALS) {
            GoalsScreen(
                onBack = { navController.popBackStack() },
                onAddGoal = {
                    navController.navigateOnce(Destinations.You.goalEditor())
                },
                onEditGoal = { goalId ->
                    navController.navigateOnce(Destinations.You.goalEditor(goalId))
                }
            )
        }

        composable(
            route = Destinations.You.GOAL_EDITOR,
            arguments = listOf(
                navArgument(GoalEditorViewModel.GOAL_ID_ARG) {
                    type = NavType.LongType
                    defaultValue = GoalEditorViewModel.NEW_GOAL_ID
                }
            )
        ) {
            GoalEditorScreen(onBack = { navController.popBackStack() })
        }

        composable(Destinations.You.COUNTDOWN) {
            DeadlineScreen(onBack = { navController.popBackStack() })
        }

        composable(Destinations.You.DESIGN_PREVIEW) {
            DesignPreviewScreen(onBack = { navController.popBackStack() })
        }
    }
}
