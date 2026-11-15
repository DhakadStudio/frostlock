package com.frostlock.app.ui.util

/**
 * Locally synthesised focus ambience. Nothing is downloaded or bundled: the
 * focus service generates each sound with an [android.media.AudioTrack], so the
 * feature works offline and needs no storage or network permission.
 */
enum class AmbienceSound(
    val key: String,
    val label: String,
    val detail: String
) {
    Off("off", "Off", "Silent focus"),
    Deep("deep", "Deep focus", "Low glacier drone"),
    Rain("rain", "Rain", "Soft steady rainfall"),
    Wind("wind", "Wind", "Slow polar gusts"),
    Snow("snow", "Snow", "Quiet high shimmer");

    val isOff: Boolean get() = this == Off
}

/** Unknown or missing preferences fall back to silence. */
fun ambienceFromKey(key: String?): AmbienceSound =
    AmbienceSound.entries.firstOrNull { it.key == key } ?: AmbienceSound.Off

/** Short label for the timer pill, or null when ambience is off. */
fun ambiencePillLabel(key: String?): String? =
    ambienceFromKey(key).takeUnless { it.isOff }?.label

/**
 * Small, pure signal helpers used by the ambience generator. Keeping them here
 * makes the audio maths unit-testable without touching Android's audio stack.
 */
object AmbienceTone {

    /** Sine wave in [-1, 1] at [frequency] Hz for time [seconds]. */
    fun sine(frequency: Double, seconds: Double): Double =
        kotlin.math.sin(2.0 * Math.PI * frequency * seconds)

    /** Multiplier in [1 - depth, 1] that breathes at [rateHz]. */
    fun tremolo(seconds: Double, rateHz: Double, depth: Double): Double {
        val safeDepth = depth.coerceIn(0.0, 1.0)
        return 1.0 - safeDepth + safeDepth * (0.5 + 0.5 * sine(rateHz, seconds))
    }

    /** One-pole low-pass; [alpha] of 1 passes the input through unchanged. */
    fun lowPass(previous: Double, input: Double, alpha: Double): Double {
        val safeAlpha = alpha.coerceIn(0.0, 1.0)
        return previous + safeAlpha * (input - previous)
    }

    /** Clamps a generated sample to the PCM range. */
    fun clampSample(value: Double): Float = value.coerceIn(-1.0, 1.0).toFloat()
}
