package com.frostlock.app.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.frostlock.app.ui.screens.block.BlockScreen
import com.frostlock.app.ui.screens.channels.ChannelsScreen
import com.frostlock.app.ui.screens.notifications.NotificationsScreen
import com.frostlock.app.ui.screens.schedule.ScheduleScreen
import com.frostlock.app.ui.screens.shield.ShieldScreen
import com.frostlock.app.ui.screens.websites.WebsitesScreen

/**
 * Shield tab: everything that blocks, limits or holds.
 *
 * Phase 2 keeps the old detail screens behind the new hub. Apps and Limits both
 * open the existing Block screen until Phase 3 splits it in two.
 */
fun NavGraphBuilder.shieldNavGraph(navController: NavHostController) {
    navigation(
        route = Destinations.Shield.GRAPH,
        startDestination = Destinations.Shield.HOME
    ) {
        composable(Destinations.Shield.HOME) {
            ShieldScreen(
                onOpenApps = { navController.navigateOnce(Destinations.Shield.APPS) },
                onOpenLimits = { navController.navigateOnce(Destinations.Shield.LIMITS) },
                onOpenSchedules = { navController.navigateOnce(Destinations.Shield.SCHEDULES) },
                onOpenWebsites = { navController.navigateOnce(Destinations.Shield.WEBSITES) },
                onOpenChannels = { navController.navigateOnce(Destinations.Shield.CHANNELS) },
                onOpenNotifications = { navController.navigateOnce(Destinations.Shield.NOTIFICATIONS) }
            )
        }

        composable(Destinations.Shield.APPS) {
            BlockScreen(
                onOpenSchedules = { navController.navigateOnce(Destinations.Shield.SCHEDULES) },
                onOpenNotifications = { navController.navigateOnce(Destinations.Shield.NOTIFICATIONS) },
                onOpenWebsites = { navController.navigateOnce(Destinations.Shield.WEBSITES) }
            )
        }

        composable(Destinations.Shield.LIMITS) {
            // Phase 3 splits limits out of BlockScreen; until then both routes
            // share the same screen so no control disappears during migration.
            BlockScreen(
                onOpenSchedules = { navController.navigateOnce(Destinations.Shield.SCHEDULES) },
                onOpenNotifications = { navController.navigateOnce(Destinations.Shield.NOTIFICATIONS) },
                onOpenWebsites = { navController.navigateOnce(Destinations.Shield.WEBSITES) }
            )
        }

        composable(Destinations.Shield.SCHEDULES) {
            ScheduleScreen(onBack = { navController.popBackStack() })
        }
        composable(Destinations.Shield.WEBSITES) {
            WebsitesScreen(onBack = { navController.popBackStack() })
        }
        composable(Destinations.Shield.CHANNELS) {
            ChannelsScreen(onBack = { navController.popBackStack() })
        }
        composable(Destinations.Shield.NOTIFICATIONS) {
            NotificationsScreen(onBack = { navController.popBackStack() })
        }
    }
}
