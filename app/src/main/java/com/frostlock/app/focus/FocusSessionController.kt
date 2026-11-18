package com.frostlock.app.focus

import android.content.Context
import com.frostlock.app.capability.Capability
import com.frostlock.app.capability.CapabilityRepository
import com.frostlock.app.capability.CapabilityState
import com.frostlock.app.data.InstalledAppsRepository
import com.frostlock.app.data.local.db.SessionDao
import com.frostlock.app.data.local.db.SessionEntity
import com.frostlock.app.data.local.prefs.BlockingPreferences
import com.frostlock.app.data.local.prefs.FocusSettings
import com.frostlock.app.data.local.prefs.SubjectStore
import com.frostlock.app.data.local.prefs.effectiveSessionPackages
import com.frostlock.app.ui.util.computeStreak
import com.frostlock.app.widget.WidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Owns the focus timer for the whole process.
 *
 * A screen ViewModel would be destroyed on every tab switch, which is exactly
 * when a session must keep running. This singleton keeps the state machine,
 * the foreground service and the persisted timer in sync, and exposes a
 * [pendingSummaryId] event so the shell can open the summary after a save.
 */
@Singleton
class FocusSessionController @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val sessionDao: SessionDao,
    private val blockingPreferences: BlockingPreferences,
    private val focusSettings: FocusSettings,
    private val subjectStore: SubjectStore,
    private val installedApps: InstalledAppsRepository,
    private val capabilities: CapabilityRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    /** The session that just ended, waiting for the shell to open its summary. */
    private val _pendingSummaryId = MutableStateFlow<Long?>(null)
    val pendingSummaryId: StateFlow<Long?> = _pendingSummaryId.asStateFlow()

    /** The newest saved session, for the quiet "Last session" card. */
    val lastSavedSession: StateFlow<SessionEntity?> = sessionDao.recentSessions(1)
        .map { it.firstOrNull() }
        .stateIn(scope, SharingStarted.Eagerly, null)

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
                ambienceKey = focusSettings.ambienceKey.value,
                strictMode = focusSettings.strictMode.value,
                subjects = subjectStore.subjects(),
                blockingAccess = capabilities.state(Capability.APP_BLOCKING),
                plannedPackages = plannedPackagesFor(savedTag)
            )
        }
        observePreferences()
        observeCapabilities()
        restoreSession()
        scope.launch {
            val apps = installedApps.apps()
            _uiState.update { it.copy(installedApps = apps) }
        }
    }

    // --- Configuration ---------------------------------------------------------

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
            it.copy(
                tag = cleaned,
                subjects = subjectStore.subjects(),
                plannedPackages = plannedPackagesFor(cleaned)
            )
        }
    }

    /** Saves the selected subject's app list; applies to the next session. */
    fun setSubjectPackages(subjectName: String, packages: Set<String>) {
        if (_uiState.value.isRunning) return
        subjectStore.setPackages(subjectName, packages)
        _uiState.update {
            it.copy(
                subjects = subjectStore.subjects(),
                plannedPackages = plannedPackagesFor(it.tag)
            )
        }
    }

    /** The one thing this session should finish. Optional, up to 80 chars. */
    fun setIntent(intent: String) {
        if (_uiState.value.isRunning) return
        val cleaned = intent.take(FocusSettings.MAX_INTENT_LENGTH)
        focusSettings.setSessionIntent(cleaned)
        _uiState.update { it.copy(intent = cleaned) }
    }

    fun setStrictMode(enabled: Boolean) = focusSettings.setStrictMode(enabled)

    fun setMaxPauses(count: Int) = focusSettings.setMaxPauses(count)

    fun setAmbience(key: String) = focusSettings.setAmbienceKey(key)

    fun refreshAccess() = capabilities.refresh()

    // --- Session control -------------------------------------------------------

    fun startOrResume() {
        val state = _uiState.value
        if (state.isRunning || state.phase == FocusPhase.BREAK) return
        if (state.justCompleted) {
            resetInternal()
            _uiState.update { it.copy(elapsedSeconds = 0, justCompleted = false) }
        }
        // A fresh run gets a fresh pause allowance. Resuming keeps the used count.
        val freshRun = !state.hasStarted || state.elapsedSeconds == 0 || state.justCompleted
        runningSince = System.currentTimeMillis()
        sessionSaved = false
        _uiState.update {
            it.copy(
                isRunning = true,
                hasStarted = true,
                justCompleted = false,
                breakFinished = false,
                immersiveVisible = true,
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
     * Ends the current run without writing a session. The intention is kept so
     * the next attempt starts from the same sentence instead of a blank field.
     */
    fun discardSession() {
        if (_uiState.value.phase == FocusPhase.BREAK) {
            skipBreak()
            return
        }
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
                breakFinished = false,
                pausesUsed = 0,
                immersiveVisible = false
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
                pausesUsed = 0,
                immersiveVisible = false
            )
        }
    }

    // --- Immersive presentation ------------------------------------------------

    /** Re-opens the full-screen session after the user minimised it. */
    fun showImmersive() {
        if (!_uiState.value.hasStarted) return
        _uiState.update { it.copy(immersiveVisible = true) }
    }

    /** Leaves the session running and shows the running-session bar instead. */
    fun minimize() {
        if (_uiState.value.strictActive) return
        _uiState.update { it.copy(immersiveVisible = false) }
    }

    /** Called by the shell once the summary route has been opened. */
    fun consumeSummary() {
        _pendingSummaryId.value = null
    }

    // --- Internals -------------------------------------------------------------

    private fun stopInternal(force: Boolean) {
        val state = _uiState.value
        if (state.strictActive && !force) return
        val elapsed = if (state.justCompleted) state.totalSeconds else currentElapsedSeconds()
        val completed = state.justCompleted ||
            (state.mode != FocusMode.STOPWATCH &&
                state.totalSeconds > 0 &&
                elapsed >= state.totalSeconds)
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
                pausesUsed = 0,
                immersiveVisible = false
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
                        justCompleted = true,
                        immersiveVisible = false
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
                breakFinished = false,
                immersiveVisible = false
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
                pausesUsed = 0,
                immersiveVisible = false
            )
        }
    }

    private fun currentElapsedSeconds(): Int {
        val runningMs = if (runningSince > 0L) System.currentTimeMillis() - runningSince else 0L
        return ((accumulatedMs + runningMs) / 1000L).toInt()
    }

    private fun startTicking() {
        stopTimer()
        tickJob = scope.launch {
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
     * Rebuilds the timer after the process was destroyed. Without this the timer
     * would look stopped while the blocker kept running in the service.
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
                sessionPackages = blockingPreferences.sessionPackages(),
                // Reopening the app lands on the tab with the session bar rather
                // than surprising the user with a full-screen timer.
                immersiveVisible = false
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

    private fun clearIntent() {
        focusSettings.setSessionIntent("")
        _uiState.update { it.copy(intent = "") }
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
            tag = state.tag,
            intent = state.intent
        )
        scope.launch(Dispatchers.IO) {
            val id = sessionDao.insert(entity)
            WidgetUpdater.refreshAsync(appContext)
            // The summary route loads the row itself, so it works even if the
            // shell is recreated before it opens.
            _pendingSummaryId.value = id
        }
    }

    private fun observePreferences() {
        scope.launch {
            focusSettings.breaksEnabled.collect { enabled ->
                if (!_uiState.value.isRunning) {
                    _uiState.update { it.copy(breaksEnabled = enabled) }
                }
            }
        }
        scope.launch {
            focusSettings.breakMinutes.collect { minutes ->
                if (!_uiState.value.isRunning) {
                    _uiState.update { it.copy(breakMinutes = minutes) }
                }
            }
        }
        scope.launch {
            focusSettings.maxPauses.collect { count ->
                // Only idle timers pick up the new cap; a running or paused session
                // keeps the allowance it started with.
                if (!_uiState.value.hasStarted) {
                    _uiState.update { it.copy(maxPauses = count) }
                }
            }
        }
        scope.launch {
            focusSettings.strictMode.collect { enabled ->
                if (!_uiState.value.hasStarted) {
                    _uiState.update { it.copy(strictMode = enabled) }
                }
            }
        }
        scope.launch {
            focusSettings.ambienceKey.collect { key ->
                _uiState.update { it.copy(ambienceKey = key) }
            }
        }
        scope.launch {
            blockingPreferences.blockedPackages.collect { packages ->
                _uiState.update {
                    it.copy(plannedPackages = plannedPackagesFor(it.tag, packages))
                }
            }
        }
    }

    private fun observeCapabilities() {
        scope.launch {
            capabilities.states.collect { states ->
                _uiState.update {
                    it.copy(
                        blockingAccess = states[Capability.APP_BLOCKING]
                            ?: CapabilityState.UNAVAILABLE
                    )
                }
            }
        }
    }

    private fun plannedPackagesFor(
        tag: String,
        fallback: Set<String> = blockingPreferences.blockedPackages.value
    ): Set<String> = effectiveSessionPackages(subjectStore.subjectFor(tag), fallback)
}
