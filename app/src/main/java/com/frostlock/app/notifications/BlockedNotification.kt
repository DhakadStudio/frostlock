package com.frostlock.app.notifications

/** A notification that was held back because its app was blocked. */
data class BlockedNotification(
    val id: Long,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val postedAt: Long,
    val reason: BlockReason,
    /** Rule name when [reason] is [BlockReason.SCHEDULE]. */
    val ruleLabel: String? = null
)

/** Why a notification was held. */
enum class BlockReason(val label: String) {
    FOCUS("Focus session"),
    SCHEDULE("Blocking rule"),
    LIMIT("Daily limit")
}
