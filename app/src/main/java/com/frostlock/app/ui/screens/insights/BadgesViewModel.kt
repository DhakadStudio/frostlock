package com.frostlock.app.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostlock.app.insights.InsightsRepository
import com.frostlock.app.ui.util.Achievement
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class BadgesUiState(
    val achievements: List<Achievement> = emptyList(),
    val unlockedCount: Int = 0,
    val totalSeconds: Long = 0L,
    val bestStreak: Int = 0
) {
    val next: Achievement? get() = achievements.firstOrNull { !it.unlocked }
}

@HiltViewModel
class BadgesViewModel @Inject constructor(
    repository: InsightsRepository
) : ViewModel() {

    val state: StateFlow<BadgesUiState> = repository.snapshot
        .map { snapshot ->
            val achievements = com.frostlock.app.ui.util.buildAchievements(
                totalSeconds = snapshot.totalSeconds,
                totalSessions = snapshot.totalSessions,
                startTimes = snapshot.recent.map { it.startedAt },
                bestStreakDays = snapshot.bestStreak
            )
            BadgesUiState(
                achievements = achievements,
                unlockedCount = achievements.count { it.unlocked },
                totalSeconds = snapshot.totalSeconds,
                bestStreak = snapshot.bestStreak
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BadgesUiState()
        )
}
