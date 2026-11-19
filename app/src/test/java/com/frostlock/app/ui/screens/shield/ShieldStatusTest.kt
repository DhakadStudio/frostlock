package com.frostlock.app.ui.screens.shield

import com.frostlock.app.capability.CapabilityState
import org.junit.Assert.assertEquals
import org.junit.Test

class ShieldStatusTest {

    private val granted = ShieldState(blockingAccess = CapabilityState.GRANTED)

    @Test
    fun `headline says access is off when blocking is not granted`() {
        val state = ShieldState(
            appsEnabled = true,
            appsCount = 4,
            activeSchedules = 2,
            blockingAccess = CapabilityState.DENIED
        )
        assertEquals("Access is off", ShieldStatus.headline(state))
    }

    @Test
    fun `headline counts active schedules`() {
        assertEquals(
            "1 schedule active",
            ShieldStatus.headline(granted.copy(activeSchedules = 1))
        )
        assertEquals(
            "3 schedules active",
            ShieldStatus.headline(granted.copy(activeSchedules = 3))
        )
    }

    @Test
    fun `headline is ready when access is on and no schedule is active`() {
        assertEquals("Ready", ShieldStatus.headline(granted))
    }

    @Test
    fun `headline never claims apps are held`() {
        val state = granted.copy(appsEnabled = true, appsCount = 9)
        assertEquals("Ready", ShieldStatus.headline(state))
    }

    @Test
    fun `apps report access missing before reporting a selection`() {
        val state = ShieldState(
            appsEnabled = true,
            appsCount = 5,
            blockingAccess = CapabilityState.DENIED
        )
        assertEquals("Off — access needed", ShieldStatus.apps(state))
    }

    @Test
    fun `apps distinguish off from on with nothing selected`() {
        assertEquals("Off", ShieldStatus.apps(granted))
        assertEquals(
            "On · no apps selected",
            ShieldStatus.apps(granted.copy(appsEnabled = true))
        )
        assertEquals(
            "On · 1 app selected",
            ShieldStatus.apps(granted.copy(appsEnabled = true, appsCount = 1))
        )
        assertEquals(
            "On · 3 apps selected",
            ShieldStatus.apps(granted.copy(appsEnabled = true, appsCount = 3))
        )
    }

    @Test
    fun `schedules show active count then configured count`() {
        assertEquals("No schedules", ShieldStatus.schedules(granted))
        assertEquals(
            "4 schedules",
            ShieldStatus.schedules(granted.copy(schedulesCount = 4))
        )
        assertEquals(
            "2 active",
            ShieldStatus.schedules(granted.copy(schedulesCount = 4, activeSchedules = 2))
        )
    }

    @Test
    fun `websites and channels flag an empty enabled list`() {
        assertEquals("Off", ShieldStatus.websites(granted))
        assertEquals(
            "On · empty list",
            ShieldStatus.websites(granted.copy(websitesEnabled = true))
        )
        assertEquals(
            "6 sites",
            ShieldStatus.websites(granted.copy(websitesEnabled = true, websitesCount = 6))
        )
        assertEquals(
            "On · empty list",
            ShieldStatus.channels(granted.copy(channelsEnabled = true))
        )
    }

    @Test
    fun `notifications only ask for access when enabled`() {
        val disabled = granted.copy(
            notificationsEnabled = false,
            notificationAccess = CapabilityState.DENIED
        )
        assertEquals("Off", ShieldStatus.notifications(disabled))

        val enabledNoAccess = granted.copy(
            notificationsEnabled = true,
            notificationAccess = CapabilityState.DENIED
        )
        assertEquals("On · access needed", ShieldStatus.notifications(enabledNoAccess))

        val enabled = granted.copy(
            notificationsEnabled = true,
            notificationAccess = CapabilityState.GRANTED
        )
        assertEquals("On · nothing held yet", ShieldStatus.notifications(enabled))
        assertEquals(
            "On · 2 held",
            ShieldStatus.notifications(enabled.copy(notificationsHeld = 2))
        )
    }
}
