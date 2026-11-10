package com.focusflow.app.blocking

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.focusflow.app.data.local.prefs.AppLimitStore
import com.focusflow.app.data.local.prefs.BlockingPreferences
import com.focusflow.app.data.local.prefs.ScheduleStore

/**
 * Watches foreground app changes and sends the user to [BlockActivity] when they
 * open an app that is blocked during a focus session, a scheduled focus window,
 * or over its daily limit.
 *
 * The service itself holds no long-lived state: everything it needs is read from
 * [BlockingPreferences] / [AppLimitStore] / [ScheduleStore] so the UI and the
 * blocker always agree.
 */
class FocusBlockAccessibilityService : AccessibilityService() {

    private var prefs: SharedPreferences? = null
    private var limitStore: AppLimitStore? = null
    private var scheduleStore: ScheduleStore? = null
    private var lastBlockAt = 0L
    private var foregroundPackage: String? = null

    private val handler = Handler(Looper.getMainLooper())
    private val usageTicker = object : Runnable {
        override fun run() {
            tickUsage()
            tickSchedule()
            handler.postDelayed(this, USAGE_TICK_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = getSharedPreferences(BlockingPreferences.PREFS_NAME, Context.MODE_PRIVATE)
        limitStore = AppLimitStore(this)
        scheduleStore = ScheduleStore(this)
        handler.postDelayed(usageTicker, USAGE_TICK_MS)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val openedPackage = event.packageName?.toString() ?: return
        val store = limitStore ?: return
        val now = System.currentTimeMillis()

        // Credit the app we are leaving before anything else. This runs for our own
        // package too, so opening the wall stops the clock on the blocked app.
        store.commitActive(now)
        foregroundPackage = openedPackage

        if (openedPackage == packageName) return

        // Daily limits apply whether or not a focus session is running.
        if (store.limitMinutes(openedPackage) > 0) {
            if (store.isLimitReached(openedPackage)) {
                showWall(openedPackage, BlockActivity.REASON_LIMIT, now)
                return
            }
            store.setActive(openedPackage, now)
        }

        val prefs = prefs ?: return
        if (!prefs.getBoolean(BlockingPreferences.KEY_ENABLED, false)) return

        val blocked = prefs.getStringSet(BlockingPreferences.KEY_PACKAGES, emptySet()) ?: return
        if (openedPackage !in blocked) return

        val schedule = scheduleStore?.activeSchedule()
        if (schedule != null) {
            showWall(openedPackage, BlockActivity.REASON_SCHEDULE, now, schedule.endsAtMillis)
            return
        }

        if (prefs.getLong(BlockingPreferences.KEY_FOCUS_UNTIL, 0L) <= now) return

        showWall(openedPackage, BlockActivity.REASON_FOCUS, now)
    }

    /**
     * Credits time every few seconds while a limited app stays in the foreground.
     * Without this, usage would only be counted when the user switches apps.
     */
    private fun tickUsage() {
        val store = limitStore ?: return
        val active = store.activePackage() ?: return
        if (foregroundPackage != active) return

        val now = System.currentTimeMillis()
        store.commitActive(now)
        if (store.isLimitReached(active)) {
            showWall(active, BlockActivity.REASON_LIMIT, now)
        } else {
            store.setActive(active, now)
        }
    }

    /**
     * Blocks the current app when a scheduled focus window starts while the user
     * is already inside it. Opening an app is handled in [onAccessibilityEvent].
     */
    private fun tickSchedule() {
        val pkg = foregroundPackage ?: return
        if (pkg == packageName) return

        val prefs = prefs ?: return
        if (!prefs.getBoolean(BlockingPreferences.KEY_ENABLED, false)) return
        val blocked = prefs.getStringSet(BlockingPreferences.KEY_PACKAGES, emptySet()) ?: return
        if (pkg !in blocked) return

        val schedule = scheduleStore?.activeSchedule() ?: return
        showWall(pkg, BlockActivity.REASON_SCHEDULE, System.currentTimeMillis(), schedule.endsAtMillis)
    }

    private fun showWall(
        packageName: String,
        reason: String,
        now: Long,
        untilMillis: Long = 0L
    ) {
        // Window events fire several times per app switch; debounce them.
        val elapsed = SystemClock.elapsedRealtime()
        if (elapsed - lastBlockAt < BLOCK_DEBOUNCE_MS) return
        lastBlockAt = elapsed

        // Do not keep crediting a limited app while its wall is showing.
        limitStore?.commitActive(now)

        val intent = Intent(this, BlockActivity::class.java)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            .putExtra(BlockActivity.EXTRA_BLOCKED_PACKAGE, packageName)
            .putExtra(BlockActivity.EXTRA_REASON, reason)
            .putExtra(BlockActivity.EXTRA_UNTIL, untilMillis)

        runCatching { startActivity(intent) }
            .onFailure { performGlobalAction(GLOBAL_ACTION_HOME) }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacks(usageTicker)
        limitStore?.commitActive()
        super.onDestroy()
    }

    companion object {
        private const val USAGE_TICK_MS = 10_000L
        private const val BLOCK_DEBOUNCE_MS = 700L

        /** True when the user has enabled this service in system accessibility settings. */
        fun isEnabled(context: Context): Boolean {
            val expected = ComponentName(context, FocusBlockAccessibilityService::class.java)
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabled
                .split(':')
                .mapNotNull { ComponentName.unflattenFromString(it) }
                .any { it == expected }
        }
    }
}
