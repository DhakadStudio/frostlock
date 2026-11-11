package com.focusflow.app.ui.screens.focus

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusflow.app.data.InstalledApp
import com.focusflow.app.data.InstalledAppsRepository
import com.focusflow.app.data.local.db.SessionDao
import com.focusflow.app.data.local.db.SessionEntity
import com.focusflow.app.data.local.prefs.BlockingPreferences
import com.focusflow.app.data.local.prefs.FocusSettings
import com.focusflow.app.data.local.prefs.FocusSubject
import com.focusflow.app.data.local.prefs.SubjectStore
import com.focusflow.app.data.local.prefs.effectiveSessionPackages
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

/** A Pomodoro session alternates between focus rounds and breaks. */
enum class FocusPhase { FOCUS, BREAK }

data class FocusUiState(
    val mode: FocusMode = FocusMode.POMODORO,
    val targetMinutes: Int = 25,
    val isRunning: Boolean = false,
    val elapsedSeconds: Int = 0,
    val hasStarted: Boolean = false,
    val justCompleted: Boolean = false,
    val strictMode: Boolean = false,
    val phase: FocusPhase = FocusPhase.FOCUS,
    val round: Int = 1,
    val breaksEnabled: Boolean = true,
    val breakMinutes: Int = FocusSettings.DEFAULT_BREAK_MINUTES,
    val breakFinished: Boolean = false,
    val tag: String = "",
    val subjects: List<FocusSubject> = emptyList(),
    val installedApps: List<InstalledApp> = emptyList()
) {
    /** The subject selected for this session, if it still exists. */
    val selectedSubject: FocusSubject?
        get() = subjects.firstOrNull { it.name.equals(tag, ignoreCase = true) }

    /** Apps this session will lock. Zero means the global focus list is used. */
    val subjectBlockedCount: Int
        get() = selectedSubject?.packages?.size ?: 0

    /** True while a Strict Mode session is running, so the UI can hide early exits. */
    val strictActive: Boolean
        get() = strictMode && isRunning && phase == FocusPhase.FOCUS

    val breakSeconds: Int
        get() = breakMinutes * 60

    val totalSeconds: Int
        get() = when {
            mode == FocusMode.STOPWATCH -> 0
            phase == FocusPhase.BREAK -> breakSeconds
            else -> targetMinutes * 60
        }

    val remainingSeconds: Int
        get() = if (mode == FocusMode.STOPWATCH) elapsedSeconds
        else (totalSeconds - elapsedSeconds).coerceAtLeast(0)

    val progress: Float
        get() = when {
            mode == FocusMode.STOPWATCH -> 0f
            totalSeconds <= 0 -> 0f
            else -> (elapsedSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)
        }

    /** True when this Pomodoro round will be followed by a break. */
    val breakAfterRound: Boolean
        get() = mode == FocusMode.POMODORO && breaksEnabled
}

@HiltViewModel
class FocusViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val sessionDao: SessionDao,
    private val blockingPreferences: BlockingPreferences,
    private val focusSettings: FocusSettings,
    private val subjectStore: SubjectStore,
    private val installedApps: InstalledAppsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    private var tickJob: Job? = null
    private var accumulatedMs = 0L

    /** Wall-clock time the current run segment started, or 0 while paused. */
    private var runningSince = 0L
    private var sessionSaved = false

    init {
        val savedTag = focusSettings.sessionTag.value
        if (savedTag.isNotBlank()) subjectStore.ensureSubject(savedTag)
        _uiState.update {
            it.copy(
                targetMinutes = focusSettings.lastDurationMinutes.value,
                tag = savedTag,
                breaksEnabled = focusSettings.breaksEnabled.value,
                breakMinutes = focusSettings.breakMinutes.value,
                subjects = subjectStore.subjects()
            )
        }
        observePreferences()
        restoreSession()
        viewModelScope.launch {
            val apps = installedApps.apps()
            _uiState.update { it.copy(installedApps = apps) }
        }
    }

    /** Keeps idle timers in sync when settings change on the Profile tab. */
    private fun observePreferences() {
        viewModelScope.launch {
            focusSettings.breaksEnabled.collect { enabled ->
                if (!_uiState.value.isRunning) {
                    _uiState.update { it.copy(breaksEnabled = enabled) }
                }
            }
        }
        viewModelScope.launch {
            focusSettings.breakMinutes.collect { minutes ->
                if (!_uiState.value.isRunning) {
                    _uiState.update { it.copy(breakMinutes = minutes) }
                }
            }
        }
    }

    fun selectMode(mode: FocusMode) {
        if (_uiState.value.isRunning) return
        stopTimer()
        resetInternal()
        _uiState.update {
            it.copy(
                mode = mode,
                phase = FocusPhase.FOCUS,
                round = 1,
                elapsedSeconds = 0,
                hasStarted = false,
                justCompleted = false,
                breakFinished = false
            )
        }
    }

    fun selectMinutes(minutes: Int) {
        if (_uiState.value.isRunning) return
        val clamped = minutes.coerceIn(
            FocusSettings.MIN_DURATION_MINUTES,
            FocusSettings.MAX_DURATION_MINUTES
        )
        stopTimer()
        resetInternal()
        focusSettings.setLastDurationMinutes(clamped)
        _uiState.update {
            it.copy(
                targetMinutes = clamped,
                phase = FocusPhase.FOCUS,
                round = 1,
                elapsedSeconds = 0,
                hasStarted = false,
                justCompleted = false,
                breakFinished = false
            )
        }
    }

    /** Sets (or clears, with a blank value) the subject for this session. */
    fun selectTag(tag: String) {
        // The app snapshot is fixed when the session starts, so changing the
        // subject mid-session would make the UI disagree with the blocker.
        if (_uiState.value.isRunning) return
        val cleaned = tag.trim().take(FocusSettings.MAX_TAG_LENGTH)
        if (cleaned.isNotBlank()) subjectStore.ensureSubject(cleaned)
        focusSettings.setSessionTag(cleaned)
        _uiState.update {
            it.copy(tag = cleaned, subjects = subjectStore.subjects())
        }
    }

    /** Saves the selected subject's app list; applies to the next session. */
    fun setSubjectPackages(subjectName: String, packages: Set<String>) {
        if (_uiState.value.isRunning) return
        subjectStore.setPackages(subjectName, packages)
        _uiState.update { it.copy(subjects = subjectStore.subjects()) }
    }

    fun startOrResume() {
        val state = _uiState.value
        if (state.isRunning || state.phase == FocusPhase.BREAK) return
        if (state.justCompleted) {
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
                breakFinished = false,
                strictMode = focusSettings.strictMode.value && it.mode != FocusMode.STOPWATCH
            )
        }
        persistTimerState()
        startBlockingService()
        startTicking()
    }

    fun pause() {
        val state = _uiState.value
        if (!state.isRunning || state.phase == FocusPhase.BREAK) return
        if (state.strictActive) return
        accumulatedMs += System.currentTimeMillis() - runningSince
        runningSince = 0L
        stopTimer()
        stopBlockingService()
        persistTimerState()
        val elapsed = currentElapsedSeconds()
        _uiState.update { it.copy(isRunning = false, elapsedSeconds = elapsed) }
    }

    fun stop() {
        if (_uiState.value.phase == FocusPhase.BREAK) {
            skipBreak()
            return
        }
        stopInternal(force = false)
    }

    /** Escape hatch for Strict Mode: the UI requires a deliberate press-and-hold. */
    fun endStrictSession() = stopInternal(force = true)

    /** Ends the break early and returns to the focus phase. */
    fun skipBreak() {
        if (_uiState.value.phase != FocusPhase.BREAK) return
        stopTimer()
        FocusSessionService.stop(appContext)
        blockingPreferences.clearTimerState()
        finishBreak(completed = false)
    }

    private fun stopInternal(force: Boolean) {
        val state = _uiState.value
        if (state.strictActive && !force) return
        val elapsed = if (state.justCompleted) state.totalSeconds else currentElapsedSeconds()
        val completed = state.justCompleted ||
            (state.mode != FocusMode.STOPWATCH && state.totalSeconds > 0 && elapsed >= state.totalSeconds)
        stopTimer()
        stopBlockingService()
        if (!sessionSaved && elapsed >= 5) {
            saveSession(elapsed, completed, state.round)
            sessionSaved = true
        }
        resetInternal()
        _uiState.update {
            it.copy(
                isRunning = false,
                elapsedSeconds = 0,
                hasStarted = false,
                justCompleted = false,
                phase = FocusPhase.FOCUS,
                breakFinished = false
            )
        }
    }

    fun reset() {
        stopTimer()
        stopBlockingService()
        resetInternal()
        _uiState.update {
            it.copy(
                isRunning = false,
                elapsedSeconds = 0,
                hasStarted = false,
                justCompleted = false,
                phase = FocusPhase.FOCUS,
                breakFinished = false
            )
        }
    }

    private fun tick() {
        val state = _uiState.value
        val elapsed = currentElapsedSeconds()

        if (state.phase == FocusPhase.BREAK) {
            if (elapsed >= state.breakSeconds) {
                // The foreground service posts the "break is over" alert; the UI
                // only needs to hand control back to the focus phase.
                stopTimer()
                blockingPreferences.clearTimerState()
                finishBreak(completed = true)
            } else {
                _uiState.update { it.copy(elapsedSeconds = elapsed) }
            }
            return
        }

        if (state.mode != FocusMode.STOPWATCH &&
            state.totalSeconds > 0 &&
            elapsed >= state.totalSeconds
        ) {
            stopTimer()
            // Turn off the blocking window straight away. For a Pomodoro break we
            // transition the *same* foreground service into break mode instead of
            // stopping it first, which avoided a stop/start race.
            blockingPreferences.clearSession()
            if (!sessionSaved) {
                saveSession(state.totalSeconds, completed = true, round = state.round)
                sessionSaved = true
            }
            if (state.breakAfterRound) {
                startBreak()
            } else {
                blockingPreferences.clearTimerState()
                FocusSessionService.stop(appContext)
                _uiState.update {
                    it.copy(
                        isRunning = false,
                        elapsedSeconds = state.totalSeconds,
                        hasStarted = true,
                        justCompleted = true
                    )
                }
            }
        } else {
            _uiState.update { it.copy(elapsedSeconds = elapsed) }
        }
    }

    /**
     * Starts the break countdown. Blocking stops for the break, so the user can
     * pick up their phone; the round is already saved.
     */
    private fun startBreak() {
        val state = _uiState.value
        accumulatedMs = 0L
        runningSince = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                phase = FocusPhase.BREAK,
                isRunning = true,
                elapsedSeconds = 0,
                hasStarted = true,
                justCompleted = false,
                breakFinished = false
            )
        }
        // The service is already running a focus session; this command replaces its
        // loop with the break countdown and clears the blocking heartbeat.
        persistTimerState()
        FocusSessionService.startBreak(
            appContext,
            System.currentTimeMillis() + state.breakSeconds * 1000L
        )
        startTicking()
    }

    private fun finishBreak(completed: Boolean) {
        accumulatedMs = 0L
        runningSince = 0L
        sessionSaved = false
        _uiState.update {
            it.copy(
                phase = FocusPhase.FOCUS,
                round = it.round + 1,
                isRunning = false,
                elapsedSeconds = 0,
                hasStarted = false,
                justCompleted = false,
                breakFinished = completed
            )
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
        // Snapshot the subject's apps for this session. The blocker reads this
        // snapshot so switching subjects later cannot change a running session.
        val subject = subjectStore.subjectFor(state.tag)
        blockingPreferences.saveSessionApps(
            packages = effectiveSessionPackages(
                subject,
                blockingPreferences.blockedPackages.value
            ),
            subject = subject?.name
        )
        FocusSessionService.start(
            context = appContext,
            openEnded = openEnded,
            until = until,
            focusGuard = focusSettings.focusGuardEnabled.value && !openEnded
        )
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
            strict = state.strictMode,
            phase = state.phase.name,
            round = state.round,
            breakMinutes = state.breakMinutes,
            breaksEnabled = state.breaksEnabled
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

        val phase = if (blockingPreferences.savedTimerPhase() == FocusPhase.BREAK.name) {
            FocusPhase.BREAK
        } else {
            FocusPhase.FOCUS
        }
        val round = blockingPreferences.savedTimerRound()
        val breakMinutes = blockingPreferences.savedTimerBreakMinutes()
        val breaksEnabled = blockingPreferences.savedTimerBreaksEnabled()
        val totalSeconds = blockingPreferences.savedTimerTotalSeconds()
        accumulatedMs = blockingPreferences.savedTimerAccumulatedMs()
        runningSince = blockingPreferences.savedTimerRunningSince()
        val strict = blockingPreferences.savedTimerStrict()

        val targetMinutes = if (mode == FocusMode.STOPWATCH || phase == FocusPhase.BREAK) {
            focusSettings.lastDurationMinutes.value
        } else {
            (totalSeconds / 60).coerceAtLeast(1)
        }

        val elapsed = currentElapsedSeconds()
        val isRunning = runningSince > 0L

        _uiState.update {
            it.copy(
                mode = mode,
                targetMinutes = targetMinutes,
                phase = phase,
                round = round,
                breakMinutes = breakMinutes,
                breaksEnabled = breaksEnabled,
                isRunning = isRunning,
                elapsedSeconds = elapsed,
                hasStarted = true,
                strictMode = strict,
                tag = focusSettings.sessionTag.value,
                subjects = subjectStore.subjects()
            )
        }

        if (phase == FocusPhase.BREAK) {
            val breakSeconds = breakMinutes * 60
            if (!isRunning) return

            if (elapsed >= breakSeconds) {
                runningSince = 0L
                accumulatedMs = 0L
                blockingPreferences.clearTimerState()
                finishBreak(completed = true)
                return
            }

            FocusSessionService.startBreak(
                appContext,
                System.currentTimeMillis() + (breakSeconds - elapsed) * 1000L
            )
            startTicking()
            return
        }

        if (!isRunning) return

        // A timer may have finished while the app was closed.
        if (mode != FocusMode.STOPWATCH && totalSeconds > 0 && elapsed >= totalSeconds) {
            runningSince = 0L
            accumulatedMs = totalSeconds * 1000L
            blockingPreferences.clearTimerState()
            stopBlockingService()
            saveSession(totalSeconds, completed = true, round = round)
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

    private fun saveSession(durationSeconds: Int, completed: Boolean, round: Int) {
        val state = _uiState.value
        val endedAt = System.currentTimeMillis()
        val entity = SessionEntity(
            mode = state.mode.name,
            plannedSeconds = state.totalSeconds,
            durationSeconds = durationSeconds,
            startedAt = endedAt - durationSeconds * 1000L,
            endedAt = endedAt,
            completed = completed,
            tag = state.tag
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
