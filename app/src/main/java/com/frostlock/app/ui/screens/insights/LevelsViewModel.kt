package com.frostlock.app.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostlock.app.insights.InsightsRepository
import com.frostlock.app.ui.util.FocusLevel
import com.frostlock.app.ui.util.focusLevelFor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class LevelsUiState(
    val level: FocusLevel = focusLevelFor(0L),
    val totalSeconds: Long = 0L
)

@HiltViewModel
class LevelsViewModel @Inject constructor(
    repository: InsightsRepository
) : ViewModel() {

    val state: StateFlow<LevelsUiState> = repository.snapshot
        .map { snapshot ->
            LevelsUiState(
                level = focusLevelFor(snapshot.totalSeconds),
                totalSeconds = snapshot.totalSeconds
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LevelsUiState()
        )
}
