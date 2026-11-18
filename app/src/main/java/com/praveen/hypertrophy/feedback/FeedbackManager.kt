package com.praveen.hypertrophy.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Biomechanical audio-tactile feedback engine.
 * Synthesizes zero-latency pure tones with AudioTrack (no external asset dependencies)
 * and dispatches tailored haptic profiles for rep cadences, countdowns, and completion.
 */
class FeedbackManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * Synthesizes and plays a pure sine-wave burst via AudioTrack with soft envelope ramps
     * to eliminate audio pops and clicks.
     */
    private fun playTone(frequencyHz: Double, durationMs: Int, volume: Float = 0.8f) {
        scope.launch {
            try {
                val sampleRate = 44100
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val samples = ShortArray(numSamples)

                val attackSamples = (sampleRate * 0.005).toInt().coerceAtLeast(1)
                val releaseSamples = (sampleRate * 0.010).toInt().coerceAtLeast(1)

                val angularFreq = 2.0 * PI * frequencyHz / sampleRate
                for (i in 0 until numSamples) {
                    val envelope = when {
                        i < attackSamples -> i.toFloat() / attackSamples
                        i > numSamples - releaseSamples -> (numSamples - i).toFloat() / releaseSamples
                        else -> 1.0f
                    }
                    val sample = (sin(i * angularFreq) * Short.MAX_VALUE * volume * envelope).toInt()
                    samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                val bufferSize = samples.size * 2
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(samples, 0, samples.size)
                audioTrack.play()
                kotlinx.coroutines.delay(durationMs.toLong() + 50L)
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {
                // Audio track fallback
            }
        }
    }

    private fun vibrate(effect: VibrationEffect) {
        try {
            vibrator?.vibrate(effect)
        } catch (_: Exception) {}
    }

    /**
     * Subtle audio-haptic click at rep cadence inflection (top / bottom of rep).
     */
    fun playCadenceTick() {
        playTone(frequencyHz = 1100.0, durationMs = 25, volume = 0.35f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            @Suppress("DEPRECATION")
            vibrate(VibrationEffect.createOneShot(10, 80))
        }
    }

    /**
     * Distinct audio warning pings at 3s, 2s, 1s rest intervals remaining.
     */
    fun playCountdownTick(secondsLeft: Int) {
        val freq = when (secondsLeft) {
            3 -> 880.0
            2 -> 988.0
            1 -> 1175.0
            else -> 800.0
        }
        playTone(frequencyHz = freq, durationMs = 60, volume = 0.85f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrate(VibrationEffect.createOneShot(25, 180))
        }
    }

    /**
     * Uplifting harmonic victory chime and double-pulse haptics on rest timer completion.
     */
    fun playCompletionFanfare() {
        scope.launch {
            playTone(frequencyHz = 523.25, durationMs = 80, volume = 0.9f) // C5
            kotlinx.coroutines.delay(70)
            playTone(frequencyHz = 659.25, durationMs = 90, volume = 0.9f) // E5
            kotlinx.coroutines.delay(80)
            playTone(frequencyHz = 783.99, durationMs = 240, volume = 1.0f) // G5
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 100, 60, 200)
            val amplitudes = intArrayOf(0, 180, 0, 255)
            vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrate(VibrationEffect.createOneShot(300, 255))
        }
    }

    /**
     * Micro-haptic tick when dragging the precision scrubber across milestones.
     */
    fun playScrubTick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            @Suppress("DEPRECATION")
            vibrate(VibrationEffect.createOneShot(8, 60))
        }
    }

    companion object {
        @Volatile
        private var instance: FeedbackManager? = null

        fun getInstance(context: Context): FeedbackManager {
            return instance ?: synchronized(this) {
                instance ?: FeedbackManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
