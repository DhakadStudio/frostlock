package com.frostlock.app.ui.screens.focus

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostlock.app.data.InstalledApp
import com.frostlock.app.data.InstalledAppsRepository
import com.frostlock.app.data.SessionPhotoStore
import com.frostlock.app.data.local.db.SessionDao
import com.frostlock.app.data.local.db.SessionEntity
import com.frostlock.app.data.local.prefs.BlockingPreferences
import com.frostlock.app.data.local.prefs.FocusSettings
import com.frostlock.app.data.local.prefs.FocusSubject
import com.frostlock.app.data.local.prefs.SubjectStore
import com.frostlock.app.data.local.prefs.effectiveSessionPackages
import com.frostlock.app.focus.FocusSessionService
import com.frostlock.app.ui.util.PauseAllowance
import com.frostlock.app.ui.util.PausePolicy
import com.frostlock.app.ui.util.computeStreak
import com.frostlock.app.ui.util.pauseAllowance as pauseAllowanceFor
import com.frostlock.app.widget.WidgetUpdater
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
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
    /** Pause cap for this session; -1 means no limit. */
    val maxPauses: Int = PausePolicy.DEFAULT,
    /** How many pauses this session has already used. */
    val pausesUsed: Int = 0,
    val tag: String = "",
    /** The one outcome this session is for, shown on the immersive timer. */
    val intent: String = "",
    val subjects: List<FocusSubject> = emptyList(),
    val installedApps: List<InstalledApp> = emptyList(),
    /** The newest saved session, shown in the post-session summary. */
    val lastSession: SessionEntity? = null,
    /** Current day streak after [lastSession] was saved. */
    val lastSessionStreak: Int = 0,
    /** The apps snapshotted for this session, shown in the immersive focus screen. */
    val sessionPackages: Set<String> = emptySet()
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

    /** How many pauses this session has left. */
    val pauseAllowance: PauseAllowance
        get() = pauseAllowanceFor(pausesUsed, maxPauses)

    /** Normal-mode pause button state; Strict Mode disables pausing separately. */
    val canPause: Boolean
        get() = !strictActive && pauseAllowance.canPause

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
                intent = focusSettings.sessionIntent.value,
                breaksEnabled = focusSettings.breaksEnabled.value,
                breakMinutes = focusSettings.breakMinutes.value,
                maxPauses = focusSettings.maxPauses.value,
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
        viewModelScope.launch {
            focusSettings.maxPauses.collect { count ->
                // Only idle timers pick up the new cap; a running or paused session
                // keeps the allowance it started with.
                if (!_uiState.value.hasStarted) {
                    _uiState.update { it.copy(maxPauses = count) }
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
                breakFinished = false,
                pausesUsed = 0
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
                breakFinished = false,
                pausesUsed = 0
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

    /** The one thing this session should finish. Optional, up to 80 chars. */
    fun setIntent(intent: String) {
        if (_uiState.value.isRunning) return
        val cleaned = intent.take(FocusSettings.MAX_INTENT_LENGTH)
        focusSettings.setSessionIntent(cleaned)
        _uiState.update { it.copy(intent = cleaned) }
    }

    private fun clearIntent() {
        focusSettings.setSessionIntent("")
        _uiState.update { it.copy(intent = "") }
    }

    /** Saves the reflection and closes the post-session summary. */
    fun updateLastSessionNote(note: String) {
        val session = _uiState.value.lastSession ?: return
        val cleaned = note.trim().take(SessionEntity.MAX_NOTE_LENGTH)
        viewModelScope.launch(Dispatchers.IO) {
            sessionDao.updateNote(session.id, cleaned)
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(lastSession = null, lastSessionStreak = 0) }
            }
        }
    }

    /** Copies a picked photo into local storage and attaches it to the session. */
    fun attachLastSessionPhoto(uri: Uri) {
        val session = _uiState.value.lastSession ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val name = SessionPhotoStore.save(appContext, session.id, uri) ?: return@launch
            sessionDao.updatePhoto(session.id, name)
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(lastSession = it.lastSession?.copy(photoPath = name))
                }
            }
        }
    }

    /** Detaches and deletes the session photo. */
    fun removeLastSessionPhoto() {
        val session = _uiState.value.lastSession ?: return
        viewModelScope.launch(Dispatchers.IO) {
            sessionDao.updatePhoto(session.id, "")
            SessionPhotoStore.delete(appContext, session.photoPath)
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(lastSession = it.lastSession?.copy(photoPath = ""))
                }
            }
        }
    }

    /** Removes an accidental session, including its reflection and photo. */
    fun deleteLastSession() {
        val session = _uiState.value.lastSession ?: return
        viewModelScope.launch(Dispatchers.IO) {
            sessionDao.deleteById(session.id)
            SessionPhotoStore.delete(appContext, session.photoPath)
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        lastSession = null,
                        lastSessionStreak = 0,
                        justCompleted = false,
                        elapsedSeconds = 0,
                        hasStarted = false
                    )
                }
                WidgetUpdater.refreshAsync(appContext)
            }
        }
    }

    /** Leaves the reflection empty; the session itself is already saved. */
    fun dismissReflection() {
        _uiState.update { it.copy(lastSession = null, lastSessionStreak = 0) }
    }

    fun startOrResume() {
        val state = _uiState.value
        if (state.isRunning || state.phase == FocusPhase.BREAK) return
        if (state.justCompleted) {
            resetInternal()
            _uiState.update { it.copy(elapsedSeconds = 0, justCompleted = false) }
        }
        // A fresh run gets a fresh pause allowance. Resuming keeps the used count.
        val freshRun = !state.hasStarted || state.elapsedSeconds == 0 || state.justCompleted
        // A new run gets its own diary entry.
        _uiState.update { it.copy(lastSession = null, lastSessionStreak = 0) }
        sessionSaved = false
        runningSince = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                isRunning = true,
                hasStarted = true,
                justCompleted = false,
                breakFinished = false,
                pausesUsed = if (freshRun) 0 else it.pausesUsed,
                maxPauses = if (freshRun) focusSettings.maxPauses.value else it.maxPauses,
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
        if (!state.pauseAllowance.canPause) return
        accumulatedMs += System.currentTimeMillis() - runningSince
        runningSince = 0L
        stopTimer()
        stopBlockingService()
        val elapsed = currentElapsedSeconds()
        _uiState.update {
            it.copy(
                isRunning = false,
                elapsedSeconds = elapsed,
                pausesUsed = it.pausesUsed + 1
            )
        }
        persistTimerState()
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

    /**
     * Ends the current run without writing a session. Used by the stop
     * confirmation so an interrupted attempt never lands in stats or the diary.
     */
    fun discardSession() {
        if (_uiState.value.phase == FocusPhase.BREAK) {
            skipBreak()
            return
        }
        stopTimer()
        stopBlockingService()
        resetInternal()
        clearIntent()
        _uiState.update {
            it.copy(
                isRunning = false,
                elapsedSeconds = 0,
                hasStarted = false,
                justCompleted = false,
                phase = FocusPhase.FOCUS,
                breakFinished = false,
                pausesUsed = 0
            )
        }
    }

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
        clearIntent()
        _uiState.update {
            it.copy(
                isRunning = false,
                elapsedSeconds = 0,
                hasStarted = false,
                justCompleted = false,
                phase = FocusPhase.FOCUS,
                breakFinished = false,
                pausesUsed = 0
            )
        }
    }

    fun reset() {
        stopTimer()
        stopBlockingService()
        resetInternal()
        clearIntent()
        _uiState.update {
            it.copy(
                isRunning = false,
                elapsedSeconds = 0,
                hasStarted = false,
                justCompleted = false,
                phase = FocusPhase.FOCUS,
                breakFinished = false,
                pausesUsed = 0
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
                breakFinished = completed,
                pausesUsed = 0
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
        val packages = effectiveSessionPackages(
            subject,
            blockingPreferences.blockedPackages.value
        )
        _uiState.update { it.copy(sessionPackages = packages) }
        blockingPreferences.saveSessionApps(
            packages = packages,
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
            breaksEnabled = state.breaksEnabled,
            pausesUsed = state.pausesUsed,
            maxPauses = state.maxPauses
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
        val pausesUsed = blockingPreferences.savedTimerPausesUsed()
        val maxPauses = blockingPreferences.savedTimerMaxPauses()
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
                pausesUsed = pausesUsed,
                maxPauses = maxPauses,
                isRunning = isRunning,
                elapsedSeconds = elapsed,
                hasStarted = true,
                strictMode = strict,
                tag = focusSettings.sessionTag.value,
                subjects = subjectStore.subjects(),
                sessionPackages = blockingPreferences.sessionPackages()
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
            val id = sessionDao.insert(entity)
            val saved = entity.copy(id = id)
            val streak = computeStreak(
                sessionDao.recentSessions(500).first().map { it.startedAt }
            )
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(lastSession = saved, lastSessionStreak = streak)
                }
            }
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
