package com.frostlock.app.ui.components.navigation

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Navigation rail for medium and expanded widths.
 *
 * Labels are always shown: the rail is the primary navigation on tablets, not a
 * secondary icon strip.
 */
@Composable
fun FrostNavigationRail(
    items: List<FrostNavItem>,
    selectedRoute: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable ColumnScope.() -> Unit)? = null
) {
    NavigationRail(
        modifier = modifier,
        containerColor = FrostTheme.colors.surfaceContainerLow,
        contentColor = FrostTheme.colors.textPrimary,
        windowInsets = WindowInsets.safeDrawing,
        header = header
    ) {
        items.forEach { item ->
            val selected = item.route == selectedRoute
            NavigationRailItem(
                selected = selected,
                onClick = { onSelect(item.route) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.icon,
                        contentDescription = item.label
                    )
                },
                label = { Text(text = item.label) },
                alwaysShowLabel = true,
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = FrostTheme.colors.onPrimary,
                    selectedTextColor = FrostTheme.colors.textPrimary,
                    indicatorColor = FrostTheme.colors.primary,
                    unselectedIconColor = FrostTheme.colors.textSecondary,
                    unselectedTextColor = FrostTheme.colors.textSecondary
                )
            )
        }
    }
}
