package com.frostlock.app.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.frostlock.app.ui.screens.channels.ChannelsScreen
import com.frostlock.app.ui.screens.notifications.NotificationsScreen
import com.frostlock.app.ui.screens.schedule.ScheduleScreen
import com.frostlock.app.ui.screens.shield.AdvancedScreen
import com.frostlock.app.ui.screens.shield.AppsScreen
import com.frostlock.app.ui.screens.shield.LimitsScreen
import com.frostlock.app.ui.screens.shield.ShieldScreen
import com.frostlock.app.ui.screens.websites.WebsitesScreen

/**
 * Shield tab: everything that blocks, limits or holds.
 *
 * The hub shows live status and opens one screen per protection feature. Each
 * screen owns one job, so apps, limits, schedules, websites, channels and
 * notifications never share a page.
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
            AppsScreen(
                onBack = { navController.popBackStack() },
                onOpenAdvanced = { navController.navigateOnce(Destinations.Shield.ADVANCED) }
            )
        }
        composable(Destinations.Shield.ADVANCED) {
            AdvancedScreen(onBack = { navController.popBackStack() })
        }
        composable(Destinations.Shield.LIMITS) {
            LimitsScreen(onBack = { navController.popBackStack() })
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
