package com.example.ui

import android.Manifest
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.config.AppConfig
import com.example.config.LumaCrashGuard
import com.example.data.media.MediaPermissionHelper
import com.example.data.model.MediaItemEntity
import com.example.data.model.MediaType
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import com.example.player.ShakeSkipDetector
import com.example.data.media.LumaCollectionsEngine
import com.example.ui.components.AmbientMixerSheetContent
import com.example.ui.components.BatchMetadataDialog
import com.example.ui.components.CollectionPickerDialog
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.CreateEditCollectionDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.DuplicatesInspectorDialog
import com.example.ui.components.LiquidMorphBottomNavigation
import com.example.ui.components.LumaIdentityPill
import com.example.ui.components.LumaWelcomeIntroOverlay
import com.example.ui.components.MediaMetadataDialog
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.MissingArtworkScannerDialog
import com.example.ui.components.PlaylistPickerDialog
import com.example.ui.components.SmartPlaylistDialog
import com.example.ui.components.StatusErrorBanner
import com.example.ui.components.WelcomeBackBanner
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AlbumDetailScreen
import com.example.ui.screens.ArtistDetailScreen
import com.example.ui.screens.AudioStudioScreen
import com.example.ui.screens.CollectionDetailScreen
import com.example.ui.screens.ConnectScreen
import com.example.ui.screens.CustomCollectionDetailScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.FileIntelligenceScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.LibraryHealthScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.MusicPlayerScreen
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.screens.SearchResultsView
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SupportedFormatsScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.theme.LiquidGlassBackground
import com.example.ui.theme.LocalLumaGlass
import com.example.ui.theme.LocalSkipSectionEntrance
import com.example.ui.theme.LumaTheme
import com.example.ui.theme.ProvideLumaStrings
import com.example.ui.viewmodel.BottomNavTab
import com.example.ui.viewmodel.DetailDestination
import com.example.ui.viewmodel.LumaViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LumaAppRoot(
    viewModel: LumaViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val settingsLoaded by viewModel.settingsLoaded.collectAsStateWithLifecycle()
    val showWelcomeBack by viewModel.showWelcomeBack.collectAsStateWithLifecycle()
    val navState by viewModel.navState.collectAsStateWithLifecycle()
    val libraryState by viewModel.libraryState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    val allMedia by viewModel.allMedia.collectAsStateWithLifecycle()
    val audioTracks by viewModel.sortedAudioTracks.collectAsStateWithLifecycle()
    val visibleAudioTracks by viewModel.visibleAudioTracks.collectAsStateWithLifecycle()
    val videos by viewModel.sortedVideos.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val continueListening by viewModel.continueListening.collectAsStateWithLifecycle()
    val mostPlayed by viewModel.mostPlayed.collectAsStateWithLifecycle()
    val recommendedTracks by viewModel.personalizedRecommendations.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val genres by viewModel.genres.collectAsStateWithLifecycle()
    val potentialDuplicates by viewModel.potentialDuplicates.collectAsStateWithLifecycle()
    val missingArtworkCount by viewModel.missingArtworkCount.collectAsStateWithLifecycle()
    val missingMetadataCount by viewModel.missingMetadataCount.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val customCollections by viewModel.customCollections.collectAsStateWithLifecycle()
    val libraryHealth by viewModel.libraryHealth.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val insightsReport by viewModel.insightsReport.collectAsStateWithLifecycle()
    val intelligenceSnapshot by viewModel.intelligenceSnapshot.collectAsStateWithLifecycle()
    val customAudioPresets by viewModel.customAudioPresets.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val deviceCodecs by viewModel.deviceCodecs.collectAsStateWithLifecycle()
    val cacheSizeBytes by viewModel.cacheSizeBytes.collectAsStateWithLifecycle()
    val lyricsSearchState by viewModel.lyricsSearchState.collectAsStateWithLifecycle()
    val ambientState by viewModel.ambientState.collectAsStateWithLifecycle()
    val allTrackBookmarks by viewModel.allTrackBookmarks.collectAsStateWithLifecycle()
    val connectState by viewModel.connectState.collectAsStateWithLifecycle()
    val fileIntelligenceReport by viewModel.fileIntelligenceReport.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val importFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                viewModel.importMediaUris(uris)
            }
        }
    )

    val importFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { treeUri ->
            viewModel.importFolderTree(treeUri)
        }
    )

    var lyricsTargetTrackId by remember { mutableStateOf<Long?>(null) }
    val importLyricsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            val targetId = lyricsTargetTrackId
            lyricsTargetTrackId = null
            if (uri != null && targetId != null) {
                viewModel.importLyricsFile(targetId, uri)
            }
        }
    )

    // Google Play compliant zero-permission Photo Picker for custom album cover editing
    var artworkTargetTrackId by remember { mutableStateOf<Long?>(null) }
    val pickArtworkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            val targetId = artworkTargetTrackId
            artworkTargetTrackId = null
            if (uri != null && targetId != null) {
                viewModel.updateTrackArtworkFromUri(targetId, uri)
            }
        }
    )

    // SAF JSON Backup Export & Import launchers
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            if (uri != null) viewModel.exportBackupToUri(uri)
        }
    )

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) viewModel.importBackupFromUri(uri)
        }
    )

    val exportReportTxtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
        onResult = { uri ->
            if (uri != null) viewModel.exportFileReportTxtToUri(uri)
        }
    )

    val exportReportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            if (uri != null) viewModel.exportFileReportJsonToUri(uri)
        }
    )

    var pendingStartupNotificationPrompt by remember { mutableStateOf(false) }

    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { results ->
            viewModel.onMediaPermissionResult(results)
            if (pendingStartupNotificationPrompt) {
                pendingStartupNotificationPrompt = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    runCatching {
                        notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        }
    )

    val launchExternalPicker: (String, () -> Unit) -> Unit = { fallbackError, block ->
        runCatching(block).onFailure {
            viewModel.reportUiError(fallbackError)
        }
    }

    val requestAutoScanOrPermissions: () -> Unit = {
        val status = runCatching { MediaPermissionHelper.checkStatus(context) }
        val resolvedStatus = status.getOrNull()
        if (resolvedStatus?.hasAllMediaPermissions == true) {
            viewModel.scanDeviceMediaStore(silent = false)
        } else {
            runCatching {
                mediaPermissionLauncher.launch(MediaPermissionHelper.requiredMediaPermissions())
            }.onFailure {
                viewModel.reportUiError("Não foi possível solicitar permissões neste dispositivo.")
            }
        }
    }

    LaunchedEffect(Unit) {
        if (LumaCrashGuard.consumeRecoveryNotice(context)) {
            viewModel.showRecoveryStatusNotice()
        }
        val status = runCatching { MediaPermissionHelper.checkStatus(context) }.getOrNull()
        if (status?.hasAnyMediaPermission == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runCatching {
                    notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            pendingStartupNotificationPrompt = true
            runCatching {
                mediaPermissionLauncher.launch(MediaPermissionHelper.requiredMediaPermissions())
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshMediaPermissionStatus(autoScanIfNewlyGranted = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(libraryState.permissionStatus.hasAnyMediaPermission) {
        if (!libraryState.permissionStatus.hasAnyMediaPermission) {
            onDispose {}
        } else {
            val handler = Handler(Looper.getMainLooper())
            val contentObserver = object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean) {
                    viewModel.onMediaStoreChanged()
                }
            }
            val resolver = context.contentResolver
            if (libraryState.permissionStatus.hasAudioPermission) {
                runCatching {
                    resolver.registerContentObserver(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        true,
                        contentObserver
                    )
                }
            }
            if (libraryState.permissionStatus.hasVideoPermission) {
                runCatching {
                    resolver.registerContentObserver(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        true,
                        contentObserver
                    )
                }
            }
            onDispose {
                runCatching { resolver.unregisterContentObserver(contentObserver) }
            }
        }
    }

    LaunchedEffect(navState.selectedTab) {
        if (navState.selectedTab == BottomNavTab.SETTINGS) {
            viewModel.refreshCacheSize()
        }
    }

    DisposableEffect(settings.shakeToSkipEnabled) {
        if (!settings.shakeToSkipEnabled) {
            onDispose {}
        } else {
            val detector = ShakeSkipDetector(context) {
                viewModel.playbackController.skipToNext()
            }
            detector.start()
            onDispose { detector.stop() }
        }
    }

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showSmartPlaylistDialog by remember { mutableStateOf(false) }
    var showCreateCollectionDialog by remember { mutableStateOf(false) }
    var showBatchEditDialog by remember { mutableStateOf(false) }
    var showBatchPlaylistPicker by remember { mutableStateOf(false) }
    var showBatchCollectionPicker by remember { mutableStateOf(false) }
    var trackForPlaylistPicker by remember { mutableStateOf<MediaItemEntity?>(null) }
    var trackForCollectionPicker by remember { mutableStateOf<MediaItemEntity?>(null) }
    var trackPendingRemoval by remember { mutableStateOf<MediaItemEntity?>(null) }
    var trackForMetadataDialog by remember { mutableStateOf<MediaItemEntity?>(null) }
    var showDuplicatesDialog by remember { mutableStateOf(false) }
    var showArtworkScannerDialog by remember { mutableStateOf(false) }
    var showAmbientSheet by remember { mutableStateOf(false) }

    LumaTheme(settings = settings) {
        ProvideLumaStrings(language = settings.appLanguage) {
        val glass = LocalLumaGlass.current
        val showMiniPlayer = playbackState.currentTrack != null && !playbackState.isMiniPlayerDismissed
        val reservedBottomPadding = if (showMiniPlayer) 166.dp else 104.dp
        val activeError = playbackState.playbackError ?: libraryState.errorBanner
        val activeStatus = libraryState.statusBanner

        LiquidGlassBackground {
            if (!settingsLoaded) {
                // Lightweight initial backdrop while DataStore loads (~first frame)
                Box(modifier = Modifier.fillMaxSize())
            } else if (!settings.hasSeenWelcomeIntro) {
                LumaWelcomeIntroOverlay(
                    selectedThemeStyle = settings.themeStyle,
                    reduceMotion = glass.reduceMotion,
                    hapticsEnabled = glass.hapticsEnabled,
                    onSelectThemeStyle = { viewModel.setThemeStyle(it) },
                    onEnter = { viewModel.markWelcomeIntroSeen() }
                )
            } else {
            val showIdentityPill = playbackState.currentTrack != null &&
                !navState.isFullMusicPlayerOpen &&
                navState.activeVideoMedia == null &&
                !navState.isSearchActive &&
                !showWelcomeBack
            val contentTopOffset by animateDpAsState(
                targetValue = if (showIdentityPill) 50.dp else 0.dp,
                animationSpec = if (glass.reduceMotion) tween(0) else spring(dampingRatio = 0.85f, stiffness = 420f),
                label = "identityPillTopOffset"
            )
            Box(modifier = Modifier.fillMaxSize()) {
                // Main Content Area
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = contentTopOffset)
                ) {
                    val tabStateHolder = rememberSaveableStateHolder()
                    val enteredTabs = remember { mutableStateMapOf<BottomNavTab, Boolean>() }
                    val detailDest = navState.detailDestination
                    val contentKey: Any = when {
                        navState.isSearchActive -> "SEARCH"
                        detailDest != null -> detailDest
                        else -> navState.selectedTab
                    }

                    AnimatedContent(
                        targetState = contentKey,
                        transitionSpec = {
                            if (glass.reduceMotion) {
                                fadeIn(tween(70)) togetherWith fadeOut(tween(70))
                            } else if (initialState is BottomNavTab && targetState is BottomNavTab) {
                                val movingForward = (targetState as BottomNavTab).ordinal > (initialState as BottomNavTab).ordinal
                                val dir = if (movingForward) 1 else -1
                                (slideInHorizontally(
                                    animationSpec = tween(170, easing = FastOutSlowInEasing),
                                    initialOffsetX = { fullWidth -> dir * (fullWidth / 16) }
                                ) + fadeIn(tween(150, easing = FastOutSlowInEasing))) togetherWith
                                    fadeOut(tween(85, easing = FastOutSlowInEasing))
                            } else {
                                (fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                                    scaleIn(initialScale = 0.985f, animationSpec = tween(180, easing = FastOutSlowInEasing))) togetherWith
                                    fadeOut(tween(110, easing = FastOutSlowInEasing))
                            }
                        },
                        label = "mainContentTransition"
                    ) { stateKey ->
                        when {
                            stateKey == "SEARCH" -> SearchResultsView(
                                query = navState.searchQuery,
                                activeFilter = navState.searchFilter,
                                results = searchResults,
                                allMediaCount = allMedia.size,
                                isScanning = libraryState.isScanning,
                                errorMessage = activeError,
                                playbackState = playbackState,
                                bottomInsetPadding = reservedBottomPadding,
                                onRetryRefresh = { viewModel.refreshLibraryFiles() },
                                onImportFiles = {
                                    launchExternalPicker("Nenhum seletor de ficheiros disponível neste dispositivo.") {
                                        importFilesLauncher.launch(AppConfig.SAF_MEDIA_MIME_TYPES)
                                    }
                                },
                                onSelectFilter = { viewModel.selectSearchFilter(it) },
                                onPlaySong = { track, list -> viewModel.playTrack(track, list) },
                                onPlayVideo = { video -> viewModel.openVideoPlayer(video) },
                                onOpenAlbum = { album -> viewModel.openDetail(DetailDestination.Album(album)) },
                                onOpenArtist = { artist -> viewModel.openDetail(DetailDestination.Artist(artist)) },
                                onOpenPlaylist = { id -> viewModel.openDetail(DetailDestination.Playlist(id)) },
                                onPlayNext = { viewModel.playbackController.playNext(it) },
                                onAddToQueue = { viewModel.playbackController.addToQueue(it) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onSmartMix = { viewModel.playSmartMix(it) },
                                onAddToPlaylist = { trackForPlaylistPicker = it },
                                onRemoveFromLibrary = { trackPendingRemoval = it },
                                onEditMetadata = { trackForMetadataDialog = it }
                            )

                            stateKey is DetailDestination.Album -> {
                                val album = albums.find { it.name == stateKey.albumName }
                                val moreByArtist = remember(albums, album) {
                                    if (album == null) emptyList()
                                    else albums.filter {
                                        it.name != album.name && it.artist.equals(album.artist, ignoreCase = true)
                                    }
                                }
                                AlbumDetailScreen(
                                    albumName = stateKey.albumName,
                                    album = album,
                                    moreByArtist = moreByArtist,
                                    playbackState = playbackState,
                                    bottomInsetPadding = reservedBottomPadding,
                                    onBack = { viewModel.closeDetail() },
                                    onOpenArtist = { artistName -> viewModel.openDetail(DetailDestination.Artist(artistName)) },
                                    onOpenAlbum = { albumName -> viewModel.openDetail(DetailDestination.Album(albumName)) },
                                    onPlayAll = { list -> list.firstOrNull()?.let { viewModel.playTrack(it, list) } },
                                    onShuffleAll = { list ->
                                        val shuffled = list.shuffled()
                                        shuffled.firstOrNull()?.let { viewModel.playTrack(it, shuffled) }
                                    },
                                    onPlayTrack = { track, list -> viewModel.playTrack(track, list) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onPlayNext = { viewModel.playbackController.playNext(it) },
                                    onAddToQueue = { viewModel.playbackController.addToQueue(it) },
                                    onSmartMix = { viewModel.playSmartMix(it) },
                                    onAddToPlaylist = { trackForPlaylistPicker = it },
                                    onAddToCollection = { trackForCollectionPicker = it },
                                    onEditMetadata = { trackForMetadataDialog = it },
                                    onOpenInStudio = { viewModel.openAudioStudio(it) },
                                    onRemoveFromLibrary = { trackPendingRemoval = it }
                                )
                            }

                            stateKey is DetailDestination.Artist -> {
                                val artist = artists.find { it.name == stateKey.artistName }
                                val artistAlbums = remember(albums, artist, stateKey.artistName) {
                                    albums.filter { alb ->
                                        alb.artist.equals(stateKey.artistName, ignoreCase = true) ||
                                            (artist != null && alb.tracks.any { t -> artist.tracks.any { it.id == t.id } })
                                    }
                                }
                                val relatedArtists = remember(artists, artist) {
                                    if (artist == null) emptyList()
                                    else {
                                        val myGenres = artist.tracks.mapNotNull { it.genre.trim().lowercase().takeIf(String::isNotBlank) }.toSet()
                                        artists.filter { other ->
                                            other.name != artist.name && (
                                                myGenres.isEmpty() || other.tracks.any { it.genre.trim().lowercase() in myGenres }
                                                )
                                        }.take(6)
                                    }
                                }
                                ArtistDetailScreen(
                                    artistName = stateKey.artistName,
                                    artist = artist,
                                    artistAlbums = artistAlbums,
                                    relatedArtists = relatedArtists,
                                    playbackState = playbackState,
                                    bottomInsetPadding = reservedBottomPadding,
                                    onBack = { viewModel.closeDetail() },
                                    onPlayAll = { list -> list.firstOrNull()?.let { viewModel.playTrack(it, list) } },
                                    onShuffleAll = { list ->
                                        val shuffled = list.shuffled()
                                        shuffled.firstOrNull()?.let { viewModel.playTrack(it, shuffled) }
                                    },
                                    onPlayTrack = { track, list -> viewModel.playTrack(track, list) },
                                    onOpenAlbum = { albumName -> viewModel.openDetail(DetailDestination.Album(albumName)) },
                                    onOpenRelatedArtist = { name -> viewModel.openDetail(DetailDestination.Artist(name)) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onPlayNext = { viewModel.playbackController.playNext(it) },
                                    onAddToQueue = { viewModel.playbackController.addToQueue(it) },
                                    onSmartMix = { viewModel.playSmartMix(it) },
                                    onAddToPlaylist = { trackForPlaylistPicker = it },
                                    onAddToCollection = { trackForCollectionPicker = it },
                                    onEditMetadata = { trackForMetadataDialog = it },
                                    onOpenInStudio = { viewModel.openAudioStudio(it) },
                                    onRemoveFromLibrary = { trackPendingRemoval = it }
                                )
                            }

                            stateKey is DetailDestination.Genre -> {
                                val genre = genres.find { it.name == stateKey.genreName }
                                CollectionDetailScreen(
                                    title = genre?.name ?: stateKey.genreName,
                                    subtitle = if (genre != null) "${genre.trackCount} faixa(s)" else "",
                                    artworkPath = genre?.artworkPath,
                                    tracks = genre?.tracks.orEmpty(),
                                    playbackState = playbackState,
                                    bottomInsetPadding = reservedBottomPadding,
                                    onBack = { viewModel.closeDetail() },
                                    onPlayAll = { list -> list.firstOrNull()?.let { viewModel.playTrack(it, list) } },
                                    onShuffleAll = { list ->
                                        val shuffled = list.shuffled()
                                        shuffled.firstOrNull()?.let { viewModel.playTrack(it, shuffled) }
                                    },
                                    onPlayTrack = { track, list -> viewModel.playTrack(track, list) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onPlayNext = { viewModel.playbackController.playNext(it) },
                                    onAddToQueue = { viewModel.playbackController.addToQueue(it) },
                                    onSmartMix = { viewModel.playSmartMix(it) },
                                    onAddToPlaylist = { trackForPlaylistPicker = it },
                                    onRemoveFromLibrary = { trackPendingRemoval = it },
                                    onEditMetadata = { trackForMetadataDialog = it }
                                )
                            }

                            stateKey is DetailDestination.Folder -> {
                                val folder = folders.find { it.name == stateKey.folderName }
                                CollectionDetailScreen(
                                    title = folder?.name ?: stateKey.folderName,
                                    subtitle = "${folder?.itemCount ?: 0} ficheiro(s)",
                                    artworkPath = folder?.tracks?.firstOrNull { it.artworkPath != null }?.artworkPath,
                                    tracks = folder?.tracks.orEmpty(),
                                    playbackState = playbackState,
                                    bottomInsetPadding = reservedBottomPadding,
                                    onBack = { viewModel.closeDetail() },
                                    onPlayAll = { list ->
                                        val audio = list.filter { it.mediaType == MediaType.AUDIO }
                                        audio.firstOrNull()?.let { viewModel.playTrack(it, audio) }
                                    },
                                    onShuffleAll = { list ->
                                        val shuffled = list.filter { it.mediaType == MediaType.AUDIO }.shuffled()
                                        shuffled.firstOrNull()?.let { viewModel.playTrack(it, shuffled) }
                                    },
                                    onPlayTrack = { track, list -> viewModel.playTrack(track, list) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onPlayNext = { viewModel.playbackController.playNext(it) },
                                    onAddToQueue = { viewModel.playbackController.addToQueue(it) },
                                    onSmartMix = { viewModel.playSmartMix(it) },
                                    onAddToPlaylist = { trackForPlaylistPicker = it },
                                    onRemoveFromLibrary = { trackPendingRemoval = it },
                                    onEditMetadata = { trackForMetadataDialog = it }
                                )
                            }

                            stateKey is DetailDestination.Mood -> {
                                val tracks = viewModel.getMoodTracks(stateKey.mood)
                                CollectionDetailScreen(
                                    title = stateKey.mood.label,
                                    subtitle = stateKey.mood.subtitle,
                                    artworkPath = tracks.firstOrNull { it.artworkPath != null }?.artworkPath,
                                    tracks = tracks,
                                    playbackState = playbackState,
                                    bottomInsetPadding = reservedBottomPadding,
                                    onBack = { viewModel.closeDetail() },
                                    onPlayAll = { list -> list.firstOrNull()?.let { viewModel.playTrack(it, list) } },
                                    onShuffleAll = { list ->
                                        val shuffled = list.shuffled()
                                        shuffled.firstOrNull()?.let { viewModel.playTrack(it, shuffled) }
                                    },
                                    onPlayTrack = { track, list -> viewModel.playTrack(track, list) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onPlayNext = { viewModel.playbackController.playNext(it) },
                                    onAddToQueue = { viewModel.playbackController.addToQueue(it) },
                                    onSmartMix = { viewModel.playSmartMix(it) },
                                    onAddToPlaylist = { trackForPlaylistPicker = it },
                                    onRemoveFromLibrary = { trackPendingRemoval = it },
                                    onEditMetadata = { trackForMetadataDialog = it }
                                )
                            }

                            stateKey is DetailDestination.Playlist -> {
                                val playlist = playlists.find { it.playlist.id == stateKey.playlistId }
                                if (playlist != null) {
                                    PlaylistDetailScreen(
                                        playlist = playlist,
                                        playbackState = playbackState,
                                        bottomInsetPadding = reservedBottomPadding,
                                        onBack = { viewModel.closeDetail() },
                                        onPlayAll = { list -> list.firstOrNull()?.let { viewModel.playTrack(it, list) } },
                                        onShuffleAll = { list ->
                                            val shuffled = list.shuffled()
                                            shuffled.firstOrNull()?.let { viewModel.playTrack(it, shuffled) }
                                        },
                                        onPlayTrack = { track, list -> viewModel.playTrack(track, list) },
                                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                                        onRenamePlaylist = { newName -> viewModel.renamePlaylist(playlist.playlist.id, newName) },
                                        onDeletePlaylist = { viewModel.deletePlaylist(playlist.playlist.id) },
                                        onRemoveTrackFromPlaylist = { mediaId ->
                                            viewModel.removeTrackFromPlaylist(playlist.playlist.id, mediaId)
                                        },
                                        onMoveTrack = { fromIdx, toIdx ->
                                            viewModel.movePlaylistTrack(playlist.playlist.id, fromIdx, toIdx)
                                        },
                                        onEditMetadata = { trackForMetadataDialog = it }
                                    )
                                }
                            }

                            stateKey is DetailDestination.Collection -> {
                                val collection = customCollections.find { it.collection.id == stateKey.collectionId }
                                if (collection != null) {
                                    CustomCollectionDetailScreen(
                                        collection = collection,
                                        playbackState = playbackState,
                                        bottomInsetPadding = reservedBottomPadding,
                                        onBack = { viewModel.closeDetail() },
                                        onPlayAll = { list -> list.firstOrNull()?.let { viewModel.playTrack(it, list) } },
                                        onShuffleAll = { list ->
                                            val shuffled = list.shuffled()
                                            shuffled.firstOrNull()?.let { viewModel.playTrack(it, shuffled) }
                                        },
                                        onPlayTrack = { track, list -> viewModel.playTrack(track, list) },
                                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                                        onTogglePinned = { pinned -> viewModel.toggleCollectionPinned(collection.collection.id, pinned) },
                                        onEditCollectionMeta = { name, desc, tag ->
                                            viewModel.updateCollectionMeta(collection.collection.id, name, desc, tag)
                                        },
                                        onDeleteCollection = { viewModel.deleteCollection(collection.collection.id) },
                                        onRemoveTrack = { mediaId ->
                                            viewModel.removeTrackFromCollection(collection.collection.id, mediaId)
                                        },
                                        onEditMetadata = { trackForMetadataDialog = it }
                                    )
                                }
                            }

                            stateKey == DetailDestination.LibraryHealth -> LibraryHealthScreen(
                                health = libraryHealth,
                                bottomInsetPadding = reservedBottomPadding,
                                onBack = { viewModel.closeDetail() },
                                onReorganizeTags = { viewModel.reorganizeLibrary() },
                                onOpenArtworkScanner = { showArtworkScannerDialog = true },
                                onOpenDuplicatesInspector = { showDuplicatesDialog = true },
                                onOpenFileIntelligence = { viewModel.openDetail(DetailDestination.FileIntelligence) },
                                onCleanInaccessibleIndex = { viewModel.cleanInaccessibleIndexEntries() }
                            )

                            stateKey is DetailDestination.SimilarTracks -> {
                                val seed = allMedia.find { it.id == stateKey.seedTrackId }
                                val similar = remember(seed, visibleAudioTracks) {
                                    if (seed != null) viewModel.findSimilarTracks(seed) else emptyList()
                                }
                                CollectionDetailScreen(
                                    title = "Semelhantes",
                                    subtitle = if (seed != null) "Baseado em ${seed.title} • ${seed.artist}" else "",
                                    artworkPath = seed?.artworkPath,
                                    tracks = similar,
                                    playbackState = playbackState,
                                    bottomInsetPadding = reservedBottomPadding,
                                    onBack = { viewModel.closeDetail() },
                                    onPlayAll = { list -> list.firstOrNull()?.let { viewModel.playTrack(it, list) } },
                                    onShuffleAll = { list ->
                                        val shuffled = list.shuffled()
                                        shuffled.firstOrNull()?.let { viewModel.playTrack(it, shuffled) }
                                    },
                                    onPlayTrack = { track, list -> viewModel.playTrack(track, list) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onPlayNext = { viewModel.playbackController.playNext(it) },
                                    onAddToQueue = { viewModel.playbackController.addToQueue(it) },
                                    onSmartMix = { viewModel.playSmartMix(it) },
                                    onAddToPlaylist = { trackForPlaylistPicker = it },
                                    onRemoveFromLibrary = { trackPendingRemoval = it },
                                    onEditMetadata = { trackForMetadataDialog = it }
                                )
                            }

                            stateKey is DetailDestination.AudioStudio -> {
                                val initialTrack = remember(stateKey.trackId, allMedia) {
                                    stateKey.trackId?.let { id -> allMedia.find { it.id == id } }
                                        ?: playbackState.currentTrack
                                        ?: visibleAudioTracks.firstOrNull()
                                }
                                AudioStudioScreen(
                                    initialTrack = initialTrack,
                                    availableTracks = visibleAudioTracks,
                                    bottomInsetPadding = reservedBottomPadding,
                                    onBack = { viewModel.closeDetail() },
                                    onPauseMainPlayer = { viewModel.playbackController.pause() },
                                    onStudioExportComplete = { res ->
                                        viewModel.notifyStudioExportCompleted(res.displayName, res.isRingtone)
                                    },
                                    onStatusMessage = { viewModel.reportUiStatus(it) }
                                )
                            }

                            stateKey == DetailDestination.Connect -> {
                                val lastSaved = remember(settings.lastMediaId, allMedia) {
                                    allMedia.find { it.id == settings.lastMediaId }
                                }
                                ConnectScreen(
                                    connectState = connectState,
                                    settings = settings,
                                    lastSavedTrack = lastSaved,
                                    bottomInsetPadding = reservedBottomPadding,
                                    onBack = { viewModel.closeDetail() },
                                    onRefreshDevices = { viewModel.refreshConnectDevices() },
                                    onToggleAutoPauseDisconnect = { viewModel.setAutoPauseOnDisconnect(it) },
                                    onToggleAutoResumeBluetooth = { viewModel.setAutoResumeOnBluetooth(it) },
                                    onToggleHeadsetDoubleTapSkip = { viewModel.setHeadsetButtonDoubleTapSkip(it) },
                                    onRestoreLastSession = { viewModel.restoreLastPlaybackSession() },
                                    onLaunchError = { viewModel.reportUiError(it) }
                                )
                            }

                            stateKey == DetailDestination.FileIntelligence -> FileIntelligenceScreen(
                                report = fileIntelligenceReport,
                                bottomInsetPadding = reservedBottomPadding,
                                onBack = { viewModel.closeDetail() },
                                onRefreshAvailability = { viewModel.refreshLibraryFiles() },
                                onCleanInaccessibleIndex = { viewModel.cleanInaccessibleIndexEntries() },
                                onReorganizeTags = { viewModel.reorganizeLibrary() },
                                onClearAppCache = { viewModel.clearAppCache() },
                                onOpenSupportedFormats = { viewModel.openDetail(DetailDestination.SupportedFormats) },
                                onExportReportTxt = {
                                    launchExternalPicker("Nenhum seletor de gravação disponível.") {
                                        exportReportTxtLauncher.launch("luma_library_report.txt")
                                    }
                                },
                                onExportReportJson = {
                                    launchExternalPicker("Nenhum seletor de gravação disponível.") {
                                        exportReportJsonLauncher.launch("luma_library_report.json")
                                    }
                                },
                                onExportFullBackup = {
                                    launchExternalPicker("Nenhum seletor de gravação disponível.") {
                                        exportBackupLauncher.launch("luma_backup_v4.json")
                                    }
                                },
                                onImportFullBackup = {
                                    launchExternalPicker("Nenhum seletor de ficheiros disponível.") {
                                        importBackupLauncher.launch(arrayOf("application/json"))
                                    }
                                }
                            )

                            stateKey == DetailDestination.History -> HistoryScreen(
                                history = history,
                                bottomInsetPadding = reservedBottomPadding,
                                onBack = { viewModel.closeDetail() },
                                onPlayMedia = { media -> viewModel.playTrack(media, visibleAudioTracks) },
                                onDeleteEntry = { viewModel.deleteHistoryEntry(it) },
                                onClearAll = { viewModel.clearHistory() }
                            )

                            stateKey == DetailDestination.Insights -> InsightsScreen(
                                report = insightsReport,
                                bottomInsetPadding = reservedBottomPadding,
                                onBack = { viewModel.closeDetail() },
                                onPlayTrack = { track, list -> viewModel.playTrack(track, list) }
                            )

                            stateKey == DetailDestination.SupportedFormats -> SupportedFormatsScreen(
                                codecs = deviceCodecs,
                                bottomInsetPadding = reservedBottomPadding,
                                onBack = { viewModel.closeDetail() }
                            )

                            stateKey == DetailDestination.About -> AboutScreen(
                                bottomInsetPadding = reservedBottomPadding,
                                onBack = { viewModel.closeDetail() },
                                onOpenSupportedFormats = { viewModel.openDetail(DetailDestination.SupportedFormats) }
                            )

                            stateKey is BottomNavTab -> tabStateHolder.SaveableStateProvider(key = "tab_${stateKey.name}") {
                                val skipTabEntrance = enteredTabs[stateKey] == true
                                LaunchedEffect(stateKey) {
                                    if (!skipTabEntrance) {
                                        kotlinx.coroutines.delay(320L)
                                        enteredTabs[stateKey] = true
                                    }
                                }
                                CompositionLocalProvider(LocalSkipSectionEntrance provides skipTabEntrance) {
                                    when (stateKey) {
                                        BottomNavTab.HOME -> HomeScreen(
                                            allMedia = allMedia,
                                            audioTracks = visibleAudioTracks,
                                            continueListening = continueListening,
                                            favorites = favorites,
                                            playlists = playlists,
                                            history = history,
                                            playbackState = playbackState,
                                            progressFlow = viewModel.playbackProgress,
                                            permissionStatus = libraryState.permissionStatus,
                                            isScanning = libraryState.isScanning,
                                            bottomInsetPadding = reservedBottomPadding,
                                            onRequestAutoScan = requestAutoScanOrPermissions,
                                            onImportFiles = {
                                                launchExternalPicker("Nenhum seletor de ficheiros disponível neste dispositivo.") {
                                                    importFilesLauncher.launch(AppConfig.SAF_MEDIA_MIME_TYPES)
                                                }
                                            },
                                            onImportFolder = {
                                                launchExternalPicker("Nenhum seletor de pastas disponível neste dispositivo.") {
                                                    importFolderLauncher.launch(null)
                                                }
                                            },
                                            onOpenHistory = { viewModel.openDetail(DetailDestination.History) },
                                            onOpenCategory = { viewModel.openLibraryCategory(it) },
                                            onOpenMood = { viewModel.openDetail(DetailDestination.Mood(it)) },
                                            onOpenPlaylist = { viewModel.openDetail(DetailDestination.Playlist(it)) },
                                            onPlayTrack = { track, list -> viewModel.playTrack(track, list) },
                                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                                            onExpandPlayer = { viewModel.openFullMusicPlayer() },
                                            onTogglePlayPause = { viewModel.playbackController.togglePlayPause() },
                                            onNext = { viewModel.playbackController.skipToNext() },
                                            onPrevious = { viewModel.playbackController.skipToPrevious() },
                                            onPlayNext = { viewModel.playbackController.playNext(it) },
                                            onAddToQueue = { viewModel.playbackController.addToQueue(it) },
                                            onSmartMix = { viewModel.playSmartMix(it) },
                                            onAddToPlaylist = { trackForPlaylistPicker = it },
                                            onRemoveFromLibrary = { trackPendingRemoval = it },
                                            onEditMetadata = { trackForMetadataDialog = it },
                                            settings = settings,
                                            recommendedTracks = recommendedTracks,
                                            mostPlayedTracks = mostPlayed,
                                            recentArtists = artists,
                                            videos = videos,
                                            onOpenInsights = { viewModel.openDetail(DetailDestination.Insights) },
                                            onOpenArtist = { viewModel.openDetail(DetailDestination.Artist(it)) },
                                            onOpenVideo = { viewModel.openVideoPlayer(it) },
                                            onCreateSmartPlaylist = { rule -> viewModel.createSmartPlaylist(rule) },
                                            isAmbientPlaying = ambientState.isPlaying,
                                            onOpenAmbientMixer = { showAmbientSheet = true }
                                        )

                                        BottomNavTab.LIBRARY -> LibraryScreen(
                                            libraryState = libraryState,
                                            audioTracks = audioTracks,
                                            videos = videos,
                                            favorites = favorites,
                                            albums = albums,
                                            artists = artists,
                                            folders = folders,
                                            playlists = playlists,
                                            playbackState = playbackState,
                                            bottomInsetPadding = reservedBottomPadding,
                                            onSelectCategory = { viewModel.selectLibraryCategory(it) },
                                            onSortChange = { viewModel.setSortOrder(it) },
                                            onToggleViewMode = { viewModel.toggleViewMode() },
                                            onRequestAutoScan = requestAutoScanOrPermissions,
                                            onImportFiles = {
                                                launchExternalPicker("Nenhum seletor de ficheiros disponível neste dispositivo.") {
                                                    importFilesLauncher.launch(AppConfig.SAF_MEDIA_MIME_TYPES)
                                                }
                                            },
                                            onImportFolder = {
                                                launchExternalPicker("Nenhum seletor de pastas disponível neste dispositivo.") {
                                                    importFolderLauncher.launch(null)
                                                }
                                            },
                                            onPlayTrack = { track, list -> viewModel.playTrack(track, list) },
                                            onPlayVideo = { viewModel.openVideoPlayer(it) },
                                            onOpenAlbum = { viewModel.openDetail(DetailDestination.Album(it)) },
                                            onOpenArtist = { viewModel.openDetail(DetailDestination.Artist(it)) },
                                            onOpenFolder = { viewModel.openDetail(DetailDestination.Folder(it)) },
                                            onOpenPlaylist = { viewModel.openDetail(DetailDestination.Playlist(it)) },
                                            onRequestCreatePlaylist = { showCreatePlaylistDialog = true },
                                            onPlayNext = { viewModel.playbackController.playNext(it) },
                                            onAddToQueue = { viewModel.playbackController.addToQueue(it) },
                                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                                            onSmartMix = { viewModel.playSmartMix(it) },
                                            onAddToPlaylist = { trackForPlaylistPicker = it },
                                            onRemoveFromLibrary = { trackPendingRemoval = it },
                                            genres = genres,
                                            duplicateCount = potentialDuplicates.size,
                                            onOpenGenre = { viewModel.openDetail(DetailDestination.Genre(it)) },
                                            onOpenDuplicatesInspector = { showDuplicatesDialog = true },
                                            onEditMetadata = { trackForMetadataDialog = it },
                                            activeFilter = libraryState.libraryFilter,
                                            onFilterChange = { viewModel.setLibraryFilter(it) },
                                            missingArtworkCount = missingArtworkCount,
                                            missingMetadataCount = missingMetadataCount,
                                            isSelectionMode = libraryState.isSelectionMode,
                                            selectedTrackIds = libraryState.selectedTrackIds,
                                            onToggleSelectionMode = { viewModel.toggleSelectionMode() },
                                            onToggleTrackSelection = { viewModel.toggleTrackSelection(it) },
                                            onSelectAllVisibleTracks = { viewModel.selectAllVisibleTracks() },
                                            onClearSelection = { viewModel.clearSelection() },
                                            onOpenBatchEdit = { showBatchEditDialog = true },
                                            onBatchAddToPlaylist = { showBatchPlaylistPicker = true },
                                            onBatchAddToCollection = { showBatchCollectionPicker = true },
                                            onBatchAddToQueue = { viewModel.batchAddSelectedToQueue() },
                                            onOpenSmartPlaylistDialog = { showSmartPlaylistDialog = true },
                                            onReorganizeLibrary = { viewModel.reorganizeLibrary() },
                                            collections = customCollections,
                                            libraryHealth = libraryHealth,
                                            onOpenCollection = { viewModel.openDetail(DetailDestination.Collection(it)) },
                                            onRequestCreateCollection = { showCreateCollectionDialog = true },
                                            onToggleCollectionPinned = { id, pinned -> viewModel.toggleCollectionPinned(id, pinned) },
                                            onAddToCollection = { trackForCollectionPicker = it },
                                            onOpenInStudio = { viewModel.openAudioStudio(it) },
                                            onOpenLibraryHealth = { viewModel.openDetail(DetailDestination.LibraryHealth) },
                                            onOpenArtworkScanner = { showArtworkScannerDialog = true }
                                        )

                                        BottomNavTab.DISCOVER -> DiscoverScreen(
                                            snapshot = intelligenceSnapshot,
                                            currentTrack = playbackState.currentTrack,
                                            audioCount = visibleAudioTracks.size,
                                            bottomInsetPadding = reservedBottomPadding,
                                            onStartAutoDj = { seed -> viewModel.startAutoDj(seed) },
                                            onPlayTrack = { track, list -> viewModel.playTrack(track, list) },
                                            onOpenSimilarTracks = { seed -> viewModel.openDetail(DetailDestination.SimilarTracks(seed.id)) },
                                            onOpenStudio = { viewModel.openAudioStudio() },
                                            onOpenConnect = { viewModel.openDetail(DetailDestination.Connect) },
                                            onOpenInsights = { viewModel.openDetail(DetailDestination.Insights) },
                                            onOpenLibraryHealth = { viewModel.openDetail(DetailDestination.LibraryHealth) },
                                            onOpenFileIntelligence = { viewModel.openDetail(DetailDestination.FileIntelligence) }
                                        )

                                        BottomNavTab.SETTINGS -> SettingsScreen(
                                            settings = settings,
                                            mostPlayed = mostPlayed,
                                            totalIndexedCount = allMedia.size,
                                            permissionStatus = libraryState.permissionStatus,
                                            bottomInsetPadding = reservedBottomPadding,
                                            onSetThemeMode = { viewModel.setThemeMode(it) },
                                            onSetAccentPalette = { viewModel.setAccentPalette(it) },
                                            onSetThemeStyle = { viewModel.setThemeStyle(it) },
                                            onSetPlayerScreenStyle = { viewModel.setPlayerScreenStyle(it) },
                                            onSetGlassQuality = { viewModel.setGlassQuality(it) },
                                            onSetReduceMotion = { viewModel.setReduceMotion(it) },
                                            onSetHaptics = { viewModel.setHapticsEnabled(it) },
                                            onSetCrossfade = { viewModel.setCrossfadeEnabled(it) },
                                            onSetDynamicCover = { viewModel.setDynamicCoverTheme(it) },
                                            onSetVisualizer = { viewModel.setVisualizerEnabled(it) },
                                            onSetCoverGestures = { viewModel.setCoverGesturesEnabled(it) },
                                            onSetMinimalistMode = { viewModel.setMinimalistPlayerMode(it) },
                                            onSetAlbumArtAnimation = { viewModel.setAlbumArtAnimationEnabled(it) },
                                            onRequestAutoScan = requestAutoScanOrPermissions,
                                            onOpenAppSettings = {
                                                launchExternalPicker("Não foi possível abrir as definições da aplicação.") {
                                                    context.startActivity(MediaPermissionHelper.createAppSettingsIntent(context))
                                                }
                                            },
                                            onImportFiles = {
                                                launchExternalPicker("Nenhum seletor de ficheiros disponível neste dispositivo.") {
                                                    importFilesLauncher.launch(AppConfig.SAF_MEDIA_MIME_TYPES)
                                                }
                                            },
                                            onImportFolder = {
                                                launchExternalPicker("Nenhum seletor de pastas disponível neste dispositivo.") {
                                                    importFolderLauncher.launch(null)
                                                }
                                            },
                                            onRefreshLibrary = { viewModel.refreshLibraryFiles() },
                                            onOpenSupportedFormats = { viewModel.openDetail(DetailDestination.SupportedFormats) },
                                            onOpenAbout = { viewModel.openDetail(DetailDestination.About) },
                                            onClearHistory = { viewModel.clearHistory() },
                                            onResetStats = { viewModel.resetLocalStats() },
                                            cacheSizeBytes = cacheSizeBytes,
                                            duplicateCount = potentialDuplicates.size,
                                            onClearCache = { viewModel.clearAppCache() },
                                            onOpenDuplicatesInspector = { showDuplicatesDialog = true },
                                            folders = folders,
                                            onOpenInsights = { viewModel.openDetail(DetailDestination.Insights) },
                                            onSetCustomAccentHex = { viewModel.setCustomAccentHex(it) },
                                            onSetGlassTransparencyScale = { viewModel.setGlassTransparencyScale(it) },
                                            onSetBackgroundBlurScale = { viewModel.setBackgroundBlurScale(it) },
                                            onSetArtworkShape = { viewModel.setArtworkShape(it) },
                                            onSetPlaybackAnimationStyle = { viewModel.setPlaybackAnimationStyle(it) },
                                            onSetNavBarShowLabels = { viewModel.setNavBarShowLabels(it) },
                                            onSetCoverDoubleTapAction = { viewModel.setCoverDoubleTapAction(it) },
                                            onSetHapticIntensity = { viewModel.setHapticIntensity(it) },
                                            onSetGaplessPlayback = { viewModel.setGaplessPlaybackEnabled(it) },
                                            onSetHomeSectionLayout = { order, hidden -> viewModel.setHomeSectionLayout(order, hidden) },
                                            onToggleExcludedFolder = { viewModel.toggleExcludedFolder(it) },
                                            onSetPauseHistory = { viewModel.setPauseHistoryTracking(it) },
                                            onReorganizeLibrary = { viewModel.reorganizeLibrary() },
                                            onExportBackup = {
                                                launchExternalPicker("Nenhum seletor de gravação disponível neste dispositivo.") {
                                                    exportBackupLauncher.launch("luma_backup.json")
                                                }
                                            },
                                            onImportBackup = {
                                                launchExternalPicker("Nenhum seletor de ficheiros disponível neste dispositivo.") {
                                                    importBackupLauncher.launch(arrayOf("application/json"))
                                                }
                                            },
                                            onSetAppLanguage = { viewModel.setAppLanguage(it) },
                                            onSetShakeToSkip = { viewModel.setShakeToSkipEnabled(it) },
                                            onSetQueueRadio = { viewModel.setQueueRadioEnabled(it) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Custom App Identity Pill – Luma Player (Samsung Now Playing-inspired)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = 8.dp)
                ) {
                    LumaIdentityPill(
                        currentTrack = if (showIdentityPill) playbackState.currentTrack else null,
                        isPlaying = playbackState.isPlaying,
                        onClick = { viewModel.openFullMusicPlayer() }
                    )
                }

                // Returning-user smooth "Bem-vindo de volta" top banner straight into Home
                AnimatedVisibility(
                    visible = showWelcomeBack,
                    enter = fadeIn(tween(260)) + slideInVertically(animationSpec = spring(dampingRatio = 0.78f, stiffness = 360f)) { -it / 2 },
                    exit = fadeOut(tween(280)) + slideOutVertically { -it / 2 },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = 12.dp, start = 18.dp, end = 18.dp)
                ) {
                    WelcomeBackBanner(onDismiss = { viewModel.dismissWelcomeBack() })
                }

                // Outside-tap barrier to close the auxiliary Filter Menu when open
                if (navState.isSearchActive && navState.isFilterMenuOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { viewModel.closeFilterMenu() }
                    )
                }

                // Bottom Overlay: Status/Error Banner + Mini Player + Morphing Bottom Nav/Search Bar
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val bannerText = activeError ?: activeStatus
                    val errorMediaId = playbackState.errorMediaId
                    if (bannerText != null) {
                        StatusErrorBanner(
                            message = bannerText,
                            isError = activeError != null,
                            actionLabel = if (errorMediaId != null) "Remover" else null,
                            onAction = errorMediaId?.let { id ->
                                { viewModel.removeMediaFromLibrary(id) }
                            },
                            onDismiss = { viewModel.dismissBanners() }
                        )
                    }

                    AnimatedVisibility(
                        visible = showMiniPlayer,
                        enter = if (glass.reduceMotion) {
                            fadeIn(tween(100))
                        } else {
                            fadeIn(tween(200)) +
                                slideInVertically(animationSpec = spring(dampingRatio = 0.72f, stiffness = 400f)) { it / 2 } +
                                scaleIn(initialScale = 0.94f, animationSpec = spring(dampingRatio = 0.72f, stiffness = 400f))
                        },
                        exit = if (glass.reduceMotion) {
                            fadeOut(tween(80))
                        } else {
                            fadeOut(tween(150)) + slideOutVertically { it / 2 } + scaleOut(targetScale = 0.95f)
                        }
                    ) {
                        MiniPlayerBar(
                            playbackState = playbackState,
                            progressFlow = viewModel.playbackProgress,
                            onExpand = { viewModel.openFullMusicPlayer() },
                            onPrevious = { viewModel.playbackController.skipToPrevious() },
                            onTogglePlayPause = { viewModel.playbackController.togglePlayPause() },
                            onNext = { viewModel.playbackController.skipToNext() },
                            onDismiss = { viewModel.playbackController.dismissMiniPlayer() }
                        )
                    }

                    LiquidMorphBottomNavigation(
                        selectedTab = navState.selectedTab,
                        isSearchActive = navState.isSearchActive,
                        searchQuery = navState.searchQuery,
                        searchFilter = navState.searchFilter,
                        isFilterMenuOpen = navState.isFilterMenuOpen,
                        showLabels = settings.navBarShowLabels,
                        onSelectTab = { viewModel.selectTab(it) },
                        onOpenSearch = { viewModel.openSearch() },
                        onCloseSearch = { viewModel.closeSearch(clearQuery = false) },
                        onQueryChange = { viewModel.updateSearchQuery(it) },
                        onToggleFilterMenu = { viewModel.toggleFilterMenu() },
                        onSelectFilter = { viewModel.selectSearchFilter(it) }
                    )
                }

                // Full-screen Music Player Overlay
                AnimatedVisibility(
                    visible = navState.isFullMusicPlayerOpen && playbackState.currentTrack != null,
                    enter = if (glass.reduceMotion) fadeIn(tween(120)) else fadeIn(tween(240)) + slideInVertically { it / 3 },
                    exit = if (glass.reduceMotion) fadeOut(tween(100)) else fadeOut(tween(200)) + slideOutVertically { it / 3 }
                ) {
                    val currentTrackId = playbackState.currentTrack?.id
                    val currentTrackBookmarks = remember(allTrackBookmarks, currentTrackId) {
                        allTrackBookmarks.filter { it.mediaId == currentTrackId }
                    }
                    MusicPlayerScreen(
                        playbackState = playbackState,
                        settings = settings,
                        playbackController = viewModel.playbackController,
                        progressFlow = viewModel.playbackProgress,
                        onClose = { viewModel.closeFullMusicPlayer() },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onSmartMix = { viewModel.playSmartMix(it) },
                        onAddToPlaylist = { trackForPlaylistPicker = it },
                        onSaveQueueAsPlaylist = { viewModel.saveCurrentQueueAsPlaylist(it) },
                        onToggleMinimalistMode = { viewModel.setMinimalistPlayerMode(it) },
                        onEditMetadata = { trackForMetadataDialog = it },
                        onImportLyrics = { trackId ->
                            lyricsTargetTrackId = trackId
                            launchExternalPicker("Nenhum seletor de letras disponível neste dispositivo.") {
                                importLyricsLauncher.launch(AppConfig.SAF_LYRICS_MIME_TYPES)
                            }
                        },
                        onClearLyrics = { trackId -> viewModel.clearTrackLyrics(trackId) },
                        onSelectPlayerStyle = { viewModel.setPlayerScreenStyle(it) },
                        customAudioPresets = customAudioPresets,
                        onSaveCustomAudioPreset = { viewModel.saveCustomAudioPreset(it) },
                        onDeleteCustomAudioPreset = { viewModel.deleteCustomAudioPreset(it) },
                        onSelectVisualizerStyle = { viewModel.setVisualizerStyle(it) },
                        onSelectVisualizerPreset = { viewModel.setVisualizerPreset(it) },
                        onToggleReactiveLighting = { viewModel.setReactiveLightingEnabled(it) },
                        onSaveLyricsAndOffset = { id, raw, offset -> viewModel.saveTrackLyricsAndOffset(id, raw, offset) },
                        onUpdateLyricsFontScale = { scale -> viewModel.setLyricsAppearance(scale, settings.lyricsCentered) },
                        onToggleLyricsCentered = { centered -> viewModel.setLyricsAppearance(settings.lyricsFontScale, centered) },
                        lyricsSearchState = lyricsSearchState,
                        onSearchOnlineLyrics = { track, artist, title -> viewModel.searchOnlineLyrics(track, artist, title) },
                        onApplyOnlineLyrics = { id, candidate -> viewModel.applyOnlineLyrics(id, candidate) },
                        onClearLyricsSearchResults = { viewModel.clearLyricsSearchResults() },
                        bookmarks = currentTrackBookmarks,
                        ambientState = ambientState,
                        queueRadioEnabled = settings.queueRadioEnabled,
                        onToggleQueueRadio = { viewModel.setQueueRadioEnabled(it) },
                        onAddBookmark = { pos, label ->
                            playbackState.currentTrack?.let { tr -> viewModel.addTrackBookmark(tr.id, pos, label) }
                        },
                        onDeleteBookmark = { id -> viewModel.deleteTrackBookmark(id) },
                        onSelectAmbientSound = { viewModel.selectAmbientSound(it) },
                        onSetAmbientVolume = { viewModel.setAmbientVolume(it) },
                        onOpenInStudio = { viewModel.openAudioStudio(it) },
                        onOpenSimilarTracks = { viewModel.openSimilarTracks(it) }
                    )
                }

                // Full-screen Video Player Overlay
                navState.activeVideoMedia?.let { activeVideo ->
                    VideoPlayerScreen(
                        video = activeVideo,
                        onClose = { pos -> viewModel.closeVideoPlayer(pos) }
                    )
                }

                if (showAmbientSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showAmbientSheet = false },
                        containerColor = glass.surfaceElevated,
                        contentColor = glass.textPrimary,
                        scrimColor = glass.scrim
                    ) {
                        AmbientMixerSheetContent(
                            ambientState = ambientState,
                            onSelectSound = { viewModel.selectAmbientSound(it) },
                            onVolumeChange = { viewModel.setAmbientVolume(it) }
                        )
                    }
                }
            }
            }
        }
        }
    }

    // Dialogs
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name ->
                showCreatePlaylistDialog = false
                viewModel.createPlaylist(name)
            }
        )
    }

    if (showSmartPlaylistDialog) {
        SmartPlaylistDialog(
            onSelectRule = { rule ->
                showSmartPlaylistDialog = false
                viewModel.createSmartPlaylist(rule)
            },
            onDismiss = { showSmartPlaylistDialog = false }
        )
    }

    if (showBatchEditDialog) {
        BatchMetadataDialog(
            selectedCount = libraryState.selectedTrackIds.size,
            onApply = { artist, album, genre, year ->
                showBatchEditDialog = false
                viewModel.batchUpdateSelectedMetadata(artist, album, genre, year)
            },
            onDismiss = { showBatchEditDialog = false }
        )
    }

    if (showBatchPlaylistPicker) {
        PlaylistPickerDialog(
            playlists = playlists,
            onSelectPlaylist = { playlistId ->
                showBatchPlaylistPicker = false
                viewModel.batchAddSelectedToPlaylist(playlistId)
            },
            onCreateNew = { name ->
                showBatchPlaylistPicker = false
                viewModel.createPlaylist(name, libraryState.selectedTrackIds.toList())
                viewModel.clearSelection()
            },
            onDismiss = { showBatchPlaylistPicker = false }
        )
    }

    trackForPlaylistPicker?.let { track ->
        PlaylistPickerDialog(
            playlists = playlists,
            onSelectPlaylist = { playlistId ->
                viewModel.addTrackToPlaylist(playlistId, track.id)
                trackForPlaylistPicker = null
            },
            onCreateNew = { name ->
                viewModel.createPlaylist(name, listOf(track.id))
                trackForPlaylistPicker = null
            },
            onDismiss = { trackForPlaylistPicker = null }
        )
    }

    trackPendingRemoval?.let { track ->
        ConfirmationDialog(
            title = "Remover da biblioteca",
            message = "Remover \"${track.title}\" da biblioteca? O ficheiro original no dispositivo não será apagado.",
            confirmLabel = "Remover",
            onConfirm = {
                viewModel.removeMediaFromLibrary(track.id)
                trackPendingRemoval = null
            },
            onDismiss = { trackPendingRemoval = null }
        )
    }

    trackForMetadataDialog?.let { track ->
        MediaMetadataDialog(
            item = track,
            onSaveExtended = { title, artist, album, genre, year, trackNumber ->
                viewModel.updateTrackMetadataExtended(track.id, title, artist, album, genre, year, trackNumber)
                trackForMetadataDialog = null
            },
            onPickArtwork = {
                artworkTargetTrackId = track.id
                launchExternalPicker("Nenhum seletor de imagens disponível neste dispositivo.") {
                    pickArtworkLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            },
            onDismiss = { trackForMetadataDialog = null }
        )
    }

    if (showDuplicatesDialog) {
        DuplicatesInspectorDialog(
            groups = potentialDuplicates,
            onRemoveIndexEntry = { copy -> viewModel.removeMediaFromLibrary(copy.id) },
            onDismiss = { showDuplicatesDialog = false }
        )
    }

    if (showCreateCollectionDialog) {
        CreateEditCollectionDialog(
            onDismiss = { showCreateCollectionDialog = false },
            onConfirm = { name, desc, tag ->
                showCreateCollectionDialog = false
                viewModel.createCollection(name, desc, tag)
            }
        )
    }

    if (showBatchCollectionPicker) {
        CollectionPickerDialog(
            collections = customCollections,
            onSelectCollection = { collectionId ->
                showBatchCollectionPicker = false
                viewModel.batchAddSelectedToCollection(collectionId)
            },
            onCreateNew = { name, desc, tag ->
                showBatchCollectionPicker = false
                viewModel.createCollection(name, desc, tag, libraryState.selectedTrackIds.toList())
                viewModel.clearSelection()
            },
            onDismiss = { showBatchCollectionPicker = false }
        )
    }

    trackForCollectionPicker?.let { track ->
        CollectionPickerDialog(
            collections = customCollections,
            onSelectCollection = { collectionId ->
                viewModel.addTrackToCollection(collectionId, track.id)
                trackForCollectionPicker = null
            },
            onCreateNew = { name, desc, tag ->
                viewModel.createCollection(name, desc, tag, listOf(track.id))
                trackForCollectionPicker = null
            },
            onDismiss = { trackForCollectionPicker = null }
        )
    }

    if (showArtworkScannerDialog) {
        val autoMatchCount = remember(allMedia) {
            LumaCollectionsEngine.matchSharedArtworkCandidates(allMedia).size
        }
        MissingArtworkScannerDialog(
            missingTracks = libraryHealth.missingArtworkTracks,
            autoMatchableCount = autoMatchCount,
            onAutoMatchAll = { viewModel.autoMatchMissingArtwork() },
            onPickCustomArtwork = { track ->
                artworkTargetTrackId = track.id
                launchExternalPicker("Nenhum seletor de imagens disponível neste dispositivo.") {
                    pickArtworkLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            },
            onDismiss = { showArtworkScannerDialog = false }
        )
    }
}
