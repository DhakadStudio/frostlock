package com.frostlock.app.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AmbienceTest {

    @Test
    fun `unknown or missing keys fall back to silence`() {
        assertEquals(AmbienceSound.Off, ambienceFromKey(null))
        assertEquals(AmbienceSound.Off, ambienceFromKey(""))
        assertEquals(AmbienceSound.Off, ambienceFromKey("ocean"))
    }

    @Test
    fun `every sound round-trips through its key`() {
        AmbienceSound.entries.forEach { sound ->
            assertEquals(sound, ambienceFromKey(sound.key))
        }
        assertEquals(AmbienceSound.entries.size, AmbienceSound.entries.map { it.key }.toSet().size)
        assertTrue(AmbienceSound.entries.all { it.label.isNotBlank() && it.detail.isNotBlank() })
    }

    @Test
    fun `timer pill only appears for audible ambience`() {
        assertNull(ambiencePillLabel("off"))
        assertNull(ambiencePillLabel(null))
        assertEquals("Rain", ambiencePillLabel("rain"))
        assertEquals("Deep focus", ambiencePillLabel("deep"))
    }

    @Test
    fun `sine stays in range and repeats each period`() {
        val samples = (0 until 1000).map { i -> AmbienceTone.sine(220.0, i / 44_100.0) }
        assertTrue(samples.all { it in -1.0000001..1.0000001 })
        assertEquals(0.0, AmbienceTone.sine(220.0, 0.0), 1e-9)
        assertEquals(
            AmbienceTone.sine(220.0, 0.25),
            AmbienceTone.sine(220.0, 0.25 + 1.0 / 220.0),
            1e-6
        )
    }

    @Test
    fun `tremolo breathes between the depth floor and full volume`() {
        val samples = (0 until 500).map { i -> AmbienceTone.tremolo(i / 100.0, 0.5, 0.4) }
        assertTrue(samples.all { it in 0.5999..1.0000001 })
        assertTrue(samples.max() > 0.99)
        assertTrue(samples.min() < 0.61)
    }

    @Test
    fun `low pass moves toward the input without overshooting`() {
        assertEquals(0.5, AmbienceTone.lowPass(0.0, 1.0, 0.5), 1e-9)
        assertEquals(0.75, AmbienceTone.lowPass(0.5, 1.0, 0.5), 1e-9)
        assertEquals(1.0, AmbienceTone.lowPass(0.5, 1.0, 1.0), 1e-9)
        assertEquals(0.5, AmbienceTone.lowPass(0.5, 1.0, 0.0), 1e-9)
        // A full alpha of 1 tracks the input exactly after one step.
        var value = 0.0
        repeat(20) { value = AmbienceTone.lowPass(value, 0.8, 0.4) }
        assertTrue(abs(value - 0.8) < 0.01)
    }

    @Test
    fun `samples are clamped to the pcm range`() {
        assertEquals(1f, AmbienceTone.clampSample(4.2), 0f)
        assertEquals(-1f, AmbienceTone.clampSample(-4.2), 0f)
        assertEquals(0.25f, AmbienceTone.clampSample(0.25), 0f)
    }

    @Test
    fun `wave shapes stay bounded`() {
        val saw = (0 until 1000).map { i -> AmbienceTone.saw(110.0, i / 44_100.0) }
        val triangle = (0 until 1000).map { i -> AmbienceTone.triangle(110.0, i / 44_100.0) }
        assertTrue(saw.all { it in -1.0000001..1.0000001 })
        assertTrue(triangle.all { it in -1.0000001..1.0000001 })
        // The triangle starts at its trough and swings through the full range.
        assertEquals(-1.0, AmbienceTone.triangle(110.0, 0.0), 1e-9)
        val cycle = (0 until 401).map { i -> AmbienceTone.triangle(110.0, i / 44_100.0) }
        assertTrue(cycle.max() > 0.99)
        assertTrue(cycle.min() < -0.99)
    }

    @Test
    fun `decay and note helpers follow their formulas`() {
        assertEquals(1.0, AmbienceTone.decay(0.0, 9.0), 1e-9)
        assertTrue(AmbienceTone.decay(0.2, 9.0) < AmbienceTone.decay(0.1, 9.0))
        assertEquals(55.0, AmbienceTone.note(55.0, 0), 1e-9)
        assertEquals(110.0, AmbienceTone.note(55.0, 12), 1e-6)
        assertEquals(440.0, AmbienceTone.note(220.0, 12), 1e-6)
    }

    @Test
    fun `energetic beds are audible and never clip`() {
        val seconds = (0 until 44_100).map { it / 44_100.0 }
        listOf(
            "pulse" to { t: Double -> AmbienceSynth.pulse(t, 0.0) },
            "surge" to { t: Double -> AmbienceSynth.surge(t, 0.0) }
        ).forEach { (name, bed) ->
            val samples = seconds.map(bed)
            assertTrue("$name clipped", samples.all { it in -1.0..1.0 })
            assertTrue("$name silent", samples.any { abs(it) > 0.1 })
            assertTrue(
                "$name flat",
                samples.maxOf { it } - samples.minOf { it } > 0.4
            )
        }
    }

    @Test
    fun `the beat is loudest on the downbeat`() {
        // The kick lands at the start of each beat, so the first 50 ms of a
        // bar must be louder than the quietest part of the same bar.
        val beatLength = 60.0 / 124.0
        val downbeat = (0 until 2_000).map { i ->
            abs(AmbienceSynth.pulse(i / 44_100.0, 0.0))
        }
        val offbeat = ((beatLength * 0.6) * 44_100).toInt()
        val quiet = (0 until 2_000).map { i ->
            abs(AmbienceSynth.pulse((offbeat + i) / 44_100.0, 0.0))
        }
        assertTrue(downbeat.max() > quiet.max())
    }
}
