package com.frostlock.app.ui.screens.profile

import com.frostlock.app.capability.CapabilityState
import com.frostlock.app.data.local.prefs.FocusDeadline
import com.frostlock.app.ui.util.deadlineCountdown
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The words You uses for its setting values.
 *
 * Kept out of the composable so the honest states are unit tested: a permission
 * row says "Needs permission" rather than "On", and a missing countdown says
 * "Set a target date" rather than "0 days left".
 */
internal object YouStatus {

    fun dailyTarget(minutes: Int): String {
        val hours = minutes / 60
        val rest = minutes % 60
        return when {
            hours == 0 -> "${rest}m"
            rest == 0 -> "${hours}h"
            else -> "${hours}h ${rest}m"
        }
    }

    fun goals(count: Int): String = when (count) {
        0 -> "Set a focus goal"
        1 -> "1 active goal"
        else -> "$count active goals"
    }

    fun deadline(
        deadline: FocusDeadline?,
        today: LocalDate = LocalDate.now()
    ): String {
        if (deadline == null) return "Set a target date"
        val date = formatDate(deadline.date)
        val countdown = deadlineCountdown(deadline, today).orEmpty()
        return if (countdown.isBlank()) date else "$date · $countdown"
    }

    fun sessionAlerts(access: CapabilityState): String = when (access) {
        CapabilityState.GRANTED -> "On"
        CapabilityState.DENIED -> "Needs permission"
        CapabilityState.UNAVAILABLE -> "Not available"
        CapabilityState.NOT_CONFIGURED -> "Off"
    }

    fun formatDate(iso: String): String = runCatching {
        LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US))
    }.getOrDefault(iso)
}
