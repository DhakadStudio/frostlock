package com.frostlock.app.ui.screens.profile

import com.frostlock.app.capability.CapabilityState
import com.frostlock.app.data.local.prefs.FocusDeadline
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class YouStatusTest {

    private val today = LocalDate.of(2026, 11, 18)

    @Test
    fun `daily target formats minutes and hours`() {
        assertEquals("45m", YouStatus.dailyTarget(45))
        assertEquals("1h", YouStatus.dailyTarget(60))
        assertEquals("1h 30m", YouStatus.dailyTarget(90))
        assertEquals("4h", YouStatus.dailyTarget(240))
    }

    @Test
    fun `goals describe the active count`() {
        assertEquals("Set a focus goal", YouStatus.goals(0))
        assertEquals("1 active goal", YouStatus.goals(1))
        assertEquals("3 active goals", YouStatus.goals(3))
    }

    @Test
    fun `no countdown invites one instead of showing zero`() {
        assertEquals("Set a target date", YouStatus.deadline(null, today))
    }

    @Test
    fun `countdown shows the date and days left`() {
        val deadline = FocusDeadline(title = "Exams", date = "2026-12-01")
        assertEquals(
            "1 Dec 2026 · 13 days left",
            YouStatus.deadline(deadline, today)
        )
    }

    @Test
    fun `countdown says today when the date is today`() {
        val deadline = FocusDeadline(title = "Launch", date = "2026-11-18")
        assertEquals("18 Nov 2026 · Today", YouStatus.deadline(deadline, today))
    }

    @Test
    fun `countdown says passed after the date`() {
        val deadline = FocusDeadline(title = "Launch", date = "2026-11-17")
        assertEquals("17 Nov 2026 · Passed", YouStatus.deadline(deadline, today))
    }

    @Test
    fun `malformed date still shows the stored value`() {
        val deadline = FocusDeadline(title = "Bad", date = "not-a-date")
        assertEquals("not-a-date", YouStatus.deadline(deadline, today))
    }

    @Test
    fun `session alerts never claim on without permission`() {
        assertEquals("On", YouStatus.sessionAlerts(CapabilityState.GRANTED))
        assertEquals("Needs permission", YouStatus.sessionAlerts(CapabilityState.DENIED))
        assertEquals("Not available", YouStatus.sessionAlerts(CapabilityState.UNAVAILABLE))
        assertEquals("Off", YouStatus.sessionAlerts(CapabilityState.NOT_CONFIGURED))
    }
}
