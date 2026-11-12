package com.frostlock.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostlock.app.data.local.datastore.SettingsDataStore
import com.frostlock.app.data.local.db.ProfileDao
import com.frostlock.app.data.local.db.ProfileEntity
import com.frostlock.app.data.local.db.StreakDao
import com.frostlock.app.data.local.db.StreakEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class OnboardingUiState(
    val currentPage: Int = 0,
    val selectedGoals: Set<String> = emptySet(),
    val selectedTriggers: Set<String> = emptySet(),
    val reason: String = "",
    val isCompleted: Boolean = false
)

/** Order of the intro slides plus the personalisation steps. */
object OnboardingPages {
    const val WELCOME = 0
    const val FOCUS = 1
    const val CONTROL = 2
    const val SCHEDULE = 3
    const val PROGRESS = 4
    const val GOALS = 5
    const val TRIGGERS = 6
    const val REASON = 7
    const val COMMITMENT = 8
    const val COUNT = 9
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileDao: ProfileDao,
    private val streakDao: StreakDao,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    var testDispatcher: CoroutineDispatcher? = null

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onPageChanged(page: Int) {
        _uiState.update { it.copy(currentPage = page) }
    }

    fun onGoalToggled(goal: String) {
        _uiState.update { state ->
            val updatedGoals = if (state.selectedGoals.contains(goal)) {
                state.selectedGoals - goal
            } else {
                state.selectedGoals + goal
            }
            state.copy(selectedGoals = updatedGoals)
        }
    }

    fun onTriggerToggled(trigger: String) {
        _uiState.update { state ->
            val updatedTriggers = if (state.selectedTriggers.contains(trigger)) {
                state.selectedTriggers - trigger
            } else {
                state.selectedTriggers + trigger
            }
            state.copy(selectedTriggers = updatedTriggers)
        }
    }

    fun onReasonChanged(reason: String) {
        _uiState.update { it.copy(reason = reason) }
    }

    fun canProceed(): Boolean {
        val state = _uiState.value
        return when (state.currentPage) {
            OnboardingPages.GOALS -> state.selectedGoals.isNotEmpty()
            OnboardingPages.TRIGGERS -> state.selectedTriggers.isNotEmpty()
            OnboardingPages.REASON -> state.reason.trim().length in 10..300
            else -> true
        }
    }

    fun finishOnboarding(onFinished: () -> Unit) {
        val state = _uiState.value
        val ioDispatcher = testDispatcher ?: Dispatchers.IO
        // Navigation mutates the NavController/Lifecycle, which is main-thread only,
        // so the callback must be dispatched back to the main thread.
        val mainDispatcher = testDispatcher ?: Dispatchers.Main.immediate
        viewModelScope.launch(ioDispatcher) {
            val profile = ProfileEntity(
                goals = state.selectedGoals.joinToString(","),
                triggers = state.selectedTriggers.joinToString(","),
                reason = state.reason.trim()
            )
            profileDao.insertProfile(profile)
            streakDao.insertStreak(
                StreakEntity(
                    startTime = System.currentTimeMillis(),
                    endTime = null
                )
            )
            settingsDataStore.setOnboardingDone(true)
            _uiState.update { it.copy(isCompleted = true) }
            withContext(mainDispatcher) { onFinished() }
        }
    }
}
