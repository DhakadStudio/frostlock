package com.frostlock.app.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import com.frostlock.app.ui.util.PausePolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User preferences that the focus timer needs synchronously when a session starts.
 * SharedPreferences keeps these reads cheap and predictable.
 */
@Singleton
class FocusSettings @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _strictMode = MutableStateFlow(prefs.getBoolean(KEY_STRICT_MODE, false))
    val strictMode: StateFlow<Boolean> = _strictMode.asStateFlow()

    private val _dailyGoalMinutes = MutableStateFlow(prefs.getInt(KEY_DAILY_GOAL, DEFAULT_GOAL_MINUTES))
    val dailyGoalMinutes: StateFlow<Int> = _dailyGoalMinutes.asStateFlow()

    /** Last timer length the user picked, so custom durations stick between visits. */
    private val _lastDurationMinutes =
        MutableStateFlow(prefs.getInt(KEY_LAST_DURATION, DEFAULT_DURATION_MINUTES))
    val lastDurationMinutes: StateFlow<Int> = _lastDurationMinutes.asStateFlow()

    /** Whether Pomodoro rounds are followed by a break. Independent of app blocking. */
    private val _breaksEnabled = MutableStateFlow(prefs.getBoolean(KEY_BREAKS_ENABLED, true))
    val breaksEnabled: StateFlow<Boolean> = _breaksEnabled.asStateFlow()

    private val _breakMinutes = MutableStateFlow(prefs.getInt(KEY_BREAK_MINUTES, DEFAULT_BREAK_MINUTES))
    val breakMinutes: StateFlow<Int> = _breakMinutes.asStateFlow()

    /** How many times a normal (non-strict) session may be paused. -1 means no cap. */
    private val _maxPauses = MutableStateFlow(
        prefs.getInt(KEY_MAX_PAUSES, PausePolicy.DEFAULT)
    )
    val maxPauses: StateFlow<Int> = _maxPauses.asStateFlow()

    /** Opt-in reminder during long focus sessions. */
    private val _focusGuardEnabled = MutableStateFlow(prefs.getBoolean(KEY_FOCUS_GUARD, false))
    val focusGuardEnabled: StateFlow<Boolean> = _focusGuardEnabled.asStateFlow()

    /** A small draggable timer floating over other apps during a focus session. */
    private val _bubbleEnabled = MutableStateFlow(prefs.getBoolean(KEY_FOCUS_BUBBLE, false))
    val bubbleEnabled: StateFlow<Boolean> = _bubbleEnabled.asStateFlow()

    /** A local notification when a blocking rule starts and when it ends. */
    private val _scheduleRemindersEnabled =
        MutableStateFlow(prefs.getBoolean(KEY_SCHEDULE_REMINDERS, true))
    val scheduleRemindersEnabled: StateFlow<Boolean> = _scheduleRemindersEnabled.asStateFlow()

    /** Hold notifications from apps that are blocked right now. Needs listener access. */
    private val _notificationBlockingEnabled =
        MutableStateFlow(prefs.getBoolean(KEY_NOTIFICATION_BLOCKING, false))
    val notificationBlockingEnabled: StateFlow<Boolean> =
        _notificationBlockingEnabled.asStateFlow()

    /** Last subject/tag, so repeated study sessions keep the same label. */
    private val _sessionTag = MutableStateFlow(prefs.getString(KEY_SESSION_TAG, "") ?: "")
    val sessionTag: StateFlow<String> = _sessionTag.asStateFlow()

    /** The one outcome the user wants from the current session. */
    private val _sessionIntent =
        MutableStateFlow(prefs.getString(KEY_SESSION_INTENT, "") ?: "")
    val sessionIntent: StateFlow<String> = _sessionIntent.asStateFlow()

    fun setStrictMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STRICT_MODE, enabled).apply()
        _strictMode.value = enabled
    }

    fun setBreaksEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BREAKS_ENABLED, enabled).apply()
        _breaksEnabled.value = enabled
    }

    fun setBreakMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(MIN_BREAK_MINUTES, MAX_BREAK_MINUTES)
        prefs.edit().putInt(KEY_BREAK_MINUTES, clamped).apply()
        _breakMinutes.value = clamped
    }

    fun setFocusGuardEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FOCUS_GUARD, enabled).apply()
        _focusGuardEnabled.value = enabled
    }

    /** Stores the pause cap; -1 means unlimited, otherwise 0–5 pauses. */
    fun setMaxPauses(count: Int) {
        val clamped = if (count < 0) {
            PausePolicy.UNLIMITED
        } else {
            count.coerceIn(0, PausePolicy.MAX)
        }
        prefs.edit().putInt(KEY_MAX_PAUSES, clamped).apply()
        _maxPauses.value = clamped
    }

    fun setBubbleEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FOCUS_BUBBLE, enabled).apply()
        _bubbleEnabled.value = enabled
    }

    fun setScheduleRemindersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SCHEDULE_REMINDERS, enabled).apply()
        _scheduleRemindersEnabled.value = enabled
    }

    fun setNotificationBlockingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_BLOCKING, enabled).apply()
        _notificationBlockingEnabled.value = enabled
    }

    fun setSessionTag(tag: String) {
        val cleaned = tag.trim().take(MAX_TAG_LENGTH)
        prefs.edit().putString(KEY_SESSION_TAG, cleaned).apply()
        _sessionTag.value = cleaned
    }

    fun setSessionIntent(intent: String) {
        val cleaned = intent.trim().take(MAX_INTENT_LENGTH)
        prefs.edit().putString(KEY_SESSION_INTENT, cleaned).apply()
        _sessionIntent.value = cleaned
    }

    fun setDailyGoalMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(15, 8 * 60)
        prefs.edit().putInt(KEY_DAILY_GOAL, clamped).apply()
        _dailyGoalMinutes.value = clamped
    }

    fun setLastDurationMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(MIN_DURATION_MINUTES, MAX_DURATION_MINUTES)
        prefs.edit().putInt(KEY_LAST_DURATION, clamped).apply()
        _lastDurationMinutes.value = clamped
    }

    companion object {
        const val PREFS_NAME = "frostlock_settings"
        const val KEY_STRICT_MODE = "strict_mode"
        const val KEY_DAILY_GOAL = "daily_goal_minutes"
        const val KEY_LAST_DURATION = "last_duration_minutes"
        const val KEY_BREAKS_ENABLED = "pomodoro_breaks_enabled"
        const val KEY_BREAK_MINUTES = "pomodoro_break_minutes"
        const val KEY_FOCUS_GUARD = "focus_guard_enabled"
        const val KEY_MAX_PAUSES = "max_pauses_per_session"
        const val KEY_FOCUS_BUBBLE = "focus_bubble_enabled"
        const val KEY_SCHEDULE_REMINDERS = "schedule_reminders_enabled"
        const val KEY_NOTIFICATION_BLOCKING = "notification_blocking_enabled"
        const val KEY_SESSION_TAG = "last_session_tag"
        const val KEY_SESSION_INTENT = "session_intent"
        const val DEFAULT_GOAL_MINUTES = 120
        const val DEFAULT_DURATION_MINUTES = 25
        const val DEFAULT_BREAK_MINUTES = 5
        const val MIN_BREAK_MINUTES = 1
        const val MAX_BREAK_MINUTES = 30
        const val MAX_TAG_LENGTH = 24
        const val MAX_INTENT_LENGTH = 80
        const val MIN_DURATION_MINUTES = 1
        const val MAX_DURATION_MINUTES = 480
    }
}
