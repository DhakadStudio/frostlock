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
import com.frostlock.app.ui.screens.profile.AboutScreen
import com.frostlock.app.ui.screens.profile.PrivacyScreen
import com.frostlock.app.ui.screens.profile.ProfileScreen
import com.frostlock.app.ui.screens.widgets.WidgetsScreen

/**
 * You tab: preferences, planning, appearance and privacy.
 *
 * Settings-only. Blocking lives in Shield and insights live in Insights, so no
 * row here duplicates another tab.
 */
fun NavGraphBuilder.youNavGraph(navController: NavHostController) {
    navigation(
        route = Destinations.You.GRAPH,
        startDestination = Destinations.You.HOME
    ) {
        composable(Destinations.You.HOME) {
            ProfileScreen(
                onOpenGoals = {
                    navController.navigateOnce(Destinations.You.GOALS)
                },
                onOpenDeadline = {
                    navController.navigateOnce(Destinations.You.COUNTDOWN)
                },
                onOpenWidgets = {
                    navController.navigateOnce(Destinations.You.WIDGETS)
                },
                onOpenPrivacy = {
                    navController.navigateOnce(Destinations.You.PRIVACY)
                },
                onOpenAbout = {
                    navController.navigateOnce(Destinations.You.ABOUT)
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

        composable(Destinations.You.WIDGETS) {
            WidgetsScreen(onBack = { navController.popBackStack() })
        }

        composable(Destinations.You.PRIVACY) {
            PrivacyScreen(
                onBack = { navController.popBackStack() },
                onOpenShield = {
                    navController.navigateToTab(Destinations.Shield.HOME)
                }
            )
        }

        composable(Destinations.You.ABOUT) {
            AboutScreen(
                onBack = { navController.popBackStack() },
                onOpenDesignPreview = {
                    navController.navigateOnce(Destinations.You.DESIGN_PREVIEW)
                }
            )
        }

        composable(Destinations.You.DESIGN_PREVIEW) {
            DesignPreviewScreen(onBack = { navController.popBackStack() })
        }
    }
}
