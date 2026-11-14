package com.frostlock.app.ui.util

import com.frostlock.app.data.local.prefs.FocusDeadline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DeadlineCountdownTest {

    private val today = LocalDate.of(2026, 11, 13)

    private fun deadline(date: String) = FocusDeadline(title = "Finals", date = date)

    @Test
    fun `days until handles future today and past`() {
        assertEquals(7L, daysUntilDeadline(deadline("2026-11-20"), today))
        assertEquals(0L, daysUntilDeadline(deadline("2026-11-13"), today))
        assertEquals(-3L, daysUntilDeadline(deadline("2026-11-10"), today))
    }

    @Test
    fun `countdown reads naturally`() {
        assertEquals("23 days left", deadlineCountdown(deadline("2026-12-06"), today))
        assertEquals("1 day left", deadlineCountdown(deadline("2026-11-14"), today))
        assertEquals("Today", deadlineCountdown(deadline("2026-11-13"), today))
        assertEquals("Passed", deadlineCountdown(deadline("2026-11-12"), today))
    }

    @Test
    fun `upcoming includes today but not past dates`() {
        assertTrue(deadlineIsUpcoming(deadline("2026-11-13"), today))
        assertTrue(deadlineIsUpcoming(deadline("2026-11-20"), today))
        assertFalse(deadlineIsUpcoming(deadline("2026-11-12"), today))
    }

    @Test
    fun `urgency turns on in the last three days`() {
        assertTrue(deadlineIsUrgent(deadline("2026-11-13"), today))
        assertTrue(deadlineIsUrgent(deadline("2026-11-16"), today))
        assertFalse(deadlineIsUrgent(deadline("2026-11-17"), today))
        assertFalse(deadlineIsUrgent(deadline("2026-11-12"), today))
    }

    @Test
    fun `malformed dates degrade safely`() {
        val broken = deadline("not-a-date")

        assertNull(deadlineDate(broken))
        assertNull(daysUntilDeadline(broken, today))
        assertNull(deadlineCountdown(broken, today))
        assertFalse(deadlineIsUpcoming(broken, today))
        assertFalse(deadlineIsUrgent(broken, today))
    }
}
