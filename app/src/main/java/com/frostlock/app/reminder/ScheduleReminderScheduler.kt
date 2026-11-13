package com.frostlock.app.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.frostlock.app.R
import com.frostlock.app.data.local.prefs.FocusSchedule
import com.frostlock.app.data.local.prefs.FocusSettings
import com.frostlock.app.data.local.prefs.ScheduleStore
import com.frostlock.app.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZonedDateTime
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules local start/end notifications for blocking rules.
 *
 * We prefer exact alarms when the OS allows them and fall back to an inexact
 * allow-while-idle alarm otherwise, so the feature works without asking for the
 * special exact-alarm permission. Everything stays on the device.
 */
@Singleton
class ScheduleReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: ScheduleStore,
    private val settings: FocusSettings
) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /** Cancels and rebuilds every alarm. Safe to call after any schedule edit. */
    fun rescheduleAll() {
        val schedules = store.schedules()
        schedules.forEach { cancel(it.id) }
        if (!settings.scheduleRemindersEnabled.value) return

        ensureChannel()
        val now = ZonedDateTime.now()
        schedules.filter { it.enabled }.forEach { schedule ->
            nextScheduleTriggers(schedule, now).forEach { trigger ->
                setAlarm(trigger, schedule)
            }
        }
    }

    /** Drops both alarms for a rule that no longer exists. */
    fun cancel(scheduleId: Long) {
        ScheduleReminderKind.entries.forEach { kind ->
            pendingIntent(scheduleId, kind, PendingIntent.FLAG_NO_CREATE)?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
    }

    /** Posts the notification for a fired alarm and refreshes future alarms. */
    fun notifyReminder(schedule: FocusSchedule, kind: ScheduleReminderKind) {
        ensureChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val title = when (kind) {
            ScheduleReminderKind.START -> "${schedule.label} starts now"
            ScheduleReminderKind.END -> "${schedule.label} has ended"
        }
        val body = when (kind) {
            ScheduleReminderKind.START -> {
                "${formatWindow(schedule)} · apps are locked until the window ends."
            }
            ScheduleReminderKind.END -> {
                "${formatWindow(schedule)} · apps are unlocked again."
            }
        }

        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context)
            .notify(notificationId(schedule.id, kind), notification)
    }

    private fun setAlarm(trigger: ScheduleReminderTrigger, schedule: FocusSchedule) {
        val intent = pendingIntent(
            scheduleId = schedule.id,
            kind = trigger.kind,
            flags = PendingIntent.FLAG_UPDATE_CURRENT
        ) ?: return
        val at = trigger.at.toInstant().toEpochMilli()
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

        if (exact) {
            val sent = runCatching {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
            }.isSuccess
            if (sent) return
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
    }

    private fun pendingIntent(
        scheduleId: Long,
        kind: ScheduleReminderKind,
        flags: Int
    ): PendingIntent? {
        val intent = Intent(context, ScheduleReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            data = Uri.Builder()
                .scheme("frostlock")
                .authority("schedule-reminder")
                .appendPath(scheduleId.toString())
                .appendPath(kind.name.lowercase(Locale.US))
                .build()
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_KIND, kind.name)
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Schedule reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "A local alert when a blocking rule starts or ends."
        }
        manager.createNotificationChannel(channel)
    }

    private fun formatWindow(schedule: FocusSchedule): String =
        String.format(
            Locale.US,
            "%02d:%02d – %02d:%02d",
            (schedule.startMinutes / 60) % 24,
            schedule.startMinutes % 60,
            (schedule.endMinutes / 60) % 24,
            schedule.endMinutes % 60
        )

    private fun notificationId(scheduleId: Long, kind: ScheduleReminderKind): Int {
        val base = (scheduleId % Int.MAX_VALUE).toInt().coerceAtLeast(0)
        return base * 2 + kind.ordinal
    }

    companion object {
        const val ACTION_REMINDER = "com.frostlock.app.action.SCHEDULE_REMINDER"
        const val EXTRA_SCHEDULE_ID = "schedule_id"
        const val EXTRA_KIND = "reminder_kind"
        const val CHANNEL_ID = "schedule_reminders"
    }
}
