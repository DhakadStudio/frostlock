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
}
