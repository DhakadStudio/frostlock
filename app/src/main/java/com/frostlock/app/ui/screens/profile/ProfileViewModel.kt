package com.frostlock.app.ui.screens.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostlock.app.capability.Capability
import com.frostlock.app.capability.CapabilityRepository
import com.frostlock.app.capability.CapabilityState
import com.frostlock.app.data.local.prefs.DeadlineStore
import com.frostlock.app.data.local.prefs.FocusDeadline
import com.frostlock.app.data.local.prefs.FocusGoalStore
import com.frostlock.app.data.local.prefs.FocusSettings
import com.frostlock.app.data.local.prefs.ThemeStore
import com.frostlock.app.ui.theme.IcePalettes
import com.frostlock.app.ui.util.AmbienceSound
import com.frostlock.app.ui.util.PausePolicy
import com.frostlock.app.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Everything the You settings page shows. No blocking state, no analytics. */
data class ProfileUiState(
    val strictMode: Boolean = false,
    val dailyGoalMinutes: Int = FocusSettings.DEFAULT_GOAL_MINUTES,
    val breaksEnabled: Boolean = true,
    val breakMinutes: Int = FocusSettings.DEFAULT_BREAK_MINUTES,
    val maxPauses: Int = PausePolicy.DEFAULT,
    val remindersEnabled: Boolean = false,
    val bubbleEnabled: Boolean = false,
    val ambienceKey: String = AmbienceSound.Off.key,
    val themeKey: String = IcePalettes.Frost.key,
    val deadline: FocusDeadline? = null,
    val goalCount: Int = 0,
    val overlayAccess: CapabilityState = CapabilityState.DENIED,
    val notificationsAccess: CapabilityState = CapabilityState.DENIED
) {
    val overlayGranted: Boolean get() = overlayAccess == CapabilityState.GRANTED
    val notificationsGranted: Boolean get() = notificationsAccess == CapabilityState.GRANTED
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val focusSettings: FocusSettings,
    private val themeStore: ThemeStore,
    private val capabilities: CapabilityRepository,
    deadlineStore: DeadlineStore,
    goalStore: FocusGoalStore
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        focusSettings.strictMode,
        focusSettings.dailyGoalMinutes,
        focusSettings.breaksEnabled,
        focusSettings.breakMinutes,
        focusSettings.maxPauses
    ) { strict, goal, breaks, breakMinutes, pauses ->
        ProfileUiState(
            strictMode = strict,
            dailyGoalMinutes = goal,
            breaksEnabled = breaks,
            breakMinutes = breakMinutes,
            maxPauses = pauses
        )
    }.combine(focusSettings.focusGuardEnabled) { state, reminders ->
        state.copy(remindersEnabled = reminders)
    }.combine(focusSettings.bubbleEnabled) { state, bubble ->
        state.copy(bubbleEnabled = bubble)
    }.combine(focusSettings.ambienceKey) { state, ambience ->
        state.copy(ambienceKey = ambience)
    }.combine(themeStore.paletteKey) { state, themeKey ->
        state.copy(themeKey = themeKey)
    }.combine(deadlineStore.deadline) { state, deadline ->
        state.copy(deadline = deadline)
    }.combine(goalStore.goals) { state, goals ->
        state.copy(goalCount = goals.size)
    }.combine(capabilities.states) { state, access ->
        state.copy(
            overlayAccess = access[Capability.OVERLAY] ?: CapabilityState.UNAVAILABLE,
            notificationsAccess = access[Capability.POST_NOTIFICATIONS]
                ?: CapabilityState.UNAVAILABLE
        )
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

    fun setRemindersEnabled(enabled: Boolean) = focusSettings.setFocusGuardEnabled(enabled)

    fun setBubbleEnabled(enabled: Boolean) = focusSettings.setBubbleEnabled(enabled)

    fun setDailyGoalMinutes(minutes: Int) {
        focusSettings.setDailyGoalMinutes(minutes)
        WidgetUpdater.refreshAsync(appContext)
    }

    /** Re-reads permission state after returning from system settings. */
    fun refreshAccess() {
        capabilities.refresh()
    }
}
