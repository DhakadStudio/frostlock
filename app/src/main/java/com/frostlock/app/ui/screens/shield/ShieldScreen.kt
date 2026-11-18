package com.frostlock.app.ui.screens.shield

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.frostlock.app.capability.CapabilityState
import com.frostlock.app.ui.components.surface.FrostCard
import com.frostlock.app.ui.components.surface.HeroCard
import com.frostlock.app.ui.components.surface.PermissionCard
import com.frostlock.app.ui.components.surface.StatusRow
import com.frostlock.app.ui.shell.FrostScreen
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Shield hub.
 *
 * Phase 2 version: one honest status surface that links to the existing detail
 * screens. Phase 3 splits Apps and Limits out of the old Block screen and moves
 * the remaining controls here.
 */
@Composable
fun ShieldScreen(
    onOpenApps: () -> Unit,
    onOpenLimits: () -> Unit,
    onOpenSchedules: () -> Unit,
    onOpenWebsites: () -> Unit,
    onOpenChannels: () -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShieldViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Limits and schedules live in SharedPreferences, so re-read their counts
    // when the hub becomes visible again after a detail screen.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    FrostScreen(modifier = modifier, scrollable = true) {
        Spacer(modifier = Modifier.height(FrostTheme.spacing.xl))
        Text(
            text = "Shield",
            style = FrostTheme.typography.headlineLarge,
            color = FrostTheme.colors.textPrimary
        )
        Text(
            text = "What's held while you focus.",
            style = FrostTheme.typography.bodyMedium,
            color = FrostTheme.colors.textSecondary
        )

        Spacer(modifier = Modifier.height(FrostTheme.spacing.xxl))
        HeroCard {
            Text(
                text = "SHIELD STATUS",
                style = FrostTheme.typography.labelMedium,
                color = FrostTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))
            Text(
                text = shieldHeadline(state),
                style = FrostTheme.typography.headlineMedium,
                color = FrostTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(FrostTheme.spacing.xs))
            Text(
                text = if (state.blockingReady) {
                    "Apps, websites and limits apply during focus and schedules."
                } else {
                    "Nothing can be held until accessibility access is on."
                },
                style = FrostTheme.typography.bodyMedium,
                color = FrostTheme.colors.textSecondary
            )
        }

        if (!state.blockingReady) {
            Spacer(modifier = Modifier.height(FrostTheme.spacing.lg))
            PermissionCard(
                title = "Access needed",
                message = "FrostLock needs accessibility access to hold apps, websites and limits. It reads only what is needed to block, and nothing leaves this device.",
                actionLabel = "Open accessibility settings",
                onAction = {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                }
            )
        }

        // Notification access is optional: only ask for it once the user has
        // actually turned notification holding on.
        if (state.notificationsEnabled && !state.notificationsReady) {
            Spacer(modifier = Modifier.height(FrostTheme.spacing.lg))
            PermissionCard(
                title = "Notification access needed",
                message = "Notification access lets FrostLock hold alerts from blocked apps. Calls, alarms and ongoing notifications always pass.",
                actionLabel = "Allow notifications",
                onAction = {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(FrostTheme.spacing.xxl))
        Text(
            text = "Protection",
            style = FrostTheme.typography.titleLarge,
            color = FrostTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))
        FrostCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = FrostTheme.spacing.lg,
            vertical = FrostTheme.spacing.xs
        )) {
            StatusRow(
                title = "Apps",
                status = appsStatus(state),
                icon = Icons.Filled.Apps,
                statusColor = statusColor(state.blockingReady),
                onClick = onOpenApps
            )
            StatusRow(
                title = "Limits",
                status = if (state.limitsCount == 0) "No limits" else "${state.limitsCount} limits",
                icon = Icons.Filled.HourglassTop,
                onClick = onOpenLimits
            )
            StatusRow(
                title = "Schedules",
                status = schedulesStatus(state),
                icon = Icons.Filled.CalendarMonth,
                onClick = onOpenSchedules
            )
            StatusRow(
                title = "Websites",
                status = websitesStatus(state),
                icon = Icons.Filled.Language,
                onClick = onOpenWebsites
            )
            StatusRow(
                title = "Channels",
                status = channelsStatus(state),
                icon = Icons.Filled.VideoLibrary,
                onClick = onOpenChannels
            )
            StatusRow(
                title = "Notifications",
                status = notificationsStatus(state),
                icon = Icons.Filled.Notifications,
                statusColor = statusColor(!state.notificationsEnabled || state.notificationsReady),
                onClick = onOpenNotifications
            )
        }

        Spacer(modifier = Modifier.height(FrostTheme.spacing.huge))
    }
}

private fun shieldHeadline(state: ShieldState): String = when {
    !state.blockingReady -> "Access is off"
    state.activeSchedules > 0 -> {
        val count = state.activeSchedules
        "$count ${if (count == 1) "schedule" else "schedules"} active"
    }
    else -> "Ready"
}

private fun appsStatus(state: ShieldState): String = when {
    !state.blockingReady -> "Off — access needed"
    !state.appsEnabled -> "Off"
    state.appsCount == 0 -> "On · no apps selected"
    else -> "On · ${state.appsCount} apps selected"
}

private fun schedulesStatus(state: ShieldState): String = when {
    state.activeSchedules > 0 -> "${state.activeSchedules} active"
    state.schedulesCount == 0 -> "No schedules"
    else -> "${state.schedulesCount} schedules"
}

private fun websitesStatus(state: ShieldState): String = when {
    !state.websitesEnabled -> "Off"
    state.websitesCount == 0 -> "On · empty list"
    else -> "${state.websitesCount} sites"
}

private fun channelsStatus(state: ShieldState): String = when {
    !state.channelsEnabled -> "Off"
    state.channelsCount == 0 -> "On · empty list"
    else -> "${state.channelsCount} channels"
}

private fun notificationsStatus(state: ShieldState): String = when {
    !state.notificationsEnabled -> "Off"
    !state.notificationsReady -> "On · access needed"
    state.notificationsHeld == 0 -> "On · nothing held yet"
    else -> "On · ${state.notificationsHeld} held"
}

@Composable
private fun statusColor(ready: Boolean) = if (ready) {
    FrostTheme.colors.textSecondary
} else {
    FrostTheme.colors.warning
}
