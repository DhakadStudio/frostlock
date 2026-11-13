package com.frostlock.app.notifications

import android.app.Notification

/**
 * The parts of a posted notification that decide whether it is held back.
 * Kept free of Android types so the rule can be unit tested.
 */
data class NotificationSnapshot(
    val packageName: String,
    val category: String? = null,
    val isOngoing: Boolean = false,
    val isForegroundService: Boolean = false,
    val isGroupSummary: Boolean = false,
    val isClearable: Boolean = true
)

/** One active blocking rule and the apps it covers. */
data class ScheduleScope(
    val label: String,
    val packages: Set<String>
)

/** Everything the decision needs, snapshotted at the moment a notification arrives. */
data class BlockContext(
    val enabled: Boolean = false,
    val ownPackage: String = "",
    val focusActive: Boolean = false,
    val focusPackages: Set<String> = emptySet(),
    val schedules: List<ScheduleScope> = emptyList(),
    val limitedPackages: Set<String> = emptySet()
)

data class BlockDecision(
    val reason: BlockReason,
    val ruleLabel: String? = null
)

/**
 * Decides whether a notification should be held.
 *
 * Deliberately conservative:
 * - only apps the user is currently blocking are affected;
 * - calls, alarms, media, ongoing and foreground-service notifications always pass;
 * - group summaries pass so the inbox keeps one row per real message.
 */
fun decideNotificationBlock(
    notification: NotificationSnapshot,
    context: BlockContext
): BlockDecision? {
    if (!context.enabled) return null
    if (notification.packageName == context.ownPackage) return null
    if (notification.isOngoing || notification.isForegroundService) return null
    if (notification.isGroupSummary || !notification.isClearable) return null
    if (notification.category == Notification.CATEGORY_CALL ||
        notification.category == Notification.CATEGORY_ALARM ||
        notification.category == Notification.CATEGORY_SERVICE
    ) {
        return null
    }

    // Same precedence as the app blocker: limits, then rules, then the manual
    // focus session, so the reason shown in the inbox matches the block wall.
    if (notification.packageName in context.limitedPackages) {
        return BlockDecision(BlockReason.LIMIT)
    }

    val rule = context.schedules.firstOrNull { notification.packageName in it.packages }
    if (rule != null) {
        return BlockDecision(BlockReason.SCHEDULE, rule.label)
    }

    if (context.focusActive && notification.packageName in context.focusPackages) {
        return BlockDecision(BlockReason.FOCUS)
    }

    return null
}
