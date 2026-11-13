package com.frostlock.app.ui.util

import com.frostlock.app.notifications.BlockReason
import com.frostlock.app.notifications.BlockedNotification
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class NotificationInboxTest {

    private fun item(id: Long, at: Long) = BlockedNotification(
        id = id,
        packageName = "com.example.app",
        appLabel = "Example",
        title = "Title $id",
        text = "Text",
        postedAt = at,
        reason = BlockReason.FOCUS
    )

    private fun at(date: LocalDate, hour: Int): Long = date
        .atTime(hour, 0)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    @Test
    fun `groups by day newest first and keeps newest entry on top`() {
        val today = LocalDate.of(2026, 11, 13)
        val yesterday = today.minusDays(1)
        val days = buildNotificationDays(
            listOf(
                item(1, at(yesterday, 9)),
                item(2, at(today, 8)),
                item(3, at(today, 14)),
                item(4, at(yesterday, 18))
            )
        )

        assertEquals(listOf(today, yesterday), days.map { it.date })
        assertEquals(listOf(3L, 2L), days.first().items.map { it.id })
        assertEquals(listOf(4L, 1L), days.last().items.map { it.id })
    }

    @Test
    fun `day labels use today, yesterday and a short date`() {
        val today = LocalDate.of(2026, 11, 13)
        assertEquals("Today", notificationDayLabel(today, today))
        assertEquals("Yesterday", notificationDayLabel(today.minusDays(1), today))
        assertEquals("9 Nov", notificationDayLabel(LocalDate.of(2026, 11, 9), today))
    }
}
