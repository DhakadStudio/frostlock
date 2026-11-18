package com.frostlock.app.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.frostlock.app.data.local.prefs.AppLimitStore
import com.frostlock.app.data.local.prefs.BlockingPreferences
import com.frostlock.app.data.local.prefs.FocusSettings
import com.frostlock.app.data.local.prefs.ScheduleStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Optional notification shield.
 *
 * While an app is blocked (focus session, blocking rule, or a reached daily limit)
 * its notifications are cancelled before they reach the shade and saved in the
 * local inbox instead. The service only inspects package/category/flags and the
 * notification's title and text; nothing leaves the device.
 *
 * Everything is read from the same shared stores as the app blocker, so the two
 * always agree about which apps are blocked.
 */
@AndroidEntryPoint
class FrostNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var settings: FocusSettings
    @Inject lateinit var blockingPreferences: BlockingPreferences
    @Inject lateinit var scheduleStore: ScheduleStore
    @Inject lateinit var limitStore: AppLimitStore
    @Inject lateinit var archive: NotificationArchiveStore

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val context = currentContext() ?: return
        val notification = sbn.notification ?: return

        val snapshot = NotificationSnapshot(
            packageName = sbn.packageName,
            category = notification.category,
            isOngoing = notification.flags and Notification.FLAG_ONGOING_EVENT != 0,
            isForegroundService =
                notification.flags and Notification.FLAG_FOREGROUND_SERVICE != 0,
            isGroupSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
            isClearable = notification.flags and Notification.FLAG_NO_CLEAR == 0
        )
        val decision = decideNotificationBlock(snapshot, context) ?: return

        val extras = notification.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)
            ?.toString()?.trim().orEmpty()
        val text = (
            extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras?.getCharSequence(Notification.EXTRA_TEXT)
            )?.toString()?.trim().orEmpty()

        val label = runCatching {
            val info = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(sbn.packageName)

        archive.add(
            packageName = sbn.packageName,
            appLabel = label,
            title = title.ifBlank { label },
            text = text,
            postedAt = sbn.postTime.takeIf { it > 0 } ?: System.currentTimeMillis(),
            reason = decision.reason,
            ruleLabel = decision.ruleLabel,
            key = sbn.key
        )

        // Cancel by key so the held notification never appears in the shade.
        runCatching { cancelNotification(sbn.key) }
    }

    private fun currentContext(): BlockContext? {
        val enabled = settings.notificationBlockingEnabled.value
        if (!enabled) return null

        val now = System.currentTimeMillis()
        val globalPackages = blockingPreferences.blockedPackages.value
        val focusActive = blockingPreferences.isFocusActive(now) &&
            blockingPreferences.blockingEnabled.value
        val focusPackages = blockingPreferences.sessionPackages().ifEmpty { globalPackages }

        val schedules = scheduleStore.activeSchedules().map { active ->
            ScheduleScope(
                label = active.schedule.label,
                packages = active.schedule.packages.ifEmpty { globalPackages }
            )
        }
        val limited = limitStore.limits().keys.filter { limitStore.isLimitReached(it) }.toSet()

        return BlockContext(
            enabled = true,
            ownPackage = packageName,
            focusActive = focusActive,
            focusPackages = focusPackages,
            schedules = schedules,
            limitedPackages = limited
        )
    }
}
