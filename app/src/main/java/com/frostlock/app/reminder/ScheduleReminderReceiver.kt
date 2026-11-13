package com.frostlock.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.frostlock.app.data.local.prefs.ScheduleStore
import com.frostlock.app.data.local.prefs.isActiveAt
import dagger.hilt.android.AndroidEntryPoint
import java.time.ZonedDateTime
import javax.inject.Inject

/** Receives a scheduled start/end alarm, posts the local reminder and re-arms. */
@AndroidEntryPoint
class ScheduleReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: ScheduleReminderScheduler
    @Inject lateinit var store: ScheduleStore

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ScheduleReminderScheduler.ACTION_REMINDER) return

        val scheduleId = intent.getLongExtra(
            ScheduleReminderScheduler.EXTRA_SCHEDULE_ID,
            -1L
        )
        val kind = intent.getStringExtra(ScheduleReminderScheduler.EXTRA_KIND)
            ?.let { raw ->
                ScheduleReminderKind.entries.firstOrNull { it.name == raw }
            }
        val schedule = store.schedules().firstOrNull { it.id == scheduleId }

        if (schedule != null && schedule.enabled && kind != null) {
            val now = ZonedDateTime.now()
            // A delayed start alert is only useful while the window is open. End
            // alerts stay useful even when Doze delivers them a little late.
            if (kind == ScheduleReminderKind.END || schedule.isActiveAt(now)) {
                scheduler.notifyReminder(schedule, kind)
            }
        }

        scheduler.rescheduleAll()
    }
}
