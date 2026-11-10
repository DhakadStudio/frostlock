package com.focusflow.app.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
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

    fun setStrictMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STRICT_MODE, enabled).apply()
        _strictMode.value = enabled
    }

    fun setDailyGoalMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(15, 8 * 60)
        prefs.edit().putInt(KEY_DAILY_GOAL, clamped).apply()
        _dailyGoalMinutes.value = clamped
    }

    companion object {
        const val PREFS_NAME = "focusflow_settings"
        const val KEY_STRICT_MODE = "strict_mode"
        const val KEY_DAILY_GOAL = "daily_goal_minutes"
        const val DEFAULT_GOAL_MINUTES = 120
    }
}
