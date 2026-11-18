package com.frostlock.app.focus

import com.frostlock.app.capability.CapabilityState
import com.frostlock.app.data.local.prefs.FocusSubject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusUiStateTest {

    @Test
    fun `timer total and remaining track the target`() {
        val state = FocusUiState(mode = FocusMode.TIMER, targetMinutes = 25, elapsedSeconds = 600)
        assertEquals(1500, state.totalSeconds)
        assertEquals(900, state.remainingSeconds)
        assertEquals(0.4f, state.progress, 0.0001f)
    }

    @Test
    fun `stopwatch counts upward and has no progress bar`() {
        val state = FocusUiState(mode = FocusMode.STOPWATCH, elapsedSeconds = 75)
        assertEquals(0, state.totalSeconds)
        assertEquals(75, state.remainingSeconds)
        assertEquals(0f, state.progress, 0.0001f)
    }

    @Test
    fun `break phase counts down from the break length`() {
        val state = FocusUiState(
            mode = FocusMode.POMODORO,
            phase = FocusPhase.BREAK,
            breakMinutes = 5,
            elapsedSeconds = 60
        )
        assertEquals(300, state.totalSeconds)
        assertEquals(240, state.remainingSeconds)
    }

    @Test
    fun `strict is active only while actually focusing`() {
        val running = FocusUiState(strictMode = true, isRunning = true, phase = FocusPhase.FOCUS)
        assertTrue(running.strictActive)
        assertFalse(running.canPause)

        val paused = running.copy(isRunning = false)
        assertFalse(paused.strictActive)

        val onBreak = running.copy(phase = FocusPhase.BREAK)
        assertFalse(onBreak.strictActive)
    }

    @Test
    fun `holding apps requires granted accessibility`() {
        val denied = FocusUiState(
            blockingAccess = CapabilityState.DENIED,
            sessionPackages = setOf("a", "b")
        )
        assertFalse(denied.canBlockApps)
        assertEquals(0, denied.heldCount)

        val granted = denied.copy(blockingAccess = CapabilityState.GRANTED)
        assertTrue(granted.canBlockApps)
        assertEquals(2, granted.heldCount)
    }

    @Test
    fun `pause allowance reflects usage`() {
        val fresh = FocusUiState(pausesUsed = 0, maxPauses = 2)
        assertTrue(fresh.canPause)
        assertEquals(2, fresh.pauseAllowance.remaining)

        val spent = FocusUiState(pausesUsed = 2, maxPauses = 2)
        assertFalse(spent.canPause)
        assertEquals(0, spent.pauseAllowance.remaining)
    }

    @Test
    fun `selected subject is matched case-insensitively`() {
        val subject = FocusSubject(
            id = 1,
            name = "Physics",
            packages = setOf("com.a", "com.b")
        )
        val state = FocusUiState(tag = "physics", subjects = listOf(subject))
        assertEquals(subject, state.selectedSubject)
        assertEquals(2, state.subjectBlockedCount)
    }
}