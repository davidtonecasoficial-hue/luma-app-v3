package com.example.player

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.runtime.Immutable
import com.example.data.model.AmbientSoundType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

@Immutable
data class AmbientMixerState(
    val soundType: AmbientSoundType = AmbientSoundType.OFF,
    val volume: Float = 0.45f,
    val isPlaying: Boolean = false
)

/**
 * Procedural real-time PCM ambient soundscape synthesizer backed by Android AudioTrack.
 * Generates organic Warm Rain, Ocean Waves, Crackling Hearth, and Focus Pink Noise without
 * bundling heavy static audio files.
 */
class AmbientSoundEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var synthJob: Job? = null
    private var audioTrack: AudioTrack? = null

    private val _state = MutableStateFlow(AmbientMixerState())
    val state: StateFlow<AmbientMixerState> = _state.asStateFlow()

    fun selectSound(type: AmbientSoundType) {
        if (type == AmbientSoundType.OFF) {
            stop()
            return
        }
        val restartNeeded = !_state.value.isPlaying
        _state.update { it.copy(soundType = type, isPlaying = true) }
        if (restartNeeded || synthJob?.isActive != true) {
            startSynthLoop()
        }
    }

    fun setVolume(volume: Float) {
        _state.update { it.copy(volume = volume.coerceIn(0f, 1f)) }
    }

    fun stop() {
        synthJob?.cancel()
        synthJob = null
        runCatching {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        }
        audioTrack = null
        _state.update { it.copy(soundType = AmbientSoundType.OFF, isPlaying = false) }
    }

    private fun startSynthLoop() {
        synthJob?.cancel()
        synthJob = scope.launch {
            val sampleRate = SAMPLE_RATE
            val bufferSamples = 2048
            val minBufBytes = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(bufferSamples * 2)

            val track = runCatching {
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
                    .setBufferSizeInBytes(minBufBytes * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            }.getOrNull()

            if (track == null) {
                _state.update { it.copy(isPlaying = false, soundType = AmbientSoundType.OFF) }
                return@launch
            }

            audioTrack = track
            val pcmBuffer = ShortArray(bufferSamples)
            val synthState = SynthState()

            try {
                track.play()
                while (isActive && _state.value.soundType != AmbientSoundType.OFF) {
                    val snapshot = _state.value
                    generateAmbientBuffer(
                        type = snapshot.soundType,
                        volume = snapshot.volume,
                        out = pcmBuffer,
                        state = synthState
                    )
                    track.write(pcmBuffer, 0, pcmBuffer.size)
                }
            } catch (_: Throwable) {
                // Graceful cleanup when audio device detaches or coroutine cancels
            } finally {
                runCatching {
                    track.pause()
                    track.flush()
                    track.release()
                }
                if (audioTrack === track) audioTrack = null
            }
        }
    }

    class SynthState(
        var b0: Double = 0.0,
        var b1: Double = 0.0,
        var b2: Double = 0.0,
        var brown: Double = 0.0,
        var lfoPhase: Double = 0.0,
        var crackleEnergy: Double = 0.0,
        var rng: Random = Random(1337)
    )

    companion object {
        const val SAMPLE_RATE = 44_100

        /**
         * Pure procedural synthesizer filling [out] with 16-bit mono PCM samples.
         * Separated cleanly from AudioTrack for deterministic JVM testing.
         */
        fun generateAmbientBuffer(
            type: AmbientSoundType,
            volume: Float,
            out: ShortArray,
            state: SynthState = SynthState()
        ) {
            val vol = volume.coerceIn(0f, 1f).toDouble()
            if (type == AmbientSoundType.OFF || vol <= 0.001) {
                out.fill(0)
                return
            }

            val lfoStep = (2.0 * PI) / (SAMPLE_RATE * 6.5) // ~6.5s organic swell cycle
            for (i in out.indices) {
                val white = state.rng.nextDouble(-1.0, 1.0)
                // Paul Kellet refined pink-noise filter
                state.b0 = 0.99765 * state.b0 + white * 0.0990460
                state.b1 = 0.96300 * state.b1 + white * 0.2965164
                state.b2 = 0.57000 * state.b2 + white * 1.0526913
                val pink = (state.b0 + state.b1 + state.b2 + white * 0.1848) * 0.14

                state.brown = (state.brown + white * 0.035).coerceIn(-1.0, 1.0) * 0.985

                val sample = when (type) {
                    AmbientSoundType.OFF -> 0.0
                    AmbientSoundType.FOCUS_PINK -> pink * 0.72
                    AmbientSoundType.WARM_RAIN -> {
                        val drop = if (state.rng.nextDouble() < 0.0018) state.rng.nextDouble(0.18, 0.45) else 0.0
                        state.crackleEnergy = (state.crackleEnergy * 0.90) + drop
                        (pink * 0.55) + (state.crackleEnergy * 0.35 * white)
                    }
                    AmbientSoundType.OCEAN_WAVES -> {
                        state.lfoPhase = (state.lfoPhase + lfoStep) % (2.0 * PI)
                        val swell = 0.32 + 0.68 * (0.5 + 0.5 * sin(state.lfoPhase))
                        (state.brown * 0.85 + pink * 0.35) * swell
                    }
                    AmbientSoundType.CRACKLING_HEARTH -> {
                        val spark = if (state.rng.nextDouble() < 0.0022) state.rng.nextDouble(0.45, 0.95) else 0.0
                        state.crackleEnergy = (state.crackleEnergy * 0.84) + spark
                        (state.brown * 0.62) + (state.crackleEnergy * white * 0.48)
                    }
                }

                val scaled = (sample * vol * 16_000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                out[i] = scaled.toShort()
            }
        }
    }
}
