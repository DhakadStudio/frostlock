package com.frostlock.app.ui.util

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.unit.sp

/**
 * Shared autosize configuration for every live session-timer readout.
 *
 * The timer used to be a fixed 64sp, which clips when the system font scale is
 * pushed past about 1.3 — exactly when the user needs it most. The readout now
 * shrinks smoothly inside its ring instead of clipping.
 *
 * The [StepBased] configuration is created on demand (it is cheap) and the
 * measurable parts stay pure so the 1.0 / 1.3 / 1.6 / 2.0 font-scale behaviour
 * is unit-tested.
 */
object TimerAutoSize {
    /** Lower bound chosen to fit an 8-glyph stopwatch readout at 2.0 font scale. */
    const val MIN_FONT_SP = 24f

    /** Matches the `displayTimer` design token, so default rendering is unchanged. */
    const val MAX_FONT_SP = 64f

    const val STEP_SP = 2f

    /** Average width of a tabular digit as a fraction of its em. */
    const val DIGIT_EM = 0.62f

    /** Longest readout we render: stopwatch past the one-hour mark. */
    const val MAX_CHARS = 8

    /** Width in dp available to the readout inside the dial / immersive ring. */
    const val DIAL_TEXT_WIDTH_DP = 240f

    /** Largest font scale we verify. */
    const val MAX_FONT_SCALE = 2.0f

    /** Step-based autosize used by every timer readout. */
    fun autoSize(): TextAutoSize =
        TextAutoSize.StepBased(MIN_FONT_SP.sp, MAX_FONT_SP.sp, STEP_SP.sp)

    /** Width of [chars] glyphs rendered at [fontSp] and [fontScale]. */
    fun widthDp(fontSp: Float, fontScale: Float, chars: Int = MAX_CHARS): Float =
        chars * DIGIT_EM * fontSp * fontScale

    /** True when the readout fits the dial at [fontScale]. */
    fun fits(fontScale: Float, fontSp: Float = MIN_FONT_SP, chars: Int = MAX_CHARS): Boolean =
        widthDp(fontSp, fontScale, chars) <= DIAL_TEXT_WIDTH_DP

    /** Step count of the range; StepBased throws on an invalid range. */
    fun stepCount(): Int = ((MAX_FONT_SP - MIN_FONT_SP) / STEP_SP).toInt() + 1
}
