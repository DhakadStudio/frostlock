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

/** One app's share of the held notifications, used by the inbox ranking. */
data class NotificationAppTotal(
    val packageName: String,
    val appLabel: String,
    val count: Int
)

/**
 * Ranks apps by how many notifications FrostLock held for them. Ties are
 * alphabetical so the order stays stable between recompositions. [limit]
 * keeps the card compact; pass a negative value for the full list.
 */
fun notificationAppTotals(
    items: List<BlockedNotification>,
    limit: Int = 3
): List<NotificationAppTotal> {
    val ranked = items
        .groupBy { it.packageName }
        .map { (packageName, group) ->
            NotificationAppTotal(
                packageName = packageName,
                appLabel = group.first().appLabel,
                count = group.size
            )
        }
        .sortedWith(compareByDescending<NotificationAppTotal> { it.count }.thenBy { it.appLabel })
    return if (limit < 0) ranked else ranked.take(limit)
}
