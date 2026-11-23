package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.example.LumaApplication
import com.example.config.LumaCrashGuard
import com.example.data.local.BackupManager
import com.example.data.local.SettingsRepository
import com.example.data.media.FileIntelligenceEngine
import com.example.data.media.FileIntelligenceReport
import com.example.data.media.FormatCapability
import com.example.data.media.FormatSupportChecker
import com.example.data.media.ImportSummary
import com.example.data.media.InsightsCalculator
import com.example.data.media.LibraryHealthReport
import com.example.data.media.LumaCollectionsEngine
import com.example.data.media.LumaInsightsReport
import com.example.data.media.LumaIntelligenceEngine
import com.example.data.media.LumaIntelligenceSnapshot
import com.example.data.media.MediaPermissionHelper
import com.example.data.media.MediaPermissionStatus
import com.example.data.media.OnlineLyricsCandidate
import com.example.data.media.OnlineLyricsClient
import com.example.data.media.SmartPlaylistGenerator
import com.example.data.media.SmartPlaylistRule
import com.example.data.model.AmbientSoundType
import com.example.data.model.AccentPalette
import com.example.data.model.AlbumSummary
import com.example.data.model.AppLanguage
import com.example.data.model.AppSettings
import com.example.data.model.AppThemeStyle
import com.example.data.model.ArtistSummary
import com.example.data.model.ArtworkShape
import com.example.data.model.CoverDoubleTapAction
import com.example.data.model.CustomAudioPreset
import com.example.data.model.CustomCollectionWithTracks
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
import com.example.data.model.TrackBookmark
import com.example.data.model.ViewMode
import com.example.data.model.VisualizerPreset
import com.example.data.model.VisualizerStyle
import com.example.player.AmbientMixerState
import com.example.player.LumaConnectUiState
import com.example.player.PlaybackProgressUiState
import com.example.player.PlaybackUiState
import com.example.player.RepeatModeState
import com.example.ui.theme.formatFileSize
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class BottomNavTab(val label: String) {
    HOME("Início"),
    LIBRARY("Biblioteca"),
    DISCOVER("Descobrir"),
    SETTINGS("Ajustes")
}

sealed interface DetailDestination {
    data class Album(val albumName: String) : DetailDestination
    data class Artist(val artistName: String) : DetailDestination
    data class Genre(val genreName: String) : DetailDestination
    data class Playlist(val playlistId: Long) : DetailDestination
    data class Collection(val collectionId: Long) : DetailDestination
    data class Folder(val folderName: String) : DetailDestination
    data class SimilarTracks(val seedTrackId: Long) : DetailDestination
    data class AudioStudio(val trackId: Long? = null) : DetailDestination
    data object Connect : DetailDestination
    data object FileIntelligence : DetailDestination
    data object LibraryHealth : DetailDestination
    data object History : DetailDestination
    data object Insights : DetailDestination
    data object SupportedFormats : DetailDestination
    data object About : DetailDestination
    data class Mood(val mood: MoodType) : DetailDestination
}

@Immutable
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

@Immutable
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

@Immutable
data class LyricsSearchUiState(
    val trackId: Long = -1L,
    val isSearching: Boolean = false,
    val results: List<OnlineLyricsCandidate> = emptyList(),
    val errorMessage: String? = null
)

class LumaViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as LumaApplication
    private val repository = app.mediaRepository
    private val scanner = app.mediaScanner
    private val settingsRepository = app.settingsRepository
    val playbackController = app.playbackController
    val ambientSoundEngine = app.ambientSoundEngine
    private val connectManager = app.connectManager

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

    private val vmExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("LumaViewModel", "Recovered coroutine failure", throwable)
        _libraryState.update {
            it.copy(
                isScanning = false,
                errorBanner = it.errorBanner ?: "Operação interrompida com segurança."
            )
        }
    }
    private val vmIoContext = Dispatchers.IO + vmExceptionHandler

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .catch { emit(AppSettings()) }
        .onEach { loadedSettings ->
            _settingsLoaded.value = true
            runCatching { playbackController.setVisualizerPipelineEnabled(loadedSettings.visualizerEnabled) }
            runCatching {
                connectManager.updateAutomationPolicies(
                    autoPauseDisconnect = loadedSettings.autoPauseOnDisconnect,
                    autoResumeBluetooth = loadedSettings.autoResumeOnBluetooth
                )
            }
            if (!welcomeDecisionMade) {
                welcomeDecisionMade = true
                if (loadedSettings.hasSeenWelcomeIntro) {
                    _showWelcomeBack.value = true
                    viewModelScope.launch(vmExceptionHandler) {
                        delay(2400L)
                        _showWelcomeBack.value = false
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val playbackState: StateFlow<PlaybackUiState> = playbackController.uiState
    val playbackProgress: StateFlow<PlaybackProgressUiState> = playbackController.progressState
    val connectState: StateFlow<LumaConnectUiState> = connectManager.state

    private val excludedFoldersFlow = settings.map { it.excludedFolders }.distinctUntilChanged()
    private val libraryFilterAndSortFlow = _libraryState
        .map { it.libraryFilter to it.sortOrder }
        .distinctUntilChanged()
    private val totalListeningTimeFlow = settings.map { it.totalListeningTimeMs }.distinctUntilChanged()

    val allMedia: StateFlow<List<MediaItemEntity>> = repository.allMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val audioTracks: StateFlow<List<MediaItemEntity>> = repository.audioTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visibleAudioTracks: StateFlow<List<MediaItemEntity>> = combine(audioTracks, excludedFoldersFlow) { list, excluded ->
        repository.filterMedia(list, LibraryFilter.ALL, excluded)
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
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
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val missingArtworkCount: StateFlow<Int> = visibleAudioTracks
        .map { repository.findMissingArtworkItems(it).size }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val missingMetadataCount: StateFlow<Int> = visibleAudioTracks
        .map { repository.findMissingMetadataItems(it).size }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val playlists: StateFlow<List<PlaylistWithTracks>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customCollections: StateFlow<List<CustomCollectionWithTracks>> = repository.customCollections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val libraryHealth: StateFlow<LibraryHealthReport> = combine(allMedia, potentialDuplicates) { media, dups ->
        LumaCollectionsEngine.computeLibraryHealth(media, dups)
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryHealthReport())

    val history: StateFlow<List<HistoryWithMedia>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val insightsReport: StateFlow<LumaInsightsReport> = combine(
        allMedia,
        repository.insightsHistory,
        totalListeningTimeFlow,
        repository.listeningSessions
    ) { media, hist, listeningTimeMs, sessions ->
        InsightsCalculator.compute(
            allMedia = media,
            history = hist,
            totalListeningTimeMs = listeningTimeMs,
            sessions = sessions
        )
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            InsightsCalculator.compute(emptyList(), emptyList(), 0L)
        )

    val intelligenceSnapshot: StateFlow<LumaIntelligenceSnapshot> = combine(
        visibleAudioTracks,
        repository.listeningSessions,
        repository.insightsHistory
    ) { tracks, sessions, hist ->
        LumaIntelligenceEngine.buildSnapshot(
            tracks = tracks,
            sessions = sessions,
            history = hist
        )
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LumaIntelligenceSnapshot())

    val customAudioPresets: StateFlow<List<CustomAudioPreset>> = settings
        .map { it.customAudioPresetsJson }
        .distinctUntilChanged()
        .map { SettingsRepository.decodeCustomPresets(it) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTrackBookmarks: StateFlow<List<TrackBookmark>> = settings
        .map { it.trackBookmarksJson }
        .distinctUntilChanged()
        .map { SettingsRepository.decodeTrackBookmarks(it) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ambientState: StateFlow<AmbientMixerState> = ambientSoundEngine.state

    val searchResults: StateFlow<com.example.data.media.SearchResultsBundle> = repository
        .searchLocal(searchQueryFlow, searchFilterFlow)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.data.media.SearchResultsBundle())

    val sortedAudioTracks: StateFlow<List<MediaItemEntity>> = combine(
        audioTracks,
        libraryFilterAndSortFlow,
        excludedFoldersFlow
    ) { list, (filter, order), excluded ->
        val filtered = repository.filterMedia(list, filter, excluded)
        repository.sortMedia(filtered, order)
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sortedVideos: StateFlow<List<MediaItemEntity>> = combine(
        videos,
        libraryFilterAndSortFlow,
        excludedFoldersFlow
    ) { list, (_, order), excluded ->
        val filtered = repository.filterMedia(list, LibraryFilter.ALL, excluded)
        repository.sortMedia(filtered, order)
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _deviceCodecs = MutableStateFlow<List<FormatCapability>>(emptyList())
    val deviceCodecs: StateFlow<List<FormatCapability>> = _deviceCodecs.asStateFlow()

    private val _cacheSizeBytes = MutableStateFlow(0L)
    val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes.asStateFlow()

    val fileIntelligenceReport: StateFlow<FileIntelligenceReport> = combine(
        allMedia,
        _cacheSizeBytes,
        _deviceCodecs
    ) { media, cacheBytes, codecs ->
        FileIntelligenceEngine.analyze(media, cacheBytes, codecs)
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FileIntelligenceReport())

    private val _lyricsSearchState = MutableStateFlow(LyricsSearchUiState())
    val lyricsSearchState: StateFlow<LyricsSearchUiState> = _lyricsSearchState.asStateFlow()

    private var observerDebounceJob: Job? = null
    private var lastAutoScanAtMs: Long = 0L
    private var codecsLoaded: Boolean = false

    private var lastRadioQueueSeedKey: String = ""

    init {
        runCatching { playbackController.connect() }
        runCatching { connectManager.start() }
        val initialPermissions = runCatching { MediaPermissionHelper.checkStatus(app) }
            .getOrDefault(MediaPermissionStatus())
        _libraryState.update { it.copy(permissionStatus = initialPermissions) }
        viewModelScope.launch(vmIoContext) {
            // Defer non-critical filesystem / MediaStore work briefly so the first UI frame renders immediately without lag
            delay(320L)
            if (initialPermissions.hasAnyMediaPermission) {
                scanDeviceMediaStore(silent = true)
            }
            _cacheSizeBytes.value = runCatching { scanner.calculateCacheSizeBytes() }.getOrDefault(0L)
            delay(1200L)
            runCatching { repository.refreshAvailability() }
        }
        viewModelScope.launch(vmExceptionHandler) {
            combine(playbackState, settings, visibleAudioTracks) { pb, s, library ->
                Triple(pb, s, library)
            }
                .catch { /* Keep radio collector resilient */ }
                .collect { (pb, s, library) ->
                    if (s.queueRadioEnabled &&
                        pb.repeatMode == RepeatModeState.OFF &&
                        pb.queue.isNotEmpty() &&
                        pb.currentIndex >= pb.queue.lastIndex &&
                        library.size > 1
                    ) {
                        val key = "${pb.currentTrack?.id ?: -1}_${pb.queue.size}"
                        if (key != lastRadioQueueSeedKey) {
                            lastRadioQueueSeedKey = key
                            val additions = runCatching {
                                SmartPlaylistGenerator.pickRadioContinuation(
                                    seed = pb.currentTrack,
                                    currentQueue = pb.queue,
                                    library = library,
                                    limit = 4
                                )
                            }.getOrDefault(emptyList())
                            if (additions.isNotEmpty()) {
                                playbackController.appendTracksToQueue(additions)
                            }
                        }
                    }
                }
        }
    }

    fun ensureDeviceCodecsLoaded() {
        if (codecsLoaded && _deviceCodecs.value.isNotEmpty()) return
        codecsLoaded = true
        viewModelScope.launch(vmIoContext) {
            _deviceCodecs.value = runCatching { FormatSupportChecker.inspectDeviceCodecs() }
                .getOrDefault(emptyList())
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
        if (destination is DetailDestination.SupportedFormats || destination is DetailDestination.FileIntelligence) {
            ensureDeviceCodecsLoaded()
            refreshCacheSize()
        }
        _navState.update { it.copy(detailDestination = destination, isSearchActive = false, isFilterMenuOpen = false) }
    }

    fun closeDetail() {
        _navState.update { it.copy(detailDestination = null) }
    }

    fun openLibraryCategory(category: LibraryCategory) {
        _libraryState.update { it.copy(selectedCategory = category) }
        _navState.update { it.copy(selectedTab = BottomNavTab.LIBRARY, detailDestination = null, isSearchActive = false) }
    }

    fun openAudioStudio(track: MediaItemEntity? = playbackState.value.currentTrack ?: visibleAudioTracks.value.firstOrNull()) {
        _navState.update {
            it.copy(
                isFullMusicPlayerOpen = false,
                isSearchActive = false,
                isFilterMenuOpen = false,
                detailDestination = DetailDestination.AudioStudio(track?.id)
            )
        }
    }

    fun openSimilarTracks(seed: MediaItemEntity) {
        _navState.update {
            it.copy(
                isFullMusicPlayerOpen = false,
                isSearchActive = false,
                isFilterMenuOpen = false,
                detailDestination = DetailDestination.SimilarTracks(seed.id)
            )
        }
    }

    fun notifyStudioExportCompleted(displayName: String, isRingtone: Boolean) {
        val msg = if (isRingtone) {
            "Toque \"$displayName\" guardado em Ringtones/Luma Studio."
        } else {
            "Exportação concluída: \"$displayName\" em Music/Luma Studio."
        }
        _libraryState.update { it.copy(statusBanner = msg) }
        scanDeviceMediaStore(silent = true)
    }

    fun reportUiStatus(message: String) {
        _libraryState.update { it.copy(statusBanner = message) }
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
        viewModelScope.launch(vmIoContext) {
            runCatching { repository.recordPlaybackStarted(video.id, pauseHistory = settings.value.pauseHistoryTracking) }
        }
        _navState.update { it.copy(activeVideoMedia = video, isFullMusicPlayerOpen = false) }
    }

    fun closeVideoPlayer(lastPositionMs: Long) {
        val video = _navState.value.activeVideoMedia
        if (video != null) {
            viewModelScope.launch(vmIoContext) {
                runCatching { repository.savePlaybackPosition(video.id, lastPositionMs) }
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

    fun selectAllVisibleTracks() {
        val visibleIds = sortedAudioTracks.value.map { it.id }.toSet()
        _libraryState.update { state ->
            val allAlreadySelected = visibleIds.isNotEmpty() && state.selectedTrackIds.containsAll(visibleIds)
            state.copy(selectedTrackIds = if (allAlreadySelected) emptySet() else visibleIds)
        }
    }

    fun batchAddSelectedToQueue() {
        val ids = _libraryState.value.selectedTrackIds
        if (ids.isEmpty()) return
        val selected = sortedAudioTracks.value.filter { it.id in ids && it.isAvailable }
        if (selected.isEmpty()) return
        playbackController.appendTracksToQueue(selected)
        _libraryState.update {
            it.copy(
                isSelectionMode = false,
                selectedTrackIds = emptySet(),
                statusBanner = "${selected.size} faixa(s) adicionada(s) à fila."
            )
        }
    }

    fun batchAddSelectedToCollection(collectionId: Long) {
        val ids = _libraryState.value.selectedTrackIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch(vmIoContext) {
            val added = runCatching { repository.addTracksToCollection(collectionId, ids) }.getOrDefault(0)
            _libraryState.update {
                it.copy(
                    isSelectionMode = false,
                    selectedTrackIds = emptySet(),
                    statusBanner = "$added faixa(s) adicionada(s) à coleção."
                )
            }
        }
    }

    fun batchUpdateSelectedMetadata(artist: String?, album: String?, genre: String?, releaseYear: Int?) {
        val ids = _libraryState.value.selectedTrackIds
        if (ids.isEmpty()) return
        viewModelScope.launch(vmIoContext) {
            val updatedCount = runCatching {
                repository.batchUpdateMetadata(ids, artist, album, genre, releaseYear)
            }.getOrElse {
                _libraryState.update { it.copy(errorBanner = "Não foi possível atualizar as faixas selecionadas.") }
                return@launch
            }
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
        viewModelScope.launch(vmIoContext) {
            ids.forEach { id -> runCatching { repository.addTrackToPlaylist(playlistId, id) } }
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
        viewModelScope.launch(vmIoContext) {
            val count = runCatching { repository.reorganizeLibraryWithSmartArtistScan() }
                .getOrElse {
                    _libraryState.update { it.copy(errorBanner = "Erro ao organizar a biblioteca.") }
                    return@launch
                }
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
        viewModelScope.launch(vmIoContext) {
            val generated = SmartPlaylistGenerator.generate(rule, visibleAudioTracks.value)
            if (generated.isEmpty()) {
                _libraryState.update {
                    it.copy(statusBanner = "Importe músicas primeiro para gerar \"${rule.title}\".")
                }
                return@launch
            }
            val playlistId = runCatching { repository.createPlaylist(customName, generated.map { it.id }) }
                .getOrElse {
                    _libraryState.update { it.copy(errorBanner = "Não foi possível criar a playlist inteligente.") }
                    return@launch
                }
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
        viewModelScope.launch(vmExceptionHandler) {
            _libraryState.update { it.copy(isScanning = true, statusBanner = null, errorBanner = null) }
            val result = runCatching { repository.importUris(uris) }
            result.onSuccess { summary ->
                applyImportSummary(summary)
            }.onFailure { err ->
                _libraryState.update {
                    it.copy(isScanning = false, errorBanner = err.message ?: "Não foi possível importar os ficheiros.")
                }
            }
        }
    }

    fun importFolderTree(treeUri: Uri?) {
        if (treeUri == null) return
        viewModelScope.launch(vmExceptionHandler) {
            _libraryState.update { it.copy(isScanning = true, statusBanner = null, errorBanner = null) }
            val result = runCatching { repository.importFolderTree(treeUri) }
            result.onSuccess { summary ->
                applyImportSummary(summary)
            }.onFailure { err ->
                _libraryState.update {
                    it.copy(isScanning = false, errorBanner = err.message ?: "Não foi possível importar a pasta.")
                }
            }
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
        observerDebounceJob = viewModelScope.launch(vmExceptionHandler) {
            delay(1500L)
            scanDeviceMediaStore(silent = true)
        }
    }

    fun scanDeviceMediaStore(silent: Boolean = false) {
        val status = runCatching { MediaPermissionHelper.checkStatus(app) }
            .getOrDefault(MediaPermissionStatus())
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
        val now = System.currentTimeMillis()
        if (silent && now - lastAutoScanAtMs < 4000L) return
        lastAutoScanAtMs = now

        viewModelScope.launch(vmExceptionHandler) {
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
        viewModelScope.launch(vmExceptionHandler) {
            _libraryState.update { it.copy(isScanning = true, statusBanner = null, errorBanner = null) }
            val status = runCatching { MediaPermissionHelper.checkStatus(app) }
                .getOrDefault(MediaPermissionStatus())
            if (status.hasAnyMediaPermission) {
                runCatching {
                    repository.syncFromMediaStore(
                        scanAudio = status.hasAudioPermission,
                        scanVideo = status.hasVideoPermission
                    )
                }
            }
            val missing = runCatching { repository.refreshAvailability() }.getOrDefault(0)
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

    fun showRecoveryStatusNotice() {
        _libraryState.update {
            it.copy(statusBanner = "Sessão restaurada com segurança após uma falha inesperada.")
        }
    }

    fun reportUiError(message: String) {
        _libraryState.update { it.copy(errorBanner = message) }
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

    fun startAutoDj(seed: MediaItemEntity? = playbackState.value.currentTrack) {
        val mix = LumaIntelligenceEngine.buildAutoDjMix(
            seed = seed,
            library = visibleAudioTracks.value,
            limit = 20
        )
        if (mix.isEmpty()) {
            _libraryState.update { it.copy(statusBanner = "Importe músicas para iniciar o Auto DJ.") }
            return
        }
        playbackController.playQueue(mix, 0)
        _libraryState.update {
            it.copy(statusBanner = "Auto DJ iniciado com ${mix.size} faixas em sequência inteligente.")
        }
    }

    fun findSimilarTracks(seed: MediaItemEntity): List<MediaItemEntity> =
        LumaIntelligenceEngine.findSimilarTracks(seed, visibleAudioTracks.value)

    fun getMoodTracks(mood: MoodType): List<MediaItemEntity> =
        repository.buildMoodCollection(mood, visibleAudioTracks.value)

    fun toggleFavorite(track: MediaItemEntity) {
        viewModelScope.launch(vmIoContext) {
            runCatching {
                repository.toggleFavorite(track)
                val updated = repository.getMediaById(track.id)
                if (updated != null) {
                    playbackController.refreshCurrentTrackMetadata(updated)
                }
            }
        }
    }

    fun removeMediaFromLibrary(mediaId: Long) {
        viewModelScope.launch(vmIoContext) {
            runCatching { repository.deleteMediaFromLibrary(mediaId) }
            playbackController.clearError()
        }
    }

    // --- Playlists, Metadata, Lyrics & Backup ---

    fun createPlaylist(name: String, trackIds: List<Long> = emptyList()) {
        if (name.isBlank()) return
        viewModelScope.launch(vmIoContext) {
            runCatching {
                repository.createPlaylist(name, trackIds)
                _libraryState.update { it.copy(statusBanner = "Playlist \"$name\" criada.") }
            }
        }
    }

    fun saveCurrentQueueAsPlaylist(name: String) {
        val ids = playbackState.value.queue.map { it.id }
        if (ids.isEmpty() || name.isBlank()) return
        createPlaylist(name, ids)
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch(vmIoContext) {
            runCatching { repository.renamePlaylist(playlistId, newName) }
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch(vmIoContext) {
            runCatching {
                repository.deletePlaylist(playlistId)
                if ((_navState.value.detailDestination as? DetailDestination.Playlist)?.playlistId == playlistId) {
                    closeDetail()
                }
            }
        }
    }

    fun addTrackToPlaylist(playlistId: Long, mediaId: Long) {
        viewModelScope.launch(vmIoContext) {
            runCatching {
                repository.addTrackToPlaylist(playlistId, mediaId)
                _libraryState.update { it.copy(statusBanner = "Adicionado à playlist.") }
            }
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, mediaId: Long) {
        viewModelScope.launch(vmIoContext) {
            runCatching { repository.removeTrackFromPlaylist(playlistId, mediaId) }
        }
    }

    fun movePlaylistTrack(playlistId: Long, fromIndex: Int, toIndex: Int) {
        viewModelScope.launch(vmIoContext) {
            runCatching { repository.movePlaylistTrack(playlistId, fromIndex, toIndex) }
        }
    }

    fun createCollection(
        name: String,
        description: String = "",
        accentTag: String = "emerald",
        initialTrackIds: List<Long> = emptyList()
    ) {
        if (name.isBlank()) return
        viewModelScope.launch(vmIoContext) {
            runCatching {
                repository.createCollection(name, description, accentTag, initialTrackIds)
                _libraryState.update { it.copy(statusBanner = "Coleção \"$name\" criada.") }
            }
        }
    }

    fun updateCollectionMeta(collectionId: Long, name: String, description: String, accentTag: String) {
        viewModelScope.launch(vmIoContext) {
            runCatching {
                repository.updateCollectionMeta(collectionId, name, description, accentTag)
                _libraryState.update { it.copy(statusBanner = "Coleção atualizada.") }
            }
        }
    }

    fun toggleCollectionPinned(collectionId: Long, pinned: Boolean) {
        viewModelScope.launch(vmIoContext) {
            runCatching { repository.toggleCollectionPinned(collectionId, pinned) }
        }
    }

    fun deleteCollection(collectionId: Long) {
        viewModelScope.launch(vmIoContext) {
            runCatching {
                repository.deleteCollection(collectionId)
                if ((_navState.value.detailDestination as? DetailDestination.Collection)?.collectionId == collectionId) {
                    closeDetail()
                }
            }
        }
    }

    fun addTrackToCollection(collectionId: Long, mediaId: Long) {
        viewModelScope.launch(vmIoContext) {
            runCatching {
                repository.addTrackToCollection(collectionId, mediaId)
                _libraryState.update { it.copy(statusBanner = "Adicionado à coleção.") }
            }
        }
    }

    fun removeTrackFromCollection(collectionId: Long, mediaId: Long) {
        viewModelScope.launch(vmIoContext) {
            runCatching { repository.removeTrackFromCollection(collectionId, mediaId) }
        }
    }

    fun autoMatchMissingArtwork() {
        viewModelScope.launch(vmIoContext) {
            val candidates = LumaCollectionsEngine.matchSharedArtworkCandidates(allMedia.value)
            if (candidates.isEmpty()) {
                _libraryState.update { it.copy(statusBanner = "Nenhuma capa partilhada encontrada no mesmo álbum ou pasta.") }
                return@launch
            }
            var updated = 0
            candidates.forEach { (id, artPath) ->
                runCatching {
                    repository.updateTrackArtwork(id, artPath)
                    updated++
                }
            }
            _libraryState.update { it.copy(statusBanner = "$updated capa(s) associada(s) com segurança.") }
        }
    }

    fun cleanInaccessibleIndexEntries() {
        viewModelScope.launch(vmIoContext) {
            val inaccessible = allMedia.value.filter { !it.isAvailable }
            inaccessible.forEach { item ->
                runCatching { repository.deleteMediaFromLibrary(item.id) }
            }
            _libraryState.update {
                it.copy(statusBanner = "${inaccessible.size} registo(s) indisponível(eis) limpo(s) da biblioteca.")
            }
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
        viewModelScope.launch(vmIoContext) {
            runCatching {
                repository.updateTrackMetadata(mediaId, title, artist, album, genre, releaseYear, trackNumber)
                val updated = repository.getMediaById(mediaId)
                if (updated != null) {
                    playbackController.refreshCurrentTrackMetadata(updated)
                }
                _libraryState.update { it.copy(statusBanner = "Metadados locais atualizados.") }
            }
        }
    }

    fun updateTrackArtworkFromUri(mediaId: Long, imageUri: Uri) {
        viewModelScope.launch(vmIoContext) {
            val saved = runCatching { scanner.saveCustomArtworkFromUri(mediaId, imageUri) }
                .getOrElse { Result.failure(it) }
            saved.onSuccess { path ->
                runCatching {
                    repository.updateTrackArtwork(mediaId, path)
                    val updated = repository.getMediaById(mediaId)
                    if (updated != null) {
                        playbackController.refreshCurrentTrackMetadata(updated)
                    }
                    _libraryState.update { it.copy(statusBanner = "Capa do álbum atualizada.") }
                }
            }.onFailure { err ->
                _libraryState.update { it.copy(errorBanner = err.message ?: "Erro ao guardar capa selecionada.") }
            }
        }
    }

    fun importLyricsFile(mediaId: Long, uri: Uri) {
        viewModelScope.launch(vmIoContext) {
            val result = runCatching { scanner.readLyricsFromUri(uri) }
                .getOrElse { Result.failure(it) }
            result.onSuccess { lyricsText ->
                runCatching {
                    repository.updateTrackLyrics(mediaId, lyricsText)
                    val updated = repository.getMediaById(mediaId)
                    if (updated != null) {
                        playbackController.refreshCurrentTrackMetadata(updated)
                    }
                    _libraryState.update { it.copy(statusBanner = "Letras locais carregadas.") }
                }
            }.onFailure { err ->
                _libraryState.update {
                    it.copy(errorBanner = err.message ?: "Não foi possível abrir o ficheiro de letras.")
                }
            }
        }
    }

    fun saveTrackLyricsAndOffset(mediaId: Long, rawLyrics: String?, offsetMs: Long) {
        viewModelScope.launch(vmIoContext) {
            runCatching {
                repository.updateTrackLyricsAndOffset(mediaId, rawLyrics, offsetMs)
                val updated = repository.getMediaById(mediaId)
                if (updated != null) {
                    playbackController.refreshCurrentTrackMetadata(updated)
                }
                _libraryState.update { it.copy(statusBanner = "Sincronização de letras guardada.") }
            }
        }
    }

    fun clearTrackLyrics(mediaId: Long) {
        viewModelScope.launch(vmIoContext) {
            runCatching {
                repository.updateTrackLyrics(mediaId, null)
                val updated = repository.getMediaById(mediaId)
                if (updated != null) {
                    playbackController.refreshCurrentTrackMetadata(updated)
                }
            }
        }
    }

    fun searchOnlineLyrics(
        track: MediaItemEntity,
        customArtist: String? = null,
        customTitle: String? = null
    ) {
        val queryArtist = (customArtist ?: track.artist).trim()
        val queryTitle = (customTitle ?: track.title).trim()
        _lyricsSearchState.value = LyricsSearchUiState(
            trackId = track.id,
            isSearching = true,
            results = emptyList(),
            errorMessage = null
        )
        viewModelScope.launch(vmExceptionHandler) {
            runCatching {
                OnlineLyricsClient.searchLyrics(
                    rawTitle = queryTitle,
                    rawArtist = queryArtist,
                    album = track.album,
                    durationMs = track.durationMs
                )
            }.onSuccess { response ->
                _lyricsSearchState.value = LyricsSearchUiState(
                    trackId = track.id,
                    isSearching = false,
                    results = response.results,
                    errorMessage = response.errorMessage
                )
            }.onFailure {
                _lyricsSearchState.value = LyricsSearchUiState(
                    trackId = track.id,
                    isSearching = false,
                    results = emptyList(),
                    errorMessage = "Não foi possível pesquisar letras online."
                )
            }
        }
    }

    fun applyOnlineLyrics(trackId: Long, candidate: OnlineLyricsCandidate) {
        val text = candidate.bestLyricsText.trim()
        if (text.isBlank()) return
        viewModelScope.launch(vmIoContext) {
            runCatching {
                repository.updateTrackLyricsAndOffset(trackId, text, 0L)
                val updated = repository.getMediaById(trackId)
                if (updated != null) {
                    playbackController.refreshCurrentTrackMetadata(updated)
                }
                _lyricsSearchState.update { it.copy(results = emptyList(), errorMessage = null) }
                _libraryState.update { it.copy(statusBanner = "Letra aplicada (${candidate.source}).") }
            }
        }
    }

    fun clearLyricsSearchResults() {
        _lyricsSearchState.value = LyricsSearchUiState()
    }

    fun exportFileReportTxtToUri(uri: Uri) {
        viewModelScope.launch(vmIoContext) {
            val content = FileIntelligenceEngine.exportReportAsText(fileIntelligenceReport.value)
            runCatching {
                app.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(content.toByteArray(Charsets.UTF_8))
                } ?: throw IllegalStateException("Não foi possível gravar o relatório TXT.")
            }.onSuccess {
                _libraryState.update { it.copy(statusBanner = "Relatório TXT exportado com sucesso.") }
            }.onFailure { err ->
                _libraryState.update { it.copy(errorBanner = err.message ?: "Erro ao exportar relatório TXT.") }
            }
        }
    }

    fun exportFileReportJsonToUri(uri: Uri) {
        viewModelScope.launch(vmIoContext) {
            val content = FileIntelligenceEngine.exportReportAsJson(fileIntelligenceReport.value)
            runCatching {
                app.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(content.toByteArray(Charsets.UTF_8))
                } ?: throw IllegalStateException("Não foi possível gravar o relatório JSON.")
            }.onSuccess {
                _libraryState.update { it.copy(statusBanner = "Relatório JSON exportado com sucesso.") }
            }.onFailure { err ->
                _libraryState.update { it.copy(errorBanner = err.message ?: "Erro ao exportar relatório JSON.") }
            }
        }
    }

    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch(vmIoContext) {
            val json = BackupManager.serializeBackup(
                settings = settings.value,
                playlists = playlists.value,
                collections = customCollections.value,
                mediaMetadata = allMedia.value
            )
            runCatching {
                app.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(json.toByteArray(Charsets.UTF_8))
                } ?: throw IllegalStateException("Não foi possível gravar o ficheiro.")
            }.onSuccess {
                _libraryState.update { it.copy(statusBanner = "Backup v2 exportado com sucesso.") }
            }.onFailure { err ->
                _libraryState.update { it.copy(errorBanner = err.message ?: "Erro ao exportar backup.") }
            }
        }
    }

    fun importBackupFromUri(uri: Uri) {
        viewModelScope.launch(vmIoContext) {
            runCatching {
                val raw = app.contentResolver.openInputStream(uri)?.use {
                    it.readBytes().toString(Charsets.UTF_8)
                } ?: throw IllegalStateException("Não foi possível ler o backup.")
                val payload = BackupManager.parseBackup(raw, allMedia.value)
                payload.settings?.let { settingsRepository.applyRestoredSettings(it) }
                payload.playlists.forEach { spec ->
                    repository.createPlaylist(spec.name, spec.trackIds)
                }
                payload.collections.forEach { col ->
                    repository.createCollection(col.name, col.description, col.accentTag, col.trackIds)
                }
                payload.trackMetadata.forEach { meta ->
                    repository.updateTrackMetadata(
                        mediaId = meta.mediaId,
                        title = meta.title,
                        artist = meta.artist,
                        album = meta.album,
                        genre = meta.genre,
                        releaseYear = meta.releaseYear,
                        trackNumber = meta.trackNumber
                    )
                    if (meta.lyricsRaw != null) {
                        repository.updateTrackLyricsAndOffset(meta.mediaId, meta.lyricsRaw, meta.lyricsOffsetMs)
                    }
                    val existing = repository.getMediaById(meta.mediaId)
                    if (existing != null && meta.isFavorite && !existing.isFavorite) {
                        repository.toggleFavorite(existing)
                    }
                }
                payload
            }.onSuccess { payload ->
                _libraryState.update {
                    it.copy(
                        statusBanner = "Backup restaurado: ${payload.playlists.size} playlist(s), ${payload.collections.size} coleção(ões), ${payload.trackMetadata.size} faixa(s)."
                    )
                }
            }.onFailure { err ->
                _libraryState.update { it.copy(errorBanner = err.message ?: "Ficheiro de backup inválido.") }
            }
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    fun clearAppCache() {
        viewModelScope.launch(vmExceptionHandler) {
            val freed = withContext(vmIoContext) {
                runCatching { app.imageLoader.memoryCache?.clear() }
                runCatching { app.imageLoader.diskCache?.clear() }
                runCatching { scanner.clearAppCache() }.getOrDefault(0L)
            }
            _cacheSizeBytes.value = runCatching { scanner.calculateCacheSizeBytes() }.getOrDefault(0L)
            val label = if (freed > 0L) "Cache limpa (${formatFileSize(freed)} libertados)." else "Cache da aplicação limpa."
            _libraryState.update { it.copy(statusBanner = label) }
        }
    }

    fun refreshCacheSize() {
        viewModelScope.launch(vmIoContext) {
            _cacheSizeBytes.value = runCatching { scanner.calculateCacheSizeBytes() }.getOrDefault(0L)
        }
    }

    fun deleteHistoryEntry(historyId: Long) {
        viewModelScope.launch(vmIoContext) {
            runCatching { repository.deleteHistoryEntry(historyId) }
        }
    }

    fun clearHistory() {
        viewModelScope.launch(vmIoContext) {
            runCatching {
                repository.clearHistory()
                _libraryState.update { it.copy(statusBanner = "Histórico limpo.") }
            }
        }
    }

    fun resetLocalStats() {
        viewModelScope.launch(vmIoContext) {
            runCatching {
                repository.resetStats()
                settingsRepository.resetListeningStats()
                _libraryState.update { it.copy(statusBanner = "Estatísticas locais repostas.") }
            }
        }
    }

    // --- Settings & Personalization ---

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setThemeMode(mode) }
    }

    fun setThemeStyle(style: AppThemeStyle) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setThemeStyle(style) }
    }

    fun setPlayerScreenStyle(style: PlayerScreenStyle) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setPlayerScreenStyle(style) }
    }

    fun setAccentPalette(palette: AccentPalette) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setAccentPalette(palette) }
    }

    fun setGlassQuality(quality: GlassQuality) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setGlassQuality(quality) }
    }

    fun setReduceMotion(enabled: Boolean) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setReduceMotion(enabled) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setHapticsEnabled(enabled) }
    }

    fun setHapticIntensity(intensity: HapticIntensity) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setHapticIntensity(intensity) }
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setCrossfadeEnabled(enabled) }
    }

    fun setGaplessPlaybackEnabled(enabled: Boolean) {
        playbackController.setGaplessPlaybackEnabled(enabled)
    }

    fun setDynamicCoverTheme(enabled: Boolean) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setDynamicCoverTheme(enabled) }
    }

    fun setVisualizerEnabled(enabled: Boolean) {
        playbackController.setVisualizerPipelineEnabled(enabled)
        viewModelScope.launch(vmIoContext) { settingsRepository.setVisualizerEnabled(enabled) }
    }

    fun setVisualizerStyle(style: VisualizerStyle) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setVisualizerStyle(style) }
    }

    fun setVisualizerPreset(preset: VisualizerPreset) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setVisualizerPreset(preset) }
    }

    fun setReactiveLightingEnabled(enabled: Boolean) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setReactiveLightingEnabled(enabled) }
    }

    fun setCoverGesturesEnabled(enabled: Boolean) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setCoverGesturesEnabled(enabled) }
    }

    fun setCoverDoubleTapAction(action: CoverDoubleTapAction) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setCoverDoubleTapAction(action) }
    }

    fun setMinimalistPlayerMode(enabled: Boolean) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setMinimalistPlayerMode(enabled) }
    }

    fun setAlbumArtAnimationEnabled(enabled: Boolean) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setAlbumArtAnimationEnabled(enabled) }
    }

    fun setPlaybackAnimationStyle(style: PlaybackAnimationStyle) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setPlaybackAnimationStyle(style) }
    }

    fun setArtworkShape(shape: ArtworkShape) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setArtworkShape(shape) }
    }

    fun setCustomAccentHex(argb: Long) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setCustomAccentHex(argb) }
    }

    fun setGlassTransparencyScale(scale: Float) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setGlassTransparencyScale(scale) }
    }

    fun setBackgroundBlurScale(scale: Float) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setBackgroundBlurScale(scale) }
    }

    fun setNavBarShowLabels(show: Boolean) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setNavBarShowLabels(show) }
    }

    fun setHomeSectionLayout(order: String, hidden: Set<String>) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setHomeSectionOrder(order, hidden) }
    }

    fun toggleExcludedFolder(folderKey: String) {
        viewModelScope.launch(vmIoContext) {
            val current = settings.value.excludedFolders
            val updated = if (folderKey in current) current - folderKey else current + folderKey
            settingsRepository.setExcludedFolders(updated)
        }
    }

    fun setPauseHistoryTracking(paused: Boolean) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setPauseHistoryTracking(paused) }
    }

    fun setLyricsAppearance(fontScale: Float, centered: Boolean) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setLyricsAppearance(fontScale, centered) }
    }

    fun saveCustomAudioPreset(preset: CustomAudioPreset) {
        viewModelScope.launch(vmIoContext) { settingsRepository.saveCustomAudioPreset(preset) }
    }

    fun deleteCustomAudioPreset(name: String) {
        viewModelScope.launch(vmIoContext) { settingsRepository.deleteCustomAudioPreset(name) }
    }

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setAppLanguage(language) }
    }

    fun setShakeToSkipEnabled(enabled: Boolean) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setShakeToSkipEnabled(enabled) }
    }

    fun setQueueRadioEnabled(enabled: Boolean) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setQueueRadioEnabled(enabled) }
    }

    fun addTrackBookmark(mediaId: Long, positionMs: Long, label: String) {
        viewModelScope.launch(vmIoContext) {
            settingsRepository.addTrackBookmark(mediaId, positionMs, label)
        }
    }

    fun deleteTrackBookmark(bookmarkId: Long) {
        viewModelScope.launch(vmIoContext) {
            settingsRepository.deleteTrackBookmark(bookmarkId)
        }
    }

    fun selectAmbientSound(type: AmbientSoundType) {
        ambientSoundEngine.selectSound(type)
    }

    fun setAmbientVolume(volume: Float) {
        ambientSoundEngine.setVolume(volume)
    }

    fun markWelcomeIntroSeen() {
        viewModelScope.launch(vmIoContext) { settingsRepository.setWelcomeIntroSeen(true) }
    }

    fun dismissWelcomeBack() {
        _showWelcomeBack.value = false
    }

    fun replayWelcomeIntro() {
        viewModelScope.launch(vmIoContext) { settingsRepository.setWelcomeIntroSeen(false) }
    }

    fun setAutoPauseOnDisconnect(enabled: Boolean) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setAutoPauseOnDisconnect(enabled) }
    }

    fun setAutoResumeOnBluetooth(enabled: Boolean) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setAutoResumeOnBluetooth(enabled) }
    }

    fun setHeadsetButtonDoubleTapSkip(enabled: Boolean) {
        viewModelScope.launch(vmIoContext) { settingsRepository.setHeadsetButtonDoubleTapSkip(enabled) }
    }

    fun refreshConnectDevices() {
        connectManager.refreshState()
    }

    fun restoreLastPlaybackSession() {
        viewModelScope.launch(vmExceptionHandler) {
            val restored = runCatching { playbackController.restoreAndShowLastSession() }.getOrDefault(false)
            _libraryState.update {
                it.copy(
                    statusBanner = if (restored) {
                        "Última fila e posição restauradas."
                    } else {
                        "Nenhuma sessão anterior disponível para restaurar."
                    }
                )
            }
        }
    }

    override fun onCleared() {
        runCatching { connectManager.stop() }
        super.onCleared()
    }
}
