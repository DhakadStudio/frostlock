package com.frostlock.app.ui.screens.insights

import com.frostlock.app.insights.DayFocus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InsightsUiStateTest {

    private val today = LocalDate.of(2026, 11, 18)

    @Test
    fun `goal progress is capped at one`() {
        val state = InsightsUiState(
            goalMinutes = 60,
            today = DayFocus(today, seconds = 5 * 3600L)
        )
        assertEquals(1f, state.goalProgress, 0.0001f)
        assertTrue(state.goalReached)
    }

    @Test
    fun `goal progress is proportional`() {
        val state = InsightsUiState(
            goalMinutes = 60,
            today = DayFocus(today, seconds = 15 * 60L)
        )
        assertEquals(0.25f, state.goalProgress, 0.0001f)
        assertFalse(state.goalReached)
    }

    @Test
    fun `zero target never divides by zero`() {
        val state = InsightsUiState(
            goalMinutes = 0,
            today = DayFocus(today, seconds = 600L)
        )
        assertEquals(0L, state.goalSeconds)
        assertEquals(0f, state.goalProgress, 0.0001f)
        assertFalse(state.goalReached)
    }

    @Test
    fun `loading state starts without history`() {
        val state = InsightsUiState()
        assertTrue(state.loading)
        assertFalse(state.hasHistory)
        assertFalse(state.error)
    }
}
