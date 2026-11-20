package com.example.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class AudioSpectrumSnapshot(
    val bands: List<Float> = List(16) { 0f },
    val waveform: List<Float> = List(32) { 0f },
    val peakEnergy: Float = 0f
)

object AudioPipelineBus {
    private val _spectrumSnapshot = MutableStateFlow(AudioSpectrumSnapshot())
    val spectrumSnapshot: StateFlow<AudioSpectrumSnapshot> = _spectrumSnapshot.asStateFlow()

    @Volatile
    var stereoBalance: Float = 0f

    @Volatile
    var spectrumEnabled: Boolean = true

    fun publishSpectrum(bands: List<Float>, waveform: List<Float>, peak: Float) {
        _spectrumSnapshot.value = AudioSpectrumSnapshot(
            bands = bands,
            waveform = waveform,
            peakEnergy = peak.coerceIn(0f, 1f)
        )
    }

    fun resetSpectrum() {
        _spectrumSnapshot.value = AudioSpectrumSnapshot()
    }
}

/**
 * Real-time ExoPlayer BaseAudioProcessor implementing L/R Stereo Balance (-1f..1f).
 */
@OptIn(UnstableApi::class)
class StereoBalanceAudioProcessor : BaseAudioProcessor() {

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val balance = AudioPipelineBus.stereoBalance.coerceIn(-1f, 1f)
        val out = replaceOutputBuffer(remaining)

        if (abs(balance) < 0.01f || inputAudioFormat.channelCount != 2) {
            out.put(inputBuffer)
            out.flip()
            return
        }

        // Balance law: -1.0 attenuates Right to 0, +1.0 attenuates Left to 0
        val leftGain = if (balance > 0f) (1f - balance).coerceIn(0f, 1f) else 1f
        val rightGain = if (balance < 0f) (1f + balance).coerceIn(0f, 1f) else 1f

        val readOrder = inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        out.order(ByteOrder.LITTLE_ENDIAN)

        while (readOrder.remaining() >= 4) {
            val l = (readOrder.short.toFloat() * leftGain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            val r = (readOrder.short.toFloat() * rightGain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            out.putShort(l.toShort())
            out.putShort(r.toShort())
        }
        while (readOrder.hasRemaining()) {
            out.put(readOrder.get())
        }
        out.flip()
    }
}

/**
 * Zero-permission real-time audio spectrum and waveform analyzer inside ExoPlayer's audio sink.
 * Uses Goertzel single-bin DFT filters across 16 perceptual bands (40 Hz -> 14 kHz).
 */
@OptIn(UnstableApi::class)
class SpectrumAudioProcessor : BaseAudioProcessor() {

    private val bandFreqsHz = floatArrayOf(
        40f, 65f, 100f, 160f, 250f, 400f, 630f, 1000f,
        1500f, 2200f, 3200f, 4500f, 6500f, 9000f, 11500f, 14000f
    )
    private val sampleWindowSize = 256
    private val monoRing = FloatArray(sampleWindowSize)
    private var ringCursor = 0
    private var frameCounter = 0
    private val smoothedBands = FloatArray(16)
    private val waveformSamples = FloatArray(32)
    private val coeffs = FloatArray(16)
    private var lastSampleRate = 0

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        if (inputAudioFormat.sampleRate != lastSampleRate && inputAudioFormat.sampleRate > 0) {
            lastSampleRate = inputAudioFormat.sampleRate
            for (b in bandFreqsHz.indices) {
                val freq = bandFreqsHz[b].coerceAtMost(lastSampleRate * 0.45f)
                val omega = (2.0 * PI * freq / lastSampleRate).toFloat()
                coeffs[b] = 2f * cos(omega)
            }
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val dup = inputBuffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
        val out = replaceOutputBuffer(remaining)
        out.put(inputBuffer)
        out.flip()

        if (!AudioPipelineBus.spectrumEnabled || inputAudioFormat.channelCount <= 0) return

        val channels = inputAudioFormat.channelCount
        val bytesPerFrame = channels * 2
        val totalFrames = remaining / bytesPerFrame
        if (totalFrames <= 0) return

        val step = (totalFrames / 64).coerceAtLeast(1)
        var i = 0
        while (i < totalFrames) {
            val byteOffset = i * bytesPerFrame
            if (byteOffset + 1 < dup.limit()) {
                val s0 = dup.getShort(dup.position() + byteOffset).toFloat() / 32768f
                val mono = if (channels >= 2 && byteOffset + 3 < dup.limit()) {
                    val s1 = dup.getShort(dup.position() + byteOffset + 2).toFloat() / 32768f
                    (s0 + s1) * 0.5f
                } else {
                    s0
                }
                monoRing[ringCursor] = mono
                ringCursor = (ringCursor + 1) % sampleWindowSize
            }
            i += step
        }

        frameCounter++
        if (frameCounter % 2 == 0) {
            computeAndPublish()
        }
    }

    private fun computeAndPublish() {
        var peak = 0f
        for (w in 0 until 32) {
            val idx = (ringCursor + w * (sampleWindowSize / 32)) % sampleWindowSize
            val sample = monoRing[idx]
            waveformSamples[w] = sample
            val absVal = abs(sample)
            if (absVal > peak) peak = absVal
        }

        val bandsOut = ArrayList<Float>(16)
        for (b in 0 until 16) {
            val coeff = coeffs[b]
            var q0: Float
            var q1 = 0f
            var q2 = 0f
            for (n in 0 until sampleWindowSize) {
                val sample = monoRing[(ringCursor + n) % sampleWindowSize]
                // Hann-like window taper
                val win = 0.5f * (1f - cos((2.0 * PI * n / (sampleWindowSize - 1)).toFloat()))
                q0 = coeff * q1 - q2 + sample * win
                q2 = q1
                q1 = q0
            }
            val magSq = (q1 * q1 + q2 * q2 - q1 * q2 * coeff).coerceAtLeast(0f)
            val norm = (sqrt(magSq) / (sampleWindowSize * 0.18f)).coerceIn(0f, 1f)
            val prev = smoothedBands[b]
            val smooth = if (norm > prev) prev * 0.35f + norm * 0.65f else prev * 0.72f + norm * 0.28f
            smoothedBands[b] = smooth
            bandsOut.add(smooth)
        }

        AudioPipelineBus.publishSpectrum(
            bands = bandsOut,
            waveform = waveformSamples.toList(),
            peak = (peak * 1.4f).coerceIn(0f, 1f)
        )
    }

    override fun onReset() {
        super.onReset()
        ringCursor = 0
        monoRing.fill(0f)
        smoothedBands.fill(0f)
    }
}
