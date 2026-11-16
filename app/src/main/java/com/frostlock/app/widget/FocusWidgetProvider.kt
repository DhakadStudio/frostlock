package com.frostlock.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

/**
 * Home-screen widget that mirrors today's focus time, progress towards the daily
 * goal, and the current streak. Tapping anywhere opens FrostLock.
 */
class FocusWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Paint cached numbers immediately, then refresh from the database.
        val cached = WidgetCache.read(context)
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildFocusViews(context, cached))
        }
        WidgetUpdater.refreshAsync(context)
    }
}

/** Compact daily-goal ring replacement: percentage, time left, streak. */
class DailyGoalWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val cached = WidgetCache.read(context)
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildGoalViews(context, cached))
        }
        WidgetUpdater.refreshAsync(context)
    }
}

/** Monday-first week strip with per-day bars, weekly total and best day. */
class WeeklyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val cached = WidgetCache.read(context)
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildWeekViews(context, cached))
        }
        WidgetUpdater.refreshAsync(context)
    }
}

/** Month heat-map with per-day intensity and the month's totals. */
class CalendarWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val cached = WidgetCache.read(context)
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildCalendarViews(context, cached))
        }
        WidgetUpdater.refreshAsync(context)
    }
}

/** Today's focus, goal progress, week/month totals and phone unlocks. */
class UsageWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val cached = WidgetCache.read(context)
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildUsageViews(context, cached))
        }
        WidgetUpdater.refreshAsync(context)
    }
}

/** Dedicated phone-unlock count for today, with focus time and streak. */
class UnlockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val cached = WidgetCache.read(context)
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildUnlockViews(context, cached))
        }
        WidgetUpdater.refreshAsync(context)
    }
}
