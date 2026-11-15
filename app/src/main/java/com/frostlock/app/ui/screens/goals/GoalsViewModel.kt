package com.frostlock.app.ui.screens.goals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostlock.app.data.local.db.SessionDao
import com.frostlock.app.data.local.prefs.FocusGoal
import com.frostlock.app.data.local.prefs.FocusGoalStore
import com.frostlock.app.ui.util.GoalProgress
import com.frostlock.app.ui.util.goalProgress
import com.frostlock.app.ui.util.sortGoalProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class GoalsUiState(
    val goals: List<GoalProgress> = emptyList(),
    val loaded: Boolean = false
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    goalStore: FocusGoalStore,
    sessionDao: SessionDao
) : ViewModel() {

    val uiState: StateFlow<GoalsUiState> = combine(
        goalStore.goals,
        sessionDao.recentSessions(500)
    ) { goals, sessions ->
        GoalsUiState(
            goals = sortGoalProgress(goals.map { goal -> goalProgress(goal, sessions) }),
            loaded = true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GoalsUiState()
    )
}

@HiltViewModel
class GoalEditorViewModel @Inject constructor(
    private val goalStore: FocusGoalStore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val goalId: Long = savedStateHandle.get<Long>(GOAL_ID_ARG) ?: NEW_GOAL_ID

    val goal: StateFlow<FocusGoal?> = goalStore.goals
        .map { goals -> goals.firstOrNull { it.id == goalId } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val isEditing: Boolean get() = goalId != NEW_GOAL_ID

    fun save(name: String, isoDate: String, targetMinutes: Int) {
        if (isEditing) {
            goalStore.update(goalId, name, isoDate, targetMinutes)
        } else {
            goalStore.add(name, isoDate, targetMinutes)
        }
    }

    fun remove() {
        if (isEditing) goalStore.remove(goalId)
    }

    companion object {
        const val GOAL_ID_ARG = "goalId"
        const val NEW_GOAL_ID = -1L
    }
}
