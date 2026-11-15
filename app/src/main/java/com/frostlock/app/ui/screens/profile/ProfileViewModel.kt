package com.frostlock.app.ui.screens.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostlock.app.data.local.prefs.DeadlineStore
import com.frostlock.app.data.local.prefs.FocusDeadline
import com.frostlock.app.data.local.prefs.FocusSettings
import com.frostlock.app.data.local.prefs.ThemeStore
import com.frostlock.app.ui.theme.IcePalettes
import com.frostlock.app.ui.util.AmbienceSound
import com.frostlock.app.ui.util.PausePolicy
import com.frostlock.app.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ProfileUiState(
    val strictMode: Boolean = false,
    val dailyGoalMinutes: Int = FocusSettings.DEFAULT_GOAL_MINUTES,
    val breaksEnabled: Boolean = true,
    val breakMinutes: Int = FocusSettings.DEFAULT_BREAK_MINUTES,
    val maxPauses: Int = PausePolicy.DEFAULT,
    val focusGuardEnabled: Boolean = false,
    val bubbleEnabled: Boolean = false,
    val ambienceKey: String = AmbienceSound.Off.key,
    val themeKey: String = IcePalettes.Frost.key,
    val deadline: FocusDeadline? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val focusSettings: FocusSettings,
    private val themeStore: ThemeStore,
    deadlineStore: DeadlineStore
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        focusSettings.strictMode,
        focusSettings.dailyGoalMinutes,
        focusSettings.breaksEnabled,
        focusSettings.breakMinutes,
        focusSettings.focusGuardEnabled
    ) { strict, goal, breaks, breakMinutes, guard ->
        ProfileUiState(
            strictMode = strict,
            dailyGoalMinutes = goal,
            breaksEnabled = breaks,
            breakMinutes = breakMinutes,
            focusGuardEnabled = guard
        )
    }.combine(deadlineStore.deadline) { state, deadline ->
        state.copy(deadline = deadline)
    }.combine(focusSettings.bubbleEnabled) { state, bubble ->
        state.copy(bubbleEnabled = bubble)
    }.combine(focusSettings.ambienceKey) { state, ambience ->
        state.copy(ambienceKey = ambience)
    }.combine(focusSettings.maxPauses) { state, pauses ->
        state.copy(maxPauses = pauses)
    }.combine(themeStore.paletteKey) { state, themeKey ->
        state.copy(themeKey = themeKey)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState()
    )

    fun setStrictMode(enabled: Boolean) = focusSettings.setStrictMode(enabled)

    fun setBreaksEnabled(enabled: Boolean) = focusSettings.setBreaksEnabled(enabled)

    fun setBreakMinutes(minutes: Int) = focusSettings.setBreakMinutes(minutes)

    fun setMaxPauses(count: Int) = focusSettings.setMaxPauses(count)

    fun setTheme(key: String) = themeStore.setPalette(key)

    fun setAmbience(key: String) = focusSettings.setAmbienceKey(key)

    fun setFocusGuardEnabled(enabled: Boolean) = focusSettings.setFocusGuardEnabled(enabled)

    fun setBubbleEnabled(enabled: Boolean) = focusSettings.setBubbleEnabled(enabled)

    fun setDailyGoalMinutes(minutes: Int) {
        focusSettings.setDailyGoalMinutes(minutes)
        WidgetUpdater.refreshAsync(appContext)
    }
}
