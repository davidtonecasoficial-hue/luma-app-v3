package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.LocalPipController
import com.example.config.AppConfig
import com.example.data.model.MediaItemEntity
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LocalLumaGlass
import com.example.ui.theme.formatDuration
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    video: MediaItemEntity,
    onClose: (lastPositionMs: Long) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val glass = LocalLumaGlass.current
    val pipState = LocalPipController.current

    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }
    val maxVolume = remember {
        audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 15
    }

    var isPlaying by remember { mutableStateOf(true) }
    var positionMs by remember { mutableLongStateOf(video.lastPositionMs) }
    var durationMs by remember { mutableLongStateOf(video.durationMs) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var isLandscape by remember { mutableStateOf(false) }
    var resizeModeIndex by remember { mutableIntStateOf(0) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var speedMenuOpen by remember { mutableStateOf(false) }
    var trackMenuOpen by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var externalSubtitleLabel by remember { mutableStateOf<String?>(null) }
    var gestureHudLabel by remember { mutableStateOf<String?>(null) }
    var gestureHudIsVolume by remember { mutableStateOf(false) }

    var brightness by remember {
        mutableFloatStateOf(activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.6f)
    }
    var volumeFraction by remember {
        mutableFloatStateOf(
            (audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC)?.toFloat() ?: 7f) / maxVolume
        )
    }

    var currentTracks by remember { mutableStateOf(Tracks.EMPTY) }

    val exoPlayer = remember(video.id) {
        ExoPlayer.Builder(context).build().apply {
            runCatching {
                setMediaItem(MediaItem.fromUri(Uri.parse(video.uri)))
                val resumePos = if (video.lastPositionMs > 2000L &&
                    (video.durationMs <= 0L || video.lastPositionMs < video.durationMs - 3000L)
                ) video.lastPositionMs else 0L
                if (resumePos > 0L) seekTo(resumePos)
                prepare()
                playWhenReady = true
            }.onFailure {
                errorMessage = "Não foi possível abrir este vídeo no dispositivo."
            }
        }
    }

    val subtitleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { subUri ->
        if (subUri != null) {
            runCatching {
                val fileName = queryDisplayName(context, subUri)
                val mime = if (fileName.endsWith(".vtt", ignoreCase = true)) {
                    MimeTypes.TEXT_VTT
                } else {
                    MimeTypes.APPLICATION_SUBRIP
                }
                val subConfig = MediaItem.SubtitleConfiguration.Builder(subUri)
                    .setMimeType(mime)
                    .setLanguage("pt")
                    .setLabel(fileName)
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
                val resumeAt = exoPlayer.currentPosition.coerceAtLeast(0L)
                val keepPlaying = exoPlayer.playWhenReady
                val updatedItem = MediaItem.Builder()
                    .setUri(Uri.parse(video.uri))
                    .setSubtitleConfigurations(listOf(subConfig))
                    .build()
                exoPlayer.setMediaItem(updatedItem, resumeAt)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = keepPlaying
                externalSubtitleLabel = fileName
            }.onFailure {
                errorMessage = "Não foi possível carregar este ficheiro de legendas."
            }
        }
    }

    val exitAction = {
        runCatching {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        val finalPos = runCatching { exoPlayer.currentPosition.coerceAtLeast(0L) }.getOrDefault(positionMs)
        onClose(finalPos)
    }

    BackHandler { exitAction() }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onTracksChanged(tracks: Tracks) {
                currentTracks = tracks
            }

            override fun onPlayerError(error: PlaybackException) {
                errorMessage = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
                    PlaybackException.ERROR_CODE_IO_NO_PERMISSION ->
                        "Ficheiro de vídeo indisponível ou movido."
                    PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
                        "Codec de vídeo não suportado neste dispositivo."
                    else -> "Não foi possível reproduzir este vídeo."
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            runCatching { exoPlayer.removeListener(listener) }
            runCatching { exoPlayer.release() }
            runCatching { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            runCatching {
                positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                val d = exoPlayer.duration
                if (d > 0L) durationMs = d
            }
            delay(350L)
        }
    }

    LaunchedEffect(controlsVisible, isPlaying, isLocked) {
        if (controlsVisible && isPlaying && !isLocked && !speedMenuOpen && !trackMenuOpen) {
            delay(3500L)
            controlsVisible = false
        }
    }

    val resizeMode = when (resizeModeIndex) {
        1 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        2 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    }

    val applyBrightness: (Float) -> Unit = { newVal ->
        val clamped = newVal.coerceIn(0.05f, 1f)
        brightness = clamped
        runCatching {
            activity?.window?.let { win ->
                val lp = win.attributes
                lp.screenBrightness = clamped
                win.attributes = lp
            }
        }
    }

    val applyVolume: (Float) -> Unit = { newVal ->
        val clamped = newVal.coerceIn(0f, 1f)
        volumeFraction = clamped
        runCatching {
            audioManager?.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                (clamped * maxVolume).toInt().coerceIn(0, maxVolume),
                0
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { controlsVisible = !controlsVisible }
            .pointerInput(isLocked, pipState.isInPipMode) {
                if (isLocked || pipState.isInPipMode) return@pointerInput
                detectVerticalDragGestures(
                    onDragEnd = { gestureHudLabel = null },
                    onDragCancel = { gestureHudLabel = null },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val isRightHalf = change.position.x > size.width / 2f
                        val delta = -dragAmount / (size.height.coerceAtLeast(400).toFloat() * 0.65f)
                        if (isRightHalf) {
                            val updated = (volumeFraction + delta).coerceIn(0f, 1f)
                            applyVolume(updated)
                            gestureHudIsVolume = true
                            gestureHudLabel = "Volume ${(updated * 100).toInt()}%"
                        } else {
                            val updated = (brightness + delta).coerceIn(0.05f, 1f)
                            applyBrightness(updated)
                            gestureHudIsVolume = false
                            gestureHudLabel = "Brilho ${(updated * 100).toInt()}%"
                        }
                    }
                )
            }
            .testTag("video_player_screen")
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    player = exoPlayer
                    this.resizeMode = resizeMode
                }
            },
            update = { view ->
                view.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        errorMessage?.let { activeErrMsg ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.78f)),
                contentAlignment = Alignment.Center
            ) {
                LiquidGlassSurface(shape = RoundedCornerShape(24.dp), elevated = true) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = activeErrMsg,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(onClick = exitAction) { Text("Voltar") }
                    }
                }
            }
            return@Box
        }

        // Brightness / Volume Swipe Gesture HUD
        val hudText = gestureHudLabel
        if (hudText != null && !pipState.isInPipMode) {
            LiquidGlassSurface(
                shape = RoundedCornerShape(20.dp),
                elevated = true,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 140.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (gestureHudIsVolume) Icons.AutoMirrored.Rounded.VolumeUp else Icons.Rounded.Brightness6,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = hudText,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = controlsVisible && !pipState.isInPipMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f))
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(16.dp)
            ) {
                if (isLocked) {
                    LiquidGlassSurface(
                        shape = CircleShape,
                        onClick = { isLocked = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Rounded.Lock, contentDescription = "Desbloquear controlos", tint = Color.White)
                        }
                    }
                } else {
                    // Top Bar
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = exitAction, modifier = Modifier.minimumInteractiveComponentSize()) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Fechar vídeo", tint = Color.White)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = video.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val subtitleText = buildString {
                                    if (video.videoWidth > 0) append("${video.videoWidth}x${video.videoHeight}")
                                    if (externalSubtitleLabel != null) {
                                        if (isNotEmpty()) append(" • ")
                                        append(externalSubtitleLabel)
                                    }
                                }
                                if (subtitleText.isNotEmpty()) {
                                    Text(
                                        text = subtitleText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.72f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (pipState.isSupported) {
                                IconButton(
                                    onClick = {
                                        controlsVisible = false
                                        pipState.enterPip(video.videoWidth, video.videoHeight)
                                    }
                                ) {
                                    Icon(
                                        Icons.Rounded.PictureInPictureAlt,
                                        contentDescription = "Picture-in-Picture",
                                        tint = Color.White
                                    )
                                }
                            }
                            IconButton(onClick = {
                                isLandscape = !isLandscape
                                activity?.requestedOrientation = if (isLandscape) {
                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                } else {
                                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                }
                            }) {
                                Icon(Icons.Rounded.ScreenRotation, contentDescription = "Orientação", tint = Color.White)
                            }
                            IconButton(onClick = { resizeModeIndex = (resizeModeIndex + 1) % 3 }) {
                                Icon(Icons.Rounded.AspectRatio, contentDescription = "Ajustar ecrã", tint = Color.White)
                            }
                            Box {
                                IconButton(onClick = { trackMenuOpen = true }) {
                                    Icon(Icons.Rounded.Subtitles, contentDescription = "Áudio e legendas", tint = Color.White)
                                }
                                DropdownMenu(
                                    expanded = trackMenuOpen,
                                    onDismissRequest = { trackMenuOpen = false },
                                    shape = RoundedCornerShape(16.dp),
                                    containerColor = Color(0xFF131A26),
                                    tonalElevation = 0.dp,
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                                ) {
                                    val videoMenuColors = MenuDefaults.itemColors(textColor = Color.White)
                                    DropdownMenuItem(
                                        text = { Text("Carregar legenda (.srt / .vtt)") },
                                        colors = videoMenuColors,
                                        onClick = {
                                            trackMenuOpen = false
                                            runCatching {
                                                subtitleLauncher.launch(AppConfig.SAF_SUBTITLE_MIME_TYPES)
                                            }.onFailure {
                                                errorMessage = "Nenhum seletor de ficheiros disponível neste dispositivo."
                                            }
                                        }
                                    )
                                    val subtitleGroups = currentTracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
                                    val audioGroups = currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
                                    audioGroups.forEachIndexed { i, group ->
                                        val label = group.getTrackFormat(0).language ?: "Áudio ${i + 1}"
                                        DropdownMenuItem(
                                            text = { Text("Áudio: $label") },
                                            colors = videoMenuColors,
                                            onClick = {
                                                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                                    .buildUpon()
                                                    .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
                                                    .build()
                                                trackMenuOpen = false
                                            }
                                        )
                                    }
                                    subtitleGroups.forEachIndexed { i, group ->
                                        val format = group.getTrackFormat(0)
                                        val label = format.label ?: format.language ?: "Legenda ${i + 1}"
                                        DropdownMenuItem(
                                            text = { Text("Legenda: $label") },
                                            colors = videoMenuColors,
                                            onClick = {
                                                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                                    .buildUpon()
                                                    .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
                                                    .build()
                                                trackMenuOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { isLocked = true }) {
                                Icon(Icons.Rounded.LockOpen, contentDescription = "Bloquear controlos", tint = Color.White)
                            }
                        }
                    }

                    // Center Transport Controls
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiquidGlassSurface(
                            shape = CircleShape,
                            onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)) },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Rounded.Replay10, contentDescription = "Recuar 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(glass.accentPrimary, glass.accentSecondary)))
                                .clickable {
                                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        LiquidGlassSurface(
                            shape = CircleShape,
                            onClick = { exoPlayer.seekTo(exoPlayer.currentPosition + 10_000L) },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Rounded.Forward10, contentDescription = "Avançar 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }
                    }

                    // Bottom Scrubber + Brightness & Volume + Speed
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(formatDuration(positionMs), style = MaterialTheme.typography.labelSmall, color = Color.White)
                                Slider(
                                    value = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
                                    onValueChange = { frac ->
                                        exoPlayer.seekTo((frac * durationMs).toLong())
                                    },
                                    colors = SliderDefaults.colors(
                                        thumbColor = glass.accentPrimary,
                                        activeTrackColor = glass.accentPrimary,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                )
                                Text(formatDuration(durationMs), style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.Brightness6, contentDescription = "Brilho", tint = Color.White, modifier = Modifier.size(18.dp))
                                Slider(
                                    value = brightness,
                                    onValueChange = applyBrightness,
                                    modifier = Modifier.weight(1f)
                                )

                                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Volume", tint = Color.White, modifier = Modifier.size(18.dp))
                                Slider(
                                    value = volumeFraction,
                                    onValueChange = applyVolume,
                                    modifier = Modifier.weight(1f)
                                )

                                Box {
                                    IconButton(onClick = { speedMenuOpen = true }) {
                                        Icon(Icons.Rounded.Speed, contentDescription = "Velocidade", tint = Color.White)
                                    }
                                    DropdownMenu(
                                        expanded = speedMenuOpen,
                                        onDismissRequest = { speedMenuOpen = false },
                                        shape = RoundedCornerShape(16.dp),
                                        containerColor = Color(0xFF131A26),
                                        tonalElevation = 0.dp,
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                                    ) {
                                        val videoSpeedColors = MenuDefaults.itemColors(textColor = Color.White)
                                        listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                                            DropdownMenuItem(
                                                text = { Text("${spd}x") },
                                                colors = videoSpeedColors,
                                                onClick = {
                                                    playbackSpeed = spd
                                                    exoPlayer.setPlaybackSpeed(spd)
                                                    speedMenuOpen = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String {
    var name = uri.lastPathSegment?.substringAfterLast('/') ?: "Legenda"
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) {
                    cursor.getString(idx)?.takeIf { it.isNotBlank() }?.let { name = it }
                }
            }
        }
    }
    return name
}
