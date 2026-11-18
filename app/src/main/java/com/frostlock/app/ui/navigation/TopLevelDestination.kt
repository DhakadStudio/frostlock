package com.frostlock.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.ui.graphics.vector.ImageVector
import com.frostlock.app.ui.components.navigation.FrostNavItem

/**
 * The four primary destinations.
 *
 * Four tabs map to the four jobs: do a session, protect it, see progress, tune
 * the app. The order here is the order in the navigation bar.
 */
enum class TopLevelDestination(
    val route: String,
    val graphRoute: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    FOCUS(
        route = Destinations.Focus.HOME,
        graphRoute = Destinations.Focus.GRAPH,
        label = "Focus",
        icon = Icons.Outlined.Timer,
        selectedIcon = Icons.Filled.Timer
    ),
    SHIELD(
        route = Destinations.Shield.HOME,
        graphRoute = Destinations.Shield.GRAPH,
        label = "Shield",
        icon = Icons.Outlined.Shield,
        selectedIcon = Icons.Filled.Shield
    ),
    INSIGHTS(
        route = Destinations.Insights.HOME,
        graphRoute = Destinations.Insights.GRAPH,
        label = "Insights",
        icon = Icons.Outlined.BarChart,
        selectedIcon = Icons.Filled.BarChart
    ),
    YOU(
        route = Destinations.You.HOME,
        graphRoute = Destinations.You.GRAPH,
        label = "You",
        icon = Icons.Outlined.Person,
        selectedIcon = Icons.Filled.Person
    );

    val navItem: FrostNavItem
        get() = FrostNavItem(
            route = route,
            label = label,
            icon = icon,
            selectedIcon = selectedIcon
        )

    companion object {
        /** Maps a leaf route such as `shield/notifications` back to its tab. */
        fun fromRoute(route: String?): TopLevelDestination? {
            if (route == null) return null
            return entries.firstOrNull { destination ->
                route == destination.graphRoute ||
                    route.startsWith("${destination.graphRoute}/")
            }
        }
    }
}
