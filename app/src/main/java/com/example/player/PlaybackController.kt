package com.example.player

import android.content.ComponentName
import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.local.SettingsRepository
import com.example.data.media.MediaRepository
import com.example.data.model.CustomAudioPreset
import com.example.data.model.MediaItemEntity
import com.example.data.model.MediaType
import com.example.data.model.ReverbEffectPreset
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.math.ln

enum class RepeatModeState {
    OFF,
    ALL,
    ONE
}

enum class SleepTimerMode {
    OFF,
    TIMED,
    END_OF_TRACK
}

data class EqualizerBandUi(
    val index: Short,
    val centerFreqHz: Int,
    val levelMillibels: Short
)

data class EqualizerUiState(
    val isAvailable: Boolean = false,
    val enabled: Boolean = false,
    val minLevel: Short = -1500,
    val maxLevel: Short = 1500,
    val presets: List<String> = emptyList(),
    val selectedPresetIndex: Int = -1,
    val bands: List<EqualizerBandUi> = emptyList(),
    val studioTenBandsMb: List<Int> = List(10) { 0 },
    val bassBoostSupported: Boolean = false,
    val bassBoostStrength: Int = 0,
    val virtualizerSupported: Boolean = false,
    val virtualizerStrength: Int = 0,
    val reverbPreset: ReverbEffectPreset = ReverbEffectPreset.NONE,
    val volumeNormalizationEnabled: Boolean = false,
    val stereoBalance: Float = 0f
)

data class PlaybackUiState(
    val currentTrack: MediaItemEntity? = null,
    val queue: List<MediaItemEntity> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatModeState = RepeatModeState.OFF,
    val playbackSpeed: Float = 1.0f,
    val isMiniPlayerDismissed: Boolean = false,
    val sleepTimerMode: SleepTimerMode = SleepTimerMode.OFF,
    val sleepRemainingMs: Long = 0L,
    val playbackError: String? = null,
    val errorMediaId: Long? = null,
    val equalizer: EqualizerUiState = EqualizerUiState()
)

class PlaybackController(
    private val context: Context,
    private val repository: MediaRepository,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    val spectrumSnapshot: StateFlow<AudioSpectrumSnapshot> = AudioPipelineBus.spectrumSnapshot

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private var positionTickerJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var crossfadeJob: Job? = null
    private var lastRecordedMediaId: Long = -1L
    private var lastSessionSaveAt = 0L
    private var pendingPlayRequest: Triple<List<MediaItemEntity>, Int, Long>? = null

    companion object {
        val STUDIO_BAND_FREQS_HZ = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) {
                startPositionTicker()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            syncFromPlayer()
            if (playbackState == Player.STATE_ENDED && _uiState.value.sleepTimerMode == SleepTimerMode.END_OF_TRACK) {
                pause()
                cancelSleepTimer()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO &&
                _uiState.value.sleepTimerMode == SleepTimerMode.END_OF_TRACK
            ) {
                mediaController?.pause()
                cancelSleepTimer()
            }
            syncFromPlayer()
            val track = _uiState.value.currentTrack
            if (track != null && track.id != lastRecordedMediaId) {
                lastRecordedMediaId = track.id
                scope.launch(Dispatchers.IO) {
                    val paused = settingsRepository.settingsFlow.first().pauseHistoryTracking
                    repository.recordPlaybackStarted(track.id, pauseHistory = paused)
                }
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _uiState.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            val mapped = when (repeatMode) {
                Player.REPEAT_MODE_ALL -> RepeatModeState.ALL
                Player.REPEAT_MODE_ONE -> RepeatModeState.ONE
                else -> RepeatModeState.OFF
            }
            _uiState.update { it.copy(repeatMode = mapped) }
        }

        override fun onPlayerError(error: PlaybackException) {
            val failedTrack = _uiState.value.currentTrack
            val friendlyMessage = when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
                PlaybackException.ERROR_CODE_IO_NO_PERMISSION ->
                    "Ficheiro indisponível ou movido do dispositivo."
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ->
                    "Formato ou codec não suportado neste dispositivo."
                else ->
                    "Não foi possível reproduzir este ficheiro (pode estar danificado)."
            }
            _uiState.update {
                it.copy(
                    isPlaying = false,
                    playbackError = friendlyMessage,
                    errorMediaId = failedTrack?.id
                )
            }
            if (failedTrack != null &&
                (error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ||
                    error.errorCode == PlaybackException.ERROR_CODE_IO_NO_PERMISSION)
            ) {
                scope.launch(Dispatchers.IO) {
                    repository.markUnavailable(failedTrack.id)
                }
            }
        }
    }

    fun connect() {
        if (controllerFuture != null) return
        val sessionToken = SessionToken(context, ComponentName(context, LumaPlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                runCatching {
                    val controller = future.get()
                    mediaController = controller
                    controller.addListener(playerListener)
                    scope.launch {
                        val settings = settingsRepository.settingsFlow.first()
                        controller.setPlaybackSpeed(settings.playbackSpeed)
                        controller.sendCustomCommand(
                            androidx.media3.session.SessionCommand(LumaPlaybackService.ACTION_SET_GAPLESS, android.os.Bundle.EMPTY),
                            android.os.Bundle().apply { putBoolean(LumaPlaybackService.EXTRA_GAPLESS_ENABLED, settings.gaplessPlaybackEnabled) }
                        )
                        AudioPipelineBus.stereoBalance = settings.stereoBalance
                        AudioPipelineBus.spectrumEnabled = settings.visualizerEnabled
                        _uiState.update {
                            it.copy(
                                playbackSpeed = settings.playbackSpeed,
                                equalizer = it.equalizer.copy(
                                    virtualizerStrength = settings.virtualizerStrength,
                                    reverbPreset = settings.reverbPreset,
                                    volumeNormalizationEnabled = settings.volumeNormalizationEnabled,
                                    stereoBalance = settings.stereoBalance
                                )
                            )
                        }
                        val pending = pendingPlayRequest
                        if (pending != null) {
                            pendingPlayRequest = null
                            playQueue(pending.first, pending.second, pending.third)
                        } else {
                            restoreLastSessionIfIdle()
                        }
                    }
                    initEqualizer()
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    private suspend fun restoreLastSessionIfIdle() {
        val controller = mediaController ?: return
        if (controller.mediaItemCount > 0) {
            syncFromPlayer()
            return
        }
        val savedQueue = repository.loadSavedQueue()
        if (savedQueue.isEmpty()) return
        val settings = settingsRepository.settingsFlow.first()
        val startIdx = savedQueue.indexOfFirst { it.id == settings.lastMediaId }.coerceAtLeast(0)
        val mediaItems = savedQueue.map { toMedia3Item(it) }
        controller.setMediaItems(mediaItems, startIdx, settings.lastPositionMs.coerceAtLeast(0L))
        controller.prepare()
        controller.playWhenReady = false
        _uiState.update {
            it.copy(
                queue = savedQueue,
                currentIndex = startIdx,
                currentTrack = savedQueue.getOrNull(startIdx),
                positionMs = settings.lastPositionMs.coerceAtLeast(0L),
                durationMs = savedQueue.getOrNull(startIdx)?.durationMs ?: 0L,
                isMiniPlayerDismissed = true
            )
        }
    }

    fun playQueue(tracks: List<MediaItemEntity>, startIndex: Int = 0, startPositionMs: Long = 0L) {
        val audioTracks = tracks.filter { it.mediaType == MediaType.AUDIO }
        if (audioTracks.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, audioTracks.lastIndex)
        val controller = mediaController
        if (controller == null) {
            pendingPlayRequest = Triple(audioTracks, safeIndex, startPositionMs)
            connect()
            return
        }

        clearError()
        val mediaItems = audioTracks.map { toMedia3Item(it) }
        controller.setMediaItems(mediaItems, safeIndex, startPositionMs)
        controller.prepare()
        controller.play()

        val selected = audioTracks[safeIndex]
        lastRecordedMediaId = selected.id
        _uiState.update {
            it.copy(
                queue = audioTracks,
                currentIndex = safeIndex,
                currentTrack = selected,
                durationMs = selected.durationMs,
                positionMs = startPositionMs,
                isMiniPlayerDismissed = false
            )
        }

        scope.launch(Dispatchers.IO) {
            val paused = settingsRepository.settingsFlow.first().pauseHistoryTracking
            repository.saveQueue(audioTracks.map { it.id })
            repository.recordPlaybackStarted(selected.id, pauseHistory = paused)
        }
        applyCrossfadeEnvelopeIfEnabled()
        startPositionTicker()
    }

    fun playSingle(track: MediaItemEntity, startPositionMs: Long = 0L) {
        playQueue(listOf(track), 0, startPositionMs)
    }

    fun addToQueue(track: MediaItemEntity) {
        if (track.mediaType != MediaType.AUDIO) return
        val currentQueue = _uiState.value.queue
        if (currentQueue.isEmpty()) {
            playSingle(track)
            return
        }
        val controller = mediaController ?: return
        val updated = currentQueue + track
        controller.addMediaItem(toMedia3Item(track))
        _uiState.update { it.copy(queue = updated, isMiniPlayerDismissed = false) }
        scope.launch(Dispatchers.IO) {
            repository.saveQueue(updated.map { it.id })
        }
    }

    fun playNext(track: MediaItemEntity) {
        if (track.mediaType != MediaType.AUDIO) return
        val currentQueue = _uiState.value.queue
        if (currentQueue.isEmpty()) {
            playSingle(track)
            return
        }
        val controller = mediaController ?: return
        val insertIdx = (_uiState.value.currentIndex + 1).coerceIn(0, currentQueue.size)
        val updated = currentQueue.toMutableList().apply { add(insertIdx, track) }
        controller.addMediaItem(insertIdx, toMedia3Item(track))
        _uiState.update { it.copy(queue = updated, isMiniPlayerDismissed = false) }
        scope.launch(Dispatchers.IO) {
            repository.saveQueue(updated.map { it.id })
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val currentQueue = _uiState.value.queue
        if (fromIndex !in currentQueue.indices || toIndex !in currentQueue.indices || fromIndex == toIndex) return
        val controller = mediaController ?: return
        controller.moveMediaItem(fromIndex, toIndex)
        val mutable = currentQueue.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        val newCurrentIdx = controller.currentMediaItemIndex
        _uiState.update {
            it.copy(
                queue = mutable,
                currentIndex = newCurrentIdx,
                currentTrack = mutable.getOrNull(newCurrentIdx)
            )
        }
        scope.launch(Dispatchers.IO) {
            repository.saveQueue(mutable.map { it.id })
        }
    }

    fun removeFromQueue(index: Int) {
        val currentQueue = _uiState.value.queue
        if (index !in currentQueue.indices) return
        val controller = mediaController ?: return
        controller.removeMediaItem(index)
        val mutable = currentQueue.toMutableList().apply { removeAt(index) }
        val newIdx = controller.currentMediaItemIndex.coerceIn(-1, mutable.lastIndex)
        _uiState.update {
            it.copy(
                queue = mutable,
                currentIndex = newIdx,
                currentTrack = mutable.getOrNull(newIdx),
                isMiniPlayerDismissed = mutable.isEmpty()
            )
        }
        scope.launch(Dispatchers.IO) {
            repository.saveQueue(mutable.map { it.id })
        }
    }

    fun clearQueue() {
        mediaController?.clearMediaItems()
        mediaController?.stop()
        _uiState.update {
            it.copy(
                queue = emptyList(),
                currentIndex = -1,
                currentTrack = null,
                isPlaying = false,
                positionMs = 0L,
                durationMs = 0L,
                isMiniPlayerDismissed = true
            )
        }
        scope.launch(Dispatchers.IO) {
            repository.saveQueue(emptyList())
        }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        clearError()
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.playbackState == Player.STATE_IDLE && controller.mediaItemCount > 0) {
                controller.prepare()
            }
            controller.play()
            _uiState.update { it.copy(isMiniPlayerDismissed = false) }
        }
    }

    fun pause() {
        mediaController?.pause()
    }

    fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        val clamped = positionMs.coerceIn(0L, (_uiState.value.durationMs).coerceAtLeast(0L))
        controller.seekTo(clamped)
        _uiState.update { it.copy(positionMs = clamped) }
    }

    fun skipToNext() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
            applyCrossfadeEnvelopeIfEnabled()
        } else if (_uiState.value.queue.isNotEmpty()) {
            controller.seekTo(0, 0L)
        }
    }

    fun skipToPrevious() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 3000L) {
            controller.seekTo(0L)
        } else if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
            applyCrossfadeEnvelopeIfEnabled()
        } else {
            controller.seekTo(0L)
        }
    }

    fun jumpToQueueIndex(index: Int) {
        val controller = mediaController ?: return
        if (index !in _uiState.value.queue.indices) return
        controller.seekTo(index, 0L)
        controller.prepare()
        controller.play()
        _uiState.update { it.copy(isMiniPlayerDismissed = false) }
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        controller.shuffleModeEnabled = !controller.shuffleModeEnabled
    }

    fun cycleRepeatMode() {
        val controller = mediaController ?: return
        val next = when (_uiState.value.repeatMode) {
            RepeatModeState.OFF -> Player.REPEAT_MODE_ALL
            RepeatModeState.ALL -> Player.REPEAT_MODE_ONE
            RepeatModeState.ONE -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = next
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        mediaController?.setPlaybackSpeed(clamped)
        _uiState.update { it.copy(playbackSpeed = clamped) }
        scope.launch(Dispatchers.IO) {
            settingsRepository.setPlaybackSpeed(clamped)
        }
    }

    fun dismissMiniPlayer() {
        mediaController?.pause()
        _uiState.update { it.copy(isMiniPlayerDismissed = true) }
    }

    fun restoreMiniPlayer() {
        if (_uiState.value.currentTrack != null) {
            _uiState.update { it.copy(isMiniPlayerDismissed = false) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(playbackError = null, errorMediaId = null) }
    }

    fun setSleepTimerMinutes(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _uiState.update { it.copy(sleepTimerMode = SleepTimerMode.OFF, sleepRemainingMs = 0L) }
            return
        }
        val totalMs = minutes * 60_000L
        _uiState.update { it.copy(sleepTimerMode = SleepTimerMode.TIMED, sleepRemainingMs = totalMs) }
        sleepTimerJob = scope.launch {
            var remaining = totalMs
            while (remaining > 0L && isActive) {
                delay(1000L)
                remaining = (remaining - 1000L).coerceAtLeast(0L)
                _uiState.update { it.copy(sleepRemainingMs = remaining) }
            }
            if (isActive) {
                pause()
                _uiState.update { it.copy(sleepTimerMode = SleepTimerMode.OFF, sleepRemainingMs = 0L) }
            }
        }
    }

    fun setSleepTimerEndOfTrack() {
        sleepTimerJob?.cancel()
        _uiState.update { it.copy(sleepTimerMode = SleepTimerMode.END_OF_TRACK, sleepRemainingMs = 0L) }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _uiState.update { it.copy(sleepTimerMode = SleepTimerMode.OFF, sleepRemainingMs = 0L) }
    }

    fun initEqualizer() {
        val sessionId = LumaPlaybackService.currentAudioSessionId
        if (sessionId == 0) {
            _uiState.update { it.copy(equalizer = it.equalizer.copy(isAvailable = false)) }
            return
        }
        runCatching {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            presetReverb?.release()
            loudnessEnhancer?.release()

            val eq = Equalizer(0, sessionId)
            equalizer = eq
            val numBands = eq.numberOfBands
            val range = eq.bandLevelRange
            val bands = (0 until numBands).map { i ->
                val band = i.toShort()
                EqualizerBandUi(
                    index = band,
                    centerFreqHz = eq.getCenterFreq(band) / 1000,
                    levelMillibels = eq.getBandLevel(band)
                )
            }
            val presets = (0 until eq.numberOfPresets).map { p -> eq.getPresetName(p.toShort()) }

            var bassSupported = false
            var bassStrength = 0
            runCatching {
                val bb = BassBoost(0, sessionId)
                bassBoost = bb
                bassSupported = bb.strengthSupported
                bassStrength = bb.roundedStrength.toInt().coerceIn(0, 1000)
            }

            var virtSupported = false
            var virtStrength = _uiState.value.equalizer.virtualizerStrength
            runCatching {
                val v = Virtualizer(0, sessionId)
                virtualizer = v
                virtSupported = v.strengthSupported
                if (virtSupported && virtStrength > 0) {
                    v.setStrength(virtStrength.toShort())
                    v.enabled = true
                }
            }

            runCatching {
                val rev = PresetReverb(0, sessionId)
                presetReverb = rev
                applyReverbInternal(_uiState.value.equalizer.reverbPreset)
            }

            runCatching {
                val le = LoudnessEnhancer(sessionId)
                loudnessEnhancer = le
                val normEnabled = _uiState.value.equalizer.volumeNormalizationEnabled
                le.setTargetGain(if (normEnabled) 450 else 0)
                le.enabled = normEnabled
            }

            val studioTen = interpolateHardwareToStudio10(bands)
            _uiState.update {
                it.copy(
                    equalizer = it.equalizer.copy(
                        isAvailable = true,
                        enabled = eq.enabled,
                        minLevel = range[0],
                        maxLevel = range[1],
                        presets = presets,
                        selectedPresetIndex = eq.currentPreset.toInt(),
                        bands = bands,
                        studioTenBandsMb = studioTen,
                        bassBoostSupported = bassSupported,
                        bassBoostStrength = bassStrength,
                        virtualizerSupported = virtSupported,
                        virtualizerStrength = virtStrength
                    )
                )
            }
        }.onFailure {
            _uiState.update { it.copy(equalizer = it.equalizer.copy(isAvailable = false)) }
        }
    }

    private fun interpolateHardwareToStudio10(bands: List<EqualizerBandUi>): List<Int> {
        if (bands.isEmpty()) return List(10) { 0 }
        return STUDIO_BAND_FREQS_HZ.map { targetFreq ->
            val nearest = bands.minByOrNull { abs(ln(it.centerFreqHz.coerceAtLeast(1).toDouble()) - ln(targetFreq.toDouble())) }
            nearest?.levelMillibels?.toInt() ?: 0
        }
    }

    fun setStudioBandLevel(studioIndex: Int, levelMillibels: Int) {
        val currentStudio = _uiState.value.equalizer.studioTenBandsMb.toMutableList()
        if (studioIndex !in currentStudio.indices) return
        val minLv = _uiState.value.equalizer.minLevel.toInt()
        val maxLv = _uiState.value.equalizer.maxLevel.toInt()
        val clamped = levelMillibels.coerceIn(minLv, maxLv)
        currentStudio[studioIndex] = clamped

        val eq = equalizer
        if (eq != null) {
            runCatching {
                eq.enabled = true
                val hwBands = eq.numberOfBands.toInt()
                for (hw in 0 until hwBands) {
                    val hwFreq = (eq.getCenterFreq(hw.toShort()) / 1000).coerceAtLeast(1)
                    // Weighted average of nearby studio bands in log frequency space
                    var weightSum = 0.0
                    var valueSum = 0.0
                    STUDIO_BAND_FREQS_HZ.forEachIndexed { sIdx, sFreq ->
                        val dist = abs(ln(hwFreq.toDouble()) - ln(sFreq.toDouble()))
                        val w = kotlin.math.exp(-dist * 1.4)
                        weightSum += w
                        valueSum += w * currentStudio[sIdx]
                    }
                    val mappedLevel = if (weightSum > 0.0) (valueSum / weightSum).toInt().coerceIn(minLv, maxLv) else clamped
                    eq.setBandLevel(hw.toShort(), mappedLevel.toShort())
                }
                val refreshedBands = (0 until hwBands).map { i ->
                    val b = i.toShort()
                    EqualizerBandUi(b, eq.getCenterFreq(b) / 1000, eq.getBandLevel(b))
                }
                _uiState.update {
                    it.copy(
                        equalizer = it.equalizer.copy(
                            enabled = true,
                            selectedPresetIndex = -1,
                            bands = refreshedBands,
                            studioTenBandsMb = currentStudio
                        )
                    )
                }
            }
        } else {
            _uiState.update {
                it.copy(
                    equalizer = it.equalizer.copy(
                        selectedPresetIndex = -1,
                        studioTenBandsMb = currentStudio
                    )
                )
            }
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        val eq = equalizer
        if (eq == null) {
            _uiState.update { it.copy(equalizer = it.equalizer.copy(enabled = enabled)) }
            return
        }
        runCatching {
            eq.enabled = enabled
            bassBoost?.let { bb ->
                if (bb.strengthSupported) {
                    bb.enabled = enabled && _uiState.value.equalizer.bassBoostStrength > 0
                }
            }
            virtualizer?.let { v ->
                if (v.strengthSupported) {
                    v.enabled = enabled && _uiState.value.equalizer.virtualizerStrength > 0
                }
            }
            _uiState.update { it.copy(equalizer = it.equalizer.copy(enabled = eq.enabled)) }
        }
    }

    fun setBassBoostStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        val bb = bassBoost
        if (bb != null && bb.strengthSupported) {
            runCatching {
                bb.setStrength(clamped.toShort())
                bb.enabled = clamped > 0 && (equalizer?.enabled ?: true)
            }
        }
        _uiState.update {
            it.copy(equalizer = it.equalizer.copy(bassBoostStrength = clamped))
        }
    }

    fun setVirtualizerStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        val v = virtualizer
        if (v != null && v.strengthSupported) {
            runCatching {
                v.setStrength(clamped.toShort())
                v.enabled = clamped > 0
            }
        }
        _uiState.update {
            it.copy(equalizer = it.equalizer.copy(virtualizerStrength = clamped))
        }
        scope.launch(Dispatchers.IO) { settingsRepository.setVirtualizerStrength(clamped) }
    }

    fun setReverbPreset(preset: ReverbEffectPreset) {
        applyReverbInternal(preset)
        _uiState.update { it.copy(equalizer = it.equalizer.copy(reverbPreset = preset)) }
        scope.launch(Dispatchers.IO) { settingsRepository.setReverbPreset(preset) }
    }

    private fun applyReverbInternal(preset: ReverbEffectPreset) {
        val rev = presetReverb ?: return
        runCatching {
            val code = when (preset) {
                ReverbEffectPreset.NONE -> PresetReverb.PRESET_NONE
                ReverbEffectPreset.SMALL_ROOM -> PresetReverb.PRESET_SMALLROOM
                ReverbEffectPreset.MEDIUM_HALL -> PresetReverb.PRESET_MEDIUMHALL
                ReverbEffectPreset.LARGE_HALL -> PresetReverb.PRESET_LARGEHALL
                ReverbEffectPreset.PLATE -> PresetReverb.PRESET_PLATE
            }
            rev.preset = code
            rev.enabled = preset != ReverbEffectPreset.NONE
        }
    }

    fun setVolumeNormalizationEnabled(enabled: Boolean) {
        runCatching {
            loudnessEnhancer?.apply {
                setTargetGain(if (enabled) 450 else 0)
                this.enabled = enabled
            }
        }
        _uiState.update {
            it.copy(equalizer = it.equalizer.copy(volumeNormalizationEnabled = enabled))
        }
        scope.launch(Dispatchers.IO) { settingsRepository.setVolumeNormalizationEnabled(enabled) }
    }

    fun setStereoBalance(balance: Float) {
        val clamped = balance.coerceIn(-1f, 1f)
        AudioPipelineBus.stereoBalance = clamped
        _uiState.update {
            it.copy(equalizer = it.equalizer.copy(stereoBalance = clamped))
        }
        scope.launch(Dispatchers.IO) { settingsRepository.setStereoBalance(clamped) }
    }

    fun setGaplessPlaybackEnabled(enabled: Boolean) {
        mediaController?.sendCustomCommand(
            androidx.media3.session.SessionCommand(LumaPlaybackService.ACTION_SET_GAPLESS, android.os.Bundle.EMPTY),
            android.os.Bundle().apply { putBoolean(LumaPlaybackService.EXTRA_GAPLESS_ENABLED, enabled) }
        )
        scope.launch(Dispatchers.IO) { settingsRepository.setGaplessPlaybackEnabled(enabled) }
    }

    fun setVisualizerPipelineEnabled(enabled: Boolean) {
        AudioPipelineBus.spectrumEnabled = enabled
        if (!enabled) AudioPipelineBus.resetSpectrum()
    }

    fun applyCustomAudioPreset(preset: CustomAudioPreset) {
        setEqualizerEnabled(true)
        preset.bandLevelsMb.take(10).forEachIndexed { index, lv ->
            setStudioBandLevel(index, lv)
        }
        setBassBoostStrength(preset.bassStrength)
        setVirtualizerStrength(preset.virtualizerStrength)
        setReverbPreset(preset.reverbPreset)
        setVolumeNormalizationEnabled(preset.loudnessGainMb > 0)
        setStereoBalance(preset.stereoBalance)
    }

    fun selectEqualizerPreset(index: Int) {
        val eq = equalizer ?: return
        runCatching {
            eq.usePreset(index.toShort())
            val bands = (0 until eq.numberOfBands).map { i ->
                val band = i.toShort()
                EqualizerBandUi(band, eq.getCenterFreq(band) / 1000, eq.getBandLevel(band))
            }
            val studioTen = interpolateHardwareToStudio10(bands)
            _uiState.update {
                it.copy(
                    equalizer = it.equalizer.copy(
                        enabled = true,
                        selectedPresetIndex = index,
                        bands = bands,
                        studioTenBandsMb = studioTen
                    )
                )
            }
            eq.enabled = true
        }
    }

    fun setEqualizerBandLevel(bandIndex: Short, levelMillibels: Short) {
        val eq = equalizer ?: return
        runCatching {
            eq.enabled = true
            eq.setBandLevel(bandIndex, levelMillibels)
            val bands = (0 until eq.numberOfBands).map { i ->
                val band = i.toShort()
                EqualizerBandUi(band, eq.getCenterFreq(band) / 1000, eq.getBandLevel(band))
            }
            val studioTen = interpolateHardwareToStudio10(bands)
            _uiState.update {
                it.copy(
                    equalizer = it.equalizer.copy(
                        enabled = true,
                        selectedPresetIndex = -1,
                        bands = bands,
                        studioTenBandsMb = studioTen
                    )
                )
            }
        }
    }

    fun refreshCurrentTrackMetadata(updated: MediaItemEntity) {
        _uiState.update { state ->
            val newQueue = state.queue.map { if (it.id == updated.id) updated else it }
            val newCurrent = if (state.currentTrack?.id == updated.id) updated else state.currentTrack
            state.copy(queue = newQueue, currentTrack = newCurrent)
        }
    }

    private fun applyCrossfadeEnvelopeIfEnabled() {
        val controller = mediaController ?: return
        crossfadeJob?.cancel()
        crossfadeJob = scope.launch {
            val settings = settingsRepository.settingsFlow.first()
            if (!settings.crossfadeEnabled) {
                controller.volume = 1.0f
                return@launch
            }
            val steps = 12
            val stepDelay = (settings.crossfadeDurationSec * 1000L / steps).coerceAtLeast(60L)
            for (i in 1..steps) {
                controller.volume = i.toFloat() / steps
                delay(stepDelay)
            }
        }
    }

    private fun syncFromPlayer() {
        val controller = mediaController ?: return
        val idx = controller.currentMediaItemIndex
        val queue = _uiState.value.queue
        val track = queue.getOrNull(idx)
        val duration = controller.duration.takeIf { it > 0L } ?: track?.durationMs ?: 0L
        _uiState.update {
            it.copy(
                currentIndex = idx,
                currentTrack = track ?: it.currentTrack,
                isPlaying = controller.isPlaying,
                positionMs = controller.currentPosition.coerceAtLeast(0L),
                durationMs = duration,
                shuffleEnabled = controller.shuffleModeEnabled
            )
        }
    }

    private fun startPositionTicker() {
        if (positionTickerJob?.isActive == true) return
        positionTickerJob = scope.launch {
            while (isActive) {
                val controller = mediaController
                if (controller != null && controller.isPlaying) {
                    val pos = controller.currentPosition.coerceAtLeast(0L)
                    val dur = controller.duration.takeIf { it > 0L } ?: _uiState.value.durationMs
                    _uiState.update { it.copy(positionMs = pos, durationMs = dur) }

                    val now = System.currentTimeMillis()
                    if (now - lastSessionSaveAt >= 3000L) {
                        lastSessionSaveAt = now
                        val currentId = _uiState.value.currentTrack?.id ?: -1L
                        if (currentId > 0L) {
                            scope.launch(Dispatchers.IO) {
                                settingsRepository.saveLastPlaybackSession(currentId, pos)
                                settingsRepository.addListeningTime(3000L)
                                repository.savePlaybackPosition(currentId, pos)
                            }
                        }
                    }
                } else {
                    break
                }
                delay(400L)
            }
        }
    }

    private fun toMedia3Item(entity: MediaItemEntity): MediaItem {
        val artUri = entity.artworkPath?.takeIf { it.isNotBlank() }?.let { path ->
            if (path.startsWith("content://") || path.startsWith("file://")) {
                Uri.parse(path)
            } else {
                Uri.fromFile(File(path))
            }
        }
        return MediaItem.Builder()
            .setMediaId(entity.id.toString())
            .setUri(Uri.parse(entity.uri))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(entity.title)
                    .setArtist(entity.artist)
                    .setAlbumTitle(entity.album)
                    .setArtworkUri(artUri)
                    .build()
            )
            .build()
    }
}
