package com.frostlock.app.ui.screens.focus

import com.frostlock.app.focus.FocusMode
import com.frostlock.app.focus.FocusPhase
import com.frostlock.app.focus.FocusUiState
import com.frostlock.app.ui.session.runningSessionDetail
import com.frostlock.app.ui.session.runningSessionTitle
import org.junit.Assert.assertEquals
import org.junit.Test

class RunningSessionBarTest {

    @Test
    fun `title shows Focusing when running`() {
        val state = FocusUiState(mode = FocusMode.TIMER, phase = FocusPhase.FOCUS, targetMinutes = 25, elapsedSeconds = 0, isRunning = true)
        assertEquals("Focusing · 25:00", runningSessionTitle(state))
    }

    @Test
    fun `title shows Break when in break`() {
        val state = FocusUiState(mode = FocusMode.POMODORO, phase = FocusPhase.BREAK, breakMinutes = 4, elapsedSeconds = 0, isRunning = true)
        assertEquals("Break · 04:00", runningSessionTitle(state))
    }

    @Test
    fun `title shows Paused when not running`() {
        val state = FocusUiState(mode = FocusMode.TIMER, phase = FocusPhase.FOCUS, targetMinutes = 25, isRunning = false)
        assertEquals("Paused · 25:00", runningSessionTitle(state))
    }

    @Test
    fun `detail shows subject and round`() {
        val state = FocusUiState(mode = FocusMode.TIMER, phase = FocusPhase.FOCUS, round = 3, tag = "Physics", targetMinutes = 25)
        assertEquals("Physics · round 3", runningSessionDetail(state))
    }

    @Test
    fun `detail shows stopwatch for stopwatch mode`() {
        val state = FocusUiState(mode = FocusMode.STOPWATCH, phase = FocusPhase.FOCUS, targetMinutes = 25)
        assertEquals("No subject · stopwatch", runningSessionDetail(state))
    }

    @Test
    fun `detail shows break before next round`() {
        val state = FocusUiState(mode = FocusMode.TIMER, phase = FocusPhase.BREAK, round = 2, tag = "Math", targetMinutes = 25)
        assertEquals("Math · break before round 3", runningSessionDetail(state))
    }
}