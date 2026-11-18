package com.frostlock.app.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostlock.app.data.local.db.SessionEntity
import com.frostlock.app.data.local.prefs.FocusSettings
import com.frostlock.app.insights.DayFocus
import com.frostlock.app.insights.InsightsRepository
import com.frostlock.app.insights.InsightsSnapshot
import com.frostlock.app.insights.PeriodFocus
import com.frostlock.app.insights.SubjectFocus
import com.frostlock.app.ui.util.Achievement
import com.frostlock.app.ui.util.FocusLevel
import com.frostlock.app.ui.util.buildAchievements
import com.frostlock.app.ui.util.focusLevelFor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import kotlin.math.roundToInt

/** Everything the Insights dashboard renders, derived once from the snapshot. */
data class InsightsUiState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val hasHistory: Boolean = false,
    val today: DayFocus = DayFocus(LocalDate.now()),
    val goalMinutes: Int = 0,
    val currentStreak: Int = 0,
    val week: PeriodFocus? = null,
    val weekDeltaPercent: Int? = null,
    val subjects: List<SubjectFocus> = emptyList(),
    val consistency: List<DayFocus> = emptyList(),
    val consistencyActiveDays: Int = 0,
    val level: FocusLevel = focusLevelFor(0L),
    val nextBadge: Achievement? = null,
    val unlockedBadges: Int = 0,
    val totalBadges: Int = 0,
    val recent: List<SessionEntity> = emptyList(),
    val reflections: Int = 0
) {
    val goalSeconds: Long get() = if (goalMinutes <= 0) 0L else goalMinutes * 60L

    val goalProgress: Float
        get() = if (goalSeconds <= 0L) 0f else {
            (today.seconds.toFloat() / goalSeconds).coerceIn(0f, 1f)
        }

    val goalReached: Boolean get() = goalSeconds > 0L && today.seconds >= goalSeconds
}

@HiltViewModel
class InsightsViewModel @Inject constructor(
    repository: InsightsRepository,
    focusSettings: FocusSettings
) : ViewModel() {

    private val retry = kotlinx.coroutines.flow.MutableStateFlow(0)

    val state: StateFlow<InsightsUiState> = combine(
        repository.snapshot,
        focusSettings.dailyGoalMinutes,
        retry
    ) { snapshot, goalMinutes, _ ->
        toState(snapshot, goalMinutes)
    }.catch {
        emit(InsightsUiState(loading = false, error = true))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InsightsUiState()
    )

    /** Re-subscribes after a local read error. */
    fun retry() {
        retry.value += 1
    }

    private fun toState(snapshot: InsightsSnapshot, goalMinutes: Int): InsightsUiState {
        val week = snapshot.week
        val lastWeekSeconds = (0L..6L).sumOf { offset ->
            snapshot.days[week.start.minusWeeks(1).plusDays(offset)]?.seconds ?: 0L
        }
        val delta = if (lastWeekSeconds <= 0L) {
            null
        } else {
            (((week.seconds - lastWeekSeconds).toDouble() / lastWeekSeconds) * 100).roundToInt()
        }

        // Twelve Monday-first weeks ending on this week's Sunday.
        val consistencyStart = week.start.minusWeeks(11)
        val consistency = (0 until 84).map { offset ->
            val date = consistencyStart.plusDays(offset.toLong())
            snapshot.days[date] ?: DayFocus(date)
        }

        val achievements = buildAchievements(
            totalSeconds = snapshot.totalSeconds,
            totalSessions = snapshot.totalSessions,
            startTimes = snapshot.recent.map { it.startedAt },
            bestStreakDays = snapshot.bestStreak
        )

        return InsightsUiState(
            loading = false,
            hasHistory = snapshot.totalSessions > 0,
            today = snapshot.today,
            goalMinutes = goalMinutes,
            currentStreak = snapshot.currentStreak,
            week = week,
            weekDeltaPercent = delta,
            subjects = snapshot.subjects,
            consistency = consistency,
            consistencyActiveDays = consistency.count { it.hasFocus },
            level = focusLevelFor(snapshot.totalSeconds),
            nextBadge = achievements.firstOrNull { !it.unlocked },
            unlockedBadges = achievements.count { it.unlocked },
            totalBadges = achievements.size,
            recent = snapshot.recent.take(3),
            reflections = snapshot.reflections
        )
    }
}
