package com.frostlock.app.blocking

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.frostlock.app.R
import com.frostlock.app.data.local.prefs.AppLimitStore
import com.frostlock.app.data.local.prefs.BlockingPreferences
import com.frostlock.app.data.local.prefs.ScheduleStore
import com.frostlock.app.data.local.prefs.UnlockStore
import com.frostlock.app.data.local.prefs.WebsiteBlockingStore
import com.frostlock.app.data.local.prefs.YouTubeChannelStore
import com.frostlock.app.data.local.prefs.blocks
import com.frostlock.app.ui.util.domainMatches
import com.frostlock.app.ui.util.hostFromUrlText
import com.frostlock.app.ui.util.isDomainAllowed
import com.frostlock.app.ui.util.MultiWindowDetector
import com.frostlock.app.ui.util.NavTab
import com.frostlock.app.ui.util.PlayStoreDetector
import com.frostlock.app.ui.util.ShortFormDetector
import com.frostlock.app.ui.util.VisibleWindow
import com.frostlock.app.ui.util.YouTubeChannelDetector
import com.frostlock.app.ui.util.YouTubeSurfaceDetector

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
    private var channelStore: YouTubeChannelStore? = null
    private var unlockStore: UnlockStore? = null
    private var homePackages: Set<String> = emptySet()
    private var lastBlockAt = 0L
    private var foregroundPackage: String? = null
    private var lastUrlScanAt = 0L
    private var lastWebsiteDomain: String? = null
    private var lastWebsiteBlockAt = 0L
    private var lastShortsScanAt = 0L
    private var lastShortsBlockAt = 0L
    private var lastShortsPackage: String? = null
    private var lastSideScanAt = 0L
    private var lastSideBlockAt = 0L
    private var lastSidePackage: String? = null
    private var lastYouTubeHomeScanAt = 0L
    private var lastYouTubeHomeBlockAt = 0L
    private var lastPlayStoreScanAt = 0L
    private var lastPlayStoreQuery: String? = null
    private var lastPlayStoreBlockAt = 0L
    private var lastChannelScanAt = 0L
    private var lastChannelBlocked: String? = null
    private var lastChannelBlockAt = 0L

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_USER_PRESENT) {
                unlockStore?.recordUnlock()
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val usageTicker = object : Runnable {
        override fun run() {
            tickUsage()
            tickSchedule()
            maybeBlockSideWindow()
            handler.postDelayed(this, USAGE_TICK_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = getSharedPreferences(BlockingPreferences.PREFS_NAME, Context.MODE_PRIVATE)
        limitStore = AppLimitStore(this)
        scheduleStore = ScheduleStore(this)
        websiteStore = WebsiteBlockingStore(this)
        channelStore = YouTubeChannelStore(this)
        unlockStore = UnlockStore(this)
        homePackages = findHomePackages()
        createLimitChannel()
        runCatching {
            ContextCompat.registerReceiver(
                this,
                unlockReceiver,
                IntentFilter(Intent.ACTION_USER_PRESENT),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
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

        if (eventPackage in SHORTS_PACKAGES &&
            event.eventType in WEBSITE_EVENT_TYPES
        ) {
            maybeBlockShorts(eventPackage)
        }

        if (eventPackage == YouTubeSurfaceDetector.PACKAGE &&
            event.eventType in WEBSITE_EVENT_TYPES
        ) {
            maybeBlockYouTubeHome()
        }

        if (eventPackage == PlayStoreDetector.PACKAGE &&
            event.eventType in WEBSITE_EVENT_TYPES
        ) {
            maybeBlockPlayStoreSearch()
        }

        if (eventPackage == YouTubeChannelDetector.PACKAGE &&
            event.eventType in WEBSITE_EVENT_TYPES
        ) {
            maybeBlockYouTubeChannel()
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            maybeBlockSideWindow()
        ) {
            return
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
            if (focusActive) {
                // A live session heartbeats focus_until, so this wall lifts as
                // soon as the session ends.
                showWall(
                    openedPackage,
                    BlockActivity.REASON_FOCUS,
                    now,
                    ruleLabel = "Home screen blocked"
                )
            } else {
                // Only a schedule is running: hold the wall until the window
                // closes. A heartbeat-based wall would dismiss itself on the
                // expired heartbeat and re-fire in a loop.
                val active = scheduleStore?.activeSchedules().orEmpty().firstOrNull()
                showWall(
                    openedPackage,
                    BlockActivity.REASON_SCHEDULE,
                    now,
                    activeRuleEnd(),
                    active?.schedule?.label
                )
            }
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
        val blocked = store.blockedDomainList()
        if (blocked.isEmpty()) return
        if (!websiteBlockingActive()) return

        val elapsed = SystemClock.elapsedRealtime()
        if (elapsed - lastUrlScanAt < URL_SCAN_THROTTLE_MS) return
        lastUrlScanAt = elapsed

        val root = rootForPackage(browserPackage) ?: return
        val host = collectAddressBarText(root)
            .firstNotNullOfOrNull { text -> hostFromUrlText(text) }
            ?: return
        if (isDomainAllowed(host, store.allowedDomainList())) return
        val domain = blocked.firstOrNull { blockedDomain -> domainMatches(host, blockedDomain) }
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

    /** Shorts/Reels blocking only runs during a focus session or a scheduled window. */
    private fun maybeBlockShorts(packageName: String) {
        if (!websiteBlockingActive()) return

        val elapsed = SystemClock.elapsedRealtime()
        if (elapsed - lastShortsScanAt < SHORTS_SCAN_THROTTLE_MS) return
        lastShortsScanAt = elapsed

        if (ShortFormDetector.keywordsFor(packageName).isEmpty()) return
        val root = rootForPackage(packageName) ?: return

        val found = if (ShortFormDetector.selectedTabLabel(packageName) != null) {
            collectShortFormSurface(root, packageName)
        } else {
            collectTextFromRoot(root) { text ->
                ShortFormDetector.matches(packageName, text)
            }
        }

        if (!found) return

        val now = System.currentTimeMillis()
        if (packageName == lastShortsPackage && now - lastShortsBlockAt < SHORTS_REPEAT_MS) return
        lastShortsPackage = packageName
        lastShortsBlockAt = now

        val label = when (packageName) {
            "com.google.android.youtube" -> "Shorts"
            "com.instagram.android" -> "Reels"
            "com.facebook.katana" -> "Reels"
            "com.snapchat.android" -> "Spotlight"
            else -> "Short-form"
        }

        showWall(
            packageName = packageName,
            reason = BlockActivity.REASON_FOCUS,
            now = now,
            ruleLabel = "$label blocked"
        )
    }

    /**
     * Root of the visible window owned by [packageName], falling back to the
     * active window only when it belongs to the same package.
     *
     * `rootInActiveWindow` can point at our own overlay while a focus banner is
     * showing, so the target app's window is looked up in the window list first.
     */
    private fun rootForPackage(packageName: String): AccessibilityNodeInfo? {
        val active = rootInActiveWindow
        if (active != null && active.packageName == packageName) return active
        return runCatching { windows }.getOrNull().orEmpty()
            .mapNotNull { window -> runCatching { window.root }.getOrNull() }
            .firstOrNull { it.packageName == packageName }
    }

    /**
     * One pass over the tree that flags this app's short-form surface.
     *
     * The surface is present when its bottom-navigation tab is the selected one,
     * or when a node's view id belongs to the app's short-form player.
     */
    private fun collectShortFormSurface(
        root: AccessibilityNodeInfo,
        packageName: String
    ): Boolean {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0

        while (queue.isNotEmpty() && visited < MAX_SCAN_NODES) {
            val node = queue.removeFirst()
            visited++
            if (ShortFormDetector.matchesPlayerSurface(packageName, node.viewIdResourceName)) {
                return true
            }
            val label = node.contentDescription?.toString()?.trim()
                ?: node.text?.toString()?.trim()
            if (label != null && node.isSelected &&
                ShortFormDetector.matchesSelectedTab(packageName, label)
            ) {
                return true
            }
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let { queue.add(it) }
            }
        }
        return false
    }

    /**
     * Blocks only the YouTube home feed while leaving search, subscriptions and
     * video playback usable. Opt-in, and only active during focus or a schedule.
     *
     * YouTube is skipped when it is fully blocked because the foreground path
     * already covers that case.
     */
    private fun maybeBlockYouTubeHome() {
        val current = prefs ?: return
        if (!current.getBoolean(BlockingPreferences.KEY_YOUTUBE_HOME_BLOCKING, false)) return
        if (!websiteBlockingActive()) return
        if (YouTubeSurfaceDetector.PACKAGE in activeBlockedPackages(System.currentTimeMillis())) return

        val elapsed = SystemClock.elapsedRealtime()
        if (elapsed - lastYouTubeHomeScanAt < YOUTUBE_HOME_SCAN_THROTTLE_MS) return
        lastYouTubeHomeScanAt = elapsed

        val root = rootForPackage(YouTubeSurfaceDetector.PACKAGE) ?: return
        if (!YouTubeSurfaceDetector.isHomeFeed(collectNavTabs(root))) return

        val now = System.currentTimeMillis()
        if (now - lastYouTubeHomeBlockAt < YOUTUBE_HOME_REPEAT_MS) return
        lastYouTubeHomeBlockAt = now

        showWall(
            packageName = YouTubeSurfaceDetector.PACKAGE,
            reason = BlockActivity.REASON_YOUTUBE_HOME,
            now = now,
            untilMillis = activeRuleEnd()
        )
    }

    /**
     * Holds the wall while the Play Store's search box names a blocked app.
     *
     * A store result page puts the blocked app one tap away, so searching by its
     * name must not work either. Opt-in, and only active during focus or a
     * schedule. Ordinary store browsing is untouched: only text typed into an
     * editable field is matched, against the blocked apps' display labels.
     */
    private fun maybeBlockPlayStoreSearch() {
        val current = prefs ?: return
        if (!current.getBoolean(BlockingPreferences.KEY_PLAY_STORE_BLOCKING, false)) return
        if (!websiteBlockingActive()) return

        val now = System.currentTimeMillis()
        val blockedPackages = activeBlockedPackages(now)
        if (blockedPackages.isEmpty()) return

        val elapsed = SystemClock.elapsedRealtime()
        if (elapsed - lastPlayStoreScanAt < PLAY_STORE_SCAN_THROTTLE_MS) return
        lastPlayStoreScanAt = elapsed

        val root = rootForPackage(PlayStoreDetector.PACKAGE) ?: return
        val query = collectSearchQuery(root) ?: return
        val label = PlayStoreDetector.matchedLabel(query, blockedAppLabels(blockedPackages))
            ?: return

        if (query == lastPlayStoreQuery && now - lastPlayStoreBlockAt < PLAY_STORE_REPEAT_MS) {
            return
        }
        lastPlayStoreQuery = query
        lastPlayStoreBlockAt = now

        showWall(
            packageName = PlayStoreDetector.PACKAGE,
            reason = BlockActivity.REASON_PLAY_STORE,
            now = now,
            untilMillis = activeRuleEnd(),
            ruleLabel = label,
            domain = label
        )
    }

    /** Text typed into an editable field, e.g. the store's search box. */
    private fun collectSearchQuery(root: AccessibilityNodeInfo): String? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < MAX_SCAN_NODES) {
            val node = queue.removeFirst()
            visited++
            val text = node.text?.toString()?.trim()
            if (!text.isNullOrEmpty() && node.isEditable) return text
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let { queue.add(it) }
            }
        }
        return null
    }

    /** Display labels of the blocked apps, used to match store search queries. */
    private fun blockedAppLabels(packages: Set<String>): List<String> =
        packages.mapNotNull { pkg ->
            runCatching {
                val info = packageManager.getApplicationInfo(pkg, 0)
                packageManager.getApplicationLabel(info).toString()
            }.getOrNull()
        }

    /**
     * Walls the video when its channel is not allowed to play.
     *
     * Two modes: a block list ("never these channels") and a study allowlist
     * ("only these channels"). Opt-in, and only active during focus or a
     * schedule; YouTube is skipped when the whole app is blocked because the
     * foreground path already covers that. The channel is read from the
     * owner-row accessibility labels of the watch page, so recommendations,
     * search results and the home feed are never judged by their channel.
     */
    private fun maybeBlockYouTubeChannel() {
        val store = channelStore ?: return
        if (!store.isEnabled()) return
        if (!websiteBlockingActive()) return
        val now = System.currentTimeMillis()
        if (YouTubeSurfaceDetector.PACKAGE in activeBlockedPackages(now)) return

        val elapsed = SystemClock.elapsedRealtime()
        if (elapsed - lastChannelScanAt < CHANNEL_SCAN_THROTTLE_MS) return
        lastChannelScanAt = elapsed

        val root = rootForPackage(YouTubeChannelDetector.PACKAGE) ?: return
        val owners = collectChannelOwnerNames(root)
        val violating = YouTubeChannelDetector.violatingOwner(
            allowOnly = store.allowOnlyMode(),
            ownerNames = owners,
            blocked = store.blockedChannels(),
            allowed = store.allowedChannels()
        ) ?: return

        if (violating == lastChannelBlocked && now - lastChannelBlockAt < CHANNEL_REPEAT_MS) {
            return
        }
        lastChannelBlocked = violating
        lastChannelBlockAt = now

        showWall(
            packageName = YouTubeChannelDetector.PACKAGE,
            reason = BlockActivity.REASON_YOUTUBE_CHANNEL,
            now = now,
            untilMillis = activeRuleEnd(),
            ruleLabel = violating,
            domain = violating
        )
    }

    /** Names in the current video's owner row, parsed from a11y labels. */
    private fun collectChannelOwnerNames(root: AccessibilityNodeInfo): Set<String> {
        val owners = mutableSetOf<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < MAX_SCAN_NODES) {
            val node = queue.removeFirst()
            visited++
            val label = node.contentDescription?.toString()?.trim()
            if (!label.isNullOrEmpty()) {
                YouTubeChannelDetector.ownerChannelName(label)?.let { owners += it }
            }
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let { queue.add(it) }
            }
        }
        return owners
    }

    /** Collects bottom-navigation tabs and whether each one is selected. */
    private fun collectNavTabs(root: AccessibilityNodeInfo): List<NavTab> {
        val tabs = mutableListOf<NavTab>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0

        while (queue.isNotEmpty() && visited < MAX_SCAN_NODES) {
            val node = queue.removeFirst()
            visited++
            val label = node.contentDescription?.toString()?.trim()
                ?: node.text?.toString()?.trim()
            if (!label.isNullOrEmpty() &&
                label.lowercase() in YouTubeSurfaceDetector.tabLabels
            ) {
                tabs += NavTab(label = label, isSelected = node.isSelected)
            }
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let { queue.add(it) }
            }
        }
        return tabs
    }

    /**
     * Blocks a blocked app that stays visible in split screen, a freeform window,
     * or picture-in-picture while another app is active.
     *
     * Returns true when a wall was shown so the caller can skip the normal
     * foreground handling for the same event.
     */
    private fun maybeBlockSideWindow(): Boolean {
        val now = System.currentTimeMillis()
        val blocked = activeBlockedPackages(now)
        if (blocked.isEmpty()) return false

        val elapsed = SystemClock.elapsedRealtime()
        if (elapsed - lastSideScanAt < SIDE_SCAN_THROTTLE_MS) return false
        lastSideScanAt = elapsed

        val visible = runCatching { windows }.getOrNull().orEmpty().map { window ->
            VisibleWindow(
                packageName = window.root?.packageName?.toString().orEmpty(),
                isActive = window.isActive,
                isApplication = window.type == AccessibilityWindowInfo.TYPE_APPLICATION
            )
        }

        val blockedPackage = MultiWindowDetector.blockedSideWindow(
            windows = visible,
            blockedPackages = blocked,
            ownPackage = packageName
        ) ?: return false

        if (blockedPackage == lastSidePackage && now - lastSideBlockAt < SIDE_REPEAT_MS) {
            return false
        }
        lastSidePackage = blockedPackage
        lastSideBlockAt = now

        showWall(
            packageName = blockedPackage,
            reason = BlockActivity.REASON_FOCUS,
            now = now,
            untilMillis = activeRuleEnd(),
            ruleLabel = "Split screen blocked"
        )
        return true
    }

    /**
     * Every package that should be hidden while a focus session or schedule is
     * active. Home launchers join the set only when home-screen blocking is on.
     */
    private fun activeBlockedPackages(now: Long): Set<String> {
        val prefs = prefs ?: return emptySet()
        val globalBlocked =
            prefs.getStringSet(BlockingPreferences.KEY_PACKAGES, emptySet()).orEmpty()
        val result = mutableSetOf<String>()

        val activeSchedules = scheduleStore?.activeSchedules().orEmpty()
        activeSchedules.forEach { active ->
            result += active.schedule.packages.ifEmpty { globalBlocked }
        }

        val focusActive =
            prefs.getBoolean(BlockingPreferences.KEY_ENABLED, false) &&
                prefs.getLong(BlockingPreferences.KEY_FOCUS_UNTIL, 0L) > now
        if (focusActive) {
            val sessionPackages =
                prefs.getStringSet(BlockingPreferences.KEY_SESSION_PACKAGES, emptySet())
                    .orEmpty()
            result += sessionPackages.ifEmpty { globalBlocked }
        }

        val homeBlocking =
            prefs.getBoolean(BlockingPreferences.KEY_HOME_BLOCKING_ENABLED, false)
        if (homeBlocking && (focusActive || activeSchedules.isNotEmpty())) {
            result += homePackages
        }
        return result
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

    private fun collectTextFromRoot(root: AccessibilityNodeInfo, predicate: (String) -> Boolean): Boolean {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < MAX_SCAN_NODES) {
            val node = queue.removeFirst()
            visited++
            val text = node.text?.toString()?.trim()
                ?: node.contentDescription?.toString()?.trim()
            if (!text.isNullOrEmpty() && predicate(text)) {
                return true
            }
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let { queue.add(it) }
            }
        }
        return false
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
        runCatching { unregisterReceiver(unlockReceiver) }
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

        private val SHORTS_PACKAGES = setOf(
            "com.google.android.youtube",
            "com.instagram.android",
            "com.facebook.katana",
            "com.snapchat.android"
        )

        private const val SHORTS_SCAN_THROTTLE_MS = 800L
        private const val SHORTS_REPEAT_MS = 7_000L
        private const val SIDE_SCAN_THROTTLE_MS = 900L
        private const val SIDE_REPEAT_MS = 8_000L
        private const val YOUTUBE_HOME_SCAN_THROTTLE_MS = 900L
        private const val YOUTUBE_HOME_REPEAT_MS = 8_000L
        private const val PLAY_STORE_SCAN_THROTTLE_MS = 900L
        private const val PLAY_STORE_REPEAT_MS = 8_000L
        private const val CHANNEL_SCAN_THROTTLE_MS = 900L
        private const val CHANNEL_REPEAT_MS = 8_000L

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
