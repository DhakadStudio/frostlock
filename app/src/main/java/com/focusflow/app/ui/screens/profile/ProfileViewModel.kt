package com.focusflow.app.ui.screens.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusflow.app.data.local.prefs.FocusSettings
import com.focusflow.app.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ProfileUiState(
    val strictMode: Boolean = false,
    val dailyGoalMinutes: Int = FocusSettings.DEFAULT_GOAL_MINUTES
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val focusSettings: FocusSettings
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        focusSettings.strictMode,
        focusSettings.dailyGoalMinutes
    ) { strict, goal ->
        ProfileUiState(strictMode = strict, dailyGoalMinutes = goal)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState()
    )

    fun setStrictMode(enabled: Boolean) = focusSettings.setStrictMode(enabled)

    fun setDailyGoalMinutes(minutes: Int) {
        focusSettings.setDailyGoalMinutes(minutes)
        WidgetUpdater.refreshAsync(appContext)
    }
}
