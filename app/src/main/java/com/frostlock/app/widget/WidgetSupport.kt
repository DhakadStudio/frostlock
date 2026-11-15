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
import com.frostlock.app.data.local.prefs.UnlockStore
import com.frostlock.app.ui.MainActivity
import com.frostlock.app.ui.util.buildFocusMonth
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
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Refreshes every FrostLock widget from the database.
 *
 * A single refresh reads sessions once and renders the focus, daily-goal and
 * weekly widgets, so adding a widget never means a second database pass.
 */
object WidgetUpdater {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun refreshAsync(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            runCatching { load(appContext) }
                .onSuccess { data ->
                    WidgetCache.write(appContext, data)
                    renderAll(appContext, data)
                }
        }
    }

    private suspend fun load(context: Context): WidgetData {
        val entryPoint =
            EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val dao = entryPoint.sessionDao()
        val zone = ZoneId.systemDefault()

        val today = LocalDate.now(zone)
        val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val weekStart = today.with(DayOfWeek.MONDAY)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

        val todaySeconds = dao.totalSecondsSince(todayStart).first()
        val recent = dao.recentSessions(500).first()
        val streak = computeStreak(recent.map { it.startedAt })

        val weekSeconds = LongArray(WidgetData.DAYS_IN_WEEK)
        recent.asSequence()
            .filter { it.startedAt >= weekStart }
            .forEach { session ->
                val day = Instant.ofEpochMilli(session.startedAt).atZone(zone).dayOfWeek.value - 1
                if (day in weekSeconds.indices) {
                    weekSeconds[day] += session.durationSeconds.toLong()
                }
            }

        val month = YearMonth.now(zone)
        val focusMonth = buildFocusMonth(month, recent)

        return WidgetData(
            todaySeconds = todaySeconds,
            goalMinutes = entryPoint.focusSettings().dailyGoalMinutes.value,
            streak = streak,
            weekSeconds = weekSeconds.toList(),
            monthSeconds = focusMonth.days.map { it.seconds },
            monthLabel = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)),
            monthTotalSeconds = focusMonth.totalSeconds,
            monthActiveDays = focusMonth.activeDays,
            unlocksToday = entryPoint.unlockStore().todayCount.value
        )
    }

    private fun renderAll(context: Context, data: WidgetData) {
        render(context, FocusWidgetProvider::class.java) { buildFocusViews(context, data) }
        render(context, DailyGoalWidgetProvider::class.java) { buildGoalViews(context, data) }
        render(context, WeeklyWidgetProvider::class.java) { buildWeekViews(context, data) }
        render(context, CalendarWidgetProvider::class.java) { buildCalendarViews(context, data) }
    }

    private fun render(
        context: Context,
        provider: Class<out AppWidgetProvider>,
        build: () -> RemoteViews
    ) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, provider))
        if (ids.isEmpty()) return
        // Reuse one RemoteViews for all instances of the same provider.
        val views = build()
        ids.forEach { id -> manager.updateAppWidget(id, views) }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun sessionDao(): SessionDao
    fun focusSettings(): FocusSettings
    fun unlockStore(): UnlockStore
}

/** Last rendered numbers, so widgets have something to show before the DB read finishes. */
internal object WidgetCache {

    private const val PREFS_NAME = "frostlock_widget"
    private const val KEY_TODAY_SECONDS = "today_seconds"
    private const val KEY_GOAL_MINUTES = "goal_minutes"
    private const val KEY_STREAK = "streak"
    private const val KEY_WEEK_SECONDS = "week_seconds"
    private const val KEY_MONTH_SECONDS = "month_seconds"
    private const val KEY_MONTH_LABEL = "month_label"
    private const val KEY_MONTH_TOTAL = "month_total"
    private const val KEY_MONTH_ACTIVE = "month_active"
    private const val KEY_UNLOCKS = "unlocks_today"

    fun read(context: Context): WidgetData {
        val prefs = prefs(context)
        return WidgetData(
            todaySeconds = prefs.getLong(KEY_TODAY_SECONDS, 0L),
            goalMinutes = prefs.getInt(KEY_GOAL_MINUTES, FocusSettings.DEFAULT_GOAL_MINUTES),
            streak = prefs.getInt(KEY_STREAK, 0),
            weekSeconds = parseWeek(prefs.getString(KEY_WEEK_SECONDS, null)),
            monthSeconds = parseMonth(prefs.getString(KEY_MONTH_SECONDS, null)),
            monthLabel = prefs.getString(KEY_MONTH_LABEL, "").orEmpty(),
            monthTotalSeconds = prefs.getLong(KEY_MONTH_TOTAL, 0L),
            monthActiveDays = prefs.getInt(KEY_MONTH_ACTIVE, 0),
            unlocksToday = prefs.getInt(KEY_UNLOCKS, 0)
        )
    }

    fun write(context: Context, data: WidgetData) {
        prefs(context).edit()
            .putLong(KEY_TODAY_SECONDS, data.todaySeconds)
            .putInt(KEY_GOAL_MINUTES, data.goalMinutes)
            .putInt(KEY_STREAK, data.streak)
            .putString(KEY_WEEK_SECONDS, data.weekSeconds.joinToString(","))
            .putString(KEY_MONTH_SECONDS, data.monthSeconds.joinToString(","))
            .putString(KEY_MONTH_LABEL, data.monthLabel)
            .putLong(KEY_MONTH_TOTAL, data.monthTotalSeconds)
            .putInt(KEY_MONTH_ACTIVE, data.monthActiveDays)
            .putInt(KEY_UNLOCKS, data.unlocksToday)
            .apply()
    }

    private fun parseWeek(raw: String?): List<Long> {
        if (raw.isNullOrBlank()) return List(WidgetData.DAYS_IN_WEEK) { 0L }
        val parsed = raw.split(",").mapNotNull { it.toLongOrNull() }
        return List(WidgetData.DAYS_IN_WEEK) { index -> parsed.getOrElse(index) { 0L } }
    }

    private fun parseMonth(raw: String?): List<Long> {
        if (raw.isNullOrBlank()) return List(WidgetData.DAYS_IN_MONTH_GRID) { 0L }
        val parsed = raw.split(",").mapNotNull { it.toLongOrNull() }
        return List(WidgetData.DAYS_IN_MONTH_GRID) { index -> parsed.getOrElse(index) { 0L } }
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

internal fun buildFocusViews(context: Context, data: WidgetData): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_focus)

    views.setTextViewText(R.id.widget_time, formatWidgetDuration(data.todaySeconds))
    views.setTextViewText(
        R.id.widget_goal,
        "of ${formatWidgetDuration(data.goalMinutes * 60L)} daily goal"
    )
    views.setProgressBar(
        R.id.widget_progress,
        100,
        goalPercent(data.todaySeconds, data.goalMinutes),
        false
    )
    views.setTextViewText(
        R.id.widget_streak,
        if (data.streak > 0) "\uD83D\uDD25 ${data.streak}-day streak" else "Start your first session"
    )
    views.setTextViewText(
        R.id.widget_unlocks,
        if (data.unlocksToday > 0) {
            "\uD83D\uDD13 ${data.unlocksToday} unlock${if (data.unlocksToday == 1) "" else "s"} today"
        } else {
            "No unlocks yet today"
        }
    )
    bindOpenApp(context, views, R.id.widget_root)
    return views
}

internal fun buildGoalViews(context: Context, data: WidgetData): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_goal)

    views.setTextViewText(
        R.id.goal_percent,
        "${goalPercent(data.todaySeconds, data.goalMinutes)}%"
    )
    views.setTextViewText(
        R.id.goal_sub,
        goalRemainingLabel(data.todaySeconds, data.goalMinutes)
    )
    views.setProgressBar(
        R.id.goal_progress,
        100,
        goalPercent(data.todaySeconds, data.goalMinutes),
        false
    )
    views.setTextViewText(
        R.id.goal_streak,
        if (data.streak > 0) "\uD83D\uDD25 ${data.streak}-day streak" else "Start your first session"
    )
    bindOpenApp(context, views, R.id.goal_root)
    return views
}

internal fun buildWeekViews(context: Context, data: WidgetData): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_week)
    val summary = summarizeWeek(data.weekSeconds, LocalDate.now().dayOfWeek)

    views.setTextViewText(R.id.week_total, formatWidgetDuration(summary.totalSeconds))
    views.setTextViewText(
        R.id.week_meta,
        buildString {
            append("avg ").append(formatWidgetDuration(summary.averageSeconds)).append("/day")
            summary.bestDay?.let { append(" · best ").append(it) }
        }
    )

    val maxSeconds = summary.bars.maxOf { it.seconds }.coerceAtLeast(1L)
    summary.bars.forEachIndexed { index, bar ->
        val dayId = WEEK_DAY_IDS[index]
        val barId = WEEK_BAR_IDS[index]
        val valueId = WEEK_VALUE_IDS[index]
        views.setTextViewText(dayId, bar.label)
        views.setTextViewText(valueId, formatWidgetDuration(bar.seconds))
        views.setProgressBar(barId, maxSeconds.toInt(), bar.seconds.toInt(), false)
        if (bar.isToday) {
            views.setTextColor(dayId, TODAY_COLOR)
            views.setTextColor(valueId, TODAY_COLOR)
        }
    }
    bindOpenApp(context, views, R.id.week_root)
    return views
}

internal fun buildCalendarViews(context: Context, data: WidgetData): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_calendar)

    views.setTextViewText(
        R.id.calendar_month,
        data.monthLabel.ifBlank {
            YearMonth.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
        }
    )
    views.setTextViewText(
        R.id.calendar_meta,
        monthMetaLabel(data.monthTotalSeconds, data.monthActiveDays)
    )
    views.setImageViewBitmap(R.id.calendar_grid, buildCalendarBitmap(data))
    bindOpenApp(context, views, R.id.calendar_root)
    return views
}

private fun bindOpenApp(context: Context, views: RemoteViews, rootId: Int) {
    val openApp = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    views.setOnClickPendingIntent(rootId, openApp)
}

private val WEEK_DAY_IDS = intArrayOf(
    R.id.week_day_0, R.id.week_day_1, R.id.week_day_2, R.id.week_day_3,
    R.id.week_day_4, R.id.week_day_5, R.id.week_day_6
)

private val WEEK_BAR_IDS = intArrayOf(
    R.id.week_bar_0, R.id.week_bar_1, R.id.week_bar_2, R.id.week_bar_3,
    R.id.week_bar_4, R.id.week_bar_5, R.id.week_bar_6
)

private val WEEK_VALUE_IDS = intArrayOf(
    R.id.week_value_0, R.id.week_value_1, R.id.week_value_2, R.id.week_value_3,
    R.id.week_value_4, R.id.week_value_5, R.id.week_value_6
)

private const val TODAY_COLOR = 0xFF7DD3FC.toInt()
