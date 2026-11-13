package com.frostlock.app.ui.util

import com.frostlock.app.notifications.BlockedNotification
import java.time.LocalDate

/** One calendar day of held notifications, newest day first. */
data class NotificationDay(
    val date: LocalDate,
    val items: List<BlockedNotification>
)

/** Groups held notifications by local day; entries inside a day stay newest-first. */
fun buildNotificationDays(items: List<BlockedNotification>): List<NotificationDay> =
    items
        .sortedByDescending { it.postedAt }
        .groupBy { localDateOf(it.postedAt) }
        .map { (date, dayItems) -> NotificationDay(date, dayItems) }
        .sortedByDescending { it.date }

/** "Today", "Yesterday", or "12 Nov". */
fun notificationDayLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM"))
}
