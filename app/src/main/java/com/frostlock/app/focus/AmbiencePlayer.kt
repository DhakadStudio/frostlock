package com.frostlock.app.focus

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.frostlock.app.ui.util.AmbienceSound
import com.frostlock.app.ui.util.AmbienceSynth
import com.frostlock.app.ui.util.AmbienceTone
import java.util.Random

/**
 * Streams a locally generated ambience bed while a focus session runs.
 *
 * There are no audio assets: every sound is synthesised sample-by-sample at
 * 44.1 kHz mono, which keeps the APK small and the feature fully offline. The
 * player is owned by [FocusSessionService], so the sound keeps going when the
 * app is in the background and stops when the session pauses or ends.
 */
class AmbiencePlayer {

    private val lock = Any()
    private var worker: Thread? = null

    @Volatile
    private var running = false

    @Volatile
    private var generation = 0

    @Volatile
    private var sound: AmbienceSound = AmbienceSound.Off

    /** Starts [next], or stops when it is [AmbienceSound.Off]. Safe to call twice. */
    fun play(next: AmbienceSound) {
        if (next.isOff) {
            stop()
            return
        }
        synchronized(lock) {
            sound = next
            if (running) return
            running = true
            val runId = ++generation
            worker = Thread({ loop(runId) }, "frostlock-ambience").apply {
                isDaemon = true
                start()
            }
        }
    }

    /** Requests silence; the audio thread releases its track on the next chunk. */
    fun stop() {
        synchronized(lock) {
            running = false
            generation++
            sound = AmbienceSound.Off
        }
    }

    private fun loop(runId: Int) {
        val sampleRate = SAMPLE_RATE
        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) {
            finish()
            return
        }

        val bufferSamples = (minBuffer / 2).coerceAtLeast(MIN_BUFFER_SAMPLES)
        val audio = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(bufferSamples * 2)
                .build()
        }.getOrNull()

        if (audio == null) {
            finish()
            return
        }

        val random = Random(7L)
        val samples = ShortArray(bufferSamples)
        var seconds = 0.0
        var smoothed = 0.0
        var smoothedTwice = 0.0
        var brown = 0.0

        runCatching { audio.play() }
        while (running && generation == runId) {
            val active = sound
            for (i in samples.indices) {
                val white = random.nextDouble(-1.0, 1.0)
                val value = when (active) {
                    AmbienceSound.Off -> 0.0
                    AmbienceSound.Pulse -> AmbienceSynth.pulse(seconds, white)
                    AmbienceSound.Surge -> AmbienceSynth.surge(seconds, white)
                    AmbienceSound.Deep -> {
                        val drone = 0.16 * AmbienceTone.sine(110.0, seconds) +
                            0.08 * AmbienceTone.sine(165.0, seconds)
                        val breathe = AmbienceTone.tremolo(seconds, 0.07, 0.25)
                        drone * breathe + 0.015 * white
                    }
                    AmbienceSound.Rain -> {
                        smoothed = AmbienceTone.lowPass(smoothed, white, 0.55)
                        val swell = AmbienceTone.tremolo(seconds, 0.11, 0.18)
                        smoothed * 0.30 * swell
                    }
                    AmbienceSound.Wind -> {
                        smoothedTwice = AmbienceTone.lowPass(smoothedTwice, white, 0.20)
                        brown = (brown * 0.999 + smoothedTwice * 0.02).coerceIn(-1.0, 1.0)
                        val gust = AmbienceTone.tremolo(seconds, 0.045, 0.6)
                        brown * gust * 0.9
                    }
                    AmbienceSound.Snow -> {
                        val shimmer = 0.05 * AmbienceTone.sine(523.25, seconds) +
                            0.035 * AmbienceTone.sine(783.99, seconds)
                        val drift = AmbienceTone.tremolo(seconds, 0.09, 0.35)
                        shimmer * drift + 0.008 * white
                    }
                }
                samples[i] = (AmbienceTone.clampSample(value) * Short.MAX_VALUE).toInt().toShort()
                seconds += 1.0 / sampleRate
            }
            val written = runCatching { audio.write(samples, 0, samples.size) }.getOrDefault(-1)
            if (written < 0) break
        }

        runCatching { audio.pause() }
        runCatching { audio.flush() }
        runCatching { audio.release() }
        finish()
    }

    private fun finish() {
        synchronized(lock) {
            running = false
            if (worker === Thread.currentThread()) worker = null
        }
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
        const val MIN_BUFFER_SAMPLES = 2_048
    }
}
