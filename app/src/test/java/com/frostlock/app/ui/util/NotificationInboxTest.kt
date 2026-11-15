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

    private fun appItem(id: Long, packageName: String, label: String) = BlockedNotification(
        id = id,
        packageName = packageName,
        appLabel = label,
        title = "Title $id",
        text = "Text",
        postedAt = 0L,
        reason = BlockReason.FOCUS
    )

    @Test
    fun `app ranking counts held notifications per package`() {
        val totals = notificationAppTotals(
            listOf(
                appItem(1, "com.chat", "Chat"),
                appItem(2, "com.mail", "Mail"),
                appItem(3, "com.chat", "Chat"),
                appItem(4, "com.news", "News"),
                appItem(5, "com.chat", "Chat"),
                appItem(6, "com.mail", "Mail")
            ),
            limit = -1
        )

        assertEquals(listOf("Chat", "Mail", "News"), totals.map { it.appLabel })
        assertEquals(listOf(3, 2, 1), totals.map { it.count })
        assertEquals("com.chat", totals.first().packageName)
    }

    @Test
    fun `app ranking breaks ties alphabetically and respects the limit`() {
        val totals = notificationAppTotals(
            listOf(
                appItem(1, "com.zeta", "Zeta"),
                appItem(2, "com.alpha", "Alpha"),
                appItem(3, "com.mid", "Mid"),
                appItem(4, "com.alpha", "Alpha")
            ),
            limit = 2
        )

        assertEquals(listOf("Alpha", "Mid"), totals.map { it.appLabel })
        assertEquals(2, totals.size)
        assertEquals(emptyList<NotificationAppTotal>(), notificationAppTotals(emptyList()))
    }
}
