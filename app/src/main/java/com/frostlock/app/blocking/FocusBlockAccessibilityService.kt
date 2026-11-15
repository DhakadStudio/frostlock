package com.frostlock.app.blocking

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import com.frostlock.app.R
import com.frostlock.app.data.local.prefs.AppLimitStore
import com.frostlock.app.data.local.prefs.BlockingPreferences
import com.frostlock.app.data.local.prefs.ScheduleStore
import com.frostlock.app.data.local.prefs.WebsiteBlockingStore
import com.frostlock.app.data.local.prefs.blocks
import com.frostlock.app.ui.util.blockedDomainFor

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
    private var websiteStore: WebsiteBlockingStore? = null
    private var homePackages: Set<String> = emptySet()
    private var lastBlockAt = 0L
    private var foregroundPackage: String? = null
    private var lastUrlScanAt = 0L
    private var lastWebsiteDomain: String? = null
    private var lastWebsiteBlockAt = 0L

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
        websiteStore = WebsiteBlockingStore(this)
        homePackages = findHomePackages()
        createLimitChannel()
        handler.postDelayed(usageTicker, USAGE_TICK_MS)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val eventPackage = event.packageName?.toString() ?: return

        // Browsers announce address-bar updates as content changes rather than
        // window changes, so check both. The scan itself is throttled.
        if (eventPackage in BROWSER_PACKAGES &&
            event.eventType in WEBSITE_EVENT_TYPES
        ) {
            maybeBlockWebsite(eventPackage)
        }

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val openedPackage = eventPackage
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
            maybeWarnLimit(store, openedPackage)
        }

        val prefs = prefs ?: return
        val globalBlocked =
            prefs.getStringSet(BlockingPreferences.KEY_PACKAGES, emptySet()) ?: emptySet()

        // Rules carry their own app list, so each rule can lock a different set.
        // Older rules without apps fall back to the global focus-session list.
        // Several rules can run at once, so check every active rule. Schedules are
        // deliberately independent of the Focus blocking switch: disabling manual
        // session blocking must not silently turn off the user's schedule.
        val rule = scheduleStore?.activeSchedules().orEmpty()
            .firstOrNull { it.schedule.blocks(openedPackage, globalBlocked) }
        if (rule != null) {
            showWall(
                openedPackage,
                BlockActivity.REASON_SCHEDULE,
                now,
                rule.endsAtMillis,
                rule.schedule.label
            )
            return
        }

        // Home screen blocking during an active focus session or schedule
        val homeBlockingEnabled = prefs.getBoolean(BlockingPreferences.KEY_HOME_BLOCKING_ENABLED, false)
        val focusActive = prefs.getBoolean(BlockingPreferences.KEY_ENABLED, false) &&
            prefs.getLong(BlockingPreferences.KEY_FOCUS_UNTIL, 0L) > now
        val scheduleActive = scheduleStore?.activeSchedules()?.isNotEmpty() == true
        if (homeBlockingEnabled && (focusActive || scheduleActive) && openedPackage in homePackages) {
            showWall(
                openedPackage,
                BlockActivity.REASON_FOCUS,
                now,
                ruleLabel = "Home screen blocked"
            )
            return
        }

        if (!prefs.getBoolean(BlockingPreferences.KEY_ENABLED, false)) return
        // A subject owns its app set for the duration of the session. Older
        // sessions and subjects without apps fall back to the global list.
        val sessionPackages =
            prefs.getStringSet(BlockingPreferences.KEY_SESSION_PACKAGES, emptySet())
                ?: emptySet()
        val focusPackages = sessionPackages.ifEmpty { globalBlocked }
        if (openedPackage !in focusPackages) return

        if (prefs.getLong(BlockingPreferences.KEY_FOCUS_UNTIL, 0L) <= now) return

        val subject = prefs.getString(BlockingPreferences.KEY_SESSION_SUBJECT, null)
        showWall(
            openedPackage,
            BlockActivity.REASON_FOCUS,
            now,
            ruleLabel = subject?.takeIf { it.isNotBlank() }?.let { "$it session" }
        )
    }

    /**
     * Detects a blocked domain in a browser's address bar and shows the wall.
     *
     * This is deliberately the same accessibility channel Regain-style blockers
     * use: it needs no VPN or network permission, but it only sees browsers whose
     * address bar is exposed to accessibility. The scan is throttled because
     * content-change events fire very often while a page loads.
     */
    private fun maybeBlockWebsite(browserPackage: String) {
        val store = websiteStore ?: return
        if (!store.isEnabled()) return
        val blocked = store.domains.value
        if (blocked.isEmpty()) return
        if (!websiteBlockingActive()) return

        val elapsed = SystemClock.elapsedRealtime()
        if (elapsed - lastUrlScanAt < URL_SCAN_THROTTLE_MS) return
        lastUrlScanAt = elapsed

        val root = rootInActiveWindow ?: return
        val domain = collectAddressBarText(root)
            .firstNotNullOfOrNull { text -> blockedDomainFor(text, blocked) }
            ?: return

        val now = System.currentTimeMillis()
        // The address bar can re-announce the same page after the wall opens.
        if (domain == lastWebsiteDomain && now - lastWebsiteBlockAt < WEBSITE_REPEAT_MS) return
        lastWebsiteDomain = domain
        lastWebsiteBlockAt = now

        showWall(
            packageName = browserPackage,
            reason = BlockActivity.REASON_WEBSITE,
            now = now,
            untilMillis = activeRuleEnd(),
            ruleLabel = domain,
            domain = domain
        )
    }

    /** Website blocks only run during a focus session or a scheduled window. */
    private fun websiteBlockingActive(): Boolean {
        val now = System.currentTimeMillis()
        val focusPrefs = prefs ?: return false
        val focusActive =
            focusPrefs.getBoolean(BlockingPreferences.KEY_ENABLED, false) &&
                focusPrefs.getLong(BlockingPreferences.KEY_FOCUS_UNTIL, 0L) > now
        val scheduleActive = scheduleStore?.activeSchedules()?.isNotEmpty() == true
        return focusActive || scheduleActive
    }

    private fun activeRuleEnd(): Long =
        scheduleStore?.activeSchedules()?.maxOfOrNull { it.endsAtMillis } ?: 0L

    /**
     * Collects likely address-bar text, preferring nodes whose view id mentions a
     * URL and then editable fields. Page titles and body text are ignored.
     */
    private fun collectAddressBarText(root: AccessibilityNodeInfo): List<String> {
        val urlTexts = mutableListOf<String>()
        val editableTexts = mutableListOf<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0

        while (queue.isNotEmpty() && visited < MAX_SCAN_NODES) {
            val node = queue.removeFirst()
            visited++
            val id = node.viewIdResourceName.orEmpty()
            val candidate = node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                ?: node.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            if (candidate != null) {
                if (id.contains("url", ignoreCase = true)) {
                    urlTexts.add(candidate)
                } else if (node.isEditable) {
                    editableTexts.add(candidate)
                }
            }
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let { queue.add(it) }
            }
        }
        return urlTexts + editableTexts
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
            return
        }
        store.setActive(active, now)
        maybeWarnLimit(store, active)
    }

    /** Sends a single "X minutes left" heads-up before a daily limit is reached. */
    private fun maybeWarnLimit(store: AppLimitStore, packageName: String) {
        if (!store.shouldWarn(packageName)) return
        store.markWarningSent(packageName)

        val remainingMinutes =
            ((store.remainingMillis(packageName) + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
        val label = runCatching {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(packageName)
        val limit = store.limitMinutes(packageName)

        val notification = NotificationCompat.Builder(this, LIMIT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle("$label: $remainingMinutes min left")
            .setContentText("Your $limit minute daily limit is almost up.")
            .setContentIntent(
                android.app.PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, com.frostlock.app.ui.MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    android.app.PendingIntent.FLAG_IMMUTABLE or
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        runCatching {
            getSystemService(NotificationManager::class.java)
                .notify(LIMIT_NOTIFICATION_BASE + (packageName.hashCode() and 0x7FFF), notification)
        }
    }

    private fun createLimitChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                LIMIT_CHANNEL_ID,
                "App limit warnings",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Heads-up before an app hits its daily limit"
            }
        )
    }

    /**
     * Blocks the current app when a scheduled focus window starts while the user
     * is already inside it. Opening an app is handled in [onAccessibilityEvent].
     */
    private fun tickSchedule() {
        val pkg = foregroundPackage ?: return
        if (pkg == packageName) return

        // Schedules run independently of the manual Focus blocking switch.
        val prefs = prefs ?: return
        val globalBlocked =
            prefs.getStringSet(BlockingPreferences.KEY_PACKAGES, emptySet()) ?: emptySet()

        val rule = scheduleStore?.activeSchedules().orEmpty()
            .firstOrNull { it.schedule.blocks(pkg, globalBlocked) }
            ?: return

        showWall(
            pkg,
            BlockActivity.REASON_SCHEDULE,
            System.currentTimeMillis(),
            rule.endsAtMillis,
            rule.schedule.label
        )
    }

    private fun showWall(
        packageName: String,
        reason: String,
        now: Long,
        untilMillis: Long = 0L,
        ruleLabel: String? = null,
        domain: String? = null
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
            .putExtra(BlockActivity.EXTRA_RULE_LABEL, ruleLabel)
        domain?.let { intent.putExtra(BlockActivity.EXTRA_DOMAIN, it) }

        runCatching { startActivity(intent) }
            .onFailure { performGlobalAction(GLOBAL_ACTION_HOME) }
    }

    override fun onInterrupt() = Unit

    private fun findHomePackages(): Set<String> {
        return try {
            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
            intent.addCategory(android.content.Intent.CATEGORY_HOME)
            packageManager.queryIntentActivities(intent, 0)
                .map { it.activityInfo.packageName }
                .toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(usageTicker)
        limitStore?.commitActive()
        super.onDestroy()
    }

    companion object {
        private const val USAGE_TICK_MS = 10_000L
        private const val BLOCK_DEBOUNCE_MS = 700L
        private const val URL_SCAN_THROTTLE_MS = 600L
        private const val WEBSITE_REPEAT_MS = 5_000L
        private const val MAX_SCAN_NODES = 400
        private const val LIMIT_CHANNEL_ID = "app_limits"
        private const val LIMIT_NOTIFICATION_BASE = 2_000

        /** Browsers whose address bar is exposed through accessibility. */
        private val BROWSER_PACKAGES = setOf(
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.dev",
            "com.brave.browser",
            "org.mozilla.firefox",
            "org.mozilla.firefox_beta",
            "com.microsoft.emmx",
            "com.sec.android.app.sbrowser",
            "com.opera.browser",
            "com.opera.mini.native",
            "com.duckduckgo.mobile.android",
            "com.android.browser",
            "com.vivaldi.browser",
            "com.ecosia.android",
            "com.qwant.liberty"
        )

        private val WEBSITE_EVENT_TYPES = setOf(
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        )

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
