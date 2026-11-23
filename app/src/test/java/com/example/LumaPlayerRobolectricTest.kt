package com.example

import android.Manifest
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BackupManager
import com.example.data.local.LumaDatabase
import com.example.data.local.SettingsRepository
import com.example.data.media.ArtistNormalizer
import com.example.data.media.FileIntelligenceEngine
import com.example.data.media.FormatSupportChecker
import com.example.data.media.InsightsCalculator
import com.example.data.media.LrcParser
import com.example.data.media.LumaCollectionsEngine
import com.example.data.media.LumaIntelligenceEngine
import com.example.data.media.LumaMoodCategory
import com.example.data.media.LumaStudioEngine
import com.example.data.media.LyricLine
import com.example.data.media.MediaPermissionHelper
import com.example.data.media.MediaRepository
import com.example.data.media.MediaScanner
import com.example.data.media.SmartPlaylistGenerator
import com.example.data.media.SmartPlaylistRule
import com.example.data.model.AccentPalette
import com.example.data.model.AmbientSoundType
import com.example.data.model.AppSettings
import com.example.data.model.AppThemeStyle
import com.example.data.model.CustomCollectionEntity
import com.example.data.model.DuplicateGroup
import com.example.data.model.HistoryWithMedia
import com.example.data.model.ListeningSessionEntity
import com.example.data.model.MediaItemEntity
import com.example.data.model.MediaType
import com.example.data.model.MoodType
import com.example.data.model.PlaybackHistoryEntity
import com.example.data.model.PlayerScreenStyle
import com.example.data.model.PlaylistEntity
import com.example.data.model.PlaylistWithTracks
import com.example.data.model.SearchFilter
import com.example.data.model.SortOrder
import com.example.data.model.SurfaceMaterial
import com.example.data.model.ThemeMode
import com.example.data.model.TrackBookmark
import com.example.data.model.VisualizerPreset
import com.example.data.model.VisualizerStyle
import com.example.player.AmbientMixerState
import com.example.player.AmbientSoundEngine
import com.example.player.AudioPipelineBus
import com.example.player.AudioSpectrumSnapshot
import com.example.player.PlaybackController
import com.example.player.PlaybackProgressUiState
import com.example.player.PlaybackUiState
import com.example.player.ShakeSkipDetector
import com.example.player.SpectrumAudioProcessor
import com.example.player.StereoBalanceAudioProcessor
import com.example.ui.components.AbLoopSheetContent
import com.example.ui.components.AmbientMixerSheetContent
import com.example.ui.components.BookmarksSheetContent
import com.example.ui.components.FloatingGlassPlayerDock
import com.example.ui.components.LiquidMorphBottomNavigation
import com.example.ui.components.LumaWelcomeIntroOverlay
import com.example.ui.components.LyricsStageView
import com.example.ui.components.MediaMetadataDialog
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.PlayerStylePickerSheetContent
import com.example.ui.components.PosterImmersiveStage
import com.example.ui.components.SpatialGlassCoverDeck
import com.example.ui.components.SpectrumVisualizerView
import com.example.ui.components.SpeedAndPitchSheetContent
import com.example.ui.components.StoryCardExporter
import com.example.ui.components.WelcomeBackBanner
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.down
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.up
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.MusicPlayerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.buildColorSchemeFromGlass
import com.example.ui.theme.AmoledDarkBg
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DeepNavyBg
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.ForestDarkBg
import com.example.ui.theme.FrostedIceBg
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LocalLumaGlass
import com.example.ui.theme.LumaAnimatedSwitch
import com.example.ui.theme.LumaGlassTokens
import com.example.ui.theme.LocalSkipSectionEntrance
import com.example.ui.theme.LumaSectionEntrance
import com.example.ui.theme.LumaTheme
import com.example.ui.theme.NeuBwDarkBg
import com.example.ui.theme.NeuBwInkBlack
import com.example.ui.theme.NeuBwInkWhite
import com.example.ui.theme.NeuBwLightBg
import com.example.ui.theme.OceanCyanLight
import com.example.ui.theme.SoftPinkPrimary
import com.example.ui.theme.SoftWhiteLightBg
import com.example.ui.theme.SoftWhiteLightElevated
import com.example.ui.theme.SunsetAmberLight
import com.example.ui.theme.VioletLight
import com.example.ui.viewmodel.BottomNavTab
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import com.example.config.AppConfig
import com.example.config.LumaCrashGuard
import com.example.data.media.OnlineLyricsCandidate
import com.example.data.media.OnlineLyricsClient
import com.example.data.model.AppLanguage
import com.example.ui.theme.LiquidGlassCapsule
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.components.LumaIdentityPill
import com.example.ui.components.LyricsStudioSheetContent
import com.example.ui.screens.computeEdgeSwipeSeekTargetMs
import com.example.ui.screens.computeEdgeSwipeTargetVolume
import com.example.ui.theme.ExtractedArtworkColors
import com.example.ui.theme.ProvideLumaStrings
import com.example.ui.theme.buildHarmonizedNpdTokens
import com.example.ui.theme.resolveLumaStrings
import com.example.ui.viewmodel.LyricsSearchUiState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LumaPlayerRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: LumaDatabase
    private lateinit var repository: MediaRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, LumaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MediaRepository(
            mediaDao = db.mediaDao(),
            playlistDao = db.playlistDao(),
            historyDao = db.historyDao(),
            queueDao = db.queueDao(),
            scanner = MediaScanner(context)
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `bottom navigation sliding tabs and morphing search bar transitions work`() {
        var selectedTab by mutableStateOf(BottomNavTab.HOME)
        var isSearchActive by mutableStateOf(false)
        var query by mutableStateOf("")
        var filter by mutableStateOf(SearchFilter.ALL)
        var filterOpen by mutableStateOf(false)

        composeTestRule.setContent {
            LumaTheme(settings = AppSettings(reduceMotion = true)) {
                LiquidMorphBottomNavigation(
                    selectedTab = selectedTab,
                    isSearchActive = isSearchActive,
                    searchQuery = query,
                    searchFilter = filter,
                    isFilterMenuOpen = filterOpen,
                    onSelectTab = { selectedTab = it },
                    onOpenSearch = { isSearchActive = true },
                    onCloseSearch = {
                        isSearchActive = false
                        filterOpen = false
                    },
                    onQueryChange = { query = it },
                    onToggleFilterMenu = { filterOpen = !filterOpen },
                    onSelectFilter = {
                        filter = it
                        filterOpen = false
                    }
                )
            }
        }

        // Slide across tabs Início -> Biblioteca -> Ajustes
        composeTestRule.onNodeWithTag("nav_tab_home").assertIsSelected()
        composeTestRule.onNodeWithTag("nav_tab_library").performClick()
        composeTestRule.waitForIdle()
        assertEquals(BottomNavTab.LIBRARY, selectedTab)
        composeTestRule.onNodeWithTag("nav_tab_library").assertIsSelected()

        composeTestRule.onNodeWithTag("nav_tab_settings").performClick()
        composeTestRule.waitForIdle()
        assertEquals(BottomNavTab.SETTINGS, selectedTab)
        composeTestRule.onNodeWithTag("nav_tab_settings").assertIsSelected()

        // Tap circular Search button -> morphs into Filter + Search Field
        composeTestRule.onNodeWithTag("search_fab_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(isSearchActive)

        // Placeholder text & input field must be displayed
        composeTestRule.onNodeWithText("Músicas, Álbuns ou Artistas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("search_input_field").performTextInput("Golden")
        composeTestRule.waitForIdle()
        assertEquals("Golden", query)

        // Open auxiliary filter button -> select Vídeos
        composeTestRule.onNodeWithTag("search_filter_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("search_filter_panel").assertIsDisplayed()
        composeTestRule.onNodeWithTag("search_filter_videos").performClick()
        composeTestRule.waitForIdle()
        assertEquals(SearchFilter.VIDEOS, filter)

        // Close search -> returns to navigation capsule
        composeTestRule.onNodeWithTag("search_close_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(false, isSearchActive)
        composeTestRule.onNodeWithTag("nav_tab_settings").assertIsDisplayed()
    }

    @Test
    fun `offline local search filters songs albums artists and videos accurately`() = runBlocking {
        val song1 = MediaItemEntity(
            uri = "content://local/audio/1",
            mediaType = MediaType.AUDIO,
            title = "Golden Hour",
            artist = "JVKE",
            album = "This Is What Feels Like",
            genre = "Pop",
            durationMs = 209_000L,
            fileName = "golden_hour.mp3",
            folderName = "Music",
            folderPath = "/Music"
        )
        val song2 = MediaItemEntity(
            uri = "content://local/audio/2",
            mediaType = MediaType.AUDIO,
            title = "Nocturne",
            artist = "Chopin",
            album = "Piano Essentials",
            genre = "Classic",
            durationMs = 260_000L,
            fileName = "nocturne.flac",
            folderName = "Classical",
            folderPath = "/Classical"
        )
        val video1 = MediaItemEntity(
            uri = "content://local/video/1",
            mediaType = MediaType.VIDEO,
            title = "Golden Sunset Clip",
            artist = "Vídeo local",
            album = "Vídeos",
            durationMs = 95_000L,
            fileName = "golden_sunset.mp4",
            folderName = "Movies",
            folderPath = "/Movies",
            videoWidth = 1920,
            videoHeight = 1080
        )

        val id1 = db.mediaDao().insert(song1)
        db.mediaDao().insert(song2)
        db.mediaDao().insert(video1)

        val playlistId = repository.createPlaylist("Favoritas Chill", listOf(id1))
        assertTrue(playlistId > 0L)

        val qFlow = MutableStateFlow("golden")
        val fFlow = MutableStateFlow(SearchFilter.ALL)

        val allBundle = repository.searchLocal(qFlow, fFlow).first()
        assertEquals(1, allBundle.songs.size)
        assertEquals("Golden Hour", allBundle.songs.first().title)
        assertEquals(1, allBundle.videos.size)
        assertEquals("Golden Sunset Clip", allBundle.videos.first().title)

        // Filter to VIDEOS only
        fFlow.value = SearchFilter.VIDEOS
        val videoOnly = repository.searchLocal(qFlow, fFlow).first()
        assertEquals(0, videoOnly.songs.size)
        assertEquals(1, videoOnly.videos.size)

        // Filter to ARTISTS
        qFlow.value = "jvke"
        fFlow.value = SearchFilter.ARTISTS
        val artistOnly = repository.searchLocal(qFlow, fFlow).first()
        assertEquals(1, artistOnly.artists.size)
        assertEquals("JVKE", artistOnly.artists.first().name)

        // Verify sorting
        val sortedByDuration = repository.sortMedia(listOf(song1, song2), SortOrder.DURATION)
        assertEquals("Nocturne", sortedByDuration.first().title)
    }

    @Test
    fun `settings sobre row navigates to unified AboutScreen with developer privacy and licenses`() {
        var showAbout by mutableStateOf(false)
        var formatsOpened by mutableStateOf(false)

        composeTestRule.setContent {
            LumaTheme(settings = AppSettings(reduceMotion = true)) {
                if (!showAbout) {
                    SettingsScreen(
                        settings = AppSettings(reduceMotion = true),
                        mostPlayed = emptyList(),
                        totalIndexedCount = 0,
                        bottomInsetPadding = 92.dp,
                        onSetThemeMode = {},
                        onSetAccentPalette = {},
                        onSetGlassQuality = {},
                        onSetReduceMotion = {},
                        onSetHaptics = {},
                        onSetCrossfade = {},
                        onSetDynamicCover = {},
                        onSetVisualizer = {},
                        onSetCoverGestures = {},
                        onSetMinimalistMode = {},
                        onSetAlbumArtAnimation = {},
                        onImportFiles = {},
                        onImportFolder = {},
                        onRefreshLibrary = {},
                        onClearHistory = {},
                        onResetStats = {},
                        onOpenSupportedFormats = {},
                        onOpenAbout = { showAbout = true }
                    )
                } else {
                    AboutScreen(
                        bottomInsetPadding = 92.dp,
                        onBack = { showAbout = false },
                        onOpenSupportedFormats = { formatsOpened = true }
                    )
                }
            }
        }

        // Verify Settings screen has the "Sobre" row and clicking it opens AboutScreen
        composeTestRule.onNodeWithTag("settings_screen")
            .performScrollToNode(hasTestTag("settings_about_row"))
        composeTestRule.onNodeWithTag("settings_about_row").performClick()
        composeTestRule.waitForIdle()
        assertTrue(showAbout)

        composeTestRule.onNodeWithTag("about_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sobre").assertIsDisplayed()
        composeTestRule.onNodeWithText("Luma Player").assertIsDisplayed()
        composeTestRule.onNodeWithText("Programador e Desenvolvimento").assertIsDisplayed()
        composeTestRule.onNodeWithText("made by David tonecas helped by google AI studio").assertIsDisplayed()
        composeTestRule.onNodeWithText("Versão ${AppConfig.APP_VERSION}").assertIsDisplayed()

        composeTestRule.onNodeWithTag("about_screen")
            .performScrollToNode(hasText("Privacidade e Controlo"))
        composeTestRule.onNodeWithText("Privacidade e Controlo").assertIsDisplayed()

        composeTestRule.onNodeWithTag("about_screen")
            .performScrollToNode(hasText("Bibliotecas e Licenças"))
        composeTestRule.onNodeWithText("Bibliotecas e Licenças").assertIsDisplayed()

        composeTestRule.onNodeWithTag("about_screen")
            .performScrollToNode(hasTestTag("about_supported_formats_button"))
        composeTestRule.onNodeWithTag("about_supported_formats_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertTrue(formatsOpened)
    }

    @Test
    fun `favorites playlists history queue video position and smart mix persist properly`() = runBlocking {
        val songAId = db.mediaDao().insert(
            MediaItemEntity(
                uri = "content://local/audio/a",
                mediaType = MediaType.AUDIO,
                title = "Skyline",
                artist = "Luma Synth",
                album = "Horizon",
                genre = "Chill",
                durationMs = 220_000L,
                fileName = "skyline.mp3",
                folderName = "Music",
                folderPath = "/Music"
            )
        )
        val songBId = db.mediaDao().insert(
            MediaItemEntity(
                uri = "content://local/audio/b",
                mediaType = MediaType.AUDIO,
                title = "Deep Focus",
                artist = "Luma Synth",
                album = "Horizon",
                genre = "Instrumental",
                durationMs = 180_000L,
                fileName = "deep_focus.flac",
                folderName = "Music",
                folderPath = "/Music"
            )
        )
        val videoId = db.mediaDao().insert(
            MediaItemEntity(
                uri = "content://local/video/v1",
                mediaType = MediaType.VIDEO,
                title = "Ocean Timelapse",
                artist = "Vídeo local",
                album = "Vídeos",
                durationMs = 120_000L,
                fileName = "ocean.mp4",
                folderName = "Movies",
                folderPath = "/Movies",
                videoWidth = 1920,
                videoHeight = 1080
            )
        )

        // Favorites toggle
        val songA = repository.getMediaById(songAId)!!
        repository.toggleFavorite(songA)
        assertEquals(1, repository.favorites.first().size)
        repository.toggleFavorite(repository.getMediaById(songAId)!!)
        assertEquals(0, repository.favorites.first().size)

        // Playlist create, add, rename, remove track, delete
        val plId = repository.createPlaylist("Noite", listOf(songAId))
        repository.addTrackToPlaylist(plId, songBId)
        repository.renamePlaylist(plId, "Noite Chill")
        val updatedPl = repository.observePlaylist(plId).first()!!
        assertEquals("Noite Chill", updatedPl.playlist.name)
        assertEquals(2, updatedPl.tracks.size)

        repository.removeTrackFromPlaylist(plId, songAId)
        assertEquals(1, repository.observePlaylist(plId).first()!!.tracks.size)
        repository.deletePlaylist(plId)
        assertEquals(0, repository.playlists.first().size)

        // History recording + individual removal + clear
        repository.recordPlaybackStarted(songAId)
        repository.recordPlaybackStarted(videoId)
        val historyList = repository.history.first()
        assertEquals(2, historyList.size)
        repository.deleteHistoryEntry(historyList.first().history.id)
        assertEquals(1, repository.history.first().size)
        repository.clearHistory()
        assertEquals(0, repository.history.first().size)

        // Queue persistence & Video resume position
        repository.saveQueue(listOf(songBId, songAId))
        val loadedQueue = repository.loadSavedQueue()
        assertEquals(listOf(songBId, songAId), loadedQueue.map { it.id })

        repository.savePlaybackPosition(videoId, 45_000L)
        assertEquals(45_000L, repository.getMediaById(videoId)!!.lastPositionMs)

        // Smart Mix & Mood Collections
        val allAudio = repository.audioTracks.first()
        val smartMix = repository.buildSmartMix(songA, allAudio)
        assertEquals(songAId, smartMix.first().id)
        assertTrue(repository.buildMoodCollection(MoodType.CHILL, allAudio).isNotEmpty())
    }

    @Test
    fun `settings repository persists theme preferences and device codecs inspect cleanly`() = runBlocking {
        val freshDefaults = AppSettings()
        assertEquals(ThemeMode.SYSTEM, freshDefaults.themeMode)
        assertEquals(AppThemeStyle.LUMA_GLASS, freshDefaults.themeStyle)
        assertEquals(false, freshDefaults.hasSeenWelcomeIntro)

        val settingsRepo = SettingsRepository(context)
        val initialStored = settingsRepo.settingsFlow.first()
        assertEquals(ThemeMode.SYSTEM, initialStored.themeMode)
        assertEquals(AppThemeStyle.LUMA_GLASS, initialStored.themeStyle)

        settingsRepo.setThemeMode(ThemeMode.LIGHT)
        settingsRepo.setThemeStyle(AppThemeStyle.ALABASTER_BW)
        settingsRepo.setReduceMotion(true)

        val savedBw = settingsRepo.settingsFlow.first()
        assertEquals(ThemeMode.LIGHT, savedBw.themeMode)
        assertEquals(AppThemeStyle.ALABASTER_BW, savedBw.themeStyle)
        assertTrue(savedBw.reduceMotion)

        settingsRepo.setThemeStyle(AppThemeStyle.VELVET_VIOLET)
        val savedViolet = settingsRepo.settingsFlow.first()
        assertEquals(AppThemeStyle.VELVET_VIOLET, savedViolet.themeStyle)
        assertEquals(AccentPalette.VIOLET_PULSE, savedViolet.accentPalette)

        assertEquals(PlayerScreenStyle.GLASS_DECK, savedViolet.playerScreenStyle)
        settingsRepo.setPlayerScreenStyle(PlayerScreenStyle.MINIMAL_POSTER)
        assertEquals(PlayerScreenStyle.MINIMAL_POSTER, settingsRepo.settingsFlow.first().playerScreenStyle)
        settingsRepo.setPlayerScreenStyle(PlayerScreenStyle.LYRICS_STAGE)
        assertEquals(PlayerScreenStyle.LYRICS_STAGE, settingsRepo.settingsFlow.first().playerScreenStyle)

        assertEquals(false, settingsRepo.settingsFlow.first().hasSeenWelcomeIntro)
        settingsRepo.setWelcomeIntroSeen(true)
        assertEquals(true, settingsRepo.settingsFlow.first().hasSeenWelcomeIntro)
        settingsRepo.setWelcomeIntroSeen(false)
        assertEquals(false, settingsRepo.settingsFlow.first().hasSeenWelcomeIntro)

        settingsRepo.setThemeStyle(AppThemeStyle.ROSE_PORCELAIN)
        settingsRepo.setAccentPalette(AccentPalette.EMERALD_MINT)
        assertEquals(AppThemeStyle.ROSE_PORCELAIN, settingsRepo.settingsFlow.first().themeStyle)

        val codecs = FormatSupportChecker.inspectDeviceCodecs()
        assertTrue(codecs.any { !it.isVideo })
        assertTrue(codecs.any { it.isVideo })
    }

    @Test
    fun `mediastore permissions check and automatic library scanner sync audio and video`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val required = MediaPermissionHelper.requiredMediaPermissions().toSet()
        assertTrue(required.contains(Manifest.permission.READ_MEDIA_AUDIO))
        assertTrue(required.contains(Manifest.permission.READ_MEDIA_VIDEO))

        val initialStatus = MediaPermissionHelper.checkStatus(app)
        assertEquals(false, initialStatus.hasAnyMediaPermission)

        Shadows.shadowOf(app).grantPermissions(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VIDEO
        )
        val grantedStatus = MediaPermissionHelper.checkStatus(app)
        assertTrue(grantedStatus.hasAudioPermission)
        assertTrue(grantedStatus.hasVideoPermission)
        assertTrue(grantedStatus.hasAllMediaPermissions)

        var includeSecondTrack = true
        val fakeMediaProvider = object : ContentProvider() {
            override fun onCreate(): Boolean = true

            override fun query(
                uri: Uri,
                projection: Array<out String>?,
                selection: String?,
                selectionArgs: Array<out String>?,
                sortOrder: String?
            ): Cursor? {
                val uriStr = uri.toString()
                return when {
                    uriStr.contains("/audio/") -> {
                        MatrixCursor(
                            arrayOf(
                                MediaStore.Audio.Media._ID,
                                MediaStore.Audio.Media.TITLE,
                                MediaStore.Audio.Media.ARTIST,
                                MediaStore.Audio.Media.ALBUM,
                                MediaStore.Audio.Media.ALBUM_ID,
                                MediaStore.Audio.Media.DURATION,
                                MediaStore.Audio.Media.DISPLAY_NAME,
                                MediaStore.Audio.Media.MIME_TYPE,
                                MediaStore.Audio.Media.SIZE,
                                MediaStore.Audio.Media.DATE_ADDED,
                                MediaStore.Audio.Media.IS_MUSIC,
                                MediaStore.Audio.Media.RELATIVE_PATH,
                                MediaStore.Audio.Media.GENRE
                            )
                        ).apply {
                            addRow(
                                arrayOf<Any>(
                                    101L, "Midnight Glass", "Luma Artist", "Liquid Vibes", 55L,
                                    195_000L, "midnight_glass.mp3", "audio/mpeg", 4_200_000L, 1_700_000_000L,
                                    1, "Music/Luma/", "Electronic"
                                )
                            )
                            if (includeSecondTrack) {
                                addRow(
                                    arrayOf<Any>(
                                        102L, "Ephemeral Echo", "Luma Artist", "Liquid Vibes", 55L,
                                        180_000L, "echo.flac", "audio/flac", 22_000_000L, 1_700_000_100L,
                                        1, "Music/Luma/", "Ambient"
                                    )
                                )
                            }
                        }
                    }
                    uriStr.contains("/video/") -> {
                        MatrixCursor(
                            arrayOf(
                                MediaStore.Video.Media._ID,
                                MediaStore.Video.Media.TITLE,
                                MediaStore.Video.Media.ARTIST,
                                MediaStore.Video.Media.ALBUM,
                                MediaStore.Video.Media.DURATION,
                                MediaStore.Video.Media.DISPLAY_NAME,
                                MediaStore.Video.Media.MIME_TYPE,
                                MediaStore.Video.Media.SIZE,
                                MediaStore.Video.Media.WIDTH,
                                MediaStore.Video.Media.HEIGHT,
                                MediaStore.Video.Media.DATE_ADDED,
                                MediaStore.Video.Media.RELATIVE_PATH
                            )
                        ).apply {
                            addRow(
                                arrayOf<Any>(
                                    201L, "Aurora Borealis", "Cam", "Trips",
                                    90_000L, "aurora.mp4", "video/mp4", 18_000_000L,
                                    1920, 1080, 1_700_000_200L, "Movies/Trips/"
                                )
                            )
                        }
                    }
                    else -> null
                }
            }

            override fun getType(uri: Uri): String? = null
            override fun insert(uri: Uri, values: ContentValues?): Uri? = null
            override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
            override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
        }

        ShadowContentResolver.registerProviderInternal(MediaStore.AUTHORITY, fakeMediaProvider)

        val firstSync = repository.syncFromMediaStore(scanAudio = true, scanVideo = true)
        assertEquals(3, firstSync.totalScanned)
        assertEquals(3, firstSync.addedCount)

        val audios = repository.audioTracks.first()
        val vids = repository.videos.first()
        assertEquals(2, audios.size)
        assertEquals(1, vids.size)
        assertEquals("Luma", audios.first { it.title == "Midnight Glass" }.folderName)
        assertTrue(audios.first { it.title == "Midnight Glass" }.artworkPath?.contains("albumart/55") == true)
        assertEquals(1920, vids.first().videoWidth)

        // Favorite one track and re-scan when second track is deleted from device
        val midnight = audios.first { it.title == "Midnight Glass" }
        repository.toggleFavorite(midnight)
        includeSecondTrack = false

        val secondSync = repository.syncFromMediaStore(scanAudio = true, scanVideo = true)
        assertEquals(2, secondSync.totalScanned)
        assertEquals(1, secondSync.missingCount)

        val refreshedMidnight = repository.getMediaById(midnight.id)!!
        assertTrue(refreshedMidnight.isFavorite)
        assertTrue(refreshedMidnight.isAvailable)

        val removedTrack = repository.audioTracks.first().first { it.title == "Ephemeral Echo" }
        assertEquals(false, removedTrack.isAvailable)
    }

    @Test
    fun `lrc parser parses timestamps chronologically and finds active lyric line`() {
        val rawLrc = """
            [ar:JVKE]
            [ti:Golden Hour]
            [00:12.50][00:45.00]It was just two lovers
            [00:05.00]Sittin' in the car
            [00:18.250]Listening to Blonde
        """.trimIndent()

        val parsed = LrcParser.parse(rawLrc)
        assertTrue(parsed.isTimed)
        assertEquals(4, parsed.lines.size)
        assertEquals(5_000L, parsed.lines[0].timeMs)
        assertEquals("Sittin' in the car", parsed.lines[0].text)
        assertEquals(12_500L, parsed.lines[1].timeMs)
        assertEquals(18_250L, parsed.lines[2].timeMs)
        assertEquals(45_000L, parsed.lines[3].timeMs)

        assertEquals(-1, LrcParser.findActiveLineIndex(parsed.lines, 1_000L))
        assertEquals(0, LrcParser.findActiveLineIndex(parsed.lines, 6_000L))
        assertEquals(1, LrcParser.findActiveLineIndex(parsed.lines, 14_000L))
        assertEquals(3, LrcParser.findActiveLineIndex(parsed.lines, 46_000L))

        val plain = LrcParser.parse("First line\nSecond line")
        assertEquals(false, plain.isTimed)
        assertEquals(2, plain.lines.size)
    }

    @Test
    fun `playlist reordering genres duplicates and metadata editing persist cleanly`() = runBlocking {
        val id1 = db.mediaDao().insert(
            MediaItemEntity(
                uri = "content://local/audio/r1",
                mediaType = MediaType.AUDIO,
                title = "Aurora",
                artist = "Nova",
                album = "Dawn",
                genre = "Electronic",
                durationMs = 190_000L,
                sizeBytes = 5_000_000L,
                fileName = "aurora.mp3",
                folderName = "Music",
                folderPath = "/Music"
            )
        )
        val id2 = db.mediaDao().insert(
            MediaItemEntity(
                uri = "content://local/audio/r2",
                mediaType = MediaType.AUDIO,
                title = "Aurora",
                artist = "Nova",
                album = "Dawn",
                genre = "Electronic",
                durationMs = 190_000L,
                sizeBytes = 5_000_000L,
                fileName = "aurora_copy.mp3",
                folderName = "Downloads",
                folderPath = "/Downloads"
            )
        )
        val id3 = db.mediaDao().insert(
            MediaItemEntity(
                uri = "content://local/audio/r3",
                mediaType = MediaType.AUDIO,
                title = "Solstice",
                artist = "Sol",
                album = "Noon",
                genre = "Ambient",
                durationMs = 210_000L,
                sizeBytes = 6_000_000L,
                fileName = "solstice.flac",
                folderName = "Music",
                folderPath = "/Music"
            )
        )

        // Playlist track reordering preserves exact positions
        val plId = repository.createPlaylist("Ordem", listOf(id1, id2, id3))
        assertEquals(listOf(id1, id2, id3), repository.observePlaylist(plId).first()!!.tracks.map { it.id })

        repository.movePlaylistTrack(plId, 0, 2)
        assertEquals(listOf(id2, id3, id1), repository.observePlaylist(plId).first()!!.tracks.map { it.id })

        // Genres grouping (sorted alphabetically by genre name)
        val genreList = repository.genres.first()
        assertEquals(listOf("Ambient", "Electronic"), genreList.map { it.name })
        assertEquals(2, genreList.first { it.name == "Electronic" }.trackCount)

        // Potential duplicates detection
        val all = repository.allMedia.first()
        val duplicates = repository.findPotentialDuplicates(all)
        assertEquals(1, duplicates.size)
        assertEquals(2, duplicates.first().items.size)

        // Metadata editing updates title, artist, album, genre
        repository.updateTrackMetadata(id3, "Solstice Remaster", "Sol", "Noon II", "Chillout")
        val updated = repository.getMediaById(id3)!!
        assertEquals("Solstice Remaster", updated.title)
        assertEquals("Chillout", updated.genre)
    }

    @Test
    fun `mini player previous button and settings clear cache dialog work`() {
        var prevClicked = false
        var cacheCleared = false

        val track = MediaItemEntity(
            id = 1L,
            uri = "content://local/audio/m1",
            mediaType = MediaType.AUDIO,
            title = "Midnight",
            artist = "Luma",
            album = "Glass",
            durationMs = 200_000L,
            fileName = "midnight.mp3",
            folderName = "Music",
            folderPath = "/Music"
        )

        composeTestRule.setContent {
            LumaTheme(settings = AppSettings(reduceMotion = true)) {
                androidx.compose.foundation.layout.Column {
                    MiniPlayerBar(
                        playbackState = PlaybackUiState(
                            currentTrack = track,
                            isPlaying = true,
                            positionMs = 45_000L,
                            durationMs = 200_000L
                        ),
                        onExpand = {},
                        onPrevious = { prevClicked = true },
                        onTogglePlayPause = {},
                        onNext = {},
                        onDismiss = {}
                    )
                    SettingsScreen(
                        settings = AppSettings(reduceMotion = true),
                        mostPlayed = emptyList(),
                        totalIndexedCount = 1,
                        bottomInsetPadding = 92.dp,
                        onSetThemeMode = {},
                        onSetAccentPalette = {},
                        onSetGlassQuality = {},
                        onSetReduceMotion = {},
                        onSetHaptics = {},
                        onSetCrossfade = {},
                        onSetDynamicCover = {},
                        onSetVisualizer = {},
                        onSetCoverGestures = {},
                        onSetMinimalistMode = {},
                        onSetAlbumArtAnimation = {},
                        onImportFiles = {},
                        onImportFolder = {},
                        onRefreshLibrary = {},
                        onOpenSupportedFormats = {},
                        onOpenAbout = {},
                        onClearHistory = {},
                        onResetStats = {},
                        cacheSizeBytes = 2_048_000L,
                        onClearCache = { cacheCleared = true }
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("mini_player_previous").performClick()
        composeTestRule.waitForIdle()
        assertTrue(prevClicked)

        composeTestRule.onNodeWithTag("settings_screen")
            .performScrollToNode(hasTestTag("settings_clear_cache_row"))
        composeTestRule.onNodeWithTag("settings_clear_cache_row").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Limpar cache").performClick()
        composeTestRule.waitForIdle()
        assertTrue(cacheCleared)
    }

    @Test
    fun `light and dark theme tokens resolve expected palettes and switch cleanly in UI`() {
        var currentThemeMode by mutableStateOf(ThemeMode.DARK)
        var capturedTokens: LumaGlassTokens? = null

        val track = MediaItemEntity(
            id = 7L,
            uri = "content://local/audio/t7",
            mediaType = MediaType.AUDIO,
            title = "Liquid Horizon",
            artist = "Luma",
            album = "Glass V2",
            durationMs = 190_000L,
            fileName = "liquid.mp3",
            folderName = "Music",
            folderPath = "/Music"
        )

        composeTestRule.setContent {
            LumaTheme(
                settings = AppSettings(
                    themeMode = currentThemeMode,
                    themeStyle = AppThemeStyle.LUMA_GLASS,
                    accentPalette = AccentPalette.EMERALD_MINT,
                    reduceMotion = true
                )
            ) {
                capturedTokens = LocalLumaGlass.current
                androidx.compose.foundation.layout.Column {
                    MiniPlayerBar(
                        playbackState = PlaybackUiState(
                            currentTrack = track,
                            isPlaying = false,
                            positionMs = 30_000L,
                            durationMs = 190_000L
                        ),
                        onExpand = {},
                        onPrevious = {},
                        onTogglePlayPause = {},
                        onNext = {},
                        onDismiss = {}
                    )
                    SettingsScreen(
                        settings = AppSettings(
                            themeMode = currentThemeMode,
                            themeStyle = AppThemeStyle.LUMA_GLASS,
                            accentPalette = AccentPalette.EMERALD_MINT,
                            reduceMotion = true
                        ),
                        mostPlayed = emptyList(),
                        totalIndexedCount = 1,
                        bottomInsetPadding = 92.dp,
                        onSetThemeMode = { currentThemeMode = it },
                        onSetAccentPalette = {},
                        onSetGlassQuality = {},
                        onSetReduceMotion = {},
                        onSetHaptics = {},
                        onSetCrossfade = {},
                        onSetDynamicCover = {},
                        onSetVisualizer = {},
                        onSetCoverGestures = {},
                        onSetMinimalistMode = {},
                        onSetAlbumArtAnimation = {},
                        onImportFiles = {},
                        onImportFolder = {},
                        onRefreshLibrary = {},
                        onOpenSupportedFormats = {},
                        onOpenAbout = {},
                        onClearHistory = {},
                        onResetStats = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        assertEquals(true, capturedTokens?.isDark)
        assertEquals(DeepNavyBg, capturedTokens?.background)
        assertEquals(LightTextPrimary, capturedTokens?.textPrimary)
        assertEquals(EmeraldLight, capturedTokens?.accentPrimary)

        // Switch to Light Mode in SettingsScreen
        composeTestRule.onNodeWithTag("settings_screen")
            .performScrollToNode(hasTestTag("theme_light"))
        composeTestRule.onNodeWithTag("theme_light").performClick()
        composeTestRule.waitForIdle()
        assertEquals(ThemeMode.LIGHT, currentThemeMode)
        assertEquals(false, capturedTokens?.isDark)
        assertEquals(FrostedIceBg, capturedTokens?.background)
        assertEquals(DarkTextPrimary, capturedTokens?.textPrimary)
        assertEquals(EmeraldMint, capturedTokens?.accentPrimary)

        // Switch back to Dark and System cleanly
        composeTestRule.onNodeWithTag("theme_dark").performClick()
        composeTestRule.waitForIdle()
        assertEquals(ThemeMode.DARK, currentThemeMode)
        assertEquals(true, capturedTokens?.isDark)

        composeTestRule.onNodeWithTag("theme_system").performClick()
        composeTestRule.waitForIdle()
        assertEquals(ThemeMode.SYSTEM, currentThemeMode)
    }

    @Test
    fun `seven theme styles switch cleanly with soft bw neumorphic amoled and liquid glass tokens`() {
        var currentThemeMode by mutableStateOf(ThemeMode.LIGHT)
        var currentStyle by mutableStateOf(AppThemeStyle.ALABASTER_BW)
        var capturedTokens: LumaGlassTokens? = null

        val track = MediaItemEntity(
            id = 9L,
            uri = "content://local/audio/t9",
            mediaType = MediaType.AUDIO,
            title = "The Box",
            artist = "Luma Session",
            album = "Minimal",
            durationMs = 165_000L,
            fileName = "the_box.mp3",
            folderName = "Music",
            folderPath = "/Music"
        )

        composeTestRule.setContent {
            val activeSettings = AppSettings(
                themeMode = currentThemeMode,
                themeStyle = currentStyle,
                reduceMotion = true
            )
            LumaTheme(settings = activeSettings) {
                capturedTokens = LocalLumaGlass.current
                androidx.compose.foundation.layout.Column {
                    MiniPlayerBar(
                        playbackState = PlaybackUiState(
                            currentTrack = track,
                            isPlaying = true,
                            positionMs = 20_000L,
                            durationMs = 165_000L
                        ),
                        onExpand = {},
                        onPrevious = {},
                        onTogglePlayPause = {},
                        onNext = {},
                        onDismiss = {}
                    )
                    SettingsScreen(
                        settings = activeSettings,
                        mostPlayed = emptyList(),
                        totalIndexedCount = 1,
                        bottomInsetPadding = 92.dp,
                        onSetThemeMode = { currentThemeMode = it },
                        onSetAccentPalette = { currentStyle = AppThemeStyle.LUMA_GLASS },
                        onSetThemeStyle = { currentStyle = it },
                        onSetGlassQuality = {},
                        onSetReduceMotion = {},
                        onSetHaptics = {},
                        onSetCrossfade = {},
                        onSetDynamicCover = {},
                        onSetVisualizer = {},
                        onSetCoverGestures = {},
                        onSetMinimalistMode = {},
                        onSetAlbumArtAnimation = {},
                        onImportFiles = {},
                        onImportFolder = {},
                        onRefreshLibrary = {},
                        onOpenSupportedFormats = {},
                        onOpenAbout = {},
                        onClearHistory = {},
                        onResetStats = {}
                    )
                }
            }
        }

        // 1. Alabaster B&W Light: Soft matte alabaster & pure black monochrome ink
        composeTestRule.waitForIdle()
        assertEquals(8, AppThemeStyle.entries.size)
        assertEquals(SurfaceMaterial.NEUMORPHIC_BW, capturedTokens?.surfaceMaterial)
        assertEquals(true, capturedTokens?.isMonochrome)
        assertEquals(NeuBwLightBg, capturedTokens?.background)
        assertEquals(NeuBwInkBlack, capturedTokens?.textPrimary)

        // Select Branco Soft (Clear White Soft UI + Pink signature) in Light mode first
        composeTestRule.onNodeWithTag("settings_screen")
            .performScrollToNode(hasTestTag("theme_carousel"))
        composeTestRule.onNodeWithTag("theme_carousel")
            .performScrollToNode(hasTestTag("theme_card_rose_porcelain"))
        composeTestRule.onNodeWithTag("theme_card_rose_porcelain").performClick()
        composeTestRule.waitForIdle()
        assertEquals(AppThemeStyle.ROSE_PORCELAIN, currentStyle)
        assertEquals(SurfaceMaterial.NEUMORPHIC_SOFT, capturedTokens?.surfaceMaterial)
        assertEquals(false, capturedTokens?.isMonochrome)
        assertEquals(true, capturedTokens?.isNeumorphic)
        assertEquals(SoftWhiteLightBg, capturedTokens?.background)
        assertEquals(SoftWhiteLightElevated, capturedTokens?.surfaceElevated)
        assertEquals(SoftPinkPrimary, capturedTokens?.accentPrimary)

        // Switch back to Alabaster B&W and toggle Dark Mode -> Matte Graphite Noir & white ink
        composeTestRule.onNodeWithTag("theme_carousel")
            .performScrollToNode(hasTestTag("theme_card_alabaster_bw"))
        composeTestRule.onNodeWithTag("theme_card_alabaster_bw").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("theme_dark").performClick()
        composeTestRule.waitForIdle()
        assertEquals(NeuBwDarkBg, capturedTokens?.background)
        assertEquals(NeuBwInkWhite, capturedTokens?.textPrimary)
        assertEquals(true, capturedTokens?.isMonochrome)

        // 2. Select Obsidian AMOLED preview card
        composeTestRule.onNodeWithTag("theme_carousel")
            .performScrollToNode(hasTestTag("theme_card_obsidian_amoled"))
        composeTestRule.onNodeWithTag("theme_card_obsidian_amoled").performClick()
        composeTestRule.waitForIdle()
        assertEquals(AppThemeStyle.OBSIDIAN_AMOLED, currentStyle)
        assertEquals(SurfaceMaterial.AMOLED_CRISP, capturedTokens?.surfaceMaterial)
        assertEquals(AmoledDarkBg, capturedTokens?.background)

        // 3. Select Ocean Sapphire Liquid Glass card
        composeTestRule.onNodeWithTag("theme_carousel")
            .performScrollToNode(hasTestTag("theme_card_ocean_sapphire"))
        composeTestRule.onNodeWithTag("theme_card_ocean_sapphire").performClick()
        composeTestRule.waitForIdle()
        assertEquals(AppThemeStyle.OCEAN_SAPPHIRE, currentStyle)
        assertEquals(SurfaceMaterial.LIQUID_GLASS, capturedTokens?.surfaceMaterial)
        assertEquals(OceanCyanLight, capturedTokens?.accentPrimary)

        // 4. Select Velvet Violet, Sunset Amber, Nordic Forest cards
        composeTestRule.onNodeWithTag("theme_carousel")
            .performScrollToNode(hasTestTag("theme_card_velvet_violet"))
        composeTestRule.onNodeWithTag("theme_card_velvet_violet").performClick()
        composeTestRule.waitForIdle()
        assertEquals(VioletLight, capturedTokens?.accentPrimary)

        composeTestRule.onNodeWithTag("theme_carousel")
            .performScrollToNode(hasTestTag("theme_card_sunset_amber"))
        composeTestRule.onNodeWithTag("theme_card_sunset_amber").performClick()
        composeTestRule.waitForIdle()
        assertEquals(SunsetAmberLight, capturedTokens?.accentPrimary)

        composeTestRule.onNodeWithTag("theme_carousel")
            .performScrollToNode(hasTestTag("theme_card_nordic_forest"))
        composeTestRule.onNodeWithTag("theme_card_nordic_forest").performClick()
        composeTestRule.waitForIdle()
        assertEquals(AppThemeStyle.NORDIC_FOREST, currentStyle)
        assertEquals(ForestDarkBg, capturedTokens?.background)

        // Switching from Dark mode to Branco Soft automatically restores crisp Light White mode
        composeTestRule.onNodeWithTag("theme_carousel")
            .performScrollToNode(hasTestTag("theme_card_rose_porcelain"))
        composeTestRule.onNodeWithTag("theme_card_rose_porcelain").performClick()
        composeTestRule.waitForIdle()
        assertEquals(AppThemeStyle.ROSE_PORCELAIN, currentStyle)
        assertEquals(ThemeMode.LIGHT, currentThemeMode)
        assertEquals(SoftWhiteLightBg, capturedTokens?.background)
    }

    @Test
    fun `now playing spatial deck shows prev current next covers and switches styles cleanly`() {
        val prevTrack = MediaItemEntity(
            id = 11L,
            uri = "content://local/audio/n1",
            mediaType = MediaType.AUDIO,
            title = "Blinding Lights",
            artist = "The Weeknd",
            album = "After Hours",
            durationMs = 200_000L,
            fileName = "blinding.mp3",
            folderName = "Music",
            folderPath = "/Music"
        )
        val currentTrack = MediaItemEntity(
            id = 12L,
            uri = "content://local/audio/n2",
            mediaType = MediaType.AUDIO,
            title = "Shape of You",
            artist = "Ed Sheeran",
            album = "Divide",
            durationMs = 234_000L,
            fileName = "shape.mp3",
            folderName = "Music",
            folderPath = "/Music",
            embeddedLyrics = "[00:05.00]The club isn't the best place to find a lover"
        )
        val nextTrack = MediaItemEntity(
            id = 13L,
            uri = "content://local/audio/n3",
            mediaType = MediaType.AUDIO,
            title = "Bad Guy",
            artist = "Billie Eilish",
            album = "WWAFA",
            durationMs = 194_000L,
            fileName = "bad_guy.mp3",
            folderName = "Music",
            folderPath = "/Music"
        )

        val queue = listOf(prevTrack, currentTrack, nextTrack)
        var currentIndex by mutableStateOf(1)
        var isPlaying by mutableStateOf(true)
        var selectedStyle by mutableStateOf(PlayerScreenStyle.GLASS_DECK)
        var lastJumpedIndex = -1
        var lyricsClicked = false
        var queueClicked = false

        composeTestRule.setContent {
            val activeTrack = queue[currentIndex]
            val playbackState = PlaybackUiState(
                queue = queue,
                currentIndex = currentIndex,
                currentTrack = activeTrack,
                isPlaying = isPlaying,
                positionMs = 12_000L,
                durationMs = activeTrack.durationMs
            )
            LumaTheme(settings = AppSettings(reduceMotion = true, playerScreenStyle = selectedStyle)) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    when (selectedStyle) {
                        PlayerScreenStyle.GLASS_DECK -> {
                            SpatialGlassCoverDeck(
                                playbackState = playbackState,
                                isPlayingForeground = isPlaying,
                                gesturesEnabled = true,
                                onPrevious = { currentIndex = (currentIndex - 1).coerceAtLeast(0) },
                                onNext = { currentIndex = (currentIndex + 1).coerceAtMost(queue.lastIndex) },
                                onTogglePlayPause = { isPlaying = !isPlaying },
                                onJumpToIndex = { idx ->
                                    lastJumpedIndex = idx
                                    currentIndex = idx
                                },
                                onToggleFavorite = {}
                            )
                            FloatingGlassPlayerDock(
                                track = activeTrack,
                                playbackState = playbackState,
                                isPlayingForeground = isPlaying,
                                minimalistMode = false,
                                onPrevious = { currentIndex = (currentIndex - 1).coerceAtLeast(0) },
                                onTogglePlayPause = { isPlaying = !isPlaying },
                                onNext = { currentIndex = (currentIndex + 1).coerceAtMost(queue.lastIndex) },
                                onOpenLyrics = { lyricsClicked = true },
                                onOpenQueue = { queueClicked = true },
                                onOpenEqualizer = {},
                                onOpenOptions = {}
                            )
                        }
                        PlayerScreenStyle.MINIMAL_POSTER -> {
                            PosterImmersiveStage(
                                track = activeTrack,
                                playbackState = playbackState,
                                gesturesEnabled = true,
                                onPrevious = {},
                                onNext = {},
                                onToggleFavorite = {}
                            )
                        }
                        PlayerScreenStyle.LYRICS_STAGE -> {
                            LyricsStageView(
                                track = activeTrack,
                                positionMs = 12_000L,
                                onSeekTo = {},
                                onImportLyrics = {},
                                onToggleFavorite = {},
                                modifier = Modifier.height(280.dp)
                            )
                        }
                        PlayerScreenStyle.CAROUSEL_VINYL -> Unit
                    }

                    PlayerStylePickerSheetContent(
                        selectedStyle = selectedStyle,
                        onSelectStyle = { selectedStyle = it }
                    )
                }
            }
        }

        // Verify left (previous), center (now playing), right (next) cover cards and floating dock
        composeTestRule.onNodeWithTag("nps_spatial_cover_deck").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nps_deck_prev_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nps_deck_current_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nps_deck_next_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nps_floating_dock").assertIsDisplayed()

        composeTestRule.onNodeWithText("Blinding Lights").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bad Guy").assertIsDisplayed()

        // Tap right next card -> jumps directly to queue index 2 ("Bad Guy")
        composeTestRule.onNodeWithTag("nps_deck_next_card").performClick()
        composeTestRule.waitForIdle()
        assertEquals(2, lastJumpedIndex)
        assertEquals(2, currentIndex)

        // Tap left prev card -> jumps directly back to queue index 1 ("Shape of You")
        composeTestRule.onNodeWithTag("nps_deck_prev_card").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, lastJumpedIndex)
        assertEquals(1, currentIndex)

        // Tap play/pause inside FloatingGlassPlayerDock
        composeTestRule.onNodeWithTag("player_play_pause_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(false, isPlaying)

        // Switch Now Playing Screen style via picker options
        composeTestRule.onNodeWithTag("player_style_option_minimal_poster")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals(PlayerScreenStyle.MINIMAL_POSTER, selectedStyle)
        composeTestRule.onNodeWithTag("nps_poster_stage")
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("player_style_option_lyrics_stage")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals(PlayerScreenStyle.LYRICS_STAGE, selectedStyle)
        composeTestRule.onNodeWithTag("nps_lyrics_stage")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("The club isn't the best place to find a lover")
            .assertIsDisplayed()
    }

    @Test
    fun `settings player style carousel updates player screen style preference`() {
        var selectedStyle by mutableStateOf(PlayerScreenStyle.GLASS_DECK)

        composeTestRule.setContent {
            val activeSettings = AppSettings(
                reduceMotion = true,
                playerScreenStyle = selectedStyle
            )
            LumaTheme(settings = activeSettings) {
                SettingsScreen(
                    settings = activeSettings,
                    mostPlayed = emptyList(),
                    totalIndexedCount = 0,
                    bottomInsetPadding = 92.dp,
                    onSetThemeMode = {},
                    onSetAccentPalette = {},
                    onSetPlayerScreenStyle = { selectedStyle = it },
                    onSetGlassQuality = {},
                    onSetReduceMotion = {},
                    onSetHaptics = {},
                    onSetCrossfade = {},
                    onSetDynamicCover = {},
                    onSetVisualizer = {},
                    onSetCoverGestures = {},
                    onSetMinimalistMode = {},
                    onSetAlbumArtAnimation = {},
                    onImportFiles = {},
                    onImportFolder = {},
                    onRefreshLibrary = {},
                    onOpenSupportedFormats = {},
                    onOpenAbout = {},
                    onClearHistory = {},
                    onResetStats = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("settings_screen")
            .performScrollToNode(hasTestTag("settings_player_style_carousel"))
        composeTestRule.onNodeWithTag("settings_player_style_carousel")
            .performScrollToNode(hasTestTag("settings_player_style_carousel_vinyl"))
        composeTestRule.onNodeWithTag("settings_player_style_carousel_vinyl").performClick()
        composeTestRule.waitForIdle()
        assertEquals(PlayerScreenStyle.CAROUSEL_VINYL, selectedStyle)
    }

    @Test
    fun `light mode harmonizes home bottom bar switches and tokens across all seven themes`() {
        var currentStyle by mutableStateOf(AppThemeStyle.ALABASTER_BW)
        var reduceMotion by mutableStateOf(true)
        var capturedTokens: LumaGlassTokens? = null
        var capturedColorScheme: ColorScheme? = null

        composeTestRule.setContent {
            val activeSettings = AppSettings(
                themeMode = ThemeMode.LIGHT,
                themeStyle = currentStyle,
                reduceMotion = reduceMotion
            )
            LumaTheme(settings = activeSettings) {
                capturedTokens = LocalLumaGlass.current
                capturedColorScheme = MaterialTheme.colorScheme
                Column {
                    LiquidMorphBottomNavigation(
                        selectedTab = BottomNavTab.SETTINGS,
                        isSearchActive = false,
                        searchQuery = "",
                        searchFilter = SearchFilter.ALL,
                        isFilterMenuOpen = false,
                        onSelectTab = {},
                        onOpenSearch = {},
                        onCloseSearch = {},
                        onQueryChange = {},
                        onToggleFilterMenu = {},
                        onSelectFilter = {}
                    )
                    SettingsScreen(
                        settings = activeSettings,
                        mostPlayed = emptyList(),
                        totalIndexedCount = 0,
                        bottomInsetPadding = 92.dp,
                        onSetThemeMode = {},
                        onSetThemeStyle = { currentStyle = it },
                        onSetGlassQuality = {},
                        onSetReduceMotion = { reduceMotion = it },
                        onSetHaptics = {},
                        onSetCrossfade = {},
                        onSetDynamicCover = {},
                        onSetVisualizer = {},
                        onSetCoverGestures = {},
                        onSetMinimalistMode = {},
                        onSetAlbumArtAnimation = {},
                        onImportFiles = {},
                        onImportFolder = {},
                        onRefreshLibrary = {},
                        onOpenSupportedFormats = {},
                        onOpenAbout = {},
                        onClearHistory = {},
                        onResetStats = {}
                    )
                }
            }
        }

        AppThemeStyle.entries.forEach { style ->
            composeTestRule.runOnUiThread { currentStyle = style }
            composeTestRule.waitForIdle()

            val tokens = capturedTokens!!
            val scheme = capturedColorScheme!!
            assertEquals(false, tokens.isDark)
            assertEquals(style, tokens.themeStyle)
            // Theme-matched container tokens ensure M3 Switches/Menus never fall back to lilac defaults
            assertNotEquals(androidx.compose.ui.graphics.Color(0xFFE6E0E9), scheme.surfaceContainerHighest)
            assertEquals(tokens.surfaceElevated, scheme.surfaceContainerHigh)
            // Elevated cards stay visually distinct from background in White Mode
            assertNotEquals(tokens.background, tokens.surfaceElevated)
        }

        // Verify bottom navigation capsule, search FAB, and Settings toggles interact cleanly in White Mode
        composeTestRule.onNodeWithTag("nav_tab_home").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_settings").assertIsSelected()
        composeTestRule.onNodeWithTag("search_fab_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_screen")
            .performScrollToNode(hasTestTag("toggle_reduce_motion"))
        composeTestRule.onNodeWithTag("toggle_reduce_motion")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals(false, reduceMotion)
    }

    @Test
    fun `icon only bottom bar omits tab text labels and runs bounce and roll animations cleanly`() {
        var selectedTab by mutableStateOf(BottomNavTab.HOME)

        composeTestRule.setContent {
            LumaTheme(
                settings = AppSettings(
                    themeMode = ThemeMode.DARK,
                    themeStyle = AppThemeStyle.OCEAN_SAPPHIRE,
                    reduceMotion = false
                )
            ) {
                LiquidMorphBottomNavigation(
                    selectedTab = selectedTab,
                    isSearchActive = false,
                    searchQuery = "",
                    searchFilter = SearchFilter.ALL,
                    isFilterMenuOpen = false,
                    onSelectTab = { selectedTab = it },
                    onOpenSearch = {},
                    onCloseSearch = {},
                    onQueryChange = {},
                    onToggleFilterMenu = {},
                    onSelectFilter = {}
                )
            }
        }

        // Icon-only: no text labels beneath tabs
        composeTestRule.onNodeWithText("Início").assertDoesNotExist()
        composeTestRule.onNodeWithText("Biblioteca").assertDoesNotExist()
        composeTestRule.onNodeWithText("Ajustes").assertDoesNotExist()

        // Accessibility content descriptions and test tags remain intact
        composeTestRule.onNodeWithContentDescription("Início").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Biblioteca").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Ajustes").assertIsDisplayed()

        // Click Library (tilt + bounce) then Settings (gear roll + bounce) then Settings again
        composeTestRule.onNodeWithTag("nav_tab_library").performClick()
        composeTestRule.waitForIdle()
        assertEquals(BottomNavTab.LIBRARY, selectedTab)

        composeTestRule.onNodeWithTag("nav_tab_settings").performClick()
        composeTestRule.waitForIdle()
        assertEquals(BottomNavTab.SETTINGS, selectedTab)

        composeTestRule.onNodeWithTag("nav_tab_settings").performClick()
        composeTestRule.waitForIdle()
        assertEquals(BottomNavTab.SETTINGS, selectedTab)
    }

    @Test
    fun `animated switcher displays ON and OFF text states and section entrance renders cleanly`() {
        var isChecked by mutableStateOf(false)

        composeTestRule.setContent {
            LumaTheme(
                settings = AppSettings(
                    themeMode = ThemeMode.LIGHT,
                    themeStyle = AppThemeStyle.LUMA_GLASS,
                    reduceMotion = false
                )
            ) {
                LumaSectionEntrance(index = 0) {
                    LumaAnimatedSwitch(
                        checked = isChecked,
                        onCheckedChange = { isChecked = it },
                        testTag = "custom_animated_switch"
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("OFF").assertIsDisplayed()
        composeTestRule.onNodeWithText("ON").assertDoesNotExist()

        composeTestRule.onNodeWithTag("custom_animated_switch").performClick()
        composeTestRule.waitForIdle()
        assertEquals(true, isChecked)
        composeTestRule.onNodeWithText("ON").assertIsDisplayed()
        composeTestRule.onNodeWithText("OFF").assertDoesNotExist()
    }

    @Test
    fun `first time welcome intro adapts to dark mode omits entrance logo allows theme choice and returning users see welcome back`() {
        var enterCount = 0
        var selectedStyle by mutableStateOf(AppThemeStyle.LUMA_GLASS)
        var capturedIsDark = false
        var showWelcomeBack by mutableStateOf(false)

        composeTestRule.setContent {
            LumaTheme(
                settings = AppSettings(
                    themeMode = ThemeMode.DARK,
                    themeStyle = selectedStyle,
                    reduceMotion = false
                )
            ) {
                capturedIsDark = LocalLumaGlass.current.isDark
                if (!showWelcomeBack) {
                    LumaWelcomeIntroOverlay(
                        selectedThemeStyle = selectedStyle,
                        reduceMotion = false,
                        hapticsEnabled = false,
                        onSelectThemeStyle = { selectedStyle = it },
                        onEnter = { enterCount++ }
                    )
                } else {
                    WelcomeBackBanner(onDismiss = { showWelcomeBack = false })
                }
            }
        }

        composeTestRule.waitForIdle()
        // Dark Mode never forces white background on welcome intro
        assertEquals(true, capturedIsDark)
        composeTestRule.onNodeWithTag("welcome_intro_overlay").assertIsDisplayed()
        // Entrance icon logo is eliminated from startup welcome screen
        composeTestRule.onNodeWithTag("animated_luma_pink_l").assertDoesNotExist()
        composeTestRule.onNodeWithTag("welcome_intro_headline").assertIsDisplayed()
        composeTestRule.onNodeWithText("Onde cada som encontra a sua luz.").assertIsDisplayed()
        composeTestRule.onNodeWithTag("welcome_color_picker_card").assertIsDisplayed()

        // First-time user selects a theme color right in the intro
        composeTestRule.onNodeWithTag("welcome_theme_option_velvet_violet")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals(AppThemeStyle.VELVET_VIOLET, selectedStyle)

        composeTestRule.onNodeWithTag("welcome_intro_enter_button")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertEquals(1, enterCount)

        // Returning users get smooth Welcome Back banner instead of onboarding overlay
        composeTestRule.runOnUiThread { showWelcomeBack = true }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("welcome_back_banner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bem-vindo de volta").assertIsDisplayed()
    }

    @Test
    fun `artist normalizer unifies same singer collaborations parses filenames and reorganizes repository`() = runBlocking {
        assertEquals("Ed Sheeran", ArtistNormalizer.normalizePrimaryArtist("Ed Sheeran feat. Khalid"))
        assertEquals("Ed Sheeran", ArtistNormalizer.normalizePrimaryArtist("ed  sheeran (Remix)"))
        assertEquals("Anselmo Ralph", ArtistNormalizer.normalizePrimaryArtist("Anselmo Ralph & Matias Damásio"))
        assertEquals("Céuzany", ArtistNormalizer.normalizePrimaryArtist("Céuzany ft. Loony Johnson"))

        assertEquals(
            ArtistNormalizer.artistGroupingKey("Ed Sheeran feat. Justin Bieber"),
            ArtistNormalizer.artistGroupingKey("ED SHEERAN")
        )
        assertEquals(
            ArtistNormalizer.artistGroupingKey("Matias Damásio"),
            ArtistNormalizer.artistGroupingKey("matias damasio")
        )

        val parsed = ArtistNormalizer.parseFilenameMetadata("03 - Calema - A Nossa Vez (2022).mp3")
        assertEquals(3, parsed.trackNumber)
        assertEquals("Calema", parsed.artist)
        assertEquals("A Nossa Vez", parsed.title)
        assertEquals(2022, parsed.releaseYear)

        val t1Id = db.mediaDao().insert(
            MediaItemEntity(
                uri = "content://local/audio/art1",
                mediaType = MediaType.AUDIO,
                title = "01 - Ed Sheeran - Bad Habits",
                artist = "Ed Sheeran feat. Lil Baby",
                album = "Equals",
                genre = "",
                durationMs = 230_000L,
                fileName = "01 - Ed Sheeran - Bad Habits.mp3",
                folderName = "Pop",
                folderPath = "/Music/Pop"
            )
        )
        val t2Id = db.mediaDao().insert(
            MediaItemEntity(
                uri = "content://local/audio/art2",
                mediaType = MediaType.AUDIO,
                title = "Shivers",
                artist = "ed sheeran",
                album = "Equals",
                genre = "",
                durationMs = 207_000L,
                fileName = "ed_sheeran_shivers.mp3",
                folderName = "Pop",
                folderPath = "/Music/Pop"
            )
        )
        val t3Id = db.mediaDao().insert(
            MediaItemEntity(
                uri = "content://local/audio/art3",
                mediaType = MediaType.AUDIO,
                title = "Unknown",
                artist = "<unknown>",
                album = "",
                genre = "",
                durationMs = 195_000L,
                fileName = "02 - Dua Lipa - Levitating (2020).mp3",
                folderName = "Hits",
                folderPath = "/Music/Hits"
            )
        )

        val updatedCount = repository.reorganizeLibraryWithSmartArtistScan()
        assertTrue(updatedCount >= 2)

        val artists = repository.artists.first()
        val sheeranSummary = artists.firstOrNull { it.name.equals("Ed Sheeran", ignoreCase = true) }
        assertTrue(sheeranSummary != null)
        assertEquals(2, sheeranSummary!!.trackCount)
        assertTrue(sheeranSummary.tracks.map { it.id }.containsAll(listOf(t1Id, t2Id)))

        val duaTrack = repository.getMediaById(t3Id)!!
        assertEquals("Dua Lipa", duaTrack.artist)
        assertEquals("Levitating", duaTrack.title)
        assertEquals(2020, duaTrack.releaseYear)
        assertEquals(2, duaTrack.trackNumber)
        assertTrue(duaTrack.genre.isNotBlank())
    }

    @Test
    fun `insights calculator smart playlists backup manager and bilingual lrc work accurately`() {
        val now = 1_750_000_000_000L
        val a1 = MediaItemEntity(
            id = 1L, uri = "content://a/1", mediaType = MediaType.AUDIO,
            title = "Midnight Glass", artist = "Aura", album = "Reflections",
            genre = "Pop", durationMs = 180_000L, fileName = "a1.mp3",
            folderName = "Music", folderPath = "/Music", playCount = 7, isFavorite = true, addedAt = now - 10_000L
        )
        val a2 = MediaItemEntity(
            id = 2L, uri = "content://a/2", mediaType = MediaType.AUDIO,
            title = "Velvet Rain", artist = "Aura feat. Sol", album = "Reflections",
            genre = "Jazz", durationMs = 260_000L, fileName = "a2.mp3",
            folderName = "Music", folderPath = "/Music", playCount = 1, isFavorite = false, addedAt = now - 1_000L
        )
        val history = listOf(
            HistoryWithMedia(PlaybackHistoryEntity(id = 1, mediaId = 1L, playedAt = now - 3_600_000L), a1),
            HistoryWithMedia(PlaybackHistoryEntity(id = 2, mediaId = 2L, playedAt = now - 2 * 86_400_000L), a2)
        )

        val report = InsightsCalculator.compute(
            allMedia = listOf(a1, a2),
            history = history,
            totalListeningTimeMs = 1_200_000L,
            nowMs = now
        )
        assertEquals(8, report.totalPlays)
        assertEquals(2, report.uniqueTracksPlayed)
        assertEquals("Aura", report.topArtists.first().label)
        assertEquals(7, report.weeklyStats.size)
        assertEquals(4, report.monthlyStats.size)
        assertTrue(report.wrappedHeadline.contains("Aura"))

        val gems = SmartPlaylistGenerator.generate(SmartPlaylistRule.HIDDEN_GEMS, listOf(a1, a2))
        assertEquals(2L, gems.first().id)
        val recs = SmartPlaylistGenerator.personalizedRecommendations(listOf(a1, a2))
        assertEquals(2, recs.size)

        // Backup serialize & parse round-trip
        val settings = AppSettings(themeMode = ThemeMode.DARK, themeStyle = AppThemeStyle.OBSIDIAN_AMOLED, customAccentHex = 0xFFFF4D9DL)
        val playlistWithTracks = PlaylistWithTracks(PlaylistEntity(id = 10L, name = "Favoritas"), listOf(a1, a2))
        val json = BackupManager.serializeBackup(settings, listOf(playlistWithTracks))
        val parsedBackup = BackupManager.parseBackup(json, listOf(a1, a2))
        assertEquals(ThemeMode.DARK, parsedBackup.settings?.themeMode)
        assertEquals(0xFFFF4D9DL, parsedBackup.settings?.customAccentHex)
        assertEquals("Favoritas", parsedBackup.playlists.single().name)
        assertEquals(listOf(1L, 2L), parsedBackup.playlists.single().trackIds)

        // LrcParser bilingual + offset + serialization
        val rawLrc = "[00:10.00]Hello darkness | Olá escuridão\n[00:15.50]My old friend // Meu velho amigo"
        val parsedLrc = LrcParser.parse(rawLrc)
        assertTrue(parsedLrc.isTimed)
        assertTrue(parsedLrc.hasTranslations)
        assertEquals("Olá escuridão", parsedLrc.lines[0].translatedText)
        assertEquals("Meu velho amigo", parsedLrc.lines[1].translatedText)
        assertEquals(1, LrcParser.findActiveLineIndex(parsedLrc.lines, positionMs = 14_000L, offsetMs = 2_000L))

        val serialized = LrcParser.serializeLrc(listOf(LyricLine(10_000L, "Hello", "Olá")))
        assertEquals("[00:10.00]Hello | Olá", serialized)
    }

    @Test
    fun `stereo balance and spectrum audio processors process 16 bit pcm deterministically`() {
        AudioPipelineBus.stereoBalance = 1.0f // Full Right -> Left silenced
        val balanceProc = StereoBalanceAudioProcessor()
        val format = AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        balanceProc.configure(format)
        balanceProc.flush()

        val inBuf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        inBuf.putShort(10000).putShort(10000)
        inBuf.putShort(-8000).putShort(6000)
        inBuf.flip()
        balanceProc.queueInput(inBuf)
        val outBuf = balanceProc.output.order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(0.toShort(), outBuf.short)
        assertEquals(10000.toShort(), outBuf.short)
        assertEquals(0.toShort(), outBuf.short)
        assertEquals(6000.toShort(), outBuf.short)
        AudioPipelineBus.stereoBalance = 0f

        AudioPipelineBus.resetSpectrum()
        AudioPipelineBus.spectrumEnabled = true
        val specProc = SpectrumAudioProcessor()
        specProc.configure(format)
        specProc.flush()

        val frames = 512
        val specBuf = ByteBuffer.allocate(frames * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frames) {
            val s = (16000.0 * kotlin.math.sin(2.0 * Math.PI * 1000.0 * i / 44100.0)).toInt().toShort()
            specBuf.putShort(s).putShort(s)
        }
        specBuf.flip()
        // Frame counter publishes every 2nd queueInput call
        specProc.queueInput(specBuf.duplicate())
        specProc.queueInput(specBuf.duplicate())
        val snap = AudioPipelineBus.spectrumSnapshot.value
        assertEquals(16, snap.bands.size)
        assertEquals(32, snap.waveform.size)
        assertTrue(snap.peakEnergy > 0.1f)
    }

    @Test
    fun `insights screen spectrum visualizer and expanded metadata dialog render and interact cleanly`() {
        val sampleTrack = MediaItemEntity(
            id = 99L,
            uri = "content://local/audio/99",
            mediaType = MediaType.AUDIO,
            title = "04 - Coldplay - Yellow",
            artist = "<unknown>",
            album = "Parachutes",
            genre = "",
            durationMs = 266_000L,
            fileName = "04 - Coldplay - Yellow (2000).mp3",
            folderName = "Rock",
            folderPath = "/Music/Rock",
            playCount = 5
        )
        val report = InsightsCalculator.compute(listOf(sampleTrack), emptyList(), 900_000L)
        var showMetadataDialog by mutableStateOf(false)
        var pickArtworkCalled = false
        var savedYear = 0
        var savedArtist = ""

        composeTestRule.setContent {
            LumaTheme(settings = AppSettings(themeMode = ThemeMode.DARK, reduceMotion = true)) {
                Column {
                    SpectrumVisualizerView(
                        snapshot = AudioSpectrumSnapshot(bands = List(16) { 0.6f }, peakEnergy = 0.7f),
                        isPlaying = true,
                        style = VisualizerStyle.CIRCULAR_SPECTRUM,
                        preset = VisualizerPreset.NEON_PULSE,
                        reactiveLighting = true,
                        reduceMotion = true,
                        accentPrimary = SoftPinkPrimary,
                        accentSecondary = EmeraldMint,
                        modifier = Modifier.height(72.dp)
                    )
                    InsightsScreen(
                        report = report,
                        bottomInsetPadding = 64.dp,
                        onBack = {},
                        onPlayTrack = { _, _ -> }
                    )
                    if (showMetadataDialog) {
                        MediaMetadataDialog(
                            item = sampleTrack,
                            onSaveExtended = { _, artist, _, _, year, _ ->
                                savedArtist = artist
                                savedYear = year
                            },
                            onPickArtwork = { pickArtworkCalled = true },
                            onDismiss = { showMetadataDialog = false }
                        )
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("spectrum_visualizer_circular_spectrum").assertIsDisplayed()
        composeTestRule.onNodeWithTag("insights_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Luma Insights 4.0").assertIsDisplayed()

        composeTestRule.runOnUiThread { showMetadataDialog = true }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("metadata_pick_artwork_button").performClick()
        assertTrue(pickArtworkCalled)

        composeTestRule.onNodeWithTag("metadata_auto_detect_button").performClick()
        composeTestRule.onNodeWithText("Guardar").performClick()
        composeTestRule.waitForIdle()
        assertEquals("Coldplay", savedArtist)
        assertEquals(2000, savedYear)
    }

    @Test
    fun `online lyrics client parses lrclib and lyrics ovh responses and cleans tokens`() {
        assertEquals("Golden Hour", OnlineLyricsClient.cleanQueryToken("Golden Hour (Official Video).mp3"))
        assertEquals("Blinding Lights", OnlineLyricsClient.cleanQueryToken("Blinding Lights [Remastered]"))

        val lrcSingleJson = """
            {
              "id": 42,
              "trackName": "Golden Hour",
              "artistName": "JVKE",
              "albumName": "This Is What Feels Like",
              "syncedLyrics": "[00:05.00]It was just two lovers",
              "plainLyrics": "It was just two lovers"
            }
        """.trimIndent()
        val single = OnlineLyricsClient.parseLrcLibSingleJson(lrcSingleJson)!!
        assertEquals("Golden Hour", single.trackName)
        assertEquals("JVKE", single.artistName)
        assertTrue(single.isSynced)
        assertEquals("LRCLIB", single.source)

        val lrcSearchJson = """
            [
              {
                "id": 100,
                "trackName": "Nocturne",
                "artistName": "Luma",
                "albumName": "Glass",
                "syncedLyrics": null,
                "plainLyrics": "Soft moonlight over glass"
              },
              {
                "id": 101,
                "trackName": "Nocturne",
                "artistName": "Luma",
                "albumName": "Glass",
                "syncedLyrics": "[00:10.00]Soft moonlight over glass",
                "plainLyrics": "Soft moonlight over glass"
              }
            ]
        """.trimIndent()
        val searchResults = OnlineLyricsClient.parseLrcLibSearchJson(lrcSearchJson)
        assertEquals(2, searchResults.size)
        assertFalse(searchResults[0].isSynced)
        assertTrue(searchResults[1].isSynced)

        val ovhJson = """{"lyrics":"Line one\nLine two"}"""
        val ovh = OnlineLyricsClient.parseLyricsOvhJson(ovhJson, trackName = "Yellow", artistName = "Coldplay")!!
        assertEquals("Lyrics.ovh", ovh.source)
        assertFalse(ovh.isSynced)
        assertTrue(ovh.bestLyricsText.contains("Line one"))
        assertNull(OnlineLyricsClient.parseLyricsOvhJson("{}", "A", "B"))
    }

    @Test
    fun `custom luma identity pill hides without track displays app icon and title and handles clicks`() {
        val track = MediaItemEntity(
            id = 501L,
            uri = "content://local/audio/501",
            mediaType = MediaType.AUDIO,
            title = "Midnight Serenade on Liquid Glass",
            artist = "Aura",
            album = "Reflections",
            durationMs = 210_000L,
            fileName = "serenade.mp3",
            folderName = "Music",
            folderPath = "/Music"
        )
        var currentTrack by mutableStateOf<MediaItemEntity?>(null)
        var themeMode by mutableStateOf(ThemeMode.LIGHT)
        var clickCount = 0

        composeTestRule.setContent {
            LumaTheme(settings = AppSettings(themeMode = themeMode, reduceMotion = true)) {
                LumaIdentityPill(
                    currentTrack = currentTrack,
                    isPlaying = true,
                    onClick = { clickCount++ }
                )
            }
        }

        // Hidden when no active track
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("luma_identity_pill").assertDoesNotExist()

        // Appears with stationary Luma icon + track title when track becomes active
        composeTestRule.runOnUiThread { currentTrack = track }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("luma_identity_pill").assertIsDisplayed()
        composeTestRule.onNodeWithTag("luma_identity_pill_icon", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("luma_identity_pill_title", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Midnight Serenade on Liquid Glass").assertIsDisplayed()

        // Responds to Dark Mode & Clicks cleanly
        composeTestRule.runOnUiThread { themeMode = ThemeMode.DARK }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("luma_identity_pill").performClick()
        assertEquals(1, clickCount)
    }

    @Test
    fun `multi language localization persists switches live and includes dev credits footer`(): Unit = runBlocking {
        val settingsRepo = SettingsRepository(context)
        settingsRepo.setAppLanguage(AppLanguage.EN)
        assertEquals(AppLanguage.EN, settingsRepo.settingsFlow.first().appLanguage)

        val backupJson = BackupManager.serializeBackup(
            settings = AppSettings(appLanguage = AppLanguage.FR),
            playlists = emptyList()
        )
        val parsedBackup = BackupManager.parseBackup(backupJson, emptyList())
        assertEquals(AppLanguage.FR, parsedBackup.settings?.appLanguage)

        assertEquals("Home", resolveLumaStrings(AppLanguage.EN).navHome)
        assertEquals("Inicio", resolveLumaStrings(AppLanguage.ES).navHome)
        assertEquals("Accueil", resolveLumaStrings(AppLanguage.FR).navHome)
        assertEquals("Home", resolveLumaStrings(AppLanguage.SYSTEM, Locale.ENGLISH).navHome)
        assertEquals("Início", resolveLumaStrings(AppLanguage.PT).navHome)

        var activeLang by mutableStateOf(AppLanguage.PT)
        composeTestRule.setContent {
            val activeSettings = AppSettings(appLanguage = activeLang, reduceMotion = true)
            ProvideLumaStrings(language = activeLang) {
                LumaTheme(settings = activeSettings) {
                    SettingsScreen(
                        settings = activeSettings,
                        mostPlayed = emptyList(),
                        totalIndexedCount = 0,
                        bottomInsetPadding = 64.dp,
                        onSetThemeMode = {},
                        onSetGlassQuality = {},
                        onSetReduceMotion = {},
                        onSetHaptics = {},
                        onSetCrossfade = {},
                        onSetDynamicCover = {},
                        onSetVisualizer = {},
                        onSetCoverGestures = {},
                        onSetMinimalistMode = {},
                        onSetAlbumArtAnimation = {},
                        onImportFiles = {},
                        onImportFolder = {},
                        onRefreshLibrary = {},
                        onOpenSupportedFormats = {},
                        onOpenAbout = {},
                        onClearHistory = {},
                        onResetStats = {},
                        onSetAppLanguage = { activeLang = it }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Ajustes").assertIsDisplayed()

        // Switch to English in Settings
        composeTestRule.onNodeWithTag("language_en").performClick()
        composeTestRule.waitForIdle()
        assertEquals(AppLanguage.EN, activeLang)
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()

        // Verify bottom footer shows app version and requested developer credit
        composeTestRule.onNodeWithTag("settings_screen")
            .performScrollToNode(hasTestTag("settings_version_footer"))
        composeTestRule.onNodeWithText("Luma Player • Version ${AppConfig.APP_VERSION}").assertIsDisplayed()
        composeTestRule.onNodeWithText("made by David tonecas helped by google AI studio").assertIsDisplayed()
    }

    @Test
    fun `edge swipe math and artwork harmonized npd tokens compute safely`() {
        // Left edge swipe up increases volume, clamped to [0, maxVol]
        assertEquals(11, computeEdgeSwipeTargetVolume(currentVolume = 5, maxVolume = 15, accumulatedDeltaY = -200f, gestureRangePx = 500f))
        assertEquals(15, computeEdgeSwipeTargetVolume(currentVolume = 12, maxVolume = 15, accumulatedDeltaY = -500f, gestureRangePx = 400f))
        assertEquals(0, computeEdgeSwipeTargetVolume(currentVolume = 3, maxVolume = 15, accumulatedDeltaY = 400f, gestureRangePx = 400f))

        // Right edge swipe up seeks forward, swipe down rewinds, clamped to [0, durationMs]
        val seekAhead = computeEdgeSwipeSeekTargetMs(startPositionMs = 30_000L, durationMs = 200_000L, accumulatedDeltaY = -250f, gestureRangePx = 500f)
        assertEquals(75_000L, seekAhead)
        val seekBack = computeEdgeSwipeSeekTargetMs(startPositionMs = 10_000L, durationMs = 200_000L, accumulatedDeltaY = 400f, gestureRangePx = 400f)
        assertEquals(0L, seekBack)

        // Cover-harmonized NPD tokens tint dark & light surfaces while preserving readable contrast
        var darkTokens: LumaGlassTokens? = null
        var lightTokens: LumaGlassTokens? = null
        composeTestRule.setContent {
            LumaTheme(settings = AppSettings(themeMode = ThemeMode.DARK, themeStyle = AppThemeStyle.LUMA_GLASS, reduceMotion = true)) {
                darkTokens = LocalLumaGlass.current
            }
            LumaTheme(settings = AppSettings(themeMode = ThemeMode.LIGHT, themeStyle = AppThemeStyle.LUMA_GLASS, reduceMotion = true)) {
                lightTokens = LocalLumaGlass.current
            }
        }
        composeTestRule.waitForIdle()

        val darkCoverColors = ExtractedArtworkColors(primary = Color(0xFF180A28), secondary = Color(0xFF2B1055))
        val harmonizedDark = buildHarmonizedNpdTokens(darkTokens!!, darkCoverColors)
        assertNotEquals(darkTokens.background, harmonizedDark.background)
        assertTrue(harmonizedDark.accentPrimary.luminance() >= 0.14f)

        val brightCoverColors = ExtractedArtworkColors(primary = Color(0xFFFFF0A8), secondary = Color(0xFFD8FFB8))
        val harmonizedLight = buildHarmonizedNpdTokens(lightTokens!!, brightCoverColors)
        assertNotEquals(lightTokens.background, harmonizedLight.background)
        assertTrue(harmonizedLight.accentPrimary.luminance() < 0.72f)
    }

    @Test
    fun `lyrics studio sheet renders online search local import manual edit and applies selected online lyrics`() {
        val track = MediaItemEntity(
            id = 777L,
            uri = "content://local/audio/777",
            mediaType = MediaType.AUDIO,
            title = "Midnight Glass",
            artist = "Aura",
            album = "Reflections",
            durationMs = 190_000L,
            fileName = "midnight.mp3",
            folderName = "Music",
            folderPath = "/Music"
        )
        val candidate = OnlineLyricsCandidate(
            id = "lrclib_777",
            trackName = "Midnight Glass",
            artistName = "Aura",
            albumName = "Reflections",
            syncedLyrics = "[00:08.00]Under the neon rain",
            plainLyrics = "Under the neon rain",
            source = "LRCLIB"
        )
        var searchState by mutableStateOf(LyricsSearchUiState())
        var appliedCandidate: OnlineLyricsCandidate? = null

        composeTestRule.setContent {
            LumaTheme(settings = AppSettings(reduceMotion = true)) {
                LyricsStudioSheetContent(
                    track = track,
                    positionMs = 0L,
                    settings = AppSettings(reduceMotion = true),
                    onSeekTo = {},
                    onImportLyrics = {},
                    onClearLyrics = {},
                    onSaveLyricsAndOffset = { _, _, _ -> },
                    onUpdateFontScale = {},
                    onToggleCentered = {},
                    onOpenFullScreen = {},
                    lyricsSearchState = searchState,
                    onSearchOnlineLyrics = { _, _ ->
                        searchState = LyricsSearchUiState(
                            trackId = track.id,
                            results = listOf(candidate)
                        )
                    },
                    onApplyOnlineLyrics = { appliedCandidate = it }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("lyrics_search_online_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("lyrics_import_local_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("lyrics_manual_edit_button").assertIsDisplayed()

        // Trigger online search -> candidate row renders -> select candidate
        composeTestRule.onNodeWithTag("lyrics_search_online_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Aura • LRCLIB").performClick()
        assertEquals("lrclib_777", appliedCandidate?.id)
    }

    @Test
    fun `new 7 features domain logic persists pitch bookmarks shake radio ambient pcm and story card`(): Unit = runBlocking {
        // 1. A-B Loop math & DJ Pitch semitones math
        assertTrue(PlaybackController.shouldAbLoopJump(positionMs = 30_000L, loopStartMs = 10_000L, loopEndMs = 29_950L, isActive = true))
        assertFalse(PlaybackController.shouldAbLoopJump(positionMs = 20_000L, loopStartMs = 10_000L, loopEndMs = 29_950L, isActive = true))
        assertFalse(PlaybackController.shouldAbLoopJump(positionMs = 30_000L, loopStartMs = 10_000L, loopEndMs = 29_950L, isActive = false))
        assertEquals(1.0f, PlaybackController.semitonesToPitchFactor(0f), 0.001f)
        assertTrue(PlaybackController.semitonesToPitchFactor(6f) > 1.35f)
        assertTrue(PlaybackController.semitonesToPitchFactor(-6f) < 0.75f)

        // 2. Ambient Sound Engine procedural PCM synthesis
        val offBuf = ShortArray(256) { 99 }
        AmbientSoundEngine.generateAmbientBuffer(AmbientSoundType.OFF, 0.5f, offBuf)
        assertTrue(offBuf.all { it == 0.toShort() })

        listOf(
            AmbientSoundType.WARM_RAIN,
            AmbientSoundType.OCEAN_WAVES,
            AmbientSoundType.CRACKLING_HEARTH,
            AmbientSoundType.FOCUS_PINK
        ).forEach { sound ->
            val buf = ShortArray(512)
            AmbientSoundEngine.generateAmbientBuffer(sound, 0.7f, buf)
            assertTrue("Expected non-silent PCM for $sound", buf.any { it != 0.toShort() })
        }

        // 3. Shake-to-Skip detector threshold & cooldown
        assertFalse(ShakeSkipDetector.evaluateShakeTrigger(0f, 9.8f, 0f, nowMs = 5_000L, lastTriggerMs = 0L))
        assertTrue(ShakeSkipDetector.evaluateShakeTrigger(25f, 12f, 5f, nowMs = 5_000L, lastTriggerMs = 0L))
        assertFalse(ShakeSkipDetector.evaluateShakeTrigger(25f, 12f, 5f, nowMs = 5_800L, lastTriggerMs = 5_000L))

        // 4. Smart Queue Radio continuation avoids tracks already in queue
        val seed = MediaItemEntity(
            id = 1L, uri = "content://1", mediaType = MediaType.AUDIO,
            title = "Neon Horizon", artist = "Aura", album = "Glass", genre = "Chillwave",
            durationMs = 200_000L, fileName = "1.mp3", folderName = "Music", folderPath = "/Music"
        )
        val sameArtist = MediaItemEntity(
            id = 2L, uri = "content://2", mediaType = MediaType.AUDIO,
            title = "Sunset Echo", artist = "Aura", album = "Glass", genre = "Chillwave",
            durationMs = 210_000L, fileName = "2.mp3", folderName = "Music", folderPath = "/Music",
            isFavorite = true
        )
        val otherTrack = MediaItemEntity(
            id = 3L, uri = "content://3", mediaType = MediaType.AUDIO,
            title = "Bassline", artist = "Pulse", album = "Club", genre = "House",
            durationMs = 190_000L, fileName = "3.mp3", folderName = "Music", folderPath = "/Music"
        )
        val radioNext = SmartPlaylistGenerator.pickRadioContinuation(
            seed = seed,
            currentQueue = listOf(seed),
            library = listOf(seed, sameArtist, otherTrack),
            limit = 4
        )
        assertEquals(listOf(2L, 3L), radioNext.map { it.id })

        // 5. SettingsRepository DataStore persistence & BackupManager serialization
        val settingsRepo = SettingsRepository(context)
        settingsRepo.setPitchSemitones(-2.5f)
        settingsRepo.setShakeToSkipEnabled(true)
        settingsRepo.setQueueRadioEnabled(true)
        settingsRepo.addTrackBookmark(mediaId = 42L, positionMs = 74_000L, label = "Guitar Solo")
        val stored = settingsRepo.settingsFlow.first()
        assertEquals(-2.5f, stored.pitchSemitones, 0.01f)
        assertTrue(stored.shakeToSkipEnabled)
        assertTrue(stored.queueRadioEnabled)
        val decodedBookmarks = SettingsRepository.decodeTrackBookmarks(stored.trackBookmarksJson)
        assertEquals(1, decodedBookmarks.size)
        assertEquals("Guitar Solo", decodedBookmarks.first().label)
        assertEquals(74_000L, decodedBookmarks.first().positionMs)

        settingsRepo.deleteTrackBookmark(decodedBookmarks.first().id)
        assertTrue(SettingsRepository.decodeTrackBookmarks(settingsRepo.settingsFlow.first().trackBookmarksJson).isEmpty())

        val backupJson = BackupManager.serializeBackup(settings = stored, playlists = emptyList())
        val parsedBackup = BackupManager.parseBackup(backupJson, emptyList())
        assertEquals(-2.5f, parsedBackup.settings?.pitchSemitones ?: 0f, 0.01f)
        assertTrue(parsedBackup.settings?.shakeToSkipEnabled == true)
        assertTrue(parsedBackup.settings?.queueRadioEnabled == true)

        // 6. Story Card 1080x1350 PNG Bitmap rendering
        val cardBitmap = StoryCardExporter.renderStoryCardBitmap(
            track = seed,
            artworkBitmap = null,
            quoteLine = "Shimmering reflections in liquid glass",
            accentPrimary = EmeraldMint,
            accentSecondary = EmeraldLight,
            isDark = true
        )
        assertEquals(1080, cardBitmap.width)
        assertEquals(1350, cardBitmap.height)
    }

    @Test
    fun `studio extra sheets and ambient home trigger render interactively`() {
        val sampleTrack = MediaItemEntity(
            id = 901L, uri = "content://local/audio/901", mediaType = MediaType.AUDIO,
            title = "Cristal Líquido", artist = "Solista", album = "Aurora",
            durationMs = 240_000L, fileName = "cristal.mp3", folderName = "Music", folderPath = "/Music"
        )
        var pointASet = false
        var speedSelected = 1.0f
        var pitchSelected = 0f
        var addedBookmarkLabel = ""
        var ambientSelected = AmbientSoundType.OFF
        var ambientBtnClicked = false

        var showHome by mutableStateOf(false)

        composeTestRule.setContent {
            LumaTheme(settings = AppSettings(reduceMotion = true)) {
                if (!showHome) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        AbLoopSheetContent(
                            playbackState = PlaybackUiState(currentTrack = sampleTrack, durationMs = 240_000L),
                            onSetPointA = { pointASet = true },
                            onSetPointB = {},
                            onNudgePoint = { _, _ -> },
                            onToggleActive = {},
                            onSeekToA = {},
                            onClear = {}
                        )
                        SpeedAndPitchSheetContent(
                            currentSpeed = speedSelected,
                            currentPitchSemitones = pitchSelected,
                            onSelectSpeed = { speedSelected = it },
                            onSelectPitchSemitones = { pitchSelected = it }
                        )
                        BookmarksSheetContent(
                            track = sampleTrack,
                            positionMs = 65_000L,
                            bookmarks = listOf(TrackBookmark(1L, sampleTrack.id, 30_000L, "Intro")),
                            onAddBookmark = { addedBookmarkLabel = it },
                            onJumpToBookmark = {},
                            onDeleteBookmark = {}
                        )
                        AmbientMixerSheetContent(
                            ambientState = AmbientMixerState(soundType = ambientSelected, volume = 0.4f, isPlaying = false),
                            onSelectSound = { ambientSelected = it },
                            onVolumeChange = {}
                        )
                    }
                } else {
                    HomeScreen(
                        allMedia = emptyList(),
                        audioTracks = emptyList(),
                        continueListening = emptyList(),
                        favorites = emptyList(),
                        playlists = emptyList(),
                        history = emptyList(),
                        playbackState = PlaybackUiState(),
                        bottomInsetPadding = 64.dp,
                        onImportFiles = {},
                        onImportFolder = {},
                        onOpenHistory = {},
                        onOpenCategory = {},
                        onOpenMood = {},
                        onOpenPlaylist = {},
                        onPlayTrack = { _, _ -> },
                        onToggleFavorite = {},
                        onExpandPlayer = {},
                        onTogglePlayPause = {},
                        onNext = {},
                        onPrevious = {},
                        onPlayNext = {},
                        onAddToQueue = {},
                        onSmartMix = {},
                        onAddToPlaylist = {},
                        onRemoveFromLibrary = {},
                        isAmbientPlaying = false,
                        onOpenAmbientMixer = { ambientBtnClicked = true }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ab_set_a_button").performScrollTo().performClick()
        assertTrue(pointASet)

        composeTestRule.onNodeWithTag("preset_warm_lofi").performScrollTo().performClick()
        assertEquals(0.85f, speedSelected, 0.01f)
        assertEquals(-2.0f, pitchSelected, 0.01f)

        composeTestRule.onNodeWithTag("bookmark_label_input").performScrollTo().performTextInput("Refrão")
        composeTestRule.onNodeWithTag("bookmark_add_button").performScrollTo().performClick()
        assertEquals("Refrão", addedBookmarkLabel)

        composeTestRule.onNodeWithTag("ambient_option_warm_rain").performScrollTo().performClick()
        assertEquals(AmbientSoundType.WARM_RAIN, ambientSelected)

        composeTestRule.runOnUiThread { showHome = true }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_ambient_button").performClick()
        assertTrue(ambientBtnClicked)
    }

    @Test
    fun `npd renders cover atmosphere header pill and left right edge swipe feedback rails`() {
        val settingsRepo = SettingsRepository(context)
        val playbackController = PlaybackController(context, repository, settingsRepo)

        val track = MediaItemEntity(
            id = 808L,
            uri = "content://local/audio/808",
            mediaType = MediaType.AUDIO,
            title = "Velvet Eclipse",
            artist = "Nova",
            album = "Solaris",
            durationMs = 200_000L,
            fileName = "velvet_eclipse.flac",
            mimeType = "audio/flac",
            folderName = "Music",
            folderPath = "/Music"
        )

        var harmonizedM3Primary: Color? = null
        var harmonizedM3Bg: Color? = null

        composeTestRule.setContent {
            val baseSettings = AppSettings(themeMode = ThemeMode.DARK, dynamicCoverTheme = true, reduceMotion = true)
            LumaTheme(settings = baseSettings) {
                val baseTokens = LocalLumaGlass.current
                val harmonized = buildHarmonizedNpdTokens(
                    baseTokens,
                    ExtractedArtworkColors(primary = Color(0xFFE05288), secondary = Color(0xFF42A5F5), tertiary = Color(0xFFFFB74D))
                )
                val scheme = buildColorSchemeFromGlass(harmonized, MaterialTheme.colorScheme)
                harmonizedM3Primary = scheme.primary
                harmonizedM3Bg = scheme.background

                MusicPlayerScreen(
                    playbackState = PlaybackUiState(
                        currentTrack = track,
                        queue = listOf(track),
                        currentIndex = 0,
                        isPlaying = false,
                        positionMs = 45_000L,
                        durationMs = 200_000L
                    ),
                    settings = baseSettings,
                    playbackController = playbackController,
                    onClose = {},
                    onToggleFavorite = {},
                    onSmartMix = {},
                    onAddToPlaylist = {},
                    onSaveQueueAsPlaylist = {},
                    onToggleMinimalistMode = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        assertNotEquals(EmeraldLight, harmonizedM3Primary)
        assertNotEquals(DeepNavyBg, harmonizedM3Bg)

        composeTestRule.onNodeWithTag("npd_cover_atmosphere").assertIsDisplayed()
        composeTestRule.onNodeWithTag("npd_header_pill").assertIsDisplayed()
        composeTestRule.onNodeWithTag("left_edge_volume_zone").assertIsDisplayed()
        composeTestRule.onNodeWithTag("right_edge_seek_zone").assertIsDisplayed()

        // Drag vertically on left edge -> shows edge_volume_rail + edge_gesture_hud
        composeTestRule.onNodeWithTag("left_edge_volume_zone").performTouchInput {
            down(center)
            moveBy(Offset(0f, -120f))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("edge_volume_rail").assertIsDisplayed()
        composeTestRule.onNodeWithTag("edge_gesture_hud").assertIsDisplayed()
        composeTestRule.onNodeWithTag("left_edge_volume_zone").performTouchInput { up() }
        composeTestRule.waitForIdle()

        // Drag vertically on right edge -> shows edge_seek_rail + edge_gesture_hud
        composeTestRule.onNodeWithTag("right_edge_seek_zone").performTouchInput {
            down(center)
            moveBy(Offset(0f, -140f))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("edge_seek_rail").assertIsDisplayed()
        composeTestRule.onNodeWithTag("edge_gesture_hud").assertIsDisplayed()
        composeTestRule.onNodeWithTag("right_edge_seek_zone").performTouchInput { up() }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `crash guard loop prevention zero-size shaders null track and NaN spectrum remain stable`() {
        // 1. Loop-protection policy: first crash or spaced crash restarts; >= 2 crashes within 10s aborts restart loop
        assertTrue(LumaCrashGuard.shouldAttemptAutoRestart(nowMs = 50_000L, lastCrashMs = 0L, recentCount = 0))
        assertTrue(LumaCrashGuard.shouldAttemptAutoRestart(nowMs = 50_000L, lastCrashMs = 45_000L, recentCount = 1))
        assertFalse(LumaCrashGuard.shouldAttemptAutoRestart(nowMs = 50_000L, lastCrashMs = 45_000L, recentCount = 2))
        assertTrue(LumaCrashGuard.shouldAttemptAutoRestart(nowMs = 70_000L, lastCrashMs = 45_000L, recentCount = 3))

        // Recovery flag round-trip
        val prefs = context.getSharedPreferences("luma_crash_guard", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("recovered_from_crash", true).commit()
        assertTrue(LumaCrashGuard.consumeRecoveryNotice(context))
        assertFalse(LumaCrashGuard.consumeRecoveryNotice(context))

        // Safe bitmap decoder handles invalid paths without throwing
        assertNull(LumaCrashGuard.decodeSampledBitmap(context, "/nonexistent/path/cover.jpg", 256))
        assertNull(LumaCrashGuard.decodeSampledBitmap(context, null, 256))

        // 2. AudioPipelineBus sanitizes NaN / Infinity inputs
        AudioPipelineBus.publishSpectrum(
            bands = listOf(Float.NaN, Float.POSITIVE_INFINITY, -5f, 0.7f),
            waveform = listOf(Float.NEGATIVE_INFINITY, Float.NaN, 2.0f),
            peak = Float.NaN
        )
        val sanitized = AudioPipelineBus.spectrumSnapshot.value
        assertTrue(sanitized.bands.all { band -> band.isFinite() && band in 0f..1f })
        assertTrue(sanitized.waveform.all { sample -> sample.isFinite() && sample in -1f..1f })
        assertTrue(sanitized.peakEnergy.isFinite() && sanitized.peakEnergy in 0f..1f)

        // 3. Zero-sized Compose radial/linear gradient surfaces & MusicPlayerScreen(null) never crash
        val settingsRepo = SettingsRepository(context)
        val playbackController = PlaybackController(context, repository, settingsRepo)
        var nullClosed = false

        composeTestRule.setContent {
            LumaTheme(settings = AppSettings(themeStyle = AppThemeStyle.ALABASTER_BW, reduceMotion = true)) {
                Column {
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(0.dp)
                    ) {
                        Text("Zero")
                    }
                    LiquidGlassCapsule(modifier = Modifier.size(0.dp)) {
                        Text("Capsule")
                    }
                    SpectrumVisualizerView(
                        snapshot = sanitized,
                        isPlaying = true,
                        style = VisualizerStyle.CIRCULAR_SPECTRUM,
                        preset = VisualizerPreset.AURORA_SILK,
                        reactiveLighting = true,
                        reduceMotion = true,
                        accentPrimary = EmeraldMint,
                        accentSecondary = EmeraldLight,
                        modifier = Modifier.size(0.dp)
                    )
                    MusicPlayerScreen(
                        playbackState = PlaybackUiState(currentTrack = null),
                        settings = AppSettings(reduceMotion = true),
                        playbackController = playbackController,
                        onClose = { nullClosed = true },
                        onToggleFavorite = {},
                        onSmartMix = {},
                        onAddToPlaylist = {},
                        onSaveQueueAsPlaylist = {},
                        onToggleMinimalistMode = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        assertTrue(nullClosed)
    }

    @Test
    fun `400ms playbackProgress ticks do not recompose MiniPlayerBar HomeScreen or interrupt edge seek`() {
        val settingsRepo = SettingsRepository(context)
        val playbackController = PlaybackController(context, repository, settingsRepo)
        val track = MediaItemEntity(
            id = 990L,
            uri = "content://local/audio/990",
            mediaType = MediaType.AUDIO,
            title = "Optimization Groove",
            artist = "Luma Studio",
            album = "60FPS",
            durationMs = 240_000L,
            fileName = "groove.mp3",
            folderName = "Music",
            folderPath = "/Music"
        )
        val progressFlow = MutableStateFlow(PlaybackProgressUiState(positionMs = 4_000L, durationMs = 240_000L))
        var miniParentCompositions = 0
        var homeParentCompositions = 0
        var showPlayer by mutableStateOf(false)

        composeTestRule.setContent {
            LumaTheme(settings = AppSettings(reduceMotion = false, themeMode = ThemeMode.DARK)) {
                if (!showPlayer) {
                    Column {
                        SideEffect { homeParentCompositions++ }
                        HomeScreen(
                            allMedia = listOf(track),
                            audioTracks = listOf(track),
                            continueListening = emptyList(),
                            favorites = emptyList(),
                            playlists = emptyList(),
                            history = emptyList(),
                            playbackState = PlaybackUiState(
                                currentTrack = track,
                                queue = listOf(track),
                                currentIndex = 0,
                                isPlaying = true,
                                durationMs = 240_000L
                            ),
                            progressFlow = progressFlow,
                            bottomInsetPadding = 64.dp,
                            onImportFiles = {},
                            onImportFolder = {},
                            onOpenHistory = {},
                            onOpenCategory = {},
                            onOpenMood = {},
                            onOpenPlaylist = {},
                            onPlayTrack = { _, _ -> },
                            onToggleFavorite = {},
                            onExpandPlayer = {},
                            onTogglePlayPause = {},
                            onNext = {},
                            onPrevious = {},
                            onPlayNext = {},
                            onAddToQueue = {},
                            onSmartMix = {},
                            onAddToPlaylist = {},
                            onRemoveFromLibrary = {}
                        )
                        SideEffect { miniParentCompositions++ }
                        MiniPlayerBar(
                            playbackState = PlaybackUiState(
                                currentTrack = track,
                                queue = listOf(track),
                                currentIndex = 0,
                                isPlaying = true,
                                durationMs = 240_000L
                            ),
                            progressFlow = progressFlow,
                            onExpand = {},
                            onPrevious = {},
                            onTogglePlayPause = {},
                            onNext = {},
                            onDismiss = {}
                        )
                    }
                } else {
                    MusicPlayerScreen(
                        playbackState = PlaybackUiState(
                            currentTrack = track,
                            queue = listOf(track),
                            currentIndex = 0,
                            isPlaying = true,
                            durationMs = 240_000L
                        ),
                        settings = AppSettings(reduceMotion = true, themeMode = ThemeMode.DARK),
                        playbackController = playbackController,
                        progressFlow = progressFlow,
                        onClose = {},
                        onToggleFavorite = {},
                        onSmartMix = {},
                        onAddToPlaylist = {},
                        onSaveQueueAsPlaylist = {},
                        onToggleMinimalistMode = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        val initialHomeCount = homeParentCompositions
        val initialMiniCount = miniParentCompositions

        // Simulate 10 consecutive 400ms ticker updates
        for (i in 1..10) {
            composeTestRule.runOnUiThread {
                progressFlow.value = PlaybackProgressUiState(positionMs = i * 400L + 4_000L, durationMs = 240_000L)
            }
            composeTestRule.waitForIdle()
        }

        assertEquals("HomeScreen parent should not recompose on 400ms ticks", initialHomeCount, homeParentCompositions)
        assertEquals("MiniPlayerBar parent should not recompose on 400ms ticks", initialMiniCount, miniParentCompositions)

        // Verify active right-edge seek survives 400ms progress ticks without cancelling drag
        composeTestRule.runOnUiThread { showPlayer = true }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("right_edge_seek_zone").performTouchInput {
            down(center)
            moveBy(Offset(0f, -120f))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("edge_seek_rail").assertIsDisplayed()

        // Tick progress while finger stays held down on right edge
        composeTestRule.runOnUiThread {
            progressFlow.value = PlaybackProgressUiState(positionMs = 12_000L, durationMs = 240_000L)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("edge_seek_rail").assertIsDisplayed()
        composeTestRule.onNodeWithTag("right_edge_seek_zone").performTouchInput { up() }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `repeated tab switching preserves saveable state and skips entrance delay on revisit`() {
        var selectedTab by mutableStateOf(BottomNavTab.HOME)
        var homeEntranceSkippedOnRevisit = false
        val entered = mutableMapOf<BottomNavTab, Boolean>()

        composeTestRule.setContent {
            val stateHolder = rememberSaveableStateHolder()
            LumaTheme(settings = AppSettings(reduceMotion = false)) {
                Column {
                    stateHolder.SaveableStateProvider(key = "tab_${selectedTab.name}") {
                        val skip = entered[selectedTab] == true
                        if (selectedTab == BottomNavTab.HOME && skip) {
                            homeEntranceSkippedOnRevisit = true
                        }
                        entered[selectedTab] = true
                        CompositionLocalProvider(LocalSkipSectionEntrance provides skip) {
                            LumaSectionEntrance(index = 0) {
                                var counter by rememberSaveable { mutableStateOf(selectedTab.ordinal * 10) }
                                Text(
                                    text = "${selectedTab.name}_$counter",
                                    modifier = Modifier.size(120.dp, 48.dp)
                                )
                            }
                        }
                    }
                    LiquidMorphBottomNavigation(
                        selectedTab = selectedTab,
                        isSearchActive = false,
                        searchQuery = "",
                        searchFilter = SearchFilter.ALL,
                        isFilterMenuOpen = false,
                        showLabels = true,
                        onSelectTab = { selectedTab = it },
                        onOpenSearch = {},
                        onCloseSearch = {},
                        onQueryChange = {},
                        onToggleFilterMenu = {},
                        onSelectFilter = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("HOME_0").assertIsDisplayed()

        // Rapidly cycle HOME -> LIBRARY -> SETTINGS -> HOME -> LIBRARY -> HOME
        val sequence = listOf(
            BottomNavTab.LIBRARY,
            BottomNavTab.SETTINGS,
            BottomNavTab.HOME,
            BottomNavTab.LIBRARY,
            BottomNavTab.HOME
        )
        sequence.forEach { tab ->
            composeTestRule.runOnUiThread { selectedTab = tab }
            composeTestRule.waitForIdle()
        }

        composeTestRule.onNodeWithText("HOME_0").assertIsDisplayed()
        assertTrue(homeEntranceSkippedOnRevisit)
    }

    @Test
    fun `500-track repository aggregation and Coil ImageLoaderFactory perform cleanly`() = runBlocking<Unit> {
        val bulkItems = (1..500).map { i ->
            MediaItemEntity(
                uri = "content://local/audio/bulk_$i",
                mediaType = MediaType.AUDIO,
                title = "Track $i",
                artist = "Artist ${i % 25}",
                album = "Album ${i % 40}",
                genre = "Genre ${i % 8}",
                durationMs = 180_000L + (i * 100L),
                playCount = i % 15,
                fileName = "track_$i.mp3",
                folderName = "Folder_${i % 10}",
                folderPath = "/Music/Folder_${i % 10}"
            )
        }
        bulkItems.forEach { db.mediaDao().insert(it) }

        val startMs = System.currentTimeMillis()
        val albums = repository.albums.first()
        val artists = repository.artists.first()
        val folders = repository.folders.first()
        val genres = repository.genres.first()
        val search = repository.searchLocal(
            MutableStateFlow("Track 42"),
            MutableStateFlow(SearchFilter.ALL)
        ).first()
        val elapsedMs = System.currentTimeMillis() - startMs

        assertEquals(40, albums.size)
        assertEquals(25, artists.size)
        assertEquals(10, folders.size)
        assertEquals(8, genres.size)
        assertTrue(search.songs.any { it.title == "Track 42" })
        assertTrue("500-track aggregation should finish well under 5s (took ${elapsedMs}ms)", elapsedMs < 5_000L)

        val app = ApplicationProvider.getApplicationContext<LumaApplication>()
        val imageLoader = app.newImageLoader()
        assertNotNull(imageLoader.memoryCache)
        assertNotNull(imageLoader.diskCache)
    }

    @Test
    fun `luma 4_0 room custom collections and listening session persistence work cleanly`() = runBlocking<Unit> {
        val repoV4 = MediaRepository(
            mediaDao = db.mediaDao(),
            playlistDao = db.playlistDao(),
            historyDao = db.historyDao(),
            queueDao = db.queueDao(),
            scanner = MediaScanner(context),
            customCollectionDao = db.customCollectionDao(),
            listeningSessionDao = db.listeningSessionDao()
        )
        val t1Id = db.mediaDao().insert(
            MediaItemEntity(
                uri = "content://local/audio/col1",
                mediaType = MediaType.AUDIO,
                title = "Skyline Drive",
                artist = "Luma Synth",
                album = "Midnight",
                genre = "Electronic",
                durationMs = 210_000L,
                fileName = "skyline.mp3",
                folderName = "Music",
                folderPath = "/Music"
            )
        )
        val colId = repoV4.createCollection(
            name = "Corrida Noturna",
            description = "Synthwave e energia",
            accentTag = "violet",
            initialTrackIds = listOf(t1Id)
        )
        assertTrue(colId > 0L)
        val collections = repoV4.customCollections.first()
        assertEquals(1, collections.size)
        assertEquals("Corrida Noturna", collections.first().collection.name)
        assertEquals(1, collections.first().tracks.size)

        repoV4.toggleCollectionPinned(colId, true)
        assertTrue(repoV4.customCollections.first().first().collection.isPinned)

        repoV4.recordListeningSession(
            mediaId = t1Id,
            startedAt = 1_700_000_000_000L,
            durationListenedMs = 195_000L,
            trackDurationMs = 210_000L,
            wasSkippedEarly = false,
            outputRoute = "Bluetooth"
        )
        val sessions = repoV4.listeningSessions.first()
        assertEquals(1, sessions.size)
        assertEquals("Bluetooth", sessions.first().outputRoute)
        assertTrue(sessions.first().completedTrack)
    }

    @Test
    fun `luma 4_0 intelligence engine classifies moods builds auto dj finds similar tracks and analyzes habits`() {
        val seed = MediaItemEntity(
            id = 1L,
            uri = "content://local/audio/s1",
            mediaType = MediaType.AUDIO,
            title = "Neon Pulse",
            artist = "Luma Waves",
            album = "Prism",
            genre = "Electronic",
            durationMs = 195_000L,
            playCount = 9,
            isFavorite = true,
            fileName = "neon.mp3",
            folderName = "Music",
            folderPath = "/Music"
        )
        val similar = MediaItemEntity(
            id = 2L,
            uri = "content://local/audio/s2",
            mediaType = MediaType.AUDIO,
            title = "Glass Echo",
            artist = "Luma Waves",
            album = "Prism",
            genre = "Electronic",
            durationMs = 205_000L,
            playCount = 3,
            fileName = "glass.flac",
            folderName = "Music",
            folderPath = "/Music"
        )
        val chill = MediaItemEntity(
            id = 3L,
            uri = "content://local/audio/s3",
            mediaType = MediaType.AUDIO,
            title = "Quiet Rain",
            artist = "Acoustic Trio",
            album = "Nights",
            genre = "Acoustic Lo-Fi",
            durationMs = 260_000L,
            playCount = 1,
            fileName = "rain.mp3",
            folderName = "Chill",
            folderPath = "/Chill"
        )
        val library = listOf(seed, similar, chill)
        assertEquals(LumaMoodCategory.CALM_CHILL, LumaIntelligenceEngine.classifyMood(chill).mood)

        val simList = LumaIntelligenceEngine.findSimilarTracks(seed, library)
        assertEquals(similar.id, simList.first().id)

        val autoDj = LumaIntelligenceEngine.buildAutoDjMix(seed, library, limit = 3)
        assertEquals(3, autoDj.size)
        assertEquals(seed.id, autoDj.first().id)

        val sessions = listOf(
            ListeningSessionEntity(
                mediaId = 1L,
                startedAt = 1_700_000_000_000L,
                durationListenedMs = 190_000L,
                completedTrack = true,
                skippedEarly = false,
                outputRoute = "Bluetooth"
            )
        )
        val snapshot = LumaIntelligenceEngine.buildSnapshot(library, sessions, emptyList())
        assertTrue(snapshot.smartRecommendations.isNotEmpty())
        assertEquals(100, snapshot.patternReport.completionRatePercent)
    }

    @Test
    fun `luma 4_0 collections engine computes library health shared artwork matches and best duplicate quality`() {
        val goodFlac = MediaItemEntity(
            id = 10L,
            uri = "content://local/audio/d1",
            mediaType = MediaType.AUDIO,
            title = "Horizonte",
            artist = "Mar",
            album = "Oceano",
            genre = "Indie",
            artworkPath = "/cache/oceano.jpg",
            sizeBytes = 28_000_000L,
            durationMs = 220_000L,
            playCount = 4,
            fileName = "horizonte.flac",
            folderName = "Music",
            folderPath = "/Music"
        )
        val missingArtSameAlbum = MediaItemEntity(
            id = 11L,
            uri = "content://local/audio/d2",
            mediaType = MediaType.AUDIO,
            title = "Horizonte",
            artist = "Artista desconhecido",
            album = "Oceano",
            artworkPath = null,
            sizeBytes = 4_000_000L,
            durationMs = 220_000L,
            playCount = 0,
            fileName = "horizonte.mp3",
            folderName = "Music",
            folderPath = "/Music"
        )
        val media = listOf(goodFlac, missingArtSameAlbum)
        val duplicateGroup = DuplicateGroup("Horizonte", "Mesmo título e duração", media)
        val health = LumaCollectionsEngine.computeLibraryHealth(
            allMedia = media,
            duplicateGroups = listOf(duplicateGroup)
        )
        assertEquals(1, health.missingArtworkTracks.size)
        assertEquals(1, health.missingMetadataTracks.size)
        assertEquals(goodFlac.id, LumaCollectionsEngine.pickRecommendedKeepId(duplicateGroup))

        val matches = LumaCollectionsEngine.matchSharedArtworkCandidates(media)
        assertEquals("/cache/oceano.jpg", matches[missingArtSameAlbum.id])
    }

    @Test
    fun `luma 4_0 studio dsp helpers compute fade envelope build wav header and fallback waveform`() {
        assertEquals(0f, LumaStudioEngine.computeFadeGain(0L, 1000L, 200L, 200L), 0.01f)
        assertEquals(1f, LumaStudioEngine.computeFadeGain(500L, 1000L, 200L, 200L), 0.01f)
        assertEquals(0f, LumaStudioEngine.computeFadeGain(999L, 1000L, 200L, 200L), 0.01f)

        val pcm = ByteArray(8)
        // Little endian +10000 (0x2710) sample
        pcm[0] = 0x10; pcm[1] = 0x27
        pcm[4] = 0x10; pcm[5] = 0x27
        LumaStudioEngine.applyFadeToPcm16LeBuffer(
            bytes = pcm,
            bytesRead = 8,
            startFrameOffset = 0L,
            totalFrames = 2L,
            channels = 2,
            fadeInFrames = 1L,
            fadeOutFrames = 0L
        )
        // Frame 0 gain = 0f -> muted
        assertEquals(0, pcm[0].toInt())
        assertEquals(0, pcm[1].toInt())

        val header = LumaStudioEngine.buildWavHeader(pcmDataSizeBytes = 1024L, sampleRate = 44100, channels = 2)
        assertEquals(44, header.size)
        assertEquals('R', header[0].toInt().toChar())
        assertEquals('W', header[8].toInt().toChar())

        val wave = LumaStudioEngine.buildDeterministicFallbackWaveform("content://a/b", 180_000L, 48)
        assertEquals(48, wave.size)
        assertTrue(wave.all { it in 0.05f..1f })
    }

    @Test
    fun `luma 4_0 insights comparison file intelligence report and backup v2 round-trip work`() = runBlocking {
        val now = 1_750_000_000_000L
        val t1 = MediaItemEntity(
            id = 21L,
            uri = "content://local/audio/t21",
            mediaType = MediaType.AUDIO,
            title = "Solstice",
            artist = "Luma",
            album = "Solar",
            genre = "Ambient",
            durationMs = 240_000L,
            sizeBytes = 12_000_000L,
            playCount = 5,
            isFavorite = true,
            embeddedLyrics = "[00:01.00]Sunrise",
            fileName = "solstice.flac",
            folderName = "Albums",
            folderPath = "/Music/Albums"
        )
        val history = listOf(
            HistoryWithMedia(PlaybackHistoryEntity(mediaId = 21L, playedAt = now - 2 * 86_400_000L), t1),
            HistoryWithMedia(PlaybackHistoryEntity(mediaId = 21L, playedAt = now - 3 * 86_400_000L), t1),
            HistoryWithMedia(PlaybackHistoryEntity(mediaId = 21L, playedAt = now - 40 * 86_400_000L), t1)
        )
        val report = InsightsCalculator.compute(
            allMedia = listOf(t1),
            history = history,
            totalListeningTimeMs = 720_000L,
            nowMs = now,
            sessions = listOf(
                ListeningSessionEntity(mediaId = 21L, startedAt = now - 1000L, durationListenedMs = 230_000L, completedTrack = true, skippedEarly = false, outputRoute = "Wired")
            )
        )
        assertEquals(2, report.comparison.thisMonthPlays)
        assertEquals(1, report.comparison.lastMonthPlays)
        assertEquals(100, report.comparison.monthDeltaPercent)
        assertEquals("Auscultadores", report.comparison.topOutputRoute)

        val fi = FileIntelligenceEngine.analyze(listOf(t1), appCacheBytes = 1024L, deviceCodecs = emptyList())
        assertEquals(1, fi.audioCount)
        assertEquals(12_000_000L, fi.totalAudioBytes)
        assertTrue(FileIntelligenceEngine.exportReportAsText(fi).contains("LUMA PLAYER 4.0"))
        assertTrue(FileIntelligenceEngine.exportReportAsJson(fi).contains("healthScorePercent"))

        val collection = com.example.data.model.CustomCollectionWithTracks(
            collection = CustomCollectionEntity(id = 1L, name = "Ambient Gems", accentTag = "ocean"),
            tracks = listOf(t1)
        )
        val backupJson = BackupManager.serializeBackup(
            settings = AppSettings(autoResumeOnBluetooth = true),
            playlists = emptyList(),
            collections = listOf(collection),
            mediaMetadata = listOf(t1)
        )
        val parsed = BackupManager.parseBackup(backupJson, listOf(t1))
        assertEquals(true, parsed.settings?.autoResumeOnBluetooth)
        assertEquals(1, parsed.collections.size)
        assertEquals("Ambient Gems", parsed.collections.first().name)
        assertEquals(1, parsed.trackMetadata.size)
        assertEquals("[00:01.00]Sunrise", parsed.trackMetadata.first().lyricsRaw)
    }

    @Test
    fun `luma 4_0 discover tab navigation wrapped switcher and developer attribution render cleanly`() {
        var selectedTab by mutableStateOf(BottomNavTab.HOME)
        composeTestRule.setContent {
            LumaTheme(settings = AppSettings(reduceMotion = true, appLanguage = AppLanguage.PT)) {
                Column {
                    LiquidMorphBottomNavigation(
                        selectedTab = selectedTab,
                        isSearchActive = false,
                        searchQuery = "",
                        searchFilter = SearchFilter.ALL,
                        isFilterMenuOpen = false,
                        showLabels = true,
                        onSelectTab = { selectedTab = it },
                        onOpenSearch = {},
                        onCloseSearch = {},
                        onQueryChange = {},
                        onToggleFilterMenu = {},
                        onSelectFilter = {}
                    )
                    when (selectedTab) {
                        BottomNavTab.DISCOVER -> DiscoverScreen(
                            snapshot = com.example.data.media.LumaIntelligenceSnapshot(),
                            currentTrack = null,
                            audioCount = 0,
                            bottomInsetPadding = 64.dp,
                            onStartAutoDj = {},
                            onPlayTrack = { _, _ -> },
                            onOpenSimilarTracks = {},
                            onOpenStudio = {},
                            onOpenConnect = {},
                            onOpenInsights = {},
                            onOpenLibraryHealth = {},
                            onOpenFileIntelligence = {}
                        )
                        else -> AboutScreen(bottomInsetPadding = 64.dp, onBack = {}, onOpenSupportedFormats = {})
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        assertEquals("4.0.0", AppConfig.APP_VERSION)
        composeTestRule.onNodeWithTag("about_screen")
            .performScrollToNode(hasText("made by David tonecas helped by google AI studio"))
        composeTestRule.onNodeWithText("made by David tonecas helped by google AI studio").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_tab_discover").performClick()
        composeTestRule.waitForIdle()
        assertEquals(BottomNavTab.DISCOVER, selectedTab)
        composeTestRule.onNodeWithTag("discover_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("discover_screen")
            .performScrollToNode(hasTestTag("discover_open_studio"))
        composeTestRule.onNodeWithTag("discover_open_studio").assertIsDisplayed()
        composeTestRule.onNodeWithTag("discover_open_connect").assertIsDisplayed()
    }
}
