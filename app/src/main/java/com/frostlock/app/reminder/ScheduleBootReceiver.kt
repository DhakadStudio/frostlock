package com.frostlock.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Alarms do not survive a reboot, package update or timezone change, so rebuild
 * them whenever one of those happens.
 */
@AndroidEntryPoint
class ScheduleBootReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: ScheduleReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        scheduler.rescheduleAll()
    }
}
