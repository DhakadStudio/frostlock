package com.frostlock.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.RemoteViews
import com.frostlock.app.R
import com.frostlock.app.data.local.db.SessionDao
import com.frostlock.app.data.local.prefs.FocusSettings
import com.frostlock.app.ui.MainActivity
import com.frostlock.app.ui.util.computeStreak
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/** Numbers the widget renders. Cached so the first frame after a reboot isn't blank. */
data class WidgetData(
    val todaySeconds: Long = 0L,
    val goalMinutes: Int = FocusSettings.DEFAULT_GOAL_MINUTES,
    val streak: Int = 0
)

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
            appWidgetManager.updateAppWidget(id, buildViews(context, cached))
        }
        WidgetUpdater.refreshAsync(context)
    }
}

/** Reads the database off the main thread and re-renders every widget instance. */
object WidgetUpdater {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun refreshAsync(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            runCatching { load(appContext) }
                .onSuccess { data ->
                    WidgetCache.write(appContext, data)
                    render(appContext, data)
                }
        }
    }

    private suspend fun load(context: Context): WidgetData {
        val entryPoint =
            EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val dao = entryPoint.sessionDao()

        val todayStart = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val todaySeconds = dao.totalSecondsSince(todayStart).first()
        val recent = dao.recentSessions(200).first()

        return WidgetData(
            todaySeconds = todaySeconds,
            goalMinutes = entryPoint.focusSettings().dailyGoalMinutes.value,
            streak = computeStreak(recent.map { it.startedAt })
        )
    }

    private fun render(context: Context, data: WidgetData) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, FocusWidgetProvider::class.java)
        )
        if (ids.isEmpty()) return
        ids.forEach { id ->
            manager.updateAppWidget(id, buildViews(context, data))
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun sessionDao(): SessionDao
    fun focusSettings(): FocusSettings
}

/** Last rendered numbers, so the widget has something to show before the DB read finishes. */
private object WidgetCache {

    private const val PREFS_NAME = "frostlock_widget"
    private const val KEY_TODAY_SECONDS = "today_seconds"
    private const val KEY_GOAL_MINUTES = "goal_minutes"
    private const val KEY_STREAK = "streak"

    fun read(context: Context): WidgetData {
        val prefs = prefs(context)
        return WidgetData(
            todaySeconds = prefs.getLong(KEY_TODAY_SECONDS, 0L),
            goalMinutes = prefs.getInt(KEY_GOAL_MINUTES, FocusSettings.DEFAULT_GOAL_MINUTES),
            streak = prefs.getInt(KEY_STREAK, 0)
        )
    }

    fun write(context: Context, data: WidgetData) {
        prefs(context).edit()
            .putLong(KEY_TODAY_SECONDS, data.todaySeconds)
            .putInt(KEY_GOAL_MINUTES, data.goalMinutes)
            .putInt(KEY_STREAK, data.streak)
            .apply()
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

private fun buildViews(context: Context, data: WidgetData): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_focus)

    views.setTextViewText(R.id.widget_time, formatWidgetDuration(data.todaySeconds))
    views.setTextViewText(
        R.id.widget_goal,
        "of ${formatWidgetDuration(data.goalMinutes * 60L)} daily goal"
    )

    val percent = if (data.goalMinutes > 0) {
        ((data.todaySeconds / 60.0) / data.goalMinutes * 100).toInt().coerceIn(0, 100)
    } else {
        0
    }
    views.setProgressBar(R.id.widget_progress, 100, percent, false)

    views.setTextViewText(
        R.id.widget_streak,
        if (data.streak > 0) "\uD83D\uDD25 ${data.streak}-day streak" else "Start your first session"
    )

    val openApp = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    views.setOnClickPendingIntent(R.id.widget_root, openApp)

    return views
}

private fun formatWidgetDuration(totalSeconds: Long): String {
    val minutes = (totalSeconds / 60L).toInt()
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours > 0 && rest > 0 -> "${hours}h ${rest}m"
        hours > 0 -> "${hours}h"
        else -> "${rest}m"
    }
}
