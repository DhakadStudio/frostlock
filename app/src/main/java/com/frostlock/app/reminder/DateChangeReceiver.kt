package com.frostlock.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.frostlock.app.capability.CapabilityRepository
import com.frostlock.app.data.local.prefs.AppLimitStore
import com.frostlock.app.data.local.prefs.UnlockStore
import com.frostlock.app.insights.TodayProvider
import com.frostlock.app.widget.WidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Keeps every local, date-sensitive surface honest when the clock moves.
 *
 * Midnight is the main case: daily limits, "held today", inbox day groups,
 * streaks, unlock counts and widgets all roll over without a background job.
 * Timezone and manual clock changes use the same path so day buckets cannot
 * drift from the device's local time.
 */
@AndroidEntryPoint
class DateChangeReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: ScheduleReminderScheduler
    @Inject lateinit var todayProvider: TodayProvider
    @Inject lateinit var limitStore: AppLimitStore
    @Inject lateinit var unlockStore: UnlockStore
    @Inject lateinit var capabilities: CapabilityRepository

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                todayProvider.refresh()
                limitStore.onDateChanged()
                unlockStore.refresh()
                capabilities.refresh()
                scheduler.rescheduleAll()
                WidgetUpdater.refreshAsync(context)
            }
        }
    }
}
