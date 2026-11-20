package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.example.LumaApplication
import com.example.data.local.BackupManager
import com.example.data.local.SettingsRepository
import com.example.data.media.FormatCapability
import com.example.data.media.FormatSupportChecker
import com.example.data.media.ImportSummary
import com.example.data.media.InsightsCalculator
import com.example.data.media.LumaInsightsReport
import com.example.data.media.MediaPermissionHelper
import com.example.data.media.MediaPermissionStatus
import com.example.data.media.SmartPlaylistGenerator
import com.example.data.media.SmartPlaylistRule
import com.example.data.model.AccentPalette
import com.example.data.model.AlbumSummary
import com.example.data.model.AppSettings
import com.example.data.model.AppThemeStyle
import com.example.data.model.ArtistSummary
import com.example.data.model.ArtworkShape
import com.example.data.model.CoverDoubleTapAction
import com.example.data.model.CustomAudioPreset
import com.example.data.model.DuplicateGroup
import com.example.data.model.FolderSummary
import com.example.data.model.GenreSummary
import com.example.data.model.GlassQuality
import com.example.data.model.HapticIntensity
import com.example.data.model.HistoryWithMedia
import com.example.data.model.LibraryCategory
import com.example.data.model.LibraryFilter
import com.example.data.model.MediaItemEntity
import com.example.data.model.MediaType
import com.example.data.model.MoodType
import com.example.data.model.PlaybackAnimationStyle
import com.example.data.model.PlayerScreenStyle
import com.example.data.model.PlaylistWithTracks
import com.example.data.model.SearchFilter
import com.example.data.model.SortOrder
import com.example.data.model.ThemeMode
import com.example.data.model.ViewMode
import com.example.data.model.VisualizerPreset
import com.example.data.model.VisualizerStyle
import com.example.player.PlaybackUiState
import com.example.ui.theme.formatFileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class BottomNavTab(val label: String) {
    HOME("Início"),
    LIBRARY("Biblioteca"),
    SETTINGS("Ajustes")
}

sealed interface DetailDestination {
    data class Album(val albumName: String) : DetailDestination
    data class Artist(val artistName: String) : DetailDestination
    data class Genre(val genreName: String) : DetailDestination
    data class Playlist(val playlistId: Long) : DetailDestination
    data class Folder(val folderName: String) : DetailDestination
    data object History : DetailDestination
    data object Insights : DetailDestination
    data object SupportedFormats : DetailDestination
    data object About : DetailDestination
    data class Mood(val mood: MoodType) : DetailDestination
}

data class NavigationUiState(
    val selectedTab: BottomNavTab = BottomNavTab.HOME,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val searchFilter: SearchFilter = SearchFilter.ALL,
    val isFilterMenuOpen: Boolean = false,
    val detailDestination: DetailDestination? = null,
    val isFullMusicPlayerOpen: Boolean = false,
    val activeVideoMedia: MediaItemEntity? = null
)

data class LibraryUiState(
    val selectedCategory: LibraryCategory = LibraryCategory.SONGS,
    val sortOrder: SortOrder = SortOrder.TITLE,
    val libraryFilter: LibraryFilter = LibraryFilter.ALL,
    val viewMode: ViewMode = ViewMode.LIST,
    val isScanning: Boolean = false,
    val permissionStatus: MediaPermissionStatus = MediaPermissionStatus(),
    val lastAutoScanTotal: Int = 0,
    val isSelectionMode: Boolean = false,
    val selectedTrackIds: Set<Long> = emptySet(),
    val statusBanner: String? = null,
    val errorBanner: String? = null
)

class LumaViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as LumaApplication
    private val repository = app.mediaRepository
    private val scanner = app.mediaScanner
    private val settingsRepository = app.settingsRepository
    val playbackController = app.playbackController

    private val _navState = MutableStateFlow(NavigationUiState())
    val navState: StateFlow<NavigationUiState> = _navState.asStateFlow()

    private val _libraryState = MutableStateFlow(LibraryUiState())
    val libraryState: StateFlow<LibraryUiState> = _libraryState.asStateFlow()

    private val _settingsLoaded = MutableStateFlow(false)
    val settingsLoaded: StateFlow<Boolean> = _settingsLoaded.asStateFlow()

    private val _showWelcomeBack = MutableStateFlow(false)
    val showWelcomeBack: StateFlow<Boolean> = _showWelcomeBack.asStateFlow()

    private var welcomeDecisionMade = false

    private val searchQueryFlow = MutableStateFlow("")
    private val searchFilterFlow = MutableStateFlow(SearchFilter.ALL)

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .onEach { loadedSettings ->
            _settingsLoaded.value = true
            playbackController.setVisualizerPipelineEnabled(loadedSettings.visualizerEnabled)
            if (!welcomeDecisionMade) {
                welcomeDecisionMade = true
                if (loadedSettings.hasSeenWelcomeIntro) {
                    _showWelcomeBack.value = true
                    viewModelScope.launch {
                        delay(2400L)
                        _showWelcomeBack.value = false
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val playbackState: StateFlow<PlaybackUiState> = playbackController.uiState

    val allMedia: StateFlow<List<MediaItemEntity>> = repository.allMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val audioTracks: StateFlow<List<MediaItemEntity>> = repository.audioTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visibleAudioTracks: StateFlow<List<MediaItemEntity>> = combine(audioTracks, settings) { list, s ->
        repository.filterMedia(list, LibraryFilter.ALL, s.excludedFolders)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videos: StateFlow<List<MediaItemEntity>> = repository.videos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<MediaItemEntity>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val continueListening: StateFlow<List<MediaItemEntity>> = repository.continueListening
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayed: StateFlow<List<MediaItemEntity>> = repository.mostPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val personalizedRecommendations: StateFlow<List<MediaItemEntity>> = visibleAudioTracks
        .map { SmartPlaylistGenerator.personalizedRecommendations(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<AlbumSummary>> = repository.albums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<ArtistSummary>> = repository.artists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<FolderSummary>> = repository.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val genres: StateFlow<List<GenreSummary>> = repository.genres
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val potentialDuplicates: StateFlow<List<DuplicateGroup>> = allMedia
        .map { repository.findPotentialDuplicates(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val missingArtworkCount: StateFlow<Int> = visibleAudioTracks
        .map { repository.findMissingArtworkItems(it).size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val missingMetadataCount: StateFlow<Int> = visibleAudioTracks
        .map { repository.findMissingMetadataItems(it).size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val playlists: StateFlow<List<PlaylistWithTracks>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryWithMedia>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val insightsReport: StateFlow<LumaInsightsReport> = combine(
        allMedia,
        repository.insightsHistory,
        settings
    ) { media, hist, s ->
        InsightsCalculator.compute(
            allMedia = media,
            history = hist,
            totalListeningTimeMs = s.totalListeningTimeMs
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        InsightsCalculator.compute(emptyList(), emptyList(), 0L)
    )

    val customAudioPresets: StateFlow<List<CustomAudioPreset>> = settings
        .map { SettingsRepository.decodeCustomPresets(it.customAudioPresetsJson) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchResults: StateFlow<com.example.data.media.SearchResultsBundle> = repository
        .searchLocal(searchQueryFlow, searchFilterFlow)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.data.media.SearchResultsBundle())

    val sortedAudioTracks: StateFlow<List<MediaItemEntity>> = combine(
        audioTracks,
        _libraryState,
        settings
    ) { list, ui, s ->
        val filtered = repository.filterMedia(list, ui.libraryFilter, s.excludedFolders)
        repository.sortMedia(filtered, ui.sortOrder)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sortedVideos: StateFlow<List<MediaItemEntity>> = combine(videos, _libraryState, settings) { list, ui, s ->
        val filtered = repository.filterMedia(list, LibraryFilter.ALL, s.excludedFolders)
        repository.sortMedia(filtered, ui.sortOrder)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _deviceCodecs = MutableStateFlow<List<FormatCapability>>(emptyList())
    val deviceCodecs: StateFlow<List<FormatCapability>> = _deviceCodecs.asStateFlow()

    private val _cacheSizeBytes = MutableStateFlow(0L)
    val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes.asStateFlow()

    private var observerDebounceJob: Job? = null

    init {
        playbackController.connect()
        val initialPermissions = MediaPermissionHelper.checkStatus(app)
        _libraryState.update { it.copy(permissionStatus = initialPermissions) }
        viewModelScope.launch(Dispatchers.IO) {
            _deviceCodecs.value = FormatSupportChecker.inspectDeviceCodecs()
            _cacheSizeBytes.value = scanner.calculateCacheSizeBytes()
            repository.refreshAvailability()
            if (initialPermissions.hasAnyMediaPermission) {
                scanDeviceMediaStore(silent = true)
            }
        }
    }

    // --- Navigation & Morphing Search Actions ---

    fun selectTab(tab: BottomNavTab) {
        _navState.update {
            it.copy(
                selectedTab = tab,
                detailDestination = null,
                isFilterMenuOpen = false
            )
        }
    }

    fun openSearch() {
        _navState.update {
            it.copy(
                isSearchActive = true,
                isFilterMenuOpen = false
            )
        }
    }

    fun closeSearch(clearQuery: Boolean = false) {
        if (clearQuery) {
            searchQueryFlow.value = ""
        }
        _navState.update {
            it.copy(
                isSearchActive = false,
                isFilterMenuOpen = false,
                searchQuery = if (clearQuery) "" else it.searchQuery
            )
        }
    }

    fun updateSearchQuery(query: String) {
        searchQueryFlow.value = query
        _navState.update { it.copy(searchQuery = query) }
    }

    fun toggleFilterMenu() {
        _navState.update { it.copy(isFilterMenuOpen = !it.isFilterMenuOpen) }
    }

    fun closeFilterMenu() {
        _navState.update { it.copy(isFilterMenuOpen = false) }
    }

    fun selectSearchFilter(filter: SearchFilter) {
        searchFilterFlow.value = filter
        _navState.update { it.copy(searchFilter = filter, isFilterMenuOpen = false) }
    }

    fun openDetail(destination: DetailDestination) {
        _navState.update { it.copy(detailDestination = destination, isSearchActive = false, isFilterMenuOpen = false) }
    }

    fun closeDetail() {
        _navState.update { it.copy(detailDestination = null) }
    }

    fun openLibraryCategory(category: LibraryCategory) {
        _libraryState.update { it.copy(selectedCategory = category) }
        _navState.update { it.copy(selectedTab = BottomNavTab.LIBRARY, detailDestination = null, isSearchActive = false) }
    }

    fun openFullMusicPlayer() {
        if (playbackState.value.currentTrack != null) {
            _navState.update { it.copy(isFullMusicPlayerOpen = true) }
        }
    }

    fun closeFullMusicPlayer() {
        _navState.update { it.copy(isFullMusicPlayerOpen = false) }
    }

    fun openVideoPlayer(video: MediaItemEntity) {
        if (video.mediaType != MediaType.VIDEO) return
        if (!video.isAvailable) {
            _libraryState.update { it.copy(errorBanner = "O vídeo \"${video.title}\" não está disponível no dispositivo.") }
            return
        }
        playbackController.pause()
        viewModelScope.launch(Dispatchers.IO) {
            repository.recordPlaybackStarted(video.id, pauseHistory = settings.value.pauseHistoryTracking)
        }
        _navState.update { it.copy(activeVideoMedia = video, isFullMusicPlayerOpen = false) }
    }

    fun closeVideoPlayer(lastPositionMs: Long) {
        val video = _navState.value.activeVideoMedia
        if (video != null) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.savePlaybackPosition(video.id, lastPositionMs)
            }
        }
        _navState.update { it.copy(activeVideoMedia = null) }
    }

    // --- Library, Filter, Multi-Select & Import Actions ---

    fun selectLibraryCategory(category: LibraryCategory) {
        _libraryState.update {
            it.copy(
                selectedCategory = category,
                isSelectionMode = false,
                selectedTrackIds = emptySet()
            )
        }
    }

    fun setSortOrder(order: SortOrder) {
        _libraryState.update { it.copy(sortOrder = order) }
    }

    fun setLibraryFilter(filter: LibraryFilter) {
        _libraryState.update { it.copy(libraryFilter = filter) }
    }

    fun toggleViewMode() {
        _libraryState.update {
            it.copy(viewMode = if (it.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST)
        }
    }

    fun toggleSelectionMode() {
        _libraryState.update {
            val next = !it.isSelectionMode
            it.copy(isSelectionMode = next, selectedTrackIds = if (next) it.selectedTrackIds else emptySet())
        }
    }

    fun toggleTrackSelection(mediaId: Long) {
        _libraryState.update { state ->
            val updated = if (mediaId in state.selectedTrackIds) {
                state.selectedTrackIds - mediaId
            } else {
                state.selectedTrackIds + mediaId
            }
            state.copy(selectedTrackIds = updated)
        }
    }

    fun clearSelection() {
        _libraryState.update { it.copy(isSelectionMode = false, selectedTrackIds = emptySet()) }
    }

    fun batchUpdateSelectedMetadata(artist: String?, album: String?, genre: String?, releaseYear: Int?) {
        val ids = _libraryState.value.selectedTrackIds
        if (ids.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val updatedCount = repository.batchUpdateMetadata(ids, artist, album, genre, releaseYear)
            _libraryState.update {
                it.copy(
                    isSelectionMode = false,
                    selectedTrackIds = emptySet(),
                    statusBanner = "$updatedCount faixa(s) atualizada(s) em lote."
                )
            }
        }
    }

    fun batchAddSelectedToPlaylist(playlistId: Long) {
        val ids = _libraryState.value.selectedTrackIds
        if (ids.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            ids.forEach { id -> repository.addTrackToPlaylist(playlistId, id) }
            _libraryState.update {
                it.copy(
                    isSelectionMode = false,
                    selectedTrackIds = emptySet(),
                    statusBanner = "${ids.size} faixa(s) adicionada(s) à playlist."
                )
            }
        }
    }

    fun reorganizeLibrary() {
        viewModelScope.launch(Dispatchers.IO) {
            val count = repository.reorganizeLibraryWithSmartArtistScan()
            _libraryState.update {
                it.copy(
                    statusBanner = if (count > 0) {
                        "Organização concluída: $count faixa(s) unificadas por cantor e género."
                    } else {
                        "Biblioteca já organizada e unificada."
                    }
                )
            }
        }
    }

    fun createSmartPlaylist(rule: SmartPlaylistRule, customName: String = rule.title) {
        viewModelScope.launch(Dispatchers.IO) {
            val generated = SmartPlaylistGenerator.generate(rule, visibleAudioTracks.value)
            if (generated.isEmpty()) {
                _libraryState.update {
                    it.copy(statusBanner = "Importe músicas primeiro para gerar \"${rule.title}\".")
                }
                return@launch
            }
            val playlistId = repository.createPlaylist(customName, generated.map { it.id })
            _libraryState.update {
                it.copy(statusBanner = "Playlist inteligente \"$customName\" criada (${generated.size} faixas).")
            }
            withContext(Dispatchers.Main) {
                openDetail(DetailDestination.Playlist(playlistId))
            }
        }
    }

    fun importMediaUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _libraryState.update { it.copy(isScanning = true, statusBanner = null, errorBanner = null) }
            val result = repository.importUris(uris)
            applyImportSummary(result)
        }
    }

    fun importFolderTree(treeUri: Uri?) {
        if (treeUri == null) return
        viewModelScope.launch {
            _libraryState.update { it.copy(isScanning = true, statusBanner = null, errorBanner = null) }
            val result = repository.importFolderTree(treeUri)
            applyImportSummary(result)
        }
    }

    fun refreshMediaPermissionStatus(autoScanIfNewlyGranted: Boolean = true) {
        val previous = _libraryState.value.permissionStatus
        val current = MediaPermissionHelper.checkStatus(app)
        _libraryState.update { it.copy(permissionStatus = current) }
        val newlyGranted = (!previous.hasAudioPermission && current.hasAudioPermission) ||
            (!previous.hasVideoPermission && current.hasVideoPermission)
        if (autoScanIfNewlyGranted && newlyGranted) {
            scanDeviceMediaStore(silent = false)
        }
    }

    fun onMediaPermissionResult(results: Map<String, Boolean>) {
        val current = MediaPermissionHelper.checkStatus(app)
        _libraryState.update { it.copy(permissionStatus = current) }
        if (current.hasAnyMediaPermission) {
            scanDeviceMediaStore(silent = false)
        } else if (results.isNotEmpty()) {
            _libraryState.update {
                it.copy(
                    statusBanner = "Permissão de média não concedida. Pode ativar nas definições ou importar ficheiros manualmente."
                )
            }
        }
    }

    fun onMediaStoreChanged() {
        observerDebounceJob?.cancel()
        observerDebounceJob = viewModelScope.launch {
            delay(1500L)
            scanDeviceMediaStore(silent = true)
        }
    }

    fun scanDeviceMediaStore(silent: Boolean = false) {
        val status = MediaPermissionHelper.checkStatus(app)
        _libraryState.update { it.copy(permissionStatus = status) }
        if (!status.hasAnyMediaPermission) {
            if (!silent) {
                _libraryState.update {
                    it.copy(statusBanner = "Conceda acesso a música e vídeo para varrer automaticamente o dispositivo.")
                }
            }
            return
        }
        if (_libraryState.value.isScanning) return

        viewModelScope.launch {
            _libraryState.update { it.copy(isScanning = true, errorBanner = null) }
            val result = runCatching {
                repository.syncFromMediaStore(
                    scanAudio = status.hasAudioPermission,
                    scanVideo = status.hasVideoPermission
                )
            }
            result.onSuccess { summary ->
                val banner = when {
                    !silent && summary.addedCount > 0 ->
                        "Varredura MediaStore: ${summary.addedCount} novo(s) ficheiro(s) encontrado(s) (${summary.totalScanned} analisado(s))."
                    !silent && summary.totalScanned > 0 ->
                        "Biblioteca MediaStore atualizada (${summary.totalScanned} ficheiro(s) no dispositivo)."
                    !silent ->
                        "Nenhum áudio ou vídeo encontrado no MediaStore."
                    summary.addedCount > 0 ->
                        "Varredura automática: ${summary.addedCount} novo(s) ficheiro(s) adicionado(s)."
                    else -> null
                }
                _libraryState.update {
                    it.copy(
                        isScanning = false,
                        lastAutoScanTotal = summary.totalScanned,
                        statusBanner = banner ?: it.statusBanner
                    )
                }
            }.onFailure {
                _libraryState.update {
                    it.copy(
                        isScanning = false,
                        errorBanner = if (silent) it.errorBanner else "Erro ao varrer a biblioteca MediaStore do dispositivo."
                    )
                }
            }
        }
    }

    fun refreshLibraryFiles() {
        viewModelScope.launch {
            _libraryState.update { it.copy(isScanning = true, statusBanner = null, errorBanner = null) }
            val status = MediaPermissionHelper.checkStatus(app)
            if (status.hasAnyMediaPermission) {
                runCatching {
                    repository.syncFromMediaStore(
                        scanAudio = status.hasAudioPermission,
                        scanVideo = status.hasVideoPermission
                    )
                }
            }
            val missing = repository.refreshAvailability()
            _libraryState.update {
                it.copy(
                    isScanning = false,
                    permissionStatus = status,
                    statusBanner = if (missing == 0) {
                        "Biblioteca verificada: todos os ficheiros estão acessíveis."
                    } else {
                        "$missing ficheiro(s) movido(s) ou indisponível(eis)."
                    }
                )
            }
        }
    }

    private fun applyImportSummary(summary: ImportSummary) {
        val status = buildString {
            if (summary.importedCount > 0) append("${summary.importedCount} importado(s)")
            if (summary.duplicateCount > 0) {
                if (isNotEmpty()) append(" • ")
                append("${summary.duplicateCount} duplicado(s) ignorado(s)")
            }
            if (isEmpty() && summary.unsupportedOrFailedCount == 0) {
                append("Nenhum ficheiro novo encontrado.")
            }
        }
        val error = if (summary.unsupportedOrFailedCount > 0) {
            "${summary.unsupportedOrFailedCount} ficheiro(s) não suportado(s) ou danificado(s)."
        } else null

        _libraryState.update {
            it.copy(
                isScanning = false,
                statusBanner = status.ifBlank { null },
                errorBanner = error
            )
        }
    }

    fun dismissBanners() {
        _libraryState.update { it.copy(statusBanner = null, errorBanner = null) }
        playbackController.clearError()
    }

    // --- Playback & Collection Actions ---

    fun playTrack(track: MediaItemEntity, queueContext: List<MediaItemEntity> = visibleAudioTracks.value) {
        if (track.mediaType == MediaType.VIDEO) {
            openVideoPlayer(track)
            return
        }
        if (!track.isAvailable) {
            _libraryState.update { it.copy(errorBanner = "Ficheiro \"${track.title}\" indisponível. Verifique ou remova da biblioteca.") }
            return
        }
        val validList = queueContext.filter { it.mediaType == MediaType.AUDIO && it.isAvailable }
        val finalQueue = if (validList.any { it.id == track.id }) validList else listOf(track)
        val idx = finalQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playbackController.playQueue(finalQueue, idx)
    }

    fun playSmartMix(seed: MediaItemEntity) {
        val mix = repository.buildSmartMix(seed, visibleAudioTracks.value)
        if (mix.isNotEmpty()) {
            playbackController.playQueue(mix, 0)
            _libraryState.update { it.copy(statusBanner = "Smart Mix criado a partir de \"${seed.title}\"") }
        }
    }

    fun getMoodTracks(mood: MoodType): List<MediaItemEntity> =
        repository.buildMoodCollection(mood, visibleAudioTracks.value)

    fun toggleFavorite(track: MediaItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleFavorite(track)
            val updated = repository.getMediaById(track.id)
            if (updated != null) {
                playbackController.refreshCurrentTrackMetadata(updated)
            }
        }
    }

    fun removeMediaFromLibrary(mediaId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMediaFromLibrary(mediaId)
            playbackController.clearError()
        }
    }

    // --- Playlists, Metadata, Lyrics & Backup ---

    fun createPlaylist(name: String, trackIds: List<Long> = emptyList()) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.createPlaylist(name, trackIds)
            _libraryState.update { it.copy(statusBanner = "Playlist \"$name\" criada.") }
        }
    }

    fun saveCurrentQueueAsPlaylist(name: String) {
        val ids = playbackState.value.queue.map { it.id }
        if (ids.isEmpty() || name.isBlank()) return
        createPlaylist(name, ids)
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.renamePlaylist(playlistId, newName)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePlaylist(playlistId)
            if ((_navState.value.detailDestination as? DetailDestination.Playlist)?.playlistId == playlistId) {
                closeDetail()
            }
        }
    }

    fun addTrackToPlaylist(playlistId: Long, mediaId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addTrackToPlaylist(playlistId, mediaId)
            _libraryState.update { it.copy(statusBanner = "Adicionado à playlist.") }
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, mediaId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeTrackFromPlaylist(playlistId, mediaId)
        }
    }

    fun movePlaylistTrack(playlistId: Long, fromIndex: Int, toIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.movePlaylistTrack(playlistId, fromIndex, toIndex)
        }
    }

    fun updateTrackMetadata(
        mediaId: Long,
        title: String,
        artist: String,
        album: String,
        genre: String
    ) {
        updateTrackMetadataExtended(mediaId, title, artist, album, genre, 0, 0)
    }

    fun updateTrackMetadataExtended(
        mediaId: Long,
        title: String,
        artist: String,
        album: String,
        genre: String,
        releaseYear: Int,
        trackNumber: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTrackMetadata(mediaId, title, artist, album, genre, releaseYear, trackNumber)
            val updated = repository.getMediaById(mediaId)
            if (updated != null) {
                playbackController.refreshCurrentTrackMetadata(updated)
            }
            _libraryState.update { it.copy(statusBanner = "Metadados locais atualizados.") }
        }
    }

    fun updateTrackArtworkFromUri(mediaId: Long, imageUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val saved = scanner.saveCustomArtworkFromUri(mediaId, imageUri)
            saved.onSuccess { path ->
                repository.updateTrackArtwork(mediaId, path)
                val updated = repository.getMediaById(mediaId)
                if (updated != null) {
                    playbackController.refreshCurrentTrackMetadata(updated)
                }
                _libraryState.update { it.copy(statusBanner = "Capa do álbum atualizada.") }
            }.onFailure { err ->
                _libraryState.update { it.copy(errorBanner = err.message ?: "Erro ao guardar capa selecionada.") }
            }
        }
    }

    fun importLyricsFile(mediaId: Long, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = scanner.readLyricsFromUri(uri)
            result.onSuccess { lyricsText ->
                repository.updateTrackLyrics(mediaId, lyricsText)
                val updated = repository.getMediaById(mediaId)
                if (updated != null) {
                    playbackController.refreshCurrentTrackMetadata(updated)
                }
                _libraryState.update { it.copy(statusBanner = "Letras locais carregadas.") }
            }.onFailure { err ->
                _libraryState.update {
                    it.copy(errorBanner = err.message ?: "Não foi possível abrir o ficheiro de letras.")
                }
            }
        }
    }

    fun saveTrackLyricsAndOffset(mediaId: Long, rawLyrics: String?, offsetMs: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTrackLyricsAndOffset(mediaId, rawLyrics, offsetMs)
            val updated = repository.getMediaById(mediaId)
            if (updated != null) {
                playbackController.refreshCurrentTrackMetadata(updated)
            }
            _libraryState.update { it.copy(statusBanner = "Sincronização de letras guardada.") }
        }
    }

    fun clearTrackLyrics(mediaId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTrackLyrics(mediaId, null)
            val updated = repository.getMediaById(mediaId)
            if (updated != null) {
                playbackController.refreshCurrentTrackMetadata(updated)
            }
        }
    }

    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val json = BackupManager.serializeBackup(
                settings = settings.value,
                playlists = playlists.value
            )
            runCatching {
                app.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(json.toByteArray(Charsets.UTF_8))
                } ?: throw IllegalStateException("Não foi possível gravar o ficheiro.")
            }.onSuccess {
                _libraryState.update { it.copy(statusBanner = "Backup exportado com sucesso.") }
            }.onFailure { err ->
                _libraryState.update { it.copy(errorBanner = err.message ?: "Erro ao exportar backup.") }
            }
        }
    }

    fun importBackupFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val raw = app.contentResolver.openInputStream(uri)?.use {
                    it.readBytes().toString(Charsets.UTF_8)
                } ?: throw IllegalStateException("Não foi possível ler o backup.")
                val payload = BackupManager.parseBackup(raw, allMedia.value)
                payload.settings?.let { settingsRepository.applyRestoredSettings(it) }
                payload.playlists.forEach { spec ->
                    repository.createPlaylist(spec.name, spec.trackIds)
                }
                payload
            }.onSuccess { payload ->
                _libraryState.update {
                    it.copy(
                        statusBanner = "Backup restaurado: ${payload.playlists.size} playlist(s) importada(s)."
                    )
                }
            }.onFailure { err ->
                _libraryState.update { it.copy(errorBanner = err.message ?: "Ficheiro de backup inválido.") }
            }
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    fun clearAppCache() {
        viewModelScope.launch {
            val freed = withContext(Dispatchers.IO) {
                runCatching { app.imageLoader.memoryCache?.clear() }
                runCatching { app.imageLoader.diskCache?.clear() }
                scanner.clearAppCache()
            }
            _cacheSizeBytes.value = scanner.calculateCacheSizeBytes()
            val label = if (freed > 0L) "Cache limpa (${formatFileSize(freed)} libertados)." else "Cache da aplicação limpa."
            _libraryState.update { it.copy(statusBanner = label) }
        }
    }

    fun refreshCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            _cacheSizeBytes.value = scanner.calculateCacheSizeBytes()
        }
    }

    fun deleteHistoryEntry(historyId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteHistoryEntry(historyId)
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearHistory()
            _libraryState.update { it.copy(statusBanner = "Histórico limpo.") }
        }
    }

    fun resetLocalStats() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.resetStats()
            settingsRepository.resetListeningStats()
            _libraryState.update { it.copy(statusBanner = "Estatísticas locais repostas.") }
        }
    }

    // --- Settings & Personalization ---

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.setThemeMode(mode) }
    }

    fun setThemeStyle(style: AppThemeStyle) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.setThemeStyle(style) }
    }

    fun setPlayerScreenStyle(style: PlayerScreenStyle) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.setPlayerScreenStyle(style) }
    }

    fun setAccentPalette(palette: AccentPalette) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.setAccentPalette(palette) }
    }

    fun setGlassQuality(quality: GlassQuality) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.setGlassQuality(quality) }
    }

    fun setReduceMotion(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.setReduceMotion(enabled) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.setHapticsEnabled(enabled) }
    }

    fun setHapticIntensity(intensity: HapticIntensity) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.setHapticIntensity(intensity) }
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.setCrossfadeEnabled(enabled) }
    }

    fun setGaplessPlaybackEnabled(enabled: Boolean) {
        playbackController.setGaplessPlaybackEnabled(enabled)
    }

    fun setDynamicCoverTheme(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.setDynamicCoverTheme(enabled) }
    }

    fun setVisualizerEnabled(enabled: Boolean) {
        playbackController.setVisualizerPipelineEnabled(enabled)
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.setVisualizerEnabled(enabled) }
    }

    fun setVisualizerStyle(style: VisualizerStyle) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.setVisualizerStyle(style) }
    }

    fun setVisualizerPreset(preset: VisualizerPreset) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.setVisualizerPreset(preset) }
    }

    fun setReactiveLightingEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.setReactiveLightingEnabled(enabled) }
    }

    fun setCoverGesturesEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.setCoverGesturesEnabled(enabled) }
    }

    fun setCoverDoubleTapAction(action: CoverDoubleTapAction) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.setCoverDoubleTapAction(action) }
    }

    fun setMinimalistPlayerMode(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.setMinimalistPlayerMode(enabled) }
    }

    fun setAlbumArtAnimationEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.setAlbumArtAnimationEnabled(enabled) }
    }

    fun setPlaybackAnimationStyle(style: PlaybackAnimationStyle) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.setPlaybackAnimationStyle(style) }
    }

    fun setArtworkShape(shape: ArtworkShape) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.setArtworkShape(shape) }
    }

    fun setCustomAccentHex(argb: Long) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.setCustomAccentHex(argb) }
    }

    fun setGlassTransparencyScale(scale: Float) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.setGlassTransparencyScale(scale) }
    }

    fun setBackgroundBlurScale(scale: Float) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.setBackgroundBlurScale(scale) }
    }

    fun setNavBarShowLabels(show: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.setNavBarShowLabels(show) }
    }

    fun setHomeSectionLayout(order: String, hidden: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.setHomeSectionOrder(order, hidden) }
    }

    fun toggleExcludedFolder(folderKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = settings.value.excludedFolders
            val updated = if (folderKey in current) current - folderKey else current + folderKey
            settingsRepository.setExcludedFolders(updated)
        }
    }

    fun setPauseHistoryTracking(paused: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.setPauseHistoryTracking(paused) }
    }

    fun setLyricsAppearance(fontScale: Float, centered: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.setLyricsAppearance(fontScale, centered) }
    }

    fun saveCustomAudioPreset(preset: CustomAudioPreset) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.saveCustomAudioPreset(preset) }
    }

    fun deleteCustomAudioPreset(name: String) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.deleteCustomAudioPreset(name) }
    }

    fun markWelcomeIntroSeen() {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.setWelcomeIntroSeen(true) }
    }

    fun dismissWelcomeBack() {
        _showWelcomeBack.value = false
    }

    fun replayWelcomeIntro() {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.setWelcomeIntroSeen(false) }
    }
}
