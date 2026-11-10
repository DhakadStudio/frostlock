package com.focusflow.app.ui.screens.focus

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusflow.app.data.local.db.SessionDao
import com.focusflow.app.data.local.db.SessionEntity
import com.focusflow.app.data.local.prefs.BlockingPreferences
import com.focusflow.app.data.local.prefs.FocusSettings
import com.focusflow.app.focus.FocusSessionService
import com.focusflow.app.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class FocusMode(val label: String) {
    POMODORO("Pomodoro"),
    TIMER("Timer"),
    STOPWATCH("Stopwatch")
}

data class FocusUiState(
    val mode: FocusMode = FocusMode.POMODORO,
    val targetMinutes: Int = 25,
    val isRunning: Boolean = false,
    val elapsedSeconds: Int = 0,
    val hasStarted: Boolean = false,
    val justCompleted: Boolean = false,
    val strictMode: Boolean = false
) {
    /** True while a Strict Mode session is running, so the UI can hide early exits. */
    val strictActive: Boolean
        get() = strictMode && isRunning

    val totalSeconds: Int
        get() = if (mode == FocusMode.STOPWATCH) 0 else targetMinutes * 60

    val remainingSeconds: Int
        get() = if (mode == FocusMode.STOPWATCH) elapsedSeconds
        else (totalSeconds - elapsedSeconds).coerceAtLeast(0)

    val progress: Float
        get() = when {
            mode == FocusMode.STOPWATCH -> 0f
            totalSeconds <= 0 -> 0f
            else -> (elapsedSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)
        }
}

@HiltViewModel
class FocusViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val sessionDao: SessionDao,
    private val blockingPreferences: BlockingPreferences,
    private val focusSettings: FocusSettings
) : ViewModel() {

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    private var tickJob: Job? = null
    private var accumulatedMs = 0L

    /** Wall-clock time the current run segment started, or 0 while paused. */
    private var runningSince = 0L
    private var sessionSaved = false

    init {
        restoreSession()
    }

    fun selectMode(mode: FocusMode) {
        if (_uiState.value.isRunning) return
        stopTimer()
        resetInternal()
        _uiState.update {
            it.copy(mode = mode, elapsedSeconds = 0, hasStarted = false, justCompleted = false)
        }
    }

    fun selectMinutes(minutes: Int) {
        if (_uiState.value.isRunning) return
        stopTimer()
        resetInternal()
        _uiState.update {
            it.copy(targetMinutes = minutes, elapsedSeconds = 0, hasStarted = false, justCompleted = false)
        }
    }

    fun startOrResume() {
        if (_uiState.value.isRunning) return
        if (_uiState.value.justCompleted) {
            resetInternal()
            _uiState.update { it.copy(elapsedSeconds = 0, justCompleted = false) }
        }
        sessionSaved = false
        runningSince = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                isRunning = true,
                hasStarted = true,
                justCompleted = false,
                strictMode = focusSettings.strictMode.value && it.mode != FocusMode.STOPWATCH
            )
        }
        persistTimerState()
        startBlockingService()
        startTicking()
    }

    fun pause() {
        if (!_uiState.value.isRunning) return
        if (_uiState.value.strictActive) return
        accumulatedMs += System.currentTimeMillis() - runningSince
        runningSince = 0L
        stopTimer()
        stopBlockingService()
        persistTimerState()
        val elapsed = currentElapsedSeconds()
        _uiState.update { it.copy(isRunning = false, elapsedSeconds = elapsed) }
    }

    fun stop() = stopInternal(force = false)

    /** Escape hatch for Strict Mode: the UI requires a deliberate press-and-hold. */
    fun endStrictSession() = stopInternal(force = true)

    private fun stopInternal(force: Boolean) {
        val state = _uiState.value
        if (state.strictActive && !force) return
        val elapsed = if (state.justCompleted) state.totalSeconds else currentElapsedSeconds()
        val completed = state.justCompleted ||
            (state.mode != FocusMode.STOPWATCH && state.totalSeconds > 0 && elapsed >= state.totalSeconds)
        stopTimer()
        stopBlockingService()
        if (!sessionSaved && elapsed >= 5) {
            saveSession(elapsed, completed)
            sessionSaved = true
        }
        resetInternal()
        _uiState.update {
            it.copy(isRunning = false, elapsedSeconds = 0, hasStarted = false, justCompleted = false)
        }
    }

    fun reset() {
        stopTimer()
        stopBlockingService()
        resetInternal()
        _uiState.update {
            it.copy(isRunning = false, elapsedSeconds = 0, hasStarted = false, justCompleted = false)
        }
    }

    private fun tick() {
        val state = _uiState.value
        val elapsed = currentElapsedSeconds()
        if (state.mode != FocusMode.STOPWATCH &&
            state.totalSeconds > 0 &&
            elapsed >= state.totalSeconds
        ) {
            stopTimer()
            stopBlockingService()
            blockingPreferences.clearTimerState()
            if (!sessionSaved) {
                saveSession(state.totalSeconds, completed = true)
                sessionSaved = true
            }
            _uiState.update {
                it.copy(
                    isRunning = false,
                    elapsedSeconds = state.totalSeconds,
                    hasStarted = true,
                    justCompleted = true
                )
            }
        } else {
            _uiState.update { it.copy(elapsedSeconds = elapsed) }
        }
    }

    private fun currentElapsedSeconds(): Int {
        val runningMs = if (runningSince > 0L) System.currentTimeMillis() - runningSince else 0L
        return ((accumulatedMs + runningMs) / 1000L).toInt()
    }

    private fun startTicking() {
        stopTimer()
        tickJob = viewModelScope.launch {
            while (isActive) {
                tick()
                delay(250)
            }
        }
    }

    private fun stopTimer() {
        tickJob?.cancel()
        tickJob = null
    }

    private fun startBlockingService() {
        val state = _uiState.value
        val openEnded = state.mode == FocusMode.STOPWATCH
        val until = if (openEnded) {
            0L
        } else {
            System.currentTimeMillis() + state.remainingSeconds * 1000L
        }
        FocusSessionService.start(appContext, openEnded, until)
    }

    private fun stopBlockingService() {
        FocusSessionService.stop(appContext)
        blockingPreferences.clearSession()
    }

    private fun persistTimerState() {
        val state = _uiState.value
        blockingPreferences.saveTimerState(
            mode = state.mode.name,
            totalSeconds = state.totalSeconds,
            accumulatedMs = accumulatedMs,
            runningSince = runningSince,
            strict = state.strictMode
        )
    }

    /**
     * Rebuilds the timer after the ViewModel was destroyed (tab switch, rotation or
     * process death). Without this the timer would look stopped while the blocker
     * kept running in the background service.
     */
    private fun restoreSession() {
        val mode = blockingPreferences.savedTimerMode()?.let { saved ->
            FocusMode.entries.firstOrNull { it.name == saved }
        } ?: return

        val totalSeconds = blockingPreferences.savedTimerTotalSeconds()
        accumulatedMs = blockingPreferences.savedTimerAccumulatedMs()
        runningSince = blockingPreferences.savedTimerRunningSince()
        val strict = blockingPreferences.savedTimerStrict()

        val targetMinutes = if (mode == FocusMode.STOPWATCH) {
            0
        } else {
            (totalSeconds / 60).coerceAtLeast(1)
        }

        val elapsed = currentElapsedSeconds()
        val isRunning = runningSince > 0L

        _uiState.update {
            it.copy(
                mode = mode,
                targetMinutes = targetMinutes,
                isRunning = isRunning,
                elapsedSeconds = elapsed,
                hasStarted = true,
                strictMode = strict
            )
        }

        if (!isRunning) return

        // A timer may have finished while the app was closed.
        if (mode != FocusMode.STOPWATCH && totalSeconds > 0 && elapsed >= totalSeconds) {
            runningSince = 0L
            accumulatedMs = totalSeconds * 1000L
            blockingPreferences.clearTimerState()
            stopBlockingService()
            saveSession(totalSeconds, completed = true)
            sessionSaved = true
            _uiState.update {
                it.copy(
                    isRunning = false,
                    elapsedSeconds = totalSeconds,
                    hasStarted = true,
                    justCompleted = true
                )
            }
            return
        }

        // The service may have been killed while the app was away; make sure it runs.
        startBlockingService()
        startTicking()
    }

    private fun resetInternal() {
        accumulatedMs = 0L
        runningSince = 0L
        sessionSaved = false
        blockingPreferences.clearTimerState()
    }

    private fun saveSession(durationSeconds: Int, completed: Boolean) {
        val state = _uiState.value
        val endedAt = System.currentTimeMillis()
        val entity = SessionEntity(
            mode = state.mode.name,
            plannedSeconds = state.totalSeconds,
            durationSeconds = durationSeconds,
            startedAt = endedAt - durationSeconds * 1000L,
            endedAt = endedAt,
            completed = completed
        )
        viewModelScope.launch(Dispatchers.IO) {
            sessionDao.insert(entity)
            WidgetUpdater.refreshAsync(appContext)
        }
    }

    override fun onCleared() {
        // Only stop the local tick loop. The foreground service owns the session so
        // blocking survives navigating away from the Focus tab.
        stopTimer()
        super.onCleared()
    }
}
