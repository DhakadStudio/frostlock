package com.frostlock.app.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Small synchronous store for the app-blocker configuration.
 *
 * SharedPreferences is used instead of DataStore because the accessibility service
 * reads this on every window change, where an asynchronous read is not practical.
 */
@Singleton
class BlockingPreferences @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _blockingEnabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    val blockingEnabled: StateFlow<Boolean> = _blockingEnabled.asStateFlow()

    private val _blockedPackages =
        MutableStateFlow(prefs.getStringSet(KEY_PACKAGES, emptySet())?.toSet() ?: emptySet())
    val blockedPackages: StateFlow<Set<String>> = _blockedPackages.asStateFlow()

    private val _focusUntil = MutableStateFlow(prefs.getLong(KEY_FOCUS_UNTIL, 0L))
    val focusUntil: StateFlow<Long> = _focusUntil.asStateFlow()

    private val _sessionEndsAt = MutableStateFlow(prefs.getLong(KEY_SESSION_ENDS_AT, 0L))
    val sessionEndsAt: StateFlow<Long> = _sessionEndsAt.asStateFlow()

    fun setBlockingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _blockingEnabled.value = enabled
    }

    fun setHomeBlockingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HOME_BLOCKING_ENABLED, enabled).apply()
    }

    fun isHomeBlockingEnabled(): Boolean =
        prefs.getBoolean(KEY_HOME_BLOCKING_ENABLED, false)

    fun setYouTubeHomeBlockingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_YOUTUBE_HOME_BLOCKING, enabled).apply()
    }

    fun isYouTubeHomeBlockingEnabled(): Boolean =
        prefs.getBoolean(KEY_YOUTUBE_HOME_BLOCKING, false)

    fun setBlockedPackages(packages: Set<String>) {
        prefs.edit().putStringSet(KEY_PACKAGES, packages).apply()
        _blockedPackages.value = packages
    }

    fun togglePackage(packageName: String) {
        val current = _blockedPackages.value
        setBlockedPackages(
            if (packageName in current) current - packageName else current + packageName
        )
    }

    /**
     * Keeps blocking active until [untilMillis]. The focus timer refreshes this as a
     * heartbeat, so if the app process dies the block lifts on its own shortly after.
     */
    fun setFocusWindow(untilMillis: Long) {
        prefs.edit().putLong(KEY_FOCUS_UNTIL, untilMillis).apply()
        _focusUntil.value = untilMillis
    }

    fun clearFocusWindow() = setFocusWindow(0L)

    /**
     * When the current focus session ends. Zero means "open ended" (stopwatch mode).
     * Kept separate from [focusUntil], which is only a short heartbeat.
     */
    fun setSessionEndsAt(endsAt: Long) {
        prefs.edit().putLong(KEY_SESSION_ENDS_AT, endsAt).apply()
        _sessionEndsAt.value = endsAt
    }

    fun clearSession() {
        setSessionEndsAt(0L)
        clearFocusWindow()
        prefs.edit()
            .remove(KEY_SESSION_PACKAGES)
            .remove(KEY_SESSION_SUBJECT)
            .apply()
    }

    /**
     * Snapshot of the apps this session blocks. The accessibility service reads
     * this instead of the global list so a "Work" session and a "Study" session
     * can lock different apps. Empty means "use the global focus-session list".
     */
    fun saveSessionApps(packages: Set<String>, subject: String?) {
        prefs.edit()
            .putStringSet(KEY_SESSION_PACKAGES, packages)
            .putString(KEY_SESSION_SUBJECT, subject?.takeIf { it.isNotBlank() })
            .apply()
    }

    fun sessionPackages(): Set<String> =
        prefs.getStringSet(KEY_SESSION_PACKAGES, emptySet())?.toSet() ?: emptySet()

    fun sessionSubject(): String? = prefs.getString(KEY_SESSION_SUBJECT, null)

    fun isFocusActive(now: Long = System.currentTimeMillis()): Boolean =
        prefs.getLong(KEY_FOCUS_UNTIL, 0L) > now

    // --- Persisted timer state -------------------------------------------------
    // The timer lives in the ViewModel, but the ViewModel is destroyed when the user
    // navigates away or the activity is recreated. These values let it pick the
    // session back up so the UI always matches what the blocker is doing.

    fun saveTimerState(
        mode: String,
        totalSeconds: Int,
        accumulatedMs: Long,
        runningSince: Long,
        strict: Boolean,
        phase: String = "FOCUS",
        round: Int = 1,
        breakMinutes: Int = 5,
        breaksEnabled: Boolean = true,
        pausesUsed: Int = 0,
        maxPauses: Int = 2
    ) {
        prefs.edit()
            .putString(KEY_TIMER_MODE, mode)
            .putInt(KEY_TIMER_TOTAL, totalSeconds)
            .putLong(KEY_TIMER_ACCUMULATED, accumulatedMs)
            .putLong(KEY_TIMER_RUNNING_SINCE, runningSince)
            .putBoolean(KEY_TIMER_STRICT, strict)
            .putString(KEY_TIMER_PHASE, phase)
            .putInt(KEY_TIMER_ROUND, round)
            .putInt(KEY_TIMER_BREAK_MINUTES, breakMinutes)
            .putBoolean(KEY_TIMER_BREAKS_ENABLED, breaksEnabled)
            .putInt(KEY_TIMER_PAUSES_USED, pausesUsed)
            .putInt(KEY_TIMER_MAX_PAUSES, maxPauses)
            .apply()
    }

    fun clearTimerState() {
        prefs.edit()
            .remove(KEY_TIMER_MODE)
            .remove(KEY_TIMER_TOTAL)
            .remove(KEY_TIMER_ACCUMULATED)
            .remove(KEY_TIMER_RUNNING_SINCE)
            .remove(KEY_TIMER_STRICT)
            .remove(KEY_TIMER_PHASE)
            .remove(KEY_TIMER_ROUND)
            .remove(KEY_TIMER_BREAK_MINUTES)
            .remove(KEY_TIMER_BREAKS_ENABLED)
            .remove(KEY_TIMER_PAUSES_USED)
            .remove(KEY_TIMER_MAX_PAUSES)
            .apply()
    }

    fun savedTimerMode(): String? = prefs.getString(KEY_TIMER_MODE, null)

    fun savedTimerTotalSeconds(): Int = prefs.getInt(KEY_TIMER_TOTAL, 0)

    fun savedTimerAccumulatedMs(): Long = prefs.getLong(KEY_TIMER_ACCUMULATED, 0L)

    /** Wall-clock time the current run segment started, or 0 when paused. */
    fun savedTimerRunningSince(): Long = prefs.getLong(KEY_TIMER_RUNNING_SINCE, 0L)

    fun savedTimerStrict(): Boolean = prefs.getBoolean(KEY_TIMER_STRICT, false)

    fun savedTimerPhase(): String = prefs.getString(KEY_TIMER_PHASE, "FOCUS") ?: "FOCUS"

    fun savedTimerRound(): Int = prefs.getInt(KEY_TIMER_ROUND, 1)

    fun savedTimerBreakMinutes(): Int = prefs.getInt(KEY_TIMER_BREAK_MINUTES, 5)

    fun savedTimerBreaksEnabled(): Boolean = prefs.getBoolean(KEY_TIMER_BREAKS_ENABLED, true)

    fun savedTimerPausesUsed(): Int = prefs.getInt(KEY_TIMER_PAUSES_USED, 0)

    fun savedTimerMaxPauses(): Int = prefs.getInt(KEY_TIMER_MAX_PAUSES, 2)

    companion object {
        const val PREFS_NAME = "frostlock_blocking"
        const val KEY_ENABLED = "blocking_enabled"
        const val KEY_HOME_BLOCKING_ENABLED = "home_blocking_enabled"
        const val KEY_YOUTUBE_HOME_BLOCKING = "youtube_home_blocking_enabled"
        const val KEY_PACKAGES = "blocked_packages"
        const val KEY_FOCUS_UNTIL = "focus_until"
        const val KEY_SESSION_ENDS_AT = "session_ends_at"
        const val KEY_SESSION_PACKAGES = "session_packages"
        const val KEY_SESSION_SUBJECT = "session_subject"
        const val KEY_TIMER_MODE = "timer_mode"
        const val KEY_TIMER_TOTAL = "timer_total_seconds"
        const val KEY_TIMER_ACCUMULATED = "timer_accumulated_ms"
        const val KEY_TIMER_RUNNING_SINCE = "timer_running_since"
        const val KEY_TIMER_STRICT = "timer_strict"
        const val KEY_TIMER_PHASE = "timer_phase"
        const val KEY_TIMER_ROUND = "timer_round"
        const val KEY_TIMER_BREAK_MINUTES = "timer_break_minutes"
        const val KEY_TIMER_BREAKS_ENABLED = "timer_breaks_enabled"
        const val KEY_TIMER_PAUSES_USED = "timer_pauses_used"
        const val KEY_TIMER_MAX_PAUSES = "timer_max_pauses"

        /** How long a single heartbeat keeps blocking alive. */
        const val HEARTBEAT_WINDOW_MS = 5_000L
    }
}
