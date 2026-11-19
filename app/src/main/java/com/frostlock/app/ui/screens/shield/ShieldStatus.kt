package com.frostlock.app.ui.screens.shield

/**
 * The words Shield uses for its status rows.
 *
 * Kept out of the composable so the honesty rules are unit tested: a feature is
 * never described as holding anything unless access is on and it has something
 * to act on.
 */
internal object ShieldStatus {

    /** The hero line. Describes readiness, never claims apps are held right now. */
    fun headline(state: ShieldState): String = when {
        !state.blockingReady -> "Access is off"
        state.activeSchedules > 0 -> {
            val count = state.activeSchedules
            "$count ${if (count == 1) "schedule" else "schedules"} active"
        }
        else -> "Ready"
    }

    fun apps(state: ShieldState): String = when {
        !state.blockingReady -> "Off — access needed"
        !state.appsEnabled -> "Off"
        state.appsCount == 0 -> "On · no apps selected"
        state.appsCount == 1 -> "On · 1 app selected"
        else -> "On · ${state.appsCount} apps selected"
    }

    fun schedules(state: ShieldState): String = when {
        state.activeSchedules > 0 -> "${state.activeSchedules} active"
        state.schedulesCount == 0 -> "No schedules"
        else -> "${state.schedulesCount} schedules"
    }

    fun websites(state: ShieldState): String = when {
        !state.websitesEnabled -> "Off"
        state.websitesCount == 0 -> "On · empty list"
        else -> "${state.websitesCount} sites"
    }

    fun channels(state: ShieldState): String = when {
        !state.channelsEnabled -> "Off"
        state.channelsCount == 0 -> "On · empty list"
        else -> "${state.channelsCount} channels"
    }

    fun notifications(state: ShieldState): String = when {
        !state.notificationsEnabled -> "Off"
        !state.notificationsReady -> "On · access needed"
        state.notificationsHeld == 0 -> "On · nothing held yet"
        else -> "On · ${state.notificationsHeld} held"
    }
}
