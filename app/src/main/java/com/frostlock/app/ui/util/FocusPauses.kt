package com.frostlock.app.ui.util

/** Limits and labels for the per-session pause allowance. */
object PausePolicy {
    /** No cap; the session can be paused as often as needed. */
    const val UNLIMITED = -1

    /** Highest finite allowance offered in the UI. */
    const val MAX = 5

    /** Two pauses keeps a session flexible without making it a break. */
    const val DEFAULT = 2
}

/**
 * How many pauses a session has left. Strict Mode disables pausing separately;
 * this only covers the normal-mode allowance.
 */
data class PauseAllowance(
    val used: Int,
    val max: Int
) {
    val unlimited: Boolean
        get() = max < 0

    /** Remaining pauses, or null when the allowance is unlimited. */
    val remaining: Int?
        get() = if (unlimited) null else (max - used).coerceAtLeast(0)

    val canPause: Boolean
        get() = unlimited || used < max

    /** Compact label for the timer pills. */
    val usageLabel: String
        get() = when {
            unlimited -> "No pause limit"
            max == 0 -> "Pauses off"
            used <= 0 -> "Pauses $max"
            else -> "Pauses $used/$max"
        }
}

/** Normalises stored values so a corrupt preference cannot lock the timer out. */
fun pauseAllowance(used: Int, max: Int): PauseAllowance {
    val safeUsed = used.coerceAtLeast(0)
    val safeMax = if (max < 0) PausePolicy.UNLIMITED else max.coerceIn(0, PausePolicy.MAX)
    return PauseAllowance(used = safeUsed, max = safeMax)
}

/** Label for the pause-allowance chips on the Profile screen. */
fun pauseOptionLabel(max: Int): String = when {
    max < 0 -> "No limit"
    max == 0 -> "Off"
    else -> max.toString()
}
