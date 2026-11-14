package com.frostlock.app.ui.util

/**
 * Label shown by the floating focus bubble. Stopwatch sessions have no target
 * time, so they show a plain "Focus" label instead of a countdown.
 */
fun focusBubbleLabel(openEnded: Boolean, remainingMs: Long): String {
    val snowflake = "\u2744"
    if (openEnded) return "$snowflake Focus"
    val seconds = (remainingMs.coerceAtLeast(0L) / 1000L).toInt()
    return "$snowflake ${formatClock(seconds)}"
}
