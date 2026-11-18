package com.frostlock.app.focus

import com.frostlock.app.capability.CapabilityState
import com.frostlock.app.data.InstalledApp
import com.frostlock.app.data.local.prefs.FocusSettings
import com.frostlock.app.data.local.prefs.FocusSubject
import com.frostlock.app.ui.util.AmbienceSound
import com.frostlock.app.ui.util.PauseAllowance
import com.frostlock.app.ui.util.PausePolicy
import com.frostlock.app.ui.util.pauseAllowance as pauseAllowanceFor

/** How the timer counts. */
enum class FocusMode(val label: String) {
    POMODORO("Pomodoro"),
    TIMER("Timer"),
    STOPWATCH("Stopwatch")
}

/** A Pomodoro session alternates between focus rounds and breaks. */
enum class FocusPhase { FOCUS, BREAK }

/**
 * Everything the Focus tab and the running-session host need.
 *
 * The state lives in [FocusSessionController], not in a screen ViewModel, so a
 * session keeps ticking while the user visits another tab.
 */
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
    /** Locally synthesised ambience that plays while this session runs. */
    val ambienceKey: String = AmbienceSound.Off.key,
    val tag: String = "",
    /** The one outcome this session is for, shown on the immersive timer. */
    val intent: String = "",
    val subjects: List<FocusSubject> = emptyList(),
    val installedApps: List<InstalledApp> = emptyList(),
    /** The apps snapshotted for this session, shown on the focusing screen. */
    val sessionPackages: Set<String> = emptySet(),
    /** The apps that *would* be held if this session started now. */
    val plannedPackages: Set<String> = emptySet(),
    /** Whether accessibility is currently able to hold apps. */
    val blockingAccess: CapabilityState = CapabilityState.DENIED,
    /**
     * True while the full-screen session is on top. Minimising hides it and
     * leaves the running-session bar above the navigation bar instead.
     */
    val immersiveVisible: Boolean = false
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

    /** True when accessibility is on, so the UI may honestly say apps are held. */
    val canBlockApps: Boolean
        get() = blockingAccess == CapabilityState.GRANTED

    /** Apps that are actually held right now, or zero when access is off. */
    val heldCount: Int
        get() = if (canBlockApps) sessionPackages.size else 0
}
