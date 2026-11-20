package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.ViewCarousel
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppSettings
import com.example.data.model.CoverDoubleTapAction
import com.example.data.model.CustomAudioPreset
import com.example.data.model.MediaItemEntity
import com.example.data.model.PlaybackAnimationStyle
import com.example.data.model.PlayerScreenStyle
import com.example.data.model.VisualizerPreset
import com.example.data.model.VisualizerStyle
import com.example.player.PlaybackController
import com.example.player.PlaybackUiState
import com.example.player.RepeatModeState
import com.example.player.SleepTimerMode
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
import com.example.ui.components.StudioEqualizerSheetContent
import com.example.ui.theme.LiquidGlassBackground
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LocalLumaGlass
import com.example.ui.theme.LumaDropdownMenu
import com.example.ui.theme.MediaArtwork
import com.example.ui.theme.SculptedCircleButton
import com.example.ui.theme.formatDuration
import com.example.ui.theme.lumaMenuItemColors
import com.example.ui.theme.rememberDynamicArtworkColors
import com.example.ui.theme.rememberLumaBounceScale
import com.example.ui.theme.resolveArtworkShape
import kotlin.math.abs
import kotlin.math.sin

private enum class PlayerSheet {
    NONE,
    QUEUE,
    EQUALIZER,
    SLEEP_TIMER,
    SPEED,
    LYRICS,
    STYLES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(
    playbackState: PlaybackUiState,
    settings: AppSettings,
    playbackController: PlaybackController,
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
    onToggleLyricsCentered: (Boolean) -> Unit = {}
) {
    BackHandler { onClose() }

    val track = playbackState.currentTrack
    if (track == null) {
        onClose()
        return
    }

    val glass = LocalLumaGlass.current
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

    var isSeeking by remember { mutableStateOf(false) }
    var seekFraction by remember { mutableFloatStateOf(0f) }

    val progressFraction = if (isSeeking) {
        seekFraction
    } else if (playbackState.durationMs > 0L) {
        (playbackState.positionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

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

    val doubleTapActionCallback: () -> Unit = {
        when (settings.coverDoubleTapAction) {
            CoverDoubleTapAction.PLAY_PAUSE -> playbackController.togglePlayPause()
            CoverDoubleTapAction.FAVORITE -> onToggleFavorite(track)
            CoverDoubleTapAction.SEEK_FORWARD -> playbackController.seekTo(playbackState.positionMs + 10_000L)
        }
    }

    val isNeumorphic = glass.isNeumorphic

    LiquidGlassBackground(dynamicAccentTint = dynamicHueTint) {
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

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "A REPRODUZIR",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isNeumorphic) glass.textSecondary else glass.accentPrimary
                    )
                    Text(
                        text = track.album,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isNeumorphic) glass.textPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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

                    // Interactive Scrubber + Shuffle/Repeat quick toggles
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 580.dp)
                    ) {
                        Slider(
                            value = progressFraction,
                            onValueChange = { value ->
                                isSeeking = true
                                seekFraction = value
                            },
                            onValueChangeFinished = {
                                val targetMs = (seekFraction * playbackState.durationMs).toLong()
                                playbackController.seekTo(targetMs)
                                isSeeking = false
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = glass.accentPrimary,
                                activeTrackColor = glass.accentPrimary,
                                inactiveTrackColor = glass.textPrimary.copy(alpha = if (glass.isDark) 0.20f else 0.15f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val shownPosition = if (isSeeking) {
                                (seekFraction * playbackState.durationMs).toLong()
                            } else playbackState.positionMs
                            val remainingMs = (playbackState.durationMs - shownPosition).coerceAtLeast(0L)
                            Text(
                                text = formatDuration(shownPosition),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                            contentDescription = "Velocidade de reprodução",
                                            tint = if (playbackState.playbackSpeed != 1.0f) glass.accentPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "-${formatDuration(remainingMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                modifier = Modifier.weight(3.2f)
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

                    // Waveform & Draggable Progress Bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 580.dp)
                    ) {
                        if (settings.playerScreenStyle == PlayerScreenStyle.CAROUSEL_VINYL) {
                            WaveformProgressPreview(
                                progress = progressFraction,
                                trackId = track.id,
                                activeColor = glass.accentPrimary,
                                inactiveColor = glass.textPrimary.copy(alpha = if (glass.isDark) 0.22f else 0.16f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(26.dp)
                            )
                        }

                        Slider(
                            value = progressFraction,
                            onValueChange = { value ->
                                isSeeking = true
                                seekFraction = value
                            },
                            onValueChangeFinished = {
                                val targetMs = (seekFraction * playbackState.durationMs).toLong()
                                playbackController.seekTo(targetMs)
                                isSeeking = false
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = glass.accentPrimary,
                                activeTrackColor = glass.accentPrimary,
                                inactiveTrackColor = glass.textPrimary.copy(alpha = if (glass.isDark) 0.20f else 0.15f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val shownPosition = if (isSeeking) {
                                (seekFraction * playbackState.durationMs).toLong()
                            } else playbackState.positionMs
                            val remainingMs = (playbackState.durationMs - shownPosition).coerceAtLeast(0L)
                            Text(
                                text = formatDuration(shownPosition),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "-${formatDuration(remainingMs)} • ${formatDuration(playbackState.durationMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                        contentDescription = "Velocidade de reprodução",
                                        tint = if (playbackState.playbackSpeed != 1.0f) glass.accentPrimary else MaterialTheme.colorScheme.onSurface
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
                    onSelectMinutes = { playbackController.setSleepTimerMinutes(it) },
                    onSelectEndOfTrack = { playbackController.setSleepTimerEndOfTrack() },
                    onCancel = { playbackController.cancelSleepTimer() }
                )

                PlayerSheet.SPEED -> SpeedSheetContent(
                    currentSpeed = playbackState.playbackSpeed,
                    onSelectSpeed = { playbackController.setPlaybackSpeed(it) }
                )

                PlayerSheet.LYRICS -> LyricsStudioSheetContent(
                    track = track,
                    positionMs = playbackState.positionMs,
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
                    }
                )

                PlayerSheet.STYLES -> PlayerStylePickerSheetContent(
                    selectedStyle = settings.playerScreenStyle,
                    onSelectStyle = { style ->
                        onSelectPlayerStyle(style)
                        activeSheet = PlayerSheet.NONE
                    }
                )

                PlayerSheet.NONE -> {}
            }
        }
    }

    if (showFullScreenLyrics) {
        FullScreenLyricsDialog(
            track = track,
            positionMs = playbackState.positionMs,
            settings = settings,
            onSeekTo = { playbackController.seekTo(it) },
            onDismiss = { showFullScreenLyrics = false }
        )
    }
}

@Composable
private fun QueueSheetContent(
    playbackState: PlaybackUiState,
    onJumpTo: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onSaveAsPlaylist: () -> Unit,
    onRequestClear: () -> Unit
) {
    val glass = LocalLumaGlass.current
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
            Text(
                text = "Pausa automática em ${formatDuration(playbackState.sleepRemainingMs)}",
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
private fun SpeedSheetContent(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit
) {
    val glass = LocalLumaGlass.current
    val speedChipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = glass.pillIndicatorBg,
        selectedLabelColor = glass.accentPrimary,
        containerColor = glass.surface,
        labelColor = glass.textPrimary
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Velocidade de reprodução", style = MaterialTheme.typography.titleLarge)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                FilterChip(
                    selected = abs(currentSpeed - speed) < 0.05f,
                    onClick = { onSelectSpeed(speed) },
                    label = { Text("${speed}x") },
                    colors = speedChipColors
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
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
