package com.example.data.media

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.runtime.Immutable
import androidx.core.net.toUri
import com.example.data.model.MediaItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class StudioExportFormat(
    val label: String,
    val shortLabel: String,
    val extension: String,
    val mimeType: String
) {
    WAV_PCM("WAV Sem Perdas (PCM 16-bit)", "WAV", "wav", "audio/wav"),
    M4A_AAC("M4A AAC Estúdio (256 kbps)", "M4A", "m4a", "audio/mp4")
}

@Immutable
data class AudioFormatDetails(
    val mimeType: String = "audio/*",
    val containerLabel: String = "Desconhecido",
    val codecName: String = "Automático",
    val sampleRateHz: Int = 44_100,
    val channelCount: Int = 2,
    val bitrateKbps: Int = 0,
    val bitsPerSample: Int = 16,
    val durationMs: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val isLossless: Boolean = false
)

@Immutable
data class StudioExportResult(
    val exportedUri: Uri,
    val displayName: String,
    val format: StudioExportFormat,
    val outputSizeBytes: Long,
    val trimmedDurationMs: Long,
    val isRingtone: Boolean
)

sealed interface RingtoneApplyResult {
    data object Success : RingtoneApplyResult
    data object NeedsWriteSettingsPermission : RingtoneApplyResult
    data class Error(val message: String) : RingtoneApplyResult
}

object LumaStudioEngine {

    suspend fun inspectAudioFormat(
        context: Context,
        track: MediaItemEntity
    ): AudioFormatDetails = withContext(Dispatchers.IO) {
        val uri = runCatching { Uri.parse(track.uri) }.getOrNull()
        var mimeType = track.mimeType.ifBlank { "audio/*" }
        var sampleRate = 44_100
        var channels = 2
        var bitrateKbps = 0
        var durationMs = track.durationMs
        var bitsPerSample = 16

        if (uri != null) {
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(context, uri, null)
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val trackMime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                    if (trackMime.startsWith("audio/")) {
                        mimeType = trackMime
                        if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        }
                        if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        }
                        if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                            bitrateKbps = (format.getInteger(MediaFormat.KEY_BIT_RATE) / 1000).coerceAtLeast(0)
                        }
                        if (format.containsKey(MediaFormat.KEY_DURATION)) {
                            val us = format.getLong(MediaFormat.KEY_DURATION)
                            if (us > 0L) durationMs = us / 1000L
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                            format.containsKey(MediaFormat.KEY_PCM_ENCODING)
                        ) {
                            bitsPerSample = when (format.getInteger(MediaFormat.KEY_PCM_ENCODING)) {
                                android.media.AudioFormat.ENCODING_PCM_8BIT -> 8
                                android.media.AudioFormat.ENCODING_PCM_FLOAT -> 32
                                android.media.AudioFormat.ENCODING_PCM_24BIT_PACKED -> 24
                                else -> 16
                            }
                        }
                        break
                    }
                }
            } catch (_: Throwable) {
                // Fallback gracefully via retriever/heuristics below
            } finally {
                runCatching { extractor.release() }
            }

            if (bitrateKbps <= 0 || durationMs <= 0L) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    if (bitrateKbps <= 0) {
                        val rawRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()
                        if (rawRate != null && rawRate > 0) bitrateKbps = rawRate / 1000
                    }
                    if (durationMs <= 0L) {
                        durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: durationMs
                    }
                    if (mimeType == "audio/*" || mimeType.isBlank()) {
                        mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: mimeType
                    }
                } catch (_: Throwable) {
                } finally {
                    runCatching { retriever.release() }
                }
            }
        }

        if (bitrateKbps <= 0 && durationMs > 0L && track.sizeBytes > 0L) {
            bitrateKbps = ((track.sizeBytes * 8.0) / durationMs.toDouble()).roundToInt().coerceAtLeast(32)
        }

        val lowerMime = mimeType.lowercase(Locale.ROOT)
        val lowerName = track.fileName.lowercase(Locale.ROOT)
        val container = when {
            lowerMime.contains("flac") || lowerName.endsWith(".flac") -> "FLAC"
            lowerMime.contains("raw") || lowerMime.contains("wav") || lowerName.endsWith(".wav") -> "WAV / PCM"
            lowerMime.contains("alac") -> "ALAC"
            lowerMime.contains("mpeg") || lowerMime.contains("mp3") || lowerName.endsWith(".mp3") -> "MP3"
            lowerMime.contains("mp4a") || lowerMime.contains("aac") || lowerName.endsWith(".m4a") || lowerName.endsWith(".aac") -> "AAC / M4A"
            lowerMime.contains("opus") || lowerName.endsWith(".opus") -> "OPUS"
            lowerMime.contains("vorbis") || lowerMime.contains("ogg") || lowerName.endsWith(".ogg") -> "OGG Vorbis"
            else -> lowerMime.removePrefix("audio/").uppercase(Locale.ROOT).ifBlank { "AUDIO" }
        }
        val codec = when {
            lowerMime.contains("flac") -> "FLAC Lossless"
            lowerMime.contains("raw") || lowerName.endsWith(".wav") -> "Linear PCM"
            lowerMime.contains("alac") -> "Apple Lossless"
            lowerMime.contains("mpeg") -> "MPEG-1 Audio Layer III"
            lowerMime.contains("mp4a") || lowerMime.contains("aac") -> "AAC-LC"
            lowerMime.contains("opus") -> "Opus Interactive"
            lowerMime.contains("vorbis") -> "Xiph Vorbis"
            else -> mimeType
        }
        val isLossless = container == "FLAC" || container == "WAV / PCM" || container == "ALAC"

        AudioFormatDetails(
            mimeType = mimeType,
            containerLabel = container,
            codecName = codec,
            sampleRateHz = sampleRate.coerceAtLeast(8_000),
            channelCount = channels.coerceIn(1, 8),
            bitrateKbps = bitrateKbps,
            bitsPerSample = bitsPerSample,
            durationMs = durationMs,
            fileSizeBytes = track.sizeBytes,
            isLossless = isLossless
        )
    }

    suspend fun extractWaveformPeaks(
        context: Context,
        uriString: String,
        durationMs: Long,
        barCount: Int = 72
    ): List<Float> = withContext(Dispatchers.IO) {
        val count = barCount.coerceIn(16, 128)
        val decodedPeaks = runCatching {
            sampleWaveformViaCodec(context, Uri.parse(uriString), durationMs, count)
        }.getOrNull()

        if (!decodedPeaks.isNullOrEmpty() && decodedPeaks.any { it > 0.12f }) {
            return@withContext normalizePeaks(decodedPeaks)
        }
        buildDeterministicFallbackWaveform(uriString, durationMs, count)
    }

    private fun sampleWaveformViaCodec(
        context: Context,
        uri: Uri,
        knownDurationMs: Long,
        barCount: Int
    ): List<Float> {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = fmt
                    break
                }
            }
            if (audioTrackIndex < 0 || audioFormat == null) return emptyList()
            extractor.selectTrack(audioTrackIndex)

            val mime = audioFormat.getString(MediaFormat.KEY_MIME).orEmpty()
            val durationUs = if (audioFormat.containsKey(MediaFormat.KEY_DURATION)) {
                audioFormat.getLong(MediaFormat.KEY_DURATION)
            } else {
                knownDurationMs * 1000L
            }.coerceAtLeast(1_000_000L)

            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(audioFormat, null, null, 0)
            decoder.start()

            val peaks = MutableList(barCount) { 0f }
            val bufferInfo = MediaCodec.BufferInfo()
            val stepUs = durationUs / barCount.toLong()

            for (barIdx in 0 until barCount) {
                val targetUs = (barIdx * stepUs).coerceIn(0L, durationUs - 10_000L)
                extractor.seekTo(targetUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                decoder.flush()

                var maxWindowRms = 0f
                var decodedFrames = 0
                var inputDone = false

                while (decodedFrames < 2) {
                    if (!inputDone) {
                        val inIdx = decoder.dequeueInputBuffer(5_000L)
                        if (inIdx >= 0) {
                            val inBuf = decoder.getInputBuffer(inIdx)
                            if (inBuf != null) {
                                val size = extractor.readSampleData(inBuf, 0)
                                if (size < 0) {
                                    decoder.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                    inputDone = true
                                } else {
                                    decoder.queueInputBuffer(inIdx, 0, size, extractor.sampleTime, 0)
                                    extractor.advance()
                                }
                            }
                        }
                    }

                    val outIdx = decoder.dequeueOutputBuffer(bufferInfo, 5_000L)
                    if (outIdx >= 0) {
                        val outBuf = decoder.getOutputBuffer(outIdx)
                        if (outBuf != null && bufferInfo.size > 0) {
                            val rms = computePcm16Rms(outBuf, bufferInfo.offset, bufferInfo.size)
                            if (rms > maxWindowRms) maxWindowRms = rms
                            decodedFrames++
                        }
                        decoder.releaseOutputBuffer(outIdx, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
                    } else if (outIdx == MediaCodec.INFO_TRY_AGAIN_LATER && inputDone) {
                        break
                    }
                }
                peaks[barIdx] = maxWindowRms
            }
            return peaks
        } finally {
            runCatching {
                decoder?.stop()
                decoder?.release()
            }
            runCatching { extractor.release() }
        }
    }

    private fun computePcm16Rms(buffer: ByteBuffer, offset: Int, size: Int): Float {
        if (size < 2) return 0f
        val dup = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        dup.position(offset)
        val sampleCount = (size / 2).coerceAtMost(512)
        val stride = max(1, (size / 2) / sampleCount)
        var sumSq = 0.0
        var counted = 0
        var i = 0
        while (i + 1 < size && counted < sampleCount) {
            val sample = dup.getShort(offset + i).toDouble() / 32768.0
            sumSq += sample * sample
            counted++
            i += stride * 2
        }
        return if (counted == 0) 0f else sqrt(sumSq / counted).toFloat()
    }

    internal fun normalizePeaks(raw: List<Float>): List<Float> {
        val peakMax = raw.maxOrNull()?.coerceAtLeast(0.001f) ?: 1f
        return raw.map { v -> (0.10f + 0.90f * (v / peakMax)).coerceIn(0.10f, 1f) }
    }

    internal fun buildDeterministicFallbackWaveform(
        seedText: String,
        durationMs: Long,
        barCount: Int
    ): List<Float> {
        val seed = (seedText.hashCode().toLong() * 31L) xor durationMs
        return List(barCount.coerceAtLeast(8)) { i ->
            val phase = i.toDouble() / barCount.toDouble()
            val wave1 = abs(sin(phase * PI * 4.5 + (seed % 97) * 0.11))
            val wave2 = abs(cos(phase * PI * 11.0 + (seed % 53) * 0.23)) * 0.55
            val bodyEnvelope = sin(phase * PI).coerceAtLeast(0.25)
            (0.14 + (wave1 * 0.52 + wave2 * 0.34) * bodyEnvelope).toFloat().coerceIn(0.12f, 0.98f)
        }
    }

    /**
     * Smooth cosine/linear hybrid fade envelope multiplier for Fade In & Fade Out.
     */
    fun computeFadeGain(
        frameIndex: Long,
        totalFrames: Long,
        fadeInFrames: Long,
        fadeOutFrames: Long
    ): Float {
        if (totalFrames <= 0L) return 1f
        var gain = 1f
        if (fadeInFrames > 0L && frameIndex < fadeInFrames) {
            val t = (frameIndex.toFloat() / fadeInFrames.toFloat()).coerceIn(0f, 1f)
            val smooth = (0.5f - 0.5f * cos(t * PI)).toFloat()
            gain = min(gain, smooth)
        }
        if (fadeOutFrames > 0L) {
            val fadeOutStart = (totalFrames - fadeOutFrames).coerceAtLeast(0L)
            if (frameIndex >= fadeOutStart) {
                val remaining = (totalFrames - 1L - frameIndex).coerceAtLeast(0L)
                val t = (remaining.toFloat() / fadeOutFrames.toFloat()).coerceIn(0f, 1f)
                val smooth = (0.5f - 0.5f * cos(t * PI)).toFloat()
                gain = min(gain, smooth)
            }
        }
        return gain.coerceIn(0f, 1f)
    }

    /**
     * Applies Fade In & Fade Out envelope in-place on a 16-bit signed LE PCM buffer.
     */
    fun applyFadeToPcm16LeBuffer(
        bytes: ByteArray,
        bytesRead: Int,
        startFrameOffset: Long,
        totalFrames: Long,
        channels: Int,
        fadeInFrames: Long,
        fadeOutFrames: Long
    ) {
        if (bytesRead <= 0 || channels <= 0 || (fadeInFrames <= 0L && fadeOutFrames <= 0L)) return
        val bytesPerFrame = channels * 2
        val frameCount = bytesRead / bytesPerFrame
        for (f in 0 until frameCount) {
            val globalFrame = startFrameOffset + f
            val gain = computeFadeGain(globalFrame, totalFrames, fadeInFrames, fadeOutFrames)
            if (gain >= 0.999f) continue
            val frameByteOffset = f * bytesPerFrame
            for (c in 0 until channels) {
                val idx = frameByteOffset + c * 2
                if (idx + 1 >= bytesRead) break
                val lo = bytes[idx].toInt() and 0xFF
                val hi = bytes[idx + 1].toInt()
                val sample = (hi shl 8) or lo
                val scaled = (sample * gain).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                bytes[idx] = (scaled and 0xFF).toByte()
                bytes[idx + 1] = ((scaled ushr 8) and 0xFF).toByte()
            }
        }
    }

    fun buildWavHeader(
        pcmDataSizeBytes: Long,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int = 16
    ): ByteArray {
        val clampedDataSize = pcmDataSizeBytes.coerceAtMost(0xFFFFFFD0L)
        val totalSize = clampedDataSize + 36L
        val byteRate = sampleRate.toLong() * channels.toLong() * (bitsPerSample / 8L)
        val blockAlign = (channels * (bitsPerSample / 8)).toShort()

        return ByteBuffer.allocate(44)
            .order(ByteOrder.LITTLE_ENDIAN)
            .apply {
                put("RIFF".toByteArray(Charsets.US_ASCII))
                putInt(totalSize.toInt())
                put("WAVE".toByteArray(Charsets.US_ASCII))
                put("fmt ".toByteArray(Charsets.US_ASCII))
                putInt(16) // PCM fmt chunk size
                putShort(1) // AudioFormat 1 = PCM
                putShort(channels.toShort())
                putInt(sampleRate)
                putInt(byteRate.toInt())
                putShort(blockAlign)
                putShort(bitsPerSample.toShort())
                put("data".toByteArray(Charsets.US_ASCII))
                putInt(clampedDataSize.toInt())
            }
            .array()
    }

    suspend fun exportTrimmedAudio(
        context: Context,
        track: MediaItemEntity,
        startMs: Long,
        endMs: Long,
        fadeInMs: Long,
        fadeOutMs: Long,
        format: StudioExportFormat,
        destinationUri: Uri? = null,
        isRingtone: Boolean = false
    ): Result<StudioExportResult> = withContext(Dispatchers.IO) {
        runCatching {
            require(endMs > startMs + 250L) { "Selecione pelo menos 0,3 segundos para exportar." }

            val cacheDir = File(context.cacheDir, "luma_studio_tmp").apply { mkdirs() }
            val rawPcmFile = File(cacheDir, "raw_trim_${System.currentTimeMillis()}.pcm")
            val encodedFile = File(cacheDir, "encoded_${System.currentTimeMillis()}.${format.extension}")

            try {
                val decodedInfo = decodeTrimRangeToPcmFile(
                    context = context,
                    sourceUri = Uri.parse(track.uri),
                    startMs = startMs,
                    endMs = endMs,
                    outputPcmFile = rawPcmFile
                )

                val totalFrames = decodedInfo.totalPcmBytes / (decodedInfo.channels * 2L)
                require(totalFrames > 0L) { "Não foi possível descodificar amostras de áudio neste intervalo." }

                val fadeInFrames = ((fadeInMs.coerceAtLeast(0L) / 1000.0) * decodedInfo.sampleRate).roundToInt().toLong()
                val fadeOutFrames = ((fadeOutMs.coerceAtLeast(0L) / 1000.0) * decodedInfo.sampleRate).roundToInt().toLong()

                when (format) {
                    StudioExportFormat.WAV_PCM -> {
                        writeWavWithFade(
                            rawPcmFile = rawPcmFile,
                            outputWavFile = encodedFile,
                            sampleRate = decodedInfo.sampleRate,
                            channels = decodedInfo.channels,
                            totalFrames = totalFrames,
                            fadeInFrames = fadeInFrames,
                            fadeOutFrames = fadeOutFrames
                        )
                    }
                    StudioExportFormat.M4A_AAC -> {
                        encodePcmToAacM4a(
                            rawPcmFile = rawPcmFile,
                            outputM4aFile = encodedFile,
                            sampleRate = decodedInfo.sampleRate,
                            channels = decodedInfo.channels,
                            totalFrames = totalFrames,
                            fadeInFrames = fadeInFrames,
                            fadeOutFrames = fadeOutFrames
                        )
                    }
                }

                val safeTitle = track.title
                    .replace(Regex("[^a-zA-Z0-9._ -]"), "")
                    .trim()
                    .ifBlank { "Luma_Edit" }
                    .take(36)
                val suffix = if (isRingtone) "Ringtone" else "Cut"
                val displayName = "${safeTitle}_${suffix}_${System.currentTimeMillis() % 10_000}.${format.extension}"

                val finalUri = if (destinationUri != null) {
                    context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                        BufferedInputStream(FileInputStream(encodedFile)).use { input ->
                            input.copyTo(out)
                        }
                    } ?: error("Não foi possível escrever no ficheiro escolhido.")
                    destinationUri
                } else {
                    publishToMediaStore(
                        context = context,
                        sourceFile = encodedFile,
                        displayName = displayName,
                        format = format,
                        isRingtone = isRingtone
                    )
                }

                StudioExportResult(
                    exportedUri = finalUri,
                    displayName = displayName,
                    format = format,
                    outputSizeBytes = encodedFile.length(),
                    trimmedDurationMs = endMs - startMs,
                    isRingtone = isRingtone
                )
            } finally {
                runCatching { rawPcmFile.delete() }
                runCatching { encodedFile.delete() }
            }
        }
    }

    private data class DecodedPcmMeta(
        val sampleRate: Int,
        val channels: Int,
        val totalPcmBytes: Long
    )

    private fun decodeTrimRangeToPcmFile(
        context: Context,
        sourceUri: Uri,
        startMs: Long,
        endMs: Long,
        outputPcmFile: File
    ): DecodedPcmMeta {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        try {
            extractor.setDataSource(context, sourceUri, null)
            var trackIndex = -1
            var inputFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    trackIndex = i
                    inputFormat = fmt
                    break
                }
            }
            require(trackIndex >= 0 && inputFormat != null) { "Nenhuma faixa de áudio encontrada no ficheiro." }
            extractor.selectTrack(trackIndex)

            val mime = inputFormat.getString(MediaFormat.KEY_MIME).orEmpty()
            var sampleRate = if (inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else 44_100
            var channels = if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 2

            val startUs = (startMs.coerceAtLeast(0L)) * 1000L
            val endUs = (endMs.coerceAtLeast(startMs + 250L)) * 1000L

            if (startUs > 0L) {
                extractor.seekTo((startUs - 50_000L).coerceAtLeast(0L), MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            }

            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(inputFormat, null, null, 0)
            decoder.start()

            val bufferInfo = MediaCodec.BufferInfo()
            var totalBytesWritten = 0L
            var inputEos = false
            var outputEos = false
            var stallCycles = 0

            BufferedOutputStream(FileOutputStream(outputPcmFile)).use { pcmOut ->
                while (!outputEos && stallCycles < 300) {
                    if (!inputEos) {
                        val inIdx = decoder.dequeueInputBuffer(10_000L)
                        if (inIdx >= 0) {
                            val inBuf = decoder.getInputBuffer(inIdx)
                            if (inBuf != null) {
                                val sampleSize = extractor.readSampleData(inBuf, 0)
                                val sampleTime = extractor.sampleTime
                                if (sampleSize < 0 || (endUs > 0L && sampleTime > endUs + 200_000L)) {
                                    decoder.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                    inputEos = true
                                } else {
                                    decoder.queueInputBuffer(inIdx, 0, sampleSize, sampleTime, 0)
                                    extractor.advance()
                                }
                            }
                        }
                    }

                    val outIdx = decoder.dequeueOutputBuffer(bufferInfo, 10_000L)
                    when {
                        outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val newFmt = decoder.outputFormat
                            if (newFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                                sampleRate = newFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            }
                            if (newFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                                channels = newFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            }
                        }
                        outIdx >= 0 -> {
                            stallCycles = 0
                            val outBuf = decoder.getOutputBuffer(outIdx)
                            val pts = bufferInfo.presentationTimeUs
                            if (outBuf != null && bufferInfo.size > 0 && pts >= startUs - 10_000L && pts <= endUs + 20_000L) {
                                val chunk = ByteArray(bufferInfo.size)
                                outBuf.position(bufferInfo.offset)
                                outBuf.get(chunk, 0, bufferInfo.size)
                                pcmOut.write(chunk)
                                totalBytesWritten += chunk.size
                            }
                            decoder.releaseOutputBuffer(outIdx, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0 || pts > endUs + 20_000L) {
                                outputEos = true
                            }
                        }
                        else -> {
                            if (inputEos) stallCycles++
                        }
                    }
                }
            }
            return DecodedPcmMeta(sampleRate = sampleRate, channels = channels.coerceIn(1, 2), totalPcmBytes = totalBytesWritten)
        } finally {
            runCatching {
                decoder?.stop()
                decoder?.release()
            }
            runCatching { extractor.release() }
        }
    }

    private fun writeWavWithFade(
        rawPcmFile: File,
        outputWavFile: File,
        sampleRate: Int,
        channels: Int,
        totalFrames: Long,
        fadeInFrames: Long,
        fadeOutFrames: Long
    ) {
        val pcmSize = rawPcmFile.length()
        val header = buildWavHeader(pcmSize, sampleRate, channels, 16)
        val bytesPerFrame = channels * 2
        val chunkSize = bytesPerFrame * 2048
        val buffer = ByteArray(chunkSize)
        var currentFrame = 0L

        BufferedOutputStream(FileOutputStream(outputWavFile)).use { out ->
            out.write(header)
            BufferedInputStream(FileInputStream(rawPcmFile)).use { input ->
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    applyFadeToPcm16LeBuffer(
                        bytes = buffer,
                        bytesRead = read,
                        startFrameOffset = currentFrame,
                        totalFrames = totalFrames,
                        channels = channels,
                        fadeInFrames = fadeInFrames,
                        fadeOutFrames = fadeOutFrames
                    )
                    out.write(buffer, 0, read)
                    currentFrame += (read / bytesPerFrame)
                }
            }
        }
    }

    private fun encodePcmToAacM4a(
        rawPcmFile: File,
        outputM4aFile: File,
        sampleRate: Int,
        channels: Int,
        totalFrames: Long,
        fadeInFrames: Long,
        fadeOutFrames: Long
    ) {
        val aacMime = MediaFormat.MIMETYPE_AUDIO_AAC
        val format = MediaFormat.createAudioFormat(aacMime, sampleRate, channels).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 256_000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 32_768)
        }

        val encoder = MediaCodec.createEncoderByType(aacMime)
        val muxer = MediaMuxer(outputM4aFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxerStarted = false
        var muxerTrackIndex = -1

        try {
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            val bytesPerFrame = channels * 2
            val chunk = ByteArray(bytesPerFrame * 1024)
            val bufferInfo = MediaCodec.BufferInfo()
            var currentFrame = 0L
            var inputDone = false
            var outputDone = false

            BufferedInputStream(FileInputStream(rawPcmFile)).use { input ->
                while (!outputDone) {
                    if (!inputDone) {
                        val inIdx = encoder.dequeueInputBuffer(10_000L)
                        if (inIdx >= 0) {
                            val inBuf = encoder.getInputBuffer(inIdx)
                            if (inBuf != null) {
                                inBuf.clear()
                                val maxToRead = min(chunk.size, inBuf.capacity())
                                val read = input.read(chunk, 0, maxToRead)
                                if (read <= 0) {
                                    val eosUs = (currentFrame * 1_000_000L) / sampleRate
                                    encoder.queueInputBuffer(inIdx, 0, 0, eosUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                    inputDone = true
                                } else {
                                    applyFadeToPcm16LeBuffer(
                                        bytes = chunk,
                                        bytesRead = read,
                                        startFrameOffset = currentFrame,
                                        totalFrames = totalFrames,
                                        channels = channels,
                                        fadeInFrames = fadeInFrames,
                                        fadeOutFrames = fadeOutFrames
                                    )
                                    inBuf.put(chunk, 0, read)
                                    val ptsUs = (currentFrame * 1_000_000L) / sampleRate
                                    encoder.queueInputBuffer(inIdx, 0, read, ptsUs, 0)
                                    currentFrame += (read / bytesPerFrame)
                                }
                            }
                        }
                    }

                    val outIdx = encoder.dequeueOutputBuffer(bufferInfo, 10_000L)
                    when {
                        outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            muxerTrackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                        outIdx >= 0 -> {
                            val encodedBuf = encoder.getOutputBuffer(outIdx)
                            if (encodedBuf != null && bufferInfo.size > 0 &&
                                (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 &&
                                muxerStarted
                            ) {
                                encodedBuf.position(bufferInfo.offset)
                                encodedBuf.limit(bufferInfo.offset + bufferInfo.size)
                                muxer.writeSampleData(muxerTrackIndex, encodedBuf, bufferInfo)
                            }
                            encoder.releaseOutputBuffer(outIdx, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                outputDone = true
                            }
                        }
                    }
                }
            }
        } finally {
            runCatching {
                encoder.stop()
                encoder.release()
            }
            runCatching {
                if (muxerStarted) muxer.stop()
                muxer.release()
            }
        }
    }

    private fun publishToMediaStore(
        context: Context,
        sourceFile: File,
        displayName: String,
        format: StudioExportFormat,
        isRingtone: Boolean
    ): Uri {
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val relativeDir = if (isRingtone) {
                "${Environment.DIRECTORY_RINGTONES}/Luma Studio"
            } else {
                "${Environment.DIRECTORY_MUSIC}/Luma Studio"
            }
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Audio.Media.MIME_TYPE, format.mimeType)
                put(MediaStore.Audio.Media.RELATIVE_PATH, relativeDir)
                put(MediaStore.Audio.Media.IS_RINGTONE, if (isRingtone) 1 else 0)
                put(MediaStore.Audio.Media.IS_MUSIC, if (isRingtone) 0 else 1)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val inserted = resolver.insert(collection, values)
                ?: error("Não foi possível criar ficheiro no MediaStore.")
            try {
                resolver.openOutputStream(inserted)?.use { out ->
                    BufferedInputStream(FileInputStream(sourceFile)).use { input ->
                        input.copyTo(out)
                    }
                } ?: error("Não foi possível gravar áudio no destino.")
                val finalizeValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.IS_PENDING, 0)
                }
                resolver.update(inserted, finalizeValues, null, null)
                return inserted
            } catch (t: Throwable) {
                runCatching { resolver.delete(inserted, null, null) }
                throw t
            }
        } else {
            val subDir = if (isRingtone) "LumaStudioRingtones" else "LumaStudioExports"
            val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
            val exportDir = File(baseDir, subDir).apply { mkdirs() }
            val dest = File(exportDir, displayName)
            sourceFile.copyTo(dest, overwrite = true)
            return dest.toUri()
        }
    }

    fun applySystemRingtone(context: Context, ringtoneUri: Uri): RingtoneApplyResult {
        if (!Settings.System.canWrite(context)) {
            return RingtoneApplyResult.NeedsWriteSettingsPermission
        }
        return runCatching {
            RingtoneManager.setActualDefaultRingtoneUri(
                context,
                RingtoneManager.TYPE_RINGTONE,
                ringtoneUri
            )
            RingtoneApplyResult.Success
        }.getOrElse { err ->
            RingtoneApplyResult.Error(err.message ?: "Não foi possível definir o toque padrão.")
        }
    }

    fun createWriteSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}
