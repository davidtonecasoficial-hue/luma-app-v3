package com.example.ui.screens

import android.content.Context
import android.media.AudioManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.VolumeMute
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.media.OnlineLyricsCandidate
import com.example.data.model.AmbientSoundType
import com.example.data.model.AppSettings
import com.example.data.model.CoverDoubleTapAction
import com.example.data.model.CustomAudioPreset
import com.example.data.model.GlassQuality
import com.example.data.model.MediaItemEntity
import com.example.data.model.PlaybackAnimationStyle
import com.example.data.model.PlayerScreenStyle
import com.example.data.model.TrackBookmark
import com.example.data.model.VisualizerPreset
import com.example.data.model.VisualizerStyle
import com.example.player.AmbientMixerState
import com.example.player.PlaybackController
import com.example.player.PlaybackProgressUiState
import com.example.player.PlaybackUiState
import com.example.player.RepeatModeState
import com.example.player.SleepTimerMode
import com.example.ui.components.AbLoopSheetContent
import com.example.ui.components.AmbientMixerSheetContent
import com.example.ui.components.BookmarksSheetContent
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.FloatingGlassPlayerDock
import com.example.ui.components.FullScreenLyricsDialog
import com.example.ui.components.LyricsStageView
import com.example.ui.components.LyricsStudioSheetContent
import com.example.ui.components.PlayerStylePickerSheetContent
import com.example.ui.components.PosterImmersiveStage
import com.example.ui.components.SpatialGlassCoverDeck
import com.example.ui.components.SpectrumVisualizerView
import com.example.ui.components.SpeedAndPitchSheetContent
import com.example.ui.components.StoryCardPreviewDialog
import com.example.ui.components.StudioEqualizerSheetContent
import com.example.ui.theme.ExtractedArtworkColors
import com.example.ui.theme.LiquidGlassBackground
import com.example.ui.theme.LiquidGlassCapsule
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LocalLumaGlass
import com.example.ui.theme.LocalLumaStrings
import com.example.ui.theme.LumaDropdownMenu
import com.example.ui.theme.MediaArtwork
import com.example.ui.theme.SculptedCircleButton
import com.example.ui.theme.buildColorSchemeFromGlass
import com.example.ui.theme.formatDuration
import com.example.ui.theme.lumaMenuItemColors
import com.example.ui.theme.rememberDynamicArtworkColors
import com.example.ui.theme.rememberHarmonizedNpdGlassTokens
import com.example.ui.theme.rememberLumaBounceScale
import com.example.ui.theme.resolveArtworkShape
import com.example.ui.viewmodel.LyricsSearchUiState
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.flow.StateFlow

private enum class PlayerSheet {
    NONE,
    QUEUE,
    EQUALIZER,
    SLEEP_TIMER,
    SPEED,
    LYRICS,
    STYLES,
    AB_LOOP,
    BOOKMARKS,
    AMBIENT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(
    playbackState: PlaybackUiState,
    settings: AppSettings,
    playbackController: PlaybackController,
    progressFlow: StateFlow<PlaybackProgressUiState>? = null,
    onClose: () -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit,
    onSmartMix: (MediaItemEntity) -> Unit,
    onAddToPlaylist: (MediaItemEntity) -> Unit,
    onSaveQueueAsPlaylist: (String) -> Unit,
    onToggleMinimalistMode: (Boolean) -> Unit,
    onEditMetadata: (MediaItemEntity) -> Unit = {},
    onImportLyrics: (Long) -> Unit = {},
    onClearLyrics: (Long) -> Unit = {},
    onSelectPlayerStyle: (PlayerScreenStyle) -> Unit = {},
    customAudioPresets: List<CustomAudioPreset> = emptyList(),
    onSaveCustomAudioPreset: (CustomAudioPreset) -> Unit = {},
    onDeleteCustomAudioPreset: (String) -> Unit = {},
    onSelectVisualizerStyle: (VisualizerStyle) -> Unit = {},
    onSelectVisualizerPreset: (VisualizerPreset) -> Unit = {},
    onToggleReactiveLighting: (Boolean) -> Unit = {},
    onSaveLyricsAndOffset: (Long, String?, Long) -> Unit = { _, _, _ -> },
    onUpdateLyricsFontScale: (Float) -> Unit = {},
    onToggleLyricsCentered: (Boolean) -> Unit = {},
    lyricsSearchState: LyricsSearchUiState = LyricsSearchUiState(),
    onSearchOnlineLyrics: (MediaItemEntity, String?, String?) -> Unit = { _, _, _ -> },
    onApplyOnlineLyrics: (Long, OnlineLyricsCandidate) -> Unit = { _, _ -> },
    onClearLyricsSearchResults: () -> Unit = {},
    bookmarks: List<TrackBookmark> = emptyList(),
    ambientState: AmbientMixerState = AmbientMixerState(),
    queueRadioEnabled: Boolean = false,
    onToggleQueueRadio: (Boolean) -> Unit = {},
    onAddBookmark: (Long, String) -> Unit = { _, _ -> },
    onDeleteBookmark: (Long) -> Unit = {},
    onSelectAmbientSound: (AmbientSoundType) -> Unit = {},
    onSetAmbientVolume: (Float) -> Unit = {},
    onOpenInStudio: (MediaItemEntity) -> Unit = {},
    onOpenSimilarTracks: (MediaItemEntity) -> Unit = {}
) {
    BackHandler { onClose() }

    val track = playbackState.currentTrack
    if (track == null) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    val harmonizedGlass = rememberHarmonizedNpdGlassTokens(
        artworkPath = track.artworkPath,
        enabled = settings.dynamicCoverTheme
    )

    CompositionLocalProvider(LocalLumaGlass provides harmonizedGlass) {
    MaterialTheme(colorScheme = buildColorSchemeFromGlass(harmonizedGlass, MaterialTheme.colorScheme)) {
    val glass = LocalLumaGlass.current
    val strings = LocalLumaStrings.current
    val context = LocalContext.current
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }
    val haptics = LocalHapticFeedback.current
    val spectrumSnapshot by playbackController.spectrumSnapshot.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    var isAppForeground by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            isAppForeground = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var activeSheet by remember { mutableStateOf(PlayerSheet.NONE) }
    var optionsMenuOpen by remember { mutableStateOf(false) }
    var showSaveQueueDialog by remember { mutableStateOf(false) }
    var showClearQueueConfirm by remember { mutableStateOf(false) }
    var showFullScreenLyrics by remember { mutableStateOf(false) }
    var showStoryCardDialog by remember { mutableStateOf(false) }

    var isSeeking by remember { mutableStateOf(false) }
    var seekFraction by remember { mutableFloatStateOf(0f) }

    // Edge-swipe gesture states (left edge vertical = volume, right edge vertical = seek)
    var activeEdgeGestureType by remember { mutableStateOf<EdgeSwipeMode?>(null) }
    var edgeVolumePercent by remember { mutableIntStateOf(0) }
    var edgeSeekPreviewMs by remember { mutableLongStateOf(0L) }
    var edgeSeekDeltaMs by remember { mutableLongStateOf(0L) }
    val isEdgeSeeking = activeEdgeGestureType == EdgeSwipeMode.SEEK

    val animStyle = settings.playbackAnimationStyle
    val shouldAnimateCover = settings.albumArtAnimationEnabled && !glass.reduceMotion && animStyle != PlaybackAnimationStyle.CALM
    val coverScale by animateFloatAsState(
        targetValue = if (playbackState.isPlaying && shouldAnimateCover) 1.02f else 0.96f,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "coverScale"
    )

    val rotationAnim = remember(track.id) { Animatable(0f) }
    LaunchedEffect(track.id, playbackState.isPlaying, shouldAnimateCover, isAppForeground, animStyle) {
        if (playbackState.isPlaying && shouldAnimateCover && isAppForeground && animStyle == PlaybackAnimationStyle.VINYL_SPIN) {
            while (true) {
                val startAngle = rotationAnim.value % 360f
                rotationAnim.snapTo(startAngle)
                rotationAnim.animateTo(
                    targetValue = startAngle + 360f,
                    animationSpec = tween(durationMillis = 18_000, easing = LinearEasing)
                )
            }
        }
    }

    val extractedArtworkColors = rememberDynamicArtworkColors(
        artworkPath = track.artworkPath,
        enabled = settings.dynamicCoverTheme && !glass.isMonochrome,
        fallbackPrimary = glass.accentPrimary,
        fallbackSecondary = glass.accentSecondary
    )
    val dynamicHueTint = if (settings.dynamicCoverTheme && !glass.isMonochrome) {
        extractedArtworkColors.primary
    } else null

    val latestPlaybackState by rememberUpdatedState(playbackState)
    val doubleTapActionCallback: () -> Unit = {
        when (settings.coverDoubleTapAction) {
            CoverDoubleTapAction.PLAY_PAUSE -> playbackController.togglePlayPause()
            CoverDoubleTapAction.FAVORITE -> onToggleFavorite(track)
            CoverDoubleTapAction.SEEK_FORWARD -> {
                val livePos = progressFlow?.value?.positionMs
                    ?: playbackController.progressState.value.positionMs.takeIf {
                        it > 0L || playbackController.progressState.value.durationMs > 0L
                    }
                    ?: latestPlaybackState.positionMs
                playbackController.seekTo(livePos + 10_000L)
            }
        }
    }

    val isNeumorphic = glass.isNeumorphic
    val edgeZonesInteractive = activeSheet == PlayerSheet.NONE && !showFullScreenLyrics && !showStoryCardDialog

    LiquidGlassBackground(dynamicAccentTint = dynamicHueTint) {
        Box(modifier = Modifier.fillMaxSize()) {
        CoverThemedAtmosphericBackdrop(
            artworkPath = track.artworkPath,
            palette = extractedArtworkColors,
            enabled = settings.dynamicCoverTheme && !glass.isMonochrome,
            isPlaying = playbackState.isPlaying && isAppForeground,
            reduceMotion = glass.reduceMotion,
            isDark = glass.isDark,
            baseBackground = glass.background,
            glassQuality = glass.glassQuality
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .testTag("music_player_screen"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 580.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isNeumorphic) {
                    SculptedCircleButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(44.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("player_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Minimizar leitor",
                            tint = glass.textPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Minimizar leitor",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                LiquidGlassCapsule(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                        .testTag("npd_header_pill")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (playbackState.isPlaying) glass.accentPrimary else glass.textSecondary)
                            )
                            Text(
                                text = strings.nowPlayingBadge,
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                                color = if (isNeumorphic) glass.textSecondary else glass.accentPrimary
                            )
                            val extLabel = remember(track.fileName, track.mimeType) {
                                track.fileName.substringAfterLast('.', "").uppercase().takeIf { it.length in 2..5 }
                            }
                            if (extLabel != null) {
                                Text(
                                    text = extLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                    color = glass.onAccent,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(glass.accentPrimary.copy(alpha = 0.85f))
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = track.album,
                            style = MaterialTheme.typography.labelMedium,
                            color = glass.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                val styleBtnInteraction = remember { MutableInteractionSource() }
                val styleBtnBounce = rememberLumaBounceScale(styleBtnInteraction)
                IconButton(
                    onClick = { activeSheet = PlayerSheet.STYLES },
                    interactionSource = styleBtnInteraction,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .graphicsLayer { scaleX = styleBtnBounce; scaleY = styleBtnBounce }
                        .testTag("player_style_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ViewCarousel,
                        contentDescription = "Estilo do ecrã A Reproduzir",
                        tint = glass.accentPrimary
                    )
                }

                val minBtnInteraction = remember { MutableInteractionSource() }
                val minBtnBounce = rememberLumaBounceScale(minBtnInteraction)
                IconButton(
                    onClick = { onToggleMinimalistMode(!settings.minimalistPlayerMode) },
                    interactionSource = minBtnInteraction,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .graphicsLayer { scaleX = minBtnBounce; scaleY = minBtnBounce }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CenterFocusStrong,
                        contentDescription = "Modo minimalista",
                        tint = if (settings.minimalistPlayerMode) glass.accentPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    if (isNeumorphic) {
                        SculptedCircleButton(
                            onClick = { optionsMenuOpen = true },
                            modifier = Modifier
                                .size(44.dp)
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Mais opções da faixa",
                                tint = glass.textPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { optionsMenuOpen = true },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Mais opções da faixa",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    val menuColors = lumaMenuItemColors()
                    LumaDropdownMenu(
                        expanded = optionsMenuOpen,
                        onDismissRequest = { optionsMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Estilo do ecrã (${settings.playerScreenStyle.title})") },
                            leadingIcon = { Icon(Icons.Rounded.ViewCarousel, contentDescription = null) },
                            colors = menuColors,
                            onClick = {
                                optionsMenuOpen = false
                                activeSheet = PlayerSheet.STYLES
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Smart Mix local") },
                            leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                            colors = menuColors,
                            onClick = {
                                optionsMenuOpen = false
                                onSmartMix(track)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Adicionar a playlist") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null) },
                            colors = menuColors,
                            onClick = {
                                optionsMenuOpen = false
                                onAddToPlaylist(track)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Detalhes e metadados") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            colors = menuColors,
                            onClick = {
                                optionsMenuOpen = false
                                onEditMetadata(track)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.abLoopTitle) },
                            leadingIcon = { Icon(Icons.Rounded.Loop, contentDescription = null) },
                            colors = menuColors,
                            onClick = {
                                optionsMenuOpen = false
                                activeSheet = PlayerSheet.AB_LOOP
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.bookmarksTitle) },
                            leadingIcon = { Icon(Icons.Rounded.Bookmark, contentDescription = null) },
                            colors = menuColors,
                            onClick = {
                                optionsMenuOpen = false
                                activeSheet = PlayerSheet.BOOKMARKS
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.ambientTitle) },
                            leadingIcon = { Icon(Icons.Rounded.Spa, contentDescription = null) },
                            colors = menuColors,
                            onClick = {
                                optionsMenuOpen = false
                                activeSheet = PlayerSheet.AMBIENT
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.shareCardTitle) },
                            leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                            colors = menuColors,
                            onClick = {
                                optionsMenuOpen = false
                                showStoryCardDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Editar no Luma Studio") },
                            leadingIcon = { Icon(Icons.Rounded.GraphicEq, contentDescription = null) },
                            colors = menuColors,
                            onClick = {
                                optionsMenuOpen = false
                                onOpenInStudio(track)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Faixas semelhantes") },
                            leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                            colors = menuColors,
                            onClick = {
                                optionsMenuOpen = false
                                onOpenSimilarTracks(track)
                            }
                        )
                    }
                }
            }

            when (settings.playerScreenStyle) {
                PlayerScreenStyle.GLASS_DECK -> {
                    Spacer(modifier = Modifier.weight(0.35f))

                    SpatialGlassCoverDeck(
                        playbackState = playbackState,
                        isPlayingForeground = playbackState.isPlaying && isAppForeground,
                        gesturesEnabled = settings.coverGesturesEnabled,
                        onPrevious = { playbackController.skipToPrevious() },
                        onNext = { playbackController.skipToNext() },
                        onTogglePlayPause = { playbackController.togglePlayPause() },
                        onJumpToIndex = { idx -> playbackController.jumpToQueueIndex(idx) },
                        onToggleFavorite = onToggleFavorite,
                        onDoubleTapCenter = doubleTapActionCallback
                    )

                    if (settings.visualizerEnabled && !settings.minimalistPlayerMode) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val vizHeight = if (settings.visualizerStyle == VisualizerStyle.CIRCULAR_SPECTRUM) 56.dp else 26.dp
                        SpectrumVisualizerView(
                            snapshot = spectrumSnapshot,
                            isPlaying = playbackState.isPlaying && isAppForeground,
                            style = settings.visualizerStyle,
                            preset = settings.visualizerPreset,
                            reactiveLighting = settings.reactiveLightingEnabled,
                            reduceMotion = glass.reduceMotion,
                            accentPrimary = extractedArtworkColors.primary,
                            accentSecondary = extractedArtworkColors.secondary,
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .fillMaxWidth(0.68f)
                                .height(vizHeight)
                        )
                    }

                    Spacer(modifier = Modifier.weight(0.45f))

                    PlayerActiveFeatureBadges(
                        playbackState = playbackState,
                        bookmarks = bookmarks,
                        ambientState = ambientState,
                        onOpenAbLoop = { activeSheet = PlayerSheet.AB_LOOP },
                        onOpenSpeedPitch = { activeSheet = PlayerSheet.SPEED },
                        onOpenAmbient = { activeSheet = PlayerSheet.AMBIENT },
                        onSeekToBookmark = { playbackController.seekTo(it) }
                    )

                    // Interactive Scrubber + Shuffle/Repeat quick toggles
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 580.dp)
                    ) {
                        PlayerProgressSliderAndWaveform(
                            progressFlow = progressFlow,
                            fallbackPositionMs = playbackState.positionMs,
                            fallbackDurationMs = playbackState.durationMs,
                            trackId = track.id,
                            showWaveform = false,
                            isSeeking = isSeeking,
                            seekFraction = seekFraction,
                            isEdgeSeeking = isEdgeSeeking,
                            edgeSeekPreviewMs = edgeSeekPreviewMs,
                            onSeekChange = { value ->
                                isSeeking = true
                                seekFraction = value
                            },
                            onSeekFinished = { targetMs ->
                                playbackController.seekTo(targetMs)
                                isSeeking = false
                            },
                            activeColor = glass.accentPrimary,
                            inactiveColor = glass.textPrimary.copy(alpha = if (glass.isDark) 0.20f else 0.15f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            PlayerElapsedTimeText(
                                progressFlow = progressFlow,
                                fallbackPositionMs = playbackState.positionMs,
                                fallbackDurationMs = playbackState.durationMs,
                                isSeeking = isSeeking,
                                seekFraction = seekFraction,
                                isEdgeSeeking = isEdgeSeeking,
                                edgeSeekPreviewMs = edgeSeekPreviewMs
                            )
                            if (!settings.minimalistPlayerMode) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    IconButton(
                                        onClick = { playbackController.toggleShuffle() },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Shuffle,
                                            contentDescription = "Reprodução aleatória",
                                            tint = if (playbackState.shuffleEnabled) glass.accentPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { playbackController.cycleRepeatMode() },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        val icon = if (playbackState.repeatMode == RepeatModeState.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = "Repetição: ${playbackState.repeatMode.name}",
                                            tint = if (playbackState.repeatMode != RepeatModeState.OFF) glass.accentPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { activeSheet = PlayerSheet.SLEEP_TIMER },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Bedtime,
                                            contentDescription = "Temporizador de sono",
                                            tint = if (playbackState.sleepTimerMode != SleepTimerMode.OFF) glass.accentPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { activeSheet = PlayerSheet.SPEED },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Speed,
                                            contentDescription = "Velocidade e Tom (DJ)",
                                            tint = if (playbackState.playbackSpeed != 1.0f || abs(playbackState.playbackPitchSemitones) > 0.1f) {
                                                glass.accentPrimary
                                            } else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            PlayerRemainingTimeText(
                                progressFlow = progressFlow,
                                fallbackPositionMs = playbackState.positionMs,
                                fallbackDurationMs = playbackState.durationMs,
                                isSeeking = isSeeking,
                                seekFraction = seekFraction,
                                isEdgeSeeking = isEdgeSeeking,
                                edgeSeekPreviewMs = edgeSeekPreviewMs,
                                includeTotalDuration = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FloatingGlassPlayerDock(
                        track = track,
                        playbackState = playbackState,
                        isPlayingForeground = playbackState.isPlaying && isAppForeground,
                        minimalistMode = settings.minimalistPlayerMode,
                        onPrevious = { playbackController.skipToPrevious() },
                        onTogglePlayPause = { playbackController.togglePlayPause() },
                        onNext = { playbackController.skipToNext() },
                        onOpenLyrics = { activeSheet = PlayerSheet.LYRICS },
                        onOpenQueue = { activeSheet = PlayerSheet.QUEUE },
                        onOpenEqualizer = {
                            playbackController.initEqualizer()
                            activeSheet = PlayerSheet.EQUALIZER
                        },
                        onOpenOptions = { optionsMenuOpen = true }
                    )
                }

                PlayerScreenStyle.CAROUSEL_VINYL,
                PlayerScreenStyle.MINIMAL_POSTER,
                PlayerScreenStyle.LYRICS_STAGE -> {
                    Spacer(modifier = Modifier.weight(0.4f))

                    when (settings.playerScreenStyle) {
                        PlayerScreenStyle.MINIMAL_POSTER -> {
                            PosterImmersiveStage(
                                track = track,
                                playbackState = playbackState,
                                gesturesEnabled = settings.coverGesturesEnabled,
                                onPrevious = { playbackController.skipToPrevious() },
                                onNext = { playbackController.skipToNext() },
                                onToggleFavorite = onToggleFavorite,
                                onDoubleTap = doubleTapActionCallback,
                                modifier = Modifier.weight(3.2f, fill = false)
                            )
                        }

                        PlayerScreenStyle.LYRICS_STAGE -> {
                            LyricsStageView(
                                track = track,
                                positionMs = playbackState.positionMs,
                                onSeekTo = { playbackController.seekTo(it) },
                                onImportLyrics = { onImportLyrics(track.id) },
                                onToggleFavorite = onToggleFavorite,
                                lyricsOffsetMs = track.lyricsOffsetMs,
                                lyricsFontScale = settings.lyricsFontScale,
                                lyricsCentered = settings.lyricsCentered,
                                onOpenFullScreenLyrics = { showFullScreenLyrics = true },
                                modifier = Modifier.weight(3.2f),
                                progressFlow = progressFlow
                            )
                        }

                        else -> {
                            // CAROUSEL_VINYL
                            val swipeModifier = if (settings.coverGesturesEnabled) {
                                var dragSum = 0f
                                Modifier.pointerInput(track.id) {
                                    detectHorizontalDragGestures(
                                        onHorizontalDrag = { _, dragAmount -> dragSum += dragAmount },
                                        onDragEnd = {
                                            if (dragSum < -110f) {
                                                playbackController.skipToNext()
                                            } else if (dragSum > 110f) {
                                                playbackController.skipToPrevious()
                                            }
                                            dragSum = 0f
                                        },
                                        onDragCancel = { dragSum = 0f }
                                    )
                                }
                            } else Modifier

                            val useCircleVinyl = animStyle == PlaybackAnimationStyle.VINYL_SPIN || isNeumorphic
                            val coverShape = if (useCircleVinyl) CircleShape else resolveArtworkShape(settings.artworkShape, 30.dp)
                            LiquidGlassSurface(
                                shape = coverShape,
                                elevated = true,
                                accentGlow = if (isNeumorphic) null else extractedArtworkColors.primary,
                                modifier = Modifier
                                    .widthIn(max = 310.dp)
                                    .fillMaxWidth(0.76f)
                                    .aspectRatio(1f)
                                    .scale(coverScale)
                                    .then(swipeModifier)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(if (isNeumorphic) 14.dp else if (useCircleVinyl) 10.dp else 0.dp)
                                        .then(
                                            if (isNeumorphic) {
                                                Modifier
                                                    .border(
                                                        width = 2.dp,
                                                        brush = Brush.linearGradient(
                                                            listOf(
                                                                glass.reliefShadowDark.copy(alpha = 0.35f),
                                                                glass.reliefShadowLight.copy(alpha = 0.85f)
                                                            )
                                                        ),
                                                        shape = CircleShape
                                                    )
                                                    .padding(4.dp)
                                            } else Modifier
                                        )
                                        .rotate(if (shouldAnimateCover && animStyle == PlaybackAnimationStyle.VINYL_SPIN) rotationAnim.value else 0f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    MediaArtwork(
                                        artworkPath = track.artworkPath,
                                        contentDescription = "Capa de ${track.title}",
                                        cornerRadius = if (useCircleVinyl) 160.dp else 30.dp,
                                        shapeOverride = coverShape,
                                        maxDecodeSize = 400.dp,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (useCircleVinyl && settings.albumArtAnimationEnabled) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(glass.background.copy(alpha = 0.84f))
                                                .border(1.dp, glass.glassBorder, CircleShape)
                                        )
                                    }
                                }
                            }

                            if (settings.visualizerEnabled && !settings.minimalistPlayerMode) {
                                Spacer(modifier = Modifier.height(14.dp))
                                val vizHeight = if (settings.visualizerStyle == VisualizerStyle.CIRCULAR_SPECTRUM) 56.dp else 26.dp
                                SpectrumVisualizerView(
                                    snapshot = spectrumSnapshot,
                                    isPlaying = playbackState.isPlaying && isAppForeground,
                                    style = settings.visualizerStyle,
                                    preset = settings.visualizerPreset,
                                    reactiveLighting = settings.reactiveLightingEnabled,
                                    reduceMotion = glass.reduceMotion,
                                    accentPrimary = extractedArtworkColors.primary,
                                    accentSecondary = extractedArtworkColors.secondary,
                                    modifier = Modifier
                                        .widthIn(max = 260.dp)
                                        .fillMaxWidth(0.65f)
                                        .height(vizHeight)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(0.45f))

                    if (settings.playerScreenStyle == PlayerScreenStyle.CAROUSEL_VINYL) {
                        // Track Info + Favorite
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 580.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${track.artist} • ${track.album}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (glass.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onToggleFavorite(track)
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("player_favorite_button")
                            ) {
                                Icon(
                                    imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = if (track.isFavorite) "Remover favorito" else "Adicionar favorito",
                                    tint = if (track.isFavorite) glass.accentSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    PlayerActiveFeatureBadges(
                        playbackState = playbackState,
                        bookmarks = bookmarks,
                        ambientState = ambientState,
                        onOpenAbLoop = { activeSheet = PlayerSheet.AB_LOOP },
                        onOpenSpeedPitch = { activeSheet = PlayerSheet.SPEED },
                        onOpenAmbient = { activeSheet = PlayerSheet.AMBIENT },
                        onSeekToBookmark = { playbackController.seekTo(it) }
                    )

                    // Waveform & Draggable Progress Bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 580.dp)
                    ) {
                        PlayerProgressSliderAndWaveform(
                            progressFlow = progressFlow,
                            fallbackPositionMs = playbackState.positionMs,
                            fallbackDurationMs = playbackState.durationMs,
                            trackId = track.id,
                            showWaveform = settings.playerScreenStyle == PlayerScreenStyle.CAROUSEL_VINYL,
                            isSeeking = isSeeking,
                            seekFraction = seekFraction,
                            isEdgeSeeking = isEdgeSeeking,
                            edgeSeekPreviewMs = edgeSeekPreviewMs,
                            onSeekChange = { value ->
                                isSeeking = true
                                seekFraction = value
                            },
                            onSeekFinished = { targetMs ->
                                playbackController.seekTo(targetMs)
                                isSeeking = false
                            },
                            activeColor = glass.accentPrimary,
                            inactiveColor = glass.textPrimary.copy(alpha = if (glass.isDark) 0.20f else 0.15f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            PlayerElapsedTimeText(
                                progressFlow = progressFlow,
                                fallbackPositionMs = playbackState.positionMs,
                                fallbackDurationMs = playbackState.durationMs,
                                isSeeking = isSeeking,
                                seekFraction = seekFraction,
                                isEdgeSeeking = isEdgeSeeking,
                                edgeSeekPreviewMs = edgeSeekPreviewMs
                            )
                            PlayerRemainingTimeText(
                                progressFlow = progressFlow,
                                fallbackPositionMs = playbackState.positionMs,
                                fallbackDurationMs = playbackState.durationMs,
                                isSeeking = isSeeking,
                                seekFraction = seekFraction,
                                isEdgeSeeking = isEdgeSeeking,
                                edgeSeekPreviewMs = edgeSeekPreviewMs,
                                includeTotalDuration = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Transport Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 580.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { playbackController.toggleShuffle() },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = "Reprodução aleatória",
                                tint = if (playbackState.shuffleEnabled) glass.accentPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isNeumorphic) {
                            SculptedCircleButton(
                                onClick = {
                                    if (glass.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    playbackController.skipToPrevious()
                                },
                                modifier = Modifier
                                    .size(52.dp)
                                    .minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipPrevious,
                                    contentDescription = "Faixa anterior",
                                    tint = glass.textPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        } else {
                            val prevInteraction = remember { MutableInteractionSource() }
                            val prevScale = rememberLumaBounceScale(prevInteraction)
                            IconButton(
                                onClick = {
                                    if (glass.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    playbackController.skipToPrevious()
                                },
                                interactionSource = prevInteraction,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .graphicsLayer { scaleX = prevScale; scaleY = prevScale }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipPrevious,
                                    contentDescription = "Faixa anterior",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        if (isNeumorphic) {
                            SculptedCircleButton(
                                onClick = {
                                    if (glass.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    playbackController.togglePlayPause()
                                },
                                emphasized = true,
                                modifier = Modifier
                                    .size(72.dp)
                                    .testTag("player_play_pause_button")
                            ) {
                                AnimatedContent(
                                    targetState = playbackState.isPlaying,
                                    transitionSpec = {
                                        if (glass.reduceMotion) {
                                            fadeIn(tween(80)) togetherWith fadeOut(tween(80))
                                        } else {
                                            (fadeIn(tween(150)) + scaleIn(initialScale = 0.8f)) togetherWith
                                                (fadeOut(tween(120)) + scaleOut(targetScale = 0.8f))
                                        }
                                    },
                                    label = "fullPlayPauseMorphBw"
                                ) { playing ->
                                    Icon(
                                        imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (playing) "Pausar" else "Reproduzir",
                                        tint = glass.textPrimary,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                        } else {
                            val playInteraction = remember { MutableInteractionSource() }
                            val playScale = rememberLumaBounceScale(playInteraction, pressedScale = 0.88f)
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .graphicsLayer { scaleX = playScale; scaleY = playScale }
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(glass.accentPrimary, glass.accentSecondary)))
                                    .clickable(
                                        interactionSource = playInteraction,
                                        indication = androidx.compose.material3.ripple(),
                                        onClick = {
                                            if (glass.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            playbackController.togglePlayPause()
                                        }
                                    )
                                    .testTag("player_play_pause_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedContent(
                                    targetState = playbackState.isPlaying,
                                    transitionSpec = {
                                        if (glass.reduceMotion) {
                                            fadeIn(tween(80)) togetherWith fadeOut(tween(80))
                                        } else {
                                            (fadeIn(tween(150)) + scaleIn(initialScale = 0.8f)) togetherWith
                                                (fadeOut(tween(120)) + scaleOut(targetScale = 0.8f))
                                        }
                                    },
                                    label = "fullPlayPauseMorph"
                                ) { playing ->
                                    Icon(
                                        imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (playing) "Pausar" else "Reproduzir",
                                        tint = glass.onAccent,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }

                        if (isNeumorphic) {
                            SculptedCircleButton(
                                onClick = {
                                    if (glass.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    playbackController.skipToNext()
                                },
                                modifier = Modifier
                                    .size(52.dp)
                                    .minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = "Próxima faixa",
                                    tint = glass.textPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        } else {
                            val nextInteraction = remember { MutableInteractionSource() }
                            val nextScale = rememberLumaBounceScale(nextInteraction)
                            IconButton(
                                onClick = {
                                    if (glass.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    playbackController.skipToNext()
                                },
                                interactionSource = nextInteraction,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .graphicsLayer { scaleX = nextScale; scaleY = nextScale }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = "Próxima faixa",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { playbackController.cycleRepeatMode() },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            val icon = if (playbackState.repeatMode == RepeatModeState.ONE) {
                                Icons.Rounded.RepeatOne
                            } else Icons.Rounded.Repeat
                            val tint = if (playbackState.repeatMode != RepeatModeState.OFF) {
                                glass.accentPrimary
                            } else MaterialTheme.colorScheme.onSurfaceVariant
                            Icon(
                                imageVector = icon,
                                contentDescription = "Repetição: ${playbackState.repeatMode.name}",
                                tint = tint
                            )
                        }
                    }

                    // Secondary Action Pill Bar (Queue, Equalizer, Sleep Timer, Speed, Lyrics)
                    if (!settings.minimalistPlayerMode) {
                        Spacer(modifier = Modifier.height(14.dp))
                        LiquidGlassSurface(
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 580.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { activeSheet = PlayerSheet.QUEUE },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.QueueMusic,
                                        contentDescription = "Fila de reprodução",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        playbackController.initEqualizer()
                                        activeSheet = PlayerSheet.EQUALIZER
                                    },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        Icons.Rounded.Equalizer,
                                        contentDescription = "Equalizador",
                                        tint = if (playbackState.equalizer.enabled) glass.accentPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = { activeSheet = PlayerSheet.SLEEP_TIMER },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        Icons.Rounded.Bedtime,
                                        contentDescription = "Temporizador de sono",
                                        tint = if (playbackState.sleepTimerMode != SleepTimerMode.OFF) glass.accentPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = { activeSheet = PlayerSheet.SPEED },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        Icons.Rounded.Speed,
                                        contentDescription = "Velocidade e Tom (DJ)",
                                        tint = if (playbackState.playbackSpeed != 1.0f || abs(playbackState.playbackPitchSemitones) > 0.1f) {
                                            glass.accentPrimary
                                        } else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = { activeSheet = PlayerSheet.LYRICS },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        Icons.Rounded.Lyrics,
                                        contentDescription = "Letras",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dedicated Left Edge Volume Gesture Zone + Visual Edge Rail
        if (edgeZonesInteractive) {
            var leftActive by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(58.dp)
                    .fillMaxHeight(0.56f)
                    .pointerInput(track.id) {
                        var startVol = 0
                        var maxVol = 15
                        var lastStepVol = -1
                        var accDeltaY = 0f
                        detectVerticalDragGestures(
                            onDragStart = {
                                leftActive = true
                                activeEdgeGestureType = EdgeSwipeMode.VOLUME
                                maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 15
                                startVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                                lastStepVol = startVol
                                accDeltaY = 0f
                                edgeVolumePercent = ((startVol.toFloat() / maxVol.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
                                if (glass.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onVerticalDrag = { _, dragAmount ->
                                accDeltaY += dragAmount
                                val rangePx = size.height.toFloat().coerceAtLeast(180f)
                                val targetVol = computeEdgeSwipeTargetVolume(
                                    currentVolume = startVol,
                                    maxVolume = maxVol,
                                    accumulatedDeltaY = accDeltaY,
                                    gestureRangePx = rangePx
                                )
                                if (targetVol != lastStepVol) {
                                    lastStepVol = targetVol
                                    audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                    if (glass.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                edgeVolumePercent = ((targetVol.toFloat() / maxVol.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
                            },
                            onDragEnd = {
                                leftActive = false
                                activeEdgeGestureType = null
                            },
                            onDragCancel = {
                                leftActive = false
                                activeEdgeGestureType = null
                            }
                        )
                    }
                    .testTag("left_edge_volume_zone"),
                contentAlignment = Alignment.CenterStart
            ) {
                EdgeSwipeMeterRail(
                    isActive = leftActive,
                    isLeftEdge = true,
                    fraction = (edgeVolumePercent / 100f).coerceIn(0f, 1f),
                    primaryColor = glass.accentPrimary,
                    secondaryColor = glass.accentSecondary,
                    surfaceColor = glass.glassElevated,
                    borderColor = glass.glassBorder,
                    textColor = glass.textPrimary,
                    activeLabel = "$edgeVolumePercent%",
                    idleIcon = Icons.Rounded.VolumeUp,
                    activeIcon = if (edgeVolumePercent == 0) Icons.Rounded.VolumeMute else Icons.Rounded.VolumeUp,
                    contentDescription = strings.edgeGestureVolume,
                    railTag = "edge_volume_rail"
                )
            }

            // Dedicated Right Edge Track Seeking Gesture Zone + Visual Edge Rail
            var rightActive by remember { mutableStateOf(false) }
            val latestHapticsEnabled by rememberUpdatedState(glass.hapticsEnabled)
            val latestProgressFlow by rememberUpdatedState(progressFlow)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(58.dp)
                    .fillMaxHeight(0.56f)
                    .pointerInput(track.id) {
                        var accDeltaY = 0f
                        var startPosMs = 0L
                        var activeDurationMs = 0L
                        var latestSeekTargetMs = 0L
                        detectVerticalDragGestures(
                            onDragStart = {
                                rightActive = true
                                activeEdgeGestureType = EdgeSwipeMode.SEEK
                                accDeltaY = 0f
                                val live = latestProgressFlow?.value
                                    ?: playbackController.progressState.value.takeIf { it.positionMs > 0L || it.durationMs > 0L }
                                startPosMs = live?.positionMs ?: latestPlaybackState.positionMs
                                activeDurationMs = live?.durationMs?.takeIf { it > 0L } ?: latestPlaybackState.durationMs
                                latestSeekTargetMs = startPosMs
                                edgeSeekPreviewMs = startPosMs
                                edgeSeekDeltaMs = 0L
                                if (latestHapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onVerticalDrag = { _, dragAmount ->
                                accDeltaY += dragAmount
                                val rangePx = size.height.toFloat().coerceAtLeast(180f)
                                val targetMs = computeEdgeSwipeSeekTargetMs(
                                    startPositionMs = startPosMs,
                                    durationMs = activeDurationMs,
                                    accumulatedDeltaY = accDeltaY,
                                    gestureRangePx = rangePx
                                )
                                latestSeekTargetMs = targetMs
                                edgeSeekPreviewMs = targetMs
                                edgeSeekDeltaMs = targetMs - startPosMs
                            },
                            onDragEnd = {
                                if (abs(edgeSeekDeltaMs) >= 350L) {
                                    playbackController.seekTo(latestSeekTargetMs)
                                    if (latestHapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                rightActive = false
                                activeEdgeGestureType = null
                            },
                            onDragCancel = {
                                rightActive = false
                                activeEdgeGestureType = null
                            }
                        )
                    }
                    .testTag("right_edge_seek_zone"),
                contentAlignment = Alignment.CenterEnd
            ) {
                val activeDurMs = progressFlow?.value?.durationMs?.takeIf { it > 0L } ?: playbackState.durationMs
                val seekFrac = if (activeDurMs > 0L) {
                    (edgeSeekPreviewMs.toFloat() / activeDurMs.toFloat()).coerceIn(0f, 1f)
                } else 0f
                val isForward = edgeSeekDeltaMs >= 0L
                val sign = if (isForward) "+" else "-"
                EdgeSwipeMeterRail(
                    isActive = rightActive,
                    isLeftEdge = false,
                    fraction = seekFrac,
                    primaryColor = glass.accentPrimary,
                    secondaryColor = glass.accentSecondary,
                    surfaceColor = glass.glassElevated,
                    borderColor = glass.glassBorder,
                    textColor = glass.textPrimary,
                    activeLabel = "$sign${formatDuration(abs(edgeSeekDeltaMs))}",
                    idleIcon = Icons.Rounded.FastForward,
                    activeIcon = if (isForward) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                    contentDescription = strings.edgeGestureSeek,
                    railTag = "edge_seek_rail"
                )
            }
        }

        // Translucent Top Edge-Swipe Gesture HUD Pill (Left = Volume, Right = Seek)
        AnimatedVisibility(
            visible = activeEdgeGestureType != null,
            enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.90f),
            exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.92f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 66.dp)
        ) {
            val mode = activeEdgeGestureType
            LiquidGlassCapsule(
                elevated = true,
                modifier = Modifier
                    .widthIn(min = 180.dp, max = 260.dp)
                    .testTag("edge_gesture_hud")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (mode == EdgeSwipeMode.VOLUME) {
                        Icon(
                            imageVector = if (edgeVolumePercent == 0) Icons.Rounded.VolumeMute else Icons.Rounded.VolumeUp,
                            contentDescription = strings.edgeGestureVolume,
                            tint = glass.accentPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "${strings.edgeGestureVolume} $edgeVolumePercent%",
                                style = MaterialTheme.typography.labelLarge,
                                color = glass.textPrimary
                            )
                            LinearProgressIndicator(
                                progress = { edgeVolumePercent / 100f },
                                color = glass.accentPrimary,
                                trackColor = glass.textPrimary.copy(alpha = 0.14f),
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape)
                            )
                        }
                    } else if (mode == EdgeSwipeMode.SEEK) {
                        val isForward = edgeSeekDeltaMs >= 0L
                        val signPrefix = if (isForward) "+" else "-"
                        val activeDurMs = progressFlow?.value?.durationMs?.takeIf { it > 0L } ?: playbackState.durationMs
                        val seekFrac = if (activeDurMs > 0L) {
                            (edgeSeekPreviewMs.toFloat() / activeDurMs.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        Icon(
                            imageVector = if (isForward) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                            contentDescription = strings.edgeGestureSeek,
                            tint = glass.accentPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "$signPrefix${formatDuration(abs(edgeSeekDeltaMs))} • ${formatDuration(edgeSeekPreviewMs)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = glass.textPrimary
                            )
                            LinearProgressIndicator(
                                progress = { seekFrac },
                                color = glass.accentSecondary,
                                trackColor = glass.textPrimary.copy(alpha = 0.14f),
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape)
                            )
                        }
                    }
                }
            }
        }
        }
    }

    if (showSaveQueueDialog) {
        CreatePlaylistDialog(
            title = "Guardar fila como playlist",
            confirmLabel = "Guardar",
            onDismiss = { showSaveQueueDialog = false },
            onConfirm = { name ->
                showSaveQueueDialog = false
                onSaveQueueAsPlaylist(name)
            }
        )
    }

    if (showClearQueueConfirm) {
        ConfirmationDialog(
            title = "Limpar fila",
            message = "Pretende remover todas as faixas da fila de reprodução atual?",
            confirmLabel = "Limpar",
            onConfirm = {
                showClearQueueConfirm = false
                activeSheet = PlayerSheet.NONE
                playbackController.clearQueue()
            },
            onDismiss = { showClearQueueConfirm = false }
        )
    }

    if (activeSheet != PlayerSheet.NONE) {
        ModalBottomSheet(
            onDismissRequest = { activeSheet = PlayerSheet.NONE },
            containerColor = glass.surfaceElevated,
            contentColor = glass.textPrimary,
            scrimColor = glass.scrim
        ) {
            when (activeSheet) {
                PlayerSheet.QUEUE -> QueueSheetContent(
                    playbackState = playbackState,
                    queueRadioEnabled = queueRadioEnabled,
                    onToggleQueueRadio = onToggleQueueRadio,
                    onJumpTo = { playbackController.jumpToQueueIndex(it) },
                    onMoveUp = { idx -> playbackController.moveQueueItem(idx, idx - 1) },
                    onMoveDown = { idx -> playbackController.moveQueueItem(idx, idx + 1) },
                    onRemove = { playbackController.removeFromQueue(it) },
                    onSaveAsPlaylist = { showSaveQueueDialog = true },
                    onRequestClear = { showClearQueueConfirm = true }
                )

                PlayerSheet.EQUALIZER -> StudioEqualizerSheetContent(
                    playbackState = playbackState,
                    settings = settings,
                    customPresets = customAudioPresets,
                    onToggleEnabled = { playbackController.setEqualizerEnabled(it) },
                    onSelectSystemPreset = { playbackController.selectEqualizerPreset(it) },
                    onStudioBandChange = { studioIdx, level -> playbackController.setStudioBandLevel(studioIdx, level) },
                    onBassBoostChange = { playbackController.setBassBoostStrength(it) },
                    onVirtualizerChange = { playbackController.setVirtualizerStrength(it) },
                    onSelectReverb = { playbackController.setReverbPreset(it) },
                    onStereoBalanceChange = { playbackController.setStereoBalance(it) },
                    onToggleVolumeNormalization = { playbackController.setVolumeNormalizationEnabled(it) },
                    onApplyCustomPreset = { playbackController.applyCustomAudioPreset(it) },
                    onSaveCustomPreset = onSaveCustomAudioPreset,
                    onDeleteCustomPreset = onDeleteCustomAudioPreset,
                    onSelectVisualizerStyle = onSelectVisualizerStyle,
                    onSelectVisualizerPreset = onSelectVisualizerPreset,
                    onToggleReactiveLighting = onToggleReactiveLighting
                )

                PlayerSheet.SLEEP_TIMER -> SleepTimerSheetContent(
                    playbackState = playbackState,
                    progressFlow = progressFlow,
                    onSelectMinutes = { playbackController.setSleepTimerMinutes(it) },
                    onSelectEndOfTrack = { playbackController.setSleepTimerEndOfTrack() },
                    onCancel = { playbackController.cancelSleepTimer() }
                )

                PlayerSheet.SPEED -> SpeedAndPitchSheetContent(
                    currentSpeed = playbackState.playbackSpeed,
                    currentPitchSemitones = playbackState.playbackPitchSemitones,
                    onSelectSpeed = { playbackController.setPlaybackSpeed(it) },
                    onSelectPitchSemitones = { playbackController.setPlaybackPitchSemitones(it) }
                )

                PlayerSheet.LYRICS -> {
                    val livePos = progressFlow?.collectAsStateWithLifecycle()?.value?.positionMs ?: playbackState.positionMs
                    LyricsStudioSheetContent(
                        track = track,
                        positionMs = livePos,
                        settings = settings,
                        onSeekTo = { playbackController.seekTo(it) },
                        onImportLyrics = { onImportLyrics(track.id) },
                        onClearLyrics = { onClearLyrics(track.id) },
                        onSaveLyricsAndOffset = onSaveLyricsAndOffset,
                        onUpdateFontScale = onUpdateLyricsFontScale,
                        onToggleCentered = onToggleLyricsCentered,
                        onOpenFullScreen = {
                            activeSheet = PlayerSheet.NONE
                            showFullScreenLyrics = true
                        },
                        lyricsSearchState = lyricsSearchState,
                        onSearchOnlineLyrics = { artist, title -> onSearchOnlineLyrics(track, artist, title) },
                        onApplyOnlineLyrics = { candidate -> onApplyOnlineLyrics(track.id, candidate) },
                        onClearLyricsSearchResults = onClearLyricsSearchResults
                    )
                }

                PlayerSheet.STYLES -> PlayerStylePickerSheetContent(
                    selectedStyle = settings.playerScreenStyle,
                    onSelectStyle = { style ->
                        onSelectPlayerStyle(style)
                        activeSheet = PlayerSheet.NONE
                    }
                )

                PlayerSheet.AB_LOOP -> AbLoopSheetContent(
                    playbackState = playbackState,
                    onSetPointA = { playbackController.setAbLoopPointA() },
                    onSetPointB = { playbackController.setAbLoopPointB() },
                    onNudgePoint = { isA, delta -> playbackController.nudgeAbLoop(isA, delta) },
                    onToggleActive = { playbackController.toggleAbLoopActive() },
                    onSeekToA = { playbackState.loopStartMs?.let { playbackController.seekTo(it) } },
                    onClear = { playbackController.clearAbLoop() }
                )

                PlayerSheet.BOOKMARKS -> {
                    val livePos = progressFlow?.collectAsStateWithLifecycle()?.value?.positionMs ?: playbackState.positionMs
                    BookmarksSheetContent(
                        track = track,
                        positionMs = livePos,
                        bookmarks = bookmarks,
                        onAddBookmark = { label -> onAddBookmark(livePos, label) },
                        onJumpToBookmark = { pos ->
                            playbackController.seekTo(pos)
                            activeSheet = PlayerSheet.NONE
                        },
                        onDeleteBookmark = onDeleteBookmark
                    )
                }

                PlayerSheet.AMBIENT -> AmbientMixerSheetContent(
                    ambientState = ambientState,
                    onSelectSound = onSelectAmbientSound,
                    onVolumeChange = onSetAmbientVolume
                )

                PlayerSheet.NONE -> {}
            }
        }
    }

    if (showFullScreenLyrics) {
        val livePos = progressFlow?.collectAsStateWithLifecycle()?.value?.positionMs ?: playbackState.positionMs
        FullScreenLyricsDialog(
            track = track,
            positionMs = livePos,
            settings = settings,
            onSeekTo = { playbackController.seekTo(it) },
            onDismiss = { showFullScreenLyrics = false }
        )
    }

    if (showStoryCardDialog) {
        StoryCardPreviewDialog(
            track = track,
            onDismiss = { showStoryCardDialog = false }
        )
    }
    }
    }
}

@Composable
private fun PlayerProgressSliderAndWaveform(
    progressFlow: StateFlow<PlaybackProgressUiState>?,
    fallbackPositionMs: Long,
    fallbackDurationMs: Long,
    trackId: Long,
    showWaveform: Boolean,
    isSeeking: Boolean,
    seekFraction: Float,
    isEdgeSeeking: Boolean,
    edgeSeekPreviewMs: Long,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: (Long) -> Unit,
    activeColor: Color,
    inactiveColor: Color
) {
    val liveProgress = progressFlow?.collectAsStateWithLifecycle()?.value
    val positionMs = liveProgress?.positionMs ?: fallbackPositionMs
    val durationMs = liveProgress?.durationMs?.takeIf { it > 0L } ?: fallbackDurationMs
    val progressFraction = when {
        isSeeking -> seekFraction
        isEdgeSeeking && durationMs > 0L -> (edgeSeekPreviewMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        durationMs > 0L -> (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        else -> 0f
    }

    if (showWaveform) {
        WaveformProgressPreview(
            progress = progressFraction,
            trackId = trackId,
            activeColor = activeColor,
            inactiveColor = inactiveColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
        )
    }

    Slider(
        value = progressFraction,
        onValueChange = onSeekChange,
        onValueChangeFinished = {
            val targetMs = (seekFraction * durationMs).toLong()
            onSeekFinished(targetMs)
        },
        colors = SliderDefaults.colors(
            thumbColor = activeColor,
            activeTrackColor = activeColor,
            inactiveTrackColor = inactiveColor
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PlayerElapsedTimeText(
    progressFlow: StateFlow<PlaybackProgressUiState>?,
    fallbackPositionMs: Long,
    fallbackDurationMs: Long,
    isSeeking: Boolean,
    seekFraction: Float,
    isEdgeSeeking: Boolean,
    edgeSeekPreviewMs: Long
) {
    val liveProgress = progressFlow?.collectAsStateWithLifecycle()?.value
    val positionMs = liveProgress?.positionMs ?: fallbackPositionMs
    val durationMs = liveProgress?.durationMs?.takeIf { it > 0L } ?: fallbackDurationMs
    val shownPosition = when {
        isSeeking -> (seekFraction * durationMs).toLong()
        isEdgeSeeking -> edgeSeekPreviewMs
        else -> positionMs
    }
    Text(
        text = formatDuration(shownPosition),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun PlayerRemainingTimeText(
    progressFlow: StateFlow<PlaybackProgressUiState>?,
    fallbackPositionMs: Long,
    fallbackDurationMs: Long,
    isSeeking: Boolean,
    seekFraction: Float,
    isEdgeSeeking: Boolean,
    edgeSeekPreviewMs: Long,
    includeTotalDuration: Boolean
) {
    val liveProgress = progressFlow?.collectAsStateWithLifecycle()?.value
    val positionMs = liveProgress?.positionMs ?: fallbackPositionMs
    val durationMs = liveProgress?.durationMs?.takeIf { it > 0L } ?: fallbackDurationMs
    val shownPosition = when {
        isSeeking -> (seekFraction * durationMs).toLong()
        isEdgeSeeking -> edgeSeekPreviewMs
        else -> positionMs
    }
    val remainingMs = (durationMs - shownPosition).coerceAtLeast(0L)
    Text(
        text = if (includeTotalDuration) {
            "-${formatDuration(remainingMs)} • ${formatDuration(durationMs)}"
        } else {
            "-${formatDuration(remainingMs)}"
        },
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun CoverThemedAtmosphericBackdrop(
    artworkPath: String?,
    palette: ExtractedArtworkColors,
    enabled: Boolean,
    isPlaying: Boolean,
    reduceMotion: Boolean,
    isDark: Boolean,
    baseBackground: Color,
    glassQuality: GlassQuality = GlassQuality.BALANCED
) {
    if (!enabled) return
    val context = LocalContext.current
    val animateDrift = isPlaying && !reduceMotion && glassQuality != GlassQuality.MINIMAL
    val drift = if (animateDrift) {
        val infinite = rememberInfiniteTransition(label = "npdCoverAurora")
        infinite.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * kotlin.math.PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(11_000, easing = LinearEasing), RepeatMode.Restart),
            label = "auroraPhase"
        ).value
    } else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("npd_cover_atmosphere")
    ) {
        val decodeSize = if (glassQuality == GlassQuality.ULTRA) 144 else 96
        Crossfade(
            targetState = artworkPath,
            animationSpec = tween(if (reduceMotion) 0 else 450),
            label = "npdCoverCrossfade"
        ) { path ->
            if (!path.isNullOrBlank()) {
                val model = remember(path) {
                    if (path.startsWith("content://") || path.startsWith("file://")) path else File(path)
                }
                val request = remember(context, model, decodeSize, reduceMotion) {
                    ImageRequest.Builder(context)
                        .data(model)
                        .crossfade(if (reduceMotion) 0 else 220)
                        .size(decodeSize)
                        .precision(coil.size.Precision.INEXACT)
                        .build()
                }
                val blurMod = if (glassQuality == GlassQuality.ULTRA && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.blur(22.dp)
                } else Modifier
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.18f
                            scaleY = 1.18f
                            alpha = if (isDark) 0.34f else 0.20f
                        }
                        .then(blurMod)
                )
            }
        }

        // GPU-cached 3-tone cover aurora orbs: shaders created once per size/palette, animated via GPU translation
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (animateDrift && size.minDimension > 1f) {
                        translationX = (kotlin.math.cos(drift.toDouble()) * size.width * 0.045f).toFloat()
                        translationY = (sin(drift.toDouble()) * size.height * 0.035f).toFloat()
                    }
                    scaleX = 1.12f
                    scaleY = 1.12f
                }
                .drawWithCache {
                    if (size.minDimension <= 1f) {
                        onDrawBehind {}
                    } else {
                        val center1 = Offset(size.width * 0.25f, size.height * 0.22f)
                        val r1 = (size.maxDimension * 0.62f).coerceAtLeast(1f)
                        val brush1 = Brush.radialGradient(
                            colors = listOf(palette.primary.copy(alpha = if (isDark) 0.38f else 0.26f), Color.Transparent),
                            center = center1,
                            radius = r1
                        )

                        val center2 = Offset(size.width * 0.78f, size.height * 0.56f)
                        val r2 = (size.maxDimension * 0.56f).coerceAtLeast(1f)
                        val brush2 = Brush.radialGradient(
                            colors = listOf(palette.secondary.copy(alpha = if (isDark) 0.32f else 0.22f), Color.Transparent),
                            center = center2,
                            radius = r2
                        )

                        val center3 = Offset(size.width * 0.45f, size.height * 0.85f)
                        val r3 = (size.maxDimension * 0.50f).coerceAtLeast(1f)
                        val brush3 = Brush.radialGradient(
                            colors = listOf(palette.tertiary.copy(alpha = if (isDark) 0.26f else 0.18f), Color.Transparent),
                            center = center3,
                            radius = r3
                        )
                        onDrawBehind {
                            drawCircle(brush = brush1, center = center1, radius = r1)
                            drawCircle(brush = brush2, center = center2, radius = r2)
                            drawCircle(brush = brush3, center = center3, radius = r3)
                        }
                    }
                }
        )

        // Cached frosted legibility gradient ensuring razor-sharp UI contrast in Light & Dark modes
        val legibilityBrush = remember(baseBackground, isDark) {
            Brush.verticalGradient(
                0.0f to baseBackground.copy(alpha = if (isDark) 0.42f else 0.52f),
                0.45f to baseBackground.copy(alpha = if (isDark) 0.26f else 0.38f),
                1.0f to baseBackground.copy(alpha = if (isDark) 0.72f else 0.66f)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(legibilityBrush)
        )
    }
}

@Composable
private fun EdgeSwipeMeterRail(
    isActive: Boolean,
    isLeftEdge: Boolean,
    fraction: Float,
    primaryColor: Color,
    secondaryColor: Color,
    surfaceColor: Color,
    borderColor: Color,
    textColor: Color,
    activeLabel: String,
    idleIcon: androidx.compose.ui.graphics.vector.ImageVector,
    activeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    railTag: String
) {
    val barFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(70),
        label = "edgeRailFraction"
    )
    if (isActive) {
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(surfaceColor.copy(alpha = 0.92f))
                .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .testTag(railTag),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = activeIcon,
                contentDescription = contentDescription,
                tint = primaryColor,
                modifier = Modifier.size(18.dp)
            )
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(112.dp)
                    .clip(CircleShape)
                    .background(textColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(barFraction.coerceAtLeast(0.06f))
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(secondaryColor, primaryColor)))
                )
            }
            Text(
                text = activeLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    } else {
        Column(
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .graphicsLayer { alpha = 0.34f },
            horizontalAlignment = if (isLeftEdge) Alignment.Start else Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = idleIcon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(textColor.copy(alpha = 0.45f))
            )
        }
    }
}

private enum class EdgeSwipeMode { VOLUME, SEEK }

internal fun computeEdgeSwipeTargetVolume(
    currentVolume: Int,
    maxVolume: Int,
    accumulatedDeltaY: Float,
    gestureRangePx: Float
): Int {
    if (maxVolume <= 0 || gestureRangePx <= 0f) return currentVolume.coerceAtLeast(0)
    // Negative dragY (swipe up) increases volume
    val deltaSteps = ((-accumulatedDeltaY / gestureRangePx) * maxVolume).roundToInt()
    return (currentVolume + deltaSteps).coerceIn(0, maxVolume)
}

internal fun computeEdgeSwipeSeekTargetMs(
    startPositionMs: Long,
    durationMs: Long,
    accumulatedDeltaY: Float,
    gestureRangePx: Float
): Long {
    if (durationMs <= 0L || gestureRangePx <= 0f) return 0L
    // Negative dragY (swipe up) seeks forward by up to +/- 90 seconds per full vertical sweep
    val sweepSpanMs = minOf(90_000L, durationMs)
    val deltaMs = ((-accumulatedDeltaY / gestureRangePx) * sweepSpanMs).toLong()
    return (startPositionMs + deltaMs).coerceIn(0L, durationMs)
}

@Composable
private fun QueueSheetContent(
    playbackState: PlaybackUiState,
    queueRadioEnabled: Boolean,
    onToggleQueueRadio: (Boolean) -> Unit,
    onJumpTo: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onSaveAsPlaylist: () -> Unit,
    onRequestClear: () -> Unit
) {
    val glass = LocalLumaGlass.current
    val strings = LocalLumaStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Fila (${playbackState.queue.size})",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onSaveAsPlaylist, enabled = playbackState.queue.isNotEmpty()) {
                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Guardar")
            }
            TextButton(onClick = onRequestClear, enabled = playbackState.queue.isNotEmpty()) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Limpar", color = MaterialTheme.colorScheme.error)
            }
        }
        FilterChip(
            selected = queueRadioEnabled,
            onClick = { onToggleQueueRadio(!queueRadioEnabled) },
            leadingIcon = { Icon(Icons.Rounded.Radio, contentDescription = null, modifier = Modifier.size(16.dp)) },
            label = { Text("${strings.queueRadioLabel}: ${if (queueRadioEnabled) "Ativo" else "Desativado"}") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = glass.pillIndicatorBg,
                selectedLabelColor = glass.accentPrimary,
                containerColor = glass.surface,
                labelColor = glass.textSecondary
            ),
            modifier = Modifier.testTag("queue_radio_chip")
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(playbackState.queue, key = { idx, item -> "${idx}_${item.id}" }) { index, item ->
                val isCurrent = index == playbackState.currentIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isCurrent) glass.pillIndicatorBg else Color.Transparent)
                        .clickable { onJumpTo(index) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MediaArtwork(
                        artworkPath = item.artworkPath,
                        contentDescription = item.title,
                        cornerRadius = 10.dp,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isCurrent) glass.accentPrimary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (index > 0) {
                        IconButton(onClick = { onMoveUp(index) }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = "Subir", modifier = Modifier.size(18.dp))
                        }
                    }
                    if (index < playbackState.queue.lastIndex) {
                        IconButton(onClick = { onMoveDown(index) }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = "Descer", modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(onClick = { onRemove(index) }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Remover", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}



@Composable
private fun SleepTimerSheetContent(
    playbackState: PlaybackUiState,
    progressFlow: StateFlow<PlaybackProgressUiState>? = null,
    onSelectMinutes: (Int) -> Unit,
    onSelectEndOfTrack: () -> Unit,
    onCancel: () -> Unit
) {
    val glass = LocalLumaGlass.current
    var customMinutesText by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Temporizador de sono", style = MaterialTheme.typography.titleLarge)
        if (playbackState.sleepTimerMode == SleepTimerMode.TIMED) {
            val liveSleepRemaining = progressFlow?.collectAsStateWithLifecycle()?.value?.sleepRemainingMs
                ?: playbackState.sleepRemainingMs
            Text(
                text = "Pausa automática em ${formatDuration(liveSleepRemaining)}",
                style = MaterialTheme.typography.titleSmall,
                color = glass.accentPrimary
            )
        } else if (playbackState.sleepTimerMode == SleepTimerMode.END_OF_TRACK) {
            Text(
                text = "Irá pausar no fim da faixa atual",
                style = MaterialTheme.typography.titleSmall,
                color = glass.accentPrimary
            )
        }

        val sleepChipColors = FilterChipDefaults.filterChipColors(
            containerColor = glass.surface,
            labelColor = glass.textPrimary
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(10, 15, 30, 45, 60).forEach { mins ->
                FilterChip(
                    selected = false,
                    onClick = { onSelectMinutes(mins) },
                    label = { Text("${mins}m") },
                    colors = sleepChipColors
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = customMinutesText,
                onValueChange = { text -> customMinutesText = text.filter { it.isDigit() }.take(3) },
                singleLine = true,
                label = { Text("Minutos personalizados") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            val parsedMins = customMinutesText.toIntOrNull() ?: 0
            OutlinedButton(
                onClick = {
                    if (parsedMins > 0) {
                        onSelectMinutes(parsedMins)
                        customMinutesText = ""
                    }
                },
                enabled = parsedMins > 0
            ) {
                Text("Definir")
            }
        }

        Button(
            onClick = onSelectEndOfTrack,
            colors = ButtonDefaults.buttonColors(
                containerColor = glass.accentPrimary,
                contentColor = glass.onAccent
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Parar no fim desta música")
        }

        if (playbackState.sleepTimerMode != SleepTimerMode.OFF) {
            TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.End)) {
                Text("Desativar temporizador")
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}



@Composable
private fun PlayerActiveFeatureBadges(
    playbackState: PlaybackUiState,
    bookmarks: List<TrackBookmark>,
    ambientState: AmbientMixerState,
    onOpenAbLoop: () -> Unit,
    onOpenSpeedPitch: () -> Unit,
    onOpenAmbient: () -> Unit,
    onSeekToBookmark: (Long) -> Unit
) {
    val glass = LocalLumaGlass.current
    val hasPitchOrSpeed = playbackState.playbackSpeed != 1.0f || abs(playbackState.playbackPitchSemitones) > 0.1f
    val hasLoop = playbackState.loopStartMs != null
    if (!hasLoop && bookmarks.isEmpty() && !ambientState.isPlaying && !hasPitchOrSpeed) return

    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = glass.pillIndicatorBg,
        selectedLabelColor = glass.accentPrimary,
        containerColor = glass.surface,
        labelColor = glass.textPrimary
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 580.dp)
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasLoop) {
            val aText = formatDuration(playbackState.loopStartMs ?: 0L)
            val bText = playbackState.loopEndMs?.let { formatDuration(it) } ?: "..."
            FilterChip(
                selected = playbackState.isAbLoopActive,
                onClick = onOpenAbLoop,
                leadingIcon = { Icon(Icons.Rounded.Loop, contentDescription = null, modifier = Modifier.size(14.dp)) },
                label = { Text("A-B $aText→$bText") },
                colors = chipColors
            )
        }
        if (ambientState.isPlaying) {
            FilterChip(
                selected = true,
                onClick = onOpenAmbient,
                leadingIcon = { Icon(Icons.Rounded.Spa, contentDescription = null, modifier = Modifier.size(14.dp)) },
                label = { Text(ambientState.soundType.label) },
                colors = chipColors
            )
        }
        if (hasPitchOrSpeed) {
            val pitchText = if (abs(playbackState.playbackPitchSemitones) > 0.1f) {
                " • ${if (playbackState.playbackPitchSemitones > 0f) "+" else ""}${playbackState.playbackPitchSemitones}st"
            } else ""
            FilterChip(
                selected = true,
                onClick = onOpenSpeedPitch,
                leadingIcon = { Icon(Icons.Rounded.Speed, contentDescription = null, modifier = Modifier.size(14.dp)) },
                label = { Text("${playbackState.playbackSpeed}x$pitchText") },
                colors = chipColors
            )
        }
        bookmarks.forEach { bm ->
            FilterChip(
                selected = false,
                onClick = { onSeekToBookmark(bm.positionMs) },
                leadingIcon = { Icon(Icons.Rounded.Bookmark, contentDescription = null, modifier = Modifier.size(14.dp)) },
                label = { Text("${bm.label} (${formatDuration(bm.positionMs)})") },
                colors = chipColors
            )
        }
    }
}



@Composable
private fun WaveformProgressPreview(
    progress: Float,
    trackId: Long,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val bars = 44
        val step = size.width / bars
        val barW = step * 0.54f
        for (i in 0 until bars) {
            val seed = abs(sin((trackId * 13 + i * 7).toDouble())).toFloat()
            val h = size.height * (0.28f + 0.72f * seed)
            val x = i * step + (step - barW) / 2f
            val y = (size.height - h) / 2f
            val fraction = i.toFloat() / bars.toFloat()
            drawRoundRect(
                color = if (fraction <= progress) activeColor else inactiveColor,
                topLeft = Offset(x, y),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f)
            )
        }
    }
}
