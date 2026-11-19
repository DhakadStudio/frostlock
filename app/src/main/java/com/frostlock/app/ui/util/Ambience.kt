package com.frostlock.app.ui.util

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.pow

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
    Pulse("pulse", "Pulse", "Driving 124 BPM beat"),
    Surge("surge", "Surge", "Bright 132 BPM arpeggio"),
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

    /** Sawtooth in [-1, 1] at [frequency] Hz for time [seconds]. */
    fun saw(frequency: Double, seconds: Double): Double {
        val phase = frequency * seconds
        return 2.0 * (phase - floor(phase + 0.5))
    }

    /** Triangle in [-1, 1] at [frequency] Hz for time [seconds]. */
    fun triangle(frequency: Double, seconds: Double): Double =
        2.0 * abs(saw(frequency, seconds)) - 1.0

    /** Exponential decay starting at 1 and approaching 0. */
    fun decay(secondsIntoHit: Double, rate: Double): Double =
        exp(-secondsIntoHit.coerceAtLeast(0.0) * rate)

    /** Equal-tempered frequency [semitones] away from [rootHz]. */
    fun note(rootHz: Double, semitones: Int): Double =
        rootHz * 2.0.pow(semitones / 12.0)

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

/**
 * The two beat-driven beds.
 *
 * They are written as pure functions of time so the same maths can be unit
 * tested: no filters with state, no allocations, and no samples above the PCM
 * range. Every note is phase-continuous at the buffer boundary because the
 * oscillators read the absolute session time.
 */
object AmbienceSynth {

    private const val PULSE_BPM = 124.0
    private const val SURGE_BPM = 132.0

    /** A minor / F major / C major / G major, two octaves down. */
    private val PULSE_ROOTS = doubleArrayOf(55.0, 43.65, 65.41, 49.0)

    /** E minor / C major / G major / D major. */
    private val SURGE_ROOTS = doubleArrayOf(82.41, 65.41, 98.0, 73.42)

    private val PULSE_ARP = intArrayOf(0, 7, 12, 15, 19, 15, 12, 7)
    private val SURGE_ARP = intArrayOf(0, 12, 7, 15, 12, 19, 15, 7)

    /**
     * Four-on-the-floor kick, eighth-note bass and a sixteenth arpeggio.
     *
     * [white] is the player's white-noise sample for this frame; it only feeds
     * the hi-hats, so the function stays deterministic apart from noise.
     */
    fun pulse(seconds: Double, white: Double): Double {
        val beatLength = 60.0 / PULSE_BPM
        val beat = seconds / beatLength
        val beatIndex = floor(beat).toInt()
        val beatTime = (beat - beatIndex) * beatLength
        val root = PULSE_ROOTS[(beatIndex / 4) % PULSE_ROOTS.size]

        // Kick: a short downward pitch sweep with a fast decay.
        val kickFrequency = 48.0 + 110.0 * AmbienceTone.decay(beatTime, 28.0)
        val kick = AmbienceTone.sine(kickFrequency, beatTime) *
            AmbienceTone.decay(beatTime, 9.0) * 0.46

        // Bass: eighth notes, lifting an octave on the last one of each bar.
        val eighth = beat * 2.0
        val eighthIndex = floor(eighth).toInt()
        val eighthTime = (eighth - eighthIndex) * beatLength / 2.0
        val bassSemitones = if (eighthIndex % 8 == 6) 12 else 0
        val bass = AmbienceTone.triangle(
            AmbienceTone.note(root, bassSemitones),
            seconds
        ) * AmbienceTone.decay(eighthTime, 5.0) * 0.20

        // Hats on the off-beats.
        val hat = if (eighthIndex % 2 == 1) {
            white * AmbienceTone.decay(eighthTime, 90.0) * 0.05
        } else {
            0.0
        }

        // Arpeggio two octaves above the bass, on sixteenths.
        val sixteenth = beat * 4.0
        val stepIndex = floor(sixteenth).toInt()
        val stepTime = (sixteenth - stepIndex) * beatLength / 4.0
        val arpFrequency = AmbienceTone.note(
            root,
            24 + PULSE_ARP[stepIndex % PULSE_ARP.size]
        )
        val arp = AmbienceTone.sine(arpFrequency, seconds) *
            AmbienceTone.decay(stepTime, 16.0) * 0.055

        return kick + bass + hat + arp
    }

    /**
     * Brighter, faster bed: quarter-note bass, backbeat snare and a saw
     * arpeggio that pushes forward.
     */
    fun surge(seconds: Double, white: Double): Double {
        val beatLength = 60.0 / SURGE_BPM
        val beat = seconds / beatLength
        val beatIndex = floor(beat).toInt()
        val beatTime = (beat - beatIndex) * beatLength
        val root = SURGE_ROOTS[(beatIndex / 4) % SURGE_ROOTS.size]
        val beatInBar = beatIndex % 4

        val kick = if (beatInBar == 0 || beatInBar == 2) {
            val kickFrequency = 46.0 + 120.0 * AmbienceTone.decay(beatTime, 30.0)
            AmbienceTone.sine(kickFrequency, beatTime) *
                AmbienceTone.decay(beatTime, 10.0) * 0.40
        } else {
            0.0
        }

        val snare = if (beatInBar == 1 || beatInBar == 3) {
            white * AmbienceTone.decay(beatTime, 22.0) * 0.14
        } else {
            0.0
        }

        val bass = AmbienceTone.triangle(root, seconds) *
            AmbienceTone.decay(beatTime, 3.0) * 0.16

        val sixteenth = beat * 4.0
        val stepIndex = floor(sixteenth).toInt()
        val stepTime = (sixteenth - stepIndex) * beatLength / 4.0
        val arpFrequency = AmbienceTone.note(
            root,
            12 + SURGE_ARP[stepIndex % SURGE_ARP.size]
        )
        val arp = (
            AmbienceTone.saw(arpFrequency, seconds) * 0.45 +
                AmbienceTone.sine(arpFrequency, seconds) * 0.55
            ) * AmbienceTone.decay(stepTime, 10.0) * 0.11

        return kick + snare + bass + arp
    }
}
