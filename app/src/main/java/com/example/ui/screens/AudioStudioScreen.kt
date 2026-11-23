package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SaveAs
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.media.AudioFormatDetails
import com.example.data.media.LumaStudioEngine
import com.example.data.media.RingtoneApplyResult
import com.example.data.media.StudioExportFormat
import com.example.data.media.StudioExportResult
import com.example.data.model.MediaItemEntity
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LocalLumaGlass
import com.example.ui.theme.MediaArtwork
import com.example.ui.theme.formatDuration
import com.example.ui.theme.formatFileSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioStudioScreen(
    initialTrack: MediaItemEntity?,
    availableTracks: List<MediaItemEntity>,
    bottomInsetPadding: Dp,
    onBack: () -> Unit,
    onPauseMainPlayer: () -> Unit,
    onStudioExportComplete: (StudioExportResult) -> Unit,
    onStatusMessage: (String) -> Unit
) {
    BackHandler { onBack() }

    val glass = LocalLumaGlass.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTrack by remember(initialTrack?.id, availableTracks) {
        mutableStateOf(initialTrack ?: availableTracks.firstOrNull())
    }
    var showTrackPicker by remember { mutableStateOf(false) }

    val track = selectedTrack
    val totalDurationMs = remember(track?.id, track?.durationMs) {
        (track?.durationMs ?: 0L).coerceAtLeast(1_000L)
    }

    var waveformPeaks by remember(track?.id) { mutableStateOf<List<Float>>(emptyList()) }
    var isLoadingWaveform by remember(track?.id) { mutableStateOf(false) }
    var formatDetails by remember(track?.id) { mutableStateOf<AudioFormatDetails?>(null) }
    var inspectorExpanded by remember { mutableStateOf(true) }

    var trimStartMs by remember(track?.id) { mutableLongStateOf(0L) }
    var trimEndMs by remember(track?.id) { mutableLongStateOf(totalDurationMs) }
    var fadeInMs by remember(track?.id) { mutableLongStateOf(0L) }
    var fadeOutMs by remember(track?.id) { mutableLongStateOf(500L) }
    var selectedFormat by remember { mutableStateOf(StudioExportFormat.M4A_AAC) }

    var isExporting by remember { mutableStateOf(false) }
    var lastExportResult by remember { mutableStateOf<StudioExportResult?>(null) }
    var pendingRingtoneUri by remember { mutableStateOf<Uri?>(null) }
    var showWriteSettingsPrompt by remember { mutableStateOf(false) }

    // Local ExoPlayer solely for Studio segment auditioning
    val previewPlayer = remember(context) {
        ExoPlayer.Builder(context).build()
    }
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var previewPositionMs by remember(track?.id) { mutableLongStateOf(0L) }

    DisposableEffect(previewPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isPreviewPlaying = isPlaying
            }
        }
        previewPlayer.addListener(listener)
        onDispose {
            previewPlayer.removeListener(listener)
            runCatching { previewPlayer.release() }
        }
    }

    LaunchedEffect(track?.id) {
        if (track != null) {
            isPreviewPlaying = false
            runCatching { previewPlayer.stop() }
            isLoadingWaveform = true
            trimStartMs = 0L
            trimEndMs = totalDurationMs
            previewPositionMs = 0L
            formatDetails = LumaStudioEngine.inspectAudioFormat(context, track)
            waveformPeaks = LumaStudioEngine.extractWaveformPeaks(
                context = context,
                uriString = track.uri,
                durationMs = totalDurationMs,
                barCount = 72
            )
            isLoadingWaveform = false
        }
    }

    // Keep preview looping inside [trimStartMs..trimEndMs] with smooth fade audition gain
    LaunchedEffect(isPreviewPlaying, trimStartMs, trimEndMs, fadeInMs, fadeOutMs) {
        while (isPreviewPlaying) {
            val pos = previewPlayer.currentPosition.coerceAtLeast(0L)
            previewPositionMs = pos
            if (pos >= trimEndMs) {
                previewPlayer.seekTo(trimStartMs)
            }
            val relPos = (pos - trimStartMs).coerceAtLeast(0L)
            val segLen = (trimEndMs - trimStartMs).coerceAtLeast(1L)
            var gain = 1f
            if (fadeInMs > 0L && relPos < fadeInMs) {
                gain = (relPos.toFloat() / fadeInMs.toFloat()).coerceIn(0f, 1f)
            }
            val remaining = (segLen - relPos).coerceAtLeast(0L)
            if (fadeOutMs > 0L && remaining < fadeOutMs) {
                gain = minOf(gain, (remaining.toFloat() / fadeOutMs.toFloat()).coerceIn(0f, 1f))
            }
            previewPlayer.volume = gain
            delay(45L)
        }
    }

    val togglePreview: () -> Unit = toggle@{
        val activeTrack = track ?: return@toggle
        if (isPreviewPlaying) {
            previewPlayer.pause()
        } else {
            onPauseMainPlayer()
            val mediaItem = MediaItem.fromUri(Uri.parse(activeTrack.uri))
            val currentUri = previewPlayer.currentMediaItem?.localConfiguration?.uri?.toString()
            if (currentUri != activeTrack.uri) {
                previewPlayer.setMediaItem(mediaItem)
                previewPlayer.prepare()
            }
            val startAt = if (previewPositionMs in trimStartMs until trimEndMs) previewPositionMs else trimStartMs
            previewPlayer.seekTo(startAt)
            previewPlayer.play()
        }
    }

    val exportCustomLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/*"),
        onResult = { destUri ->
            val activeTrack = track ?: return@rememberLauncherForActivityResult
            if (destUri != null) {
                scope.launch {
                    isExporting = true
                    val res = LumaStudioEngine.exportTrimmedAudio(
                        context = context,
                        track = activeTrack,
                        startMs = trimStartMs,
                        endMs = trimEndMs,
                        fadeInMs = fadeInMs,
                        fadeOutMs = fadeOutMs,
                        format = selectedFormat,
                        destinationUri = destUri,
                        isRingtone = false
                    )
                    isExporting = false
                    res.onSuccess { exported ->
                        lastExportResult = exported
                        onStudioExportComplete(exported)
                    }.onFailure { err ->
                        onStatusMessage(err.message ?: "Erro ao exportar áudio editado.")
                    }
                }
            }
        }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("audio_studio_screen"),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = bottomInsetPadding + 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("studio_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Luma Studio", style = MaterialTheme.typography.headlineMedium, color = glass.textPrimary)
                    Text(
                        "Corte preciso, waveform, fade e criador de toques",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                }
                if (availableTracks.isNotEmpty()) {
                    TextButton(
                        onClick = { showTrackPicker = true },
                        modifier = Modifier.testTag("studio_pick_track_button")
                    ) {
                        Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Faixa")
                    }
                }
            }
        }

        if (track == null) {
            item {
                LiquidGlassSurface(shape = RoundedCornerShape(24.dp)) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Rounded.AudioFile,
                            contentDescription = null,
                            tint = glass.accentPrimary,
                            modifier = Modifier.size(42.dp)
                        )
                        Text("Nenhuma música selecionada", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Importe ou selecione uma faixa de áudio da biblioteca para editar no Luma Studio.",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                    }
                }
            }
        } else {
            // Active Track Card
            item {
                LiquidGlassSurface(shape = RoundedCornerShape(22.dp), elevated = true, accentGlow = glass.accentPrimary) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MediaArtwork(
                            artworkPath = track.artworkPath,
                            contentDescription = track.title,
                            cornerRadius = 14.dp,
                            maxDecodeSize = 120.dp,
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(track.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, style = MaterialTheme.typography.bodySmall, color = glass.textSecondary, maxLines = 1)
                            val badge = buildString {
                                formatDetails?.let {
                                    append(it.containerLabel)
                                    append(" • ${it.sampleRateHz / 1000.0} kHz")
                                    if (it.bitrateKbps > 0) append(" • ${it.bitrateKbps} kbps")
                                } ?: append(formatDuration(totalDurationMs))
                            }
                            Text(badge, style = MaterialTheme.typography.labelSmall, color = glass.accentPrimary)
                        }
                    }
                }
            }

            // Waveform Visualization & Trimmer Card
            item {
                LiquidGlassSurface(shape = RoundedCornerShape(24.dp), elevated = true) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = glass.accentPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Editor de Waveform & Corte", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text(
                                text = formatDuration((trimEndMs - trimStartMs).coerceAtLeast(0L)),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = glass.accentPrimary
                            )
                        }

                        // Waveform Canvas with Fade Envelope + Start/End Markers + Playhead
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(glass.glassSurface.copy(alpha = 0.7f))
                                .border(0.8.dp, glass.glassBorder, RoundedCornerShape(18.dp))
                                .padding(horizontal = 10.dp, vertical = 12.dp)
                        ) {
                            if (isLoadingWaveform) {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center).size(26.dp),
                                    color = glass.accentPrimary
                                )
                            } else {
                                val peaks = waveformPeaks
                                val startFraction = (trimStartMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                                val endFraction = (trimEndMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                                val playFraction = (previewPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                                val fadeInFraction = (fadeInMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 0.5f)
                                val fadeOutFraction = (fadeOutMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 0.5f)

                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(totalDurationMs, trimStartMs, trimEndMs) {
                                            detectTapGestures { offset ->
                                                val frac = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                                                val tappedMs = (frac * totalDurationMs).roundToLong()
                                                previewPositionMs = tappedMs.coerceIn(trimStartMs, trimEndMs)
                                                if (isPreviewPlaying) {
                                                    previewPlayer.seekTo(previewPositionMs)
                                                }
                                            }
                                        }
                                ) {
                                    val w = size.width
                                    val h = size.height
                                    val count = peaks.size.coerceAtLeast(1)
                                    val slotW = w / count
                                    val barW = (slotW * 0.62f).coerceAtLeast(3f)

                                    // Selected trim region backdrop
                                    val leftX = startFraction * w
                                    val rightX = endFraction * w
                                    drawRoundRect(
                                        color = glass.accentPrimary.copy(alpha = 0.13f),
                                        topLeft = Offset(leftX, 0f),
                                        size = Size((rightX - leftX).coerceAtLeast(4f), h),
                                        cornerRadius = CornerRadius(8f, 8f)
                                    )

                                    // Draw waveform bars
                                    peaks.forEachIndexed { idx, amp ->
                                        val barCenterFrac = (idx + 0.5f) / count
                                        val inRegion = barCenterFrac in startFraction..endFraction
                                        val barH = (amp * h * 0.86f).coerceAtLeast(8f)
                                        val x = idx * slotW + (slotW - barW) / 2f
                                        val top = (h - barH) / 2f
                                        val barColor = if (inRegion) glass.accentPrimary else glass.textDisabled.copy(alpha = 0.35f)
                                        drawRoundRect(
                                            color = barColor,
                                            topLeft = Offset(x, top),
                                            size = Size(barW, barH),
                                            cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                                        )
                                    }

                                    // Fade In / Fade Out envelope curve
                                    if (fadeInMs > 0L || fadeOutMs > 0L) {
                                        val path = Path()
                                        val fiEndX = ((startFraction + fadeInFraction) * w).coerceAtMost(rightX)
                                        val foStartX = ((endFraction - fadeOutFraction) * w).coerceAtLeast(leftX)
                                        path.moveTo(leftX, h - 4f)
                                        path.lineTo(fiEndX, 6f)
                                        path.lineTo(foStartX.coerceAtLeast(fiEndX), 6f)
                                        path.lineTo(rightX, h - 4f)
                                        drawPath(
                                            path = path,
                                            color = glass.accentSecondary,
                                            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                                        )
                                    }

                                    // Trim Start & End handles
                                    drawLine(
                                        color = glass.accentSecondary,
                                        start = Offset(leftX, 0f),
                                        end = Offset(leftX, h),
                                        strokeWidth = 4f,
                                        cap = StrokeCap.Round
                                    )
                                    drawLine(
                                        color = glass.accentSecondary,
                                        start = Offset(rightX, 0f),
                                        end = Offset(rightX, h),
                                        strokeWidth = 4f,
                                        cap = StrokeCap.Round
                                    )

                                    // Playhead indicator
                                    val playX = (playFraction * w).coerceIn(leftX, rightX)
                                    drawLine(
                                        color = glass.textPrimary,
                                        start = Offset(playX, 0f),
                                        end = Offset(playX, h),
                                        strokeWidth = 3f,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }

                        // Precision Range Slider for Trim A..B
                        RangeSlider(
                            value = (trimStartMs.toFloat() / totalDurationMs)..(trimEndMs.toFloat() / totalDurationMs),
                            onValueChange = { range ->
                                val newStart = (range.start * totalDurationMs).roundToLong()
                                val newEnd = (range.endInclusive * totalDurationMs).roundToLong()
                                if (newEnd - newStart >= 500L) {
                                    trimStartMs = newStart.coerceIn(0L, totalDurationMs - 500L)
                                    trimEndMs = newEnd.coerceIn(trimStartMs + 500L, totalDurationMs)
                                }
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = glass.accentPrimary,
                                activeTrackColor = glass.accentPrimary,
                                inactiveTrackColor = glass.glassBorder
                            ),
                            modifier = Modifier.testTag("studio_trim_slider")
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Início (A): ${formatDuration(trimStartMs)}", style = MaterialTheme.typography.labelMedium, color = glass.textSecondary)
                            Text("Fim (B): ${formatDuration(trimEndMs)}", style = MaterialTheme.typography.labelMedium, color = glass.textSecondary)
                        }

                        // Quick Segment Presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StudioPresetChip("Intro 30s") {
                                trimStartMs = 0L
                                trimEndMs = minOf(30_000L, totalDurationMs)
                                previewPositionMs = trimStartMs
                            }
                            StudioPresetChip("Refrão 30s") {
                                val center = totalDurationMs / 2L
                                trimStartMs = (center - 15_000L).coerceAtLeast(0L)
                                trimEndMs = (trimStartMs + 30_000L).coerceAtMost(totalDurationMs)
                                previewPositionMs = trimStartMs
                            }
                            StudioPresetChip("Tudo") {
                                trimStartMs = 0L
                                trimEndMs = totalDurationMs
                                previewPositionMs = 0L
                            }
                        }

                        // Preview Transport Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = togglePreview,
                                colors = ButtonDefaults.buttonColors(containerColor = glass.accentPrimary, contentColor = glass.onAccent),
                                modifier = Modifier.weight(1f).testTag("studio_preview_button")
                            ) {
                                Icon(
                                    if (isPreviewPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isPreviewPlaying) "Pausar Pré-Escuta" else "Pré-Ouvir Corte")
                            }
                            OutlinedButton(
                                onClick = {
                                    trimStartMs = 0L
                                    trimEndMs = totalDurationMs
                                    fadeInMs = 0L
                                    fadeOutMs = 500L
                                    previewPositionMs = 0L
                                }
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Repor", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Fade In & Fade Out Controls
            item {
                LiquidGlassSurface(shape = RoundedCornerShape(22.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Fade In & Fade Out", style = MaterialTheme.typography.titleMedium)

                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Fade In (Entrada suave)", style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
                                Text("${fadeInMs / 1000f} s", style = MaterialTheme.typography.labelLarge, color = glass.accentPrimary)
                            }
                            Slider(
                                value = fadeInMs.toFloat(),
                                onValueChange = { fadeInMs = ((it / 250f).roundToLong() * 250L).coerceIn(0L, 5_000L) },
                                valueRange = 0f..5000f,
                                colors = SliderDefaults.colors(thumbColor = glass.accentPrimary, activeTrackColor = glass.accentPrimary)
                            )
                        }

                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Fade Out (Saída suave)", style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
                                Text("${fadeOutMs / 1000f} s", style = MaterialTheme.typography.labelLarge, color = glass.accentSecondary)
                            }
                            Slider(
                                value = fadeOutMs.toFloat(),
                                onValueChange = { fadeOutMs = ((it / 250f).roundToLong() * 250L).coerceIn(0L, 5_000L) },
                                valueRange = 0f..5000f,
                                colors = SliderDefaults.colors(thumbColor = glass.accentSecondary, activeTrackColor = glass.accentSecondary)
                            )
                        }
                    }
                }
            }

            // Audio Format Inspector Card
            item {
                LiquidGlassSurface(shape = RoundedCornerShape(22.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { inspectorExpanded = !inspectorExpanded },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Info, contentDescription = null, tint = glass.accentPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Inspetor de Formato de Áudio", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Icon(
                                if (inspectorExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = if (inspectorExpanded) "Recolher" else "Expandir"
                            )
                        }
                        AnimatedVisibility(visible = inspectorExpanded) {
                            val details = formatDetails
                            if (details == null) {
                                Text("A analisar fluxo de áudio...", style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FormatInspectorRow("Contentor / Qualidade", if (details.isLossless) "${details.containerLabel} (Lossless)" else details.containerLabel)
                                    FormatInspectorRow("Codec", details.codecName)
                                    FormatInspectorRow("Frequência", "${details.sampleRateHz} Hz")
                                    FormatInspectorRow("Canais", if (details.channelCount == 1) "Mono (1)" else "Estéreo (${details.channelCount})")
                                    FormatInspectorRow("Taxa de bits", if (details.bitrateKbps > 0) "${details.bitrateKbps} kbps" else "Variável / PCM")
                                    FormatInspectorRow("Profundidade / Tamanho", "${details.bitsPerSample}-bit • ${formatFileSize(details.fileSizeBytes)}")
                                    FormatInspectorRow("MIME", details.mimeType)
                                }
                            }
                        }
                    }
                }
            }

            // Export & Custom Ringtone Maker Card
            item {
                LiquidGlassSurface(shape = RoundedCornerShape(24.dp), elevated = true) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Exportação & Criador de Toques", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "O ficheiro original permanece intacto em segurança — é sempre criada uma nova cópia editada.",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StudioExportFormat.entries.forEach { fmt ->
                                FilterChip(
                                    selected = selectedFormat == fmt,
                                    onClick = { selectedFormat = fmt },
                                    label = { Text(fmt.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = glass.accentPrimary.copy(alpha = 0.22f),
                                        selectedLabelColor = glass.accentPrimary
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        if (isExporting) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(vertical = 6.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = glass.accentPrimary)
                                Text("A processar corte e fade em segundo plano...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        isExporting = true
                                        val res = LumaStudioEngine.exportTrimmedAudio(
                                            context = context,
                                            track = track,
                                            startMs = trimStartMs,
                                            endMs = trimEndMs,
                                            fadeInMs = fadeInMs,
                                            fadeOutMs = fadeOutMs,
                                            format = selectedFormat,
                                            destinationUri = null,
                                            isRingtone = false
                                        )
                                        isExporting = false
                                        res.onSuccess { exported ->
                                            lastExportResult = exported
                                            onStudioExportComplete(exported)
                                        }.onFailure { err ->
                                            onStatusMessage(err.message ?: "Erro ao exportar para Música.")
                                        }
                                    }
                                },
                                enabled = !isExporting,
                                colors = ButtonDefaults.buttonColors(containerColor = glass.accentPrimary, contentColor = glass.onAccent),
                                modifier = Modifier.weight(1f).testTag("studio_export_music_button")
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Exportar Áudio")
                            }

                            OutlinedButton(
                                onClick = {
                                    val cleanName = track.title.replace(Regex("[^a-zA-Z0-9._ -]"), "").ifBlank { "luma_cut" }
                                    exportCustomLocationLauncher.launch("${cleanName}_luma.${selectedFormat.extension}")
                                },
                                enabled = !isExporting
                            ) {
                                Icon(Icons.Rounded.SaveAs, contentDescription = "Guardar como", modifier = Modifier.size(18.dp))
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isExporting = true
                                    val res = LumaStudioEngine.exportTrimmedAudio(
                                        context = context,
                                        track = track,
                                        startMs = trimStartMs,
                                        endMs = trimEndMs,
                                        fadeInMs = fadeInMs,
                                        fadeOutMs = fadeOutMs,
                                        format = StudioExportFormat.M4A_AAC,
                                        destinationUri = null,
                                        isRingtone = true
                                    )
                                    isExporting = false
                                    res.onSuccess { exported ->
                                        lastExportResult = exported
                                        when (val applyRes = LumaStudioEngine.applySystemRingtone(context, exported.exportedUri)) {
                                            RingtoneApplyResult.Success -> {
                                                onStudioExportComplete(exported)
                                                onStatusMessage("Toque personalizado \"${exported.displayName}\" aplicado!")
                                            }
                                            RingtoneApplyResult.NeedsWriteSettingsPermission -> {
                                                pendingRingtoneUri = exported.exportedUri
                                                showWriteSettingsPrompt = true
                                                onStudioExportComplete(exported)
                                            }
                                            is RingtoneApplyResult.Error -> {
                                                onStatusMessage(applyRes.message)
                                            }
                                        }
                                    }.onFailure { err ->
                                        onStatusMessage(err.message ?: "Erro ao gerar toque.")
                                    }
                                }
                            },
                            enabled = !isExporting,
                            modifier = Modifier.fillMaxWidth().testTag("studio_ringtone_button")
                        ) {
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = glass.accentSecondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Criar & Definir como Toque do Telemóvel")
                        }

                        lastExportResult?.let { result ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(glass.accentPrimary.copy(alpha = 0.14f))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = glass.accentPrimary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(result.displayName, style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        "${result.format.shortLabel} • ${formatDuration(result.trimmedDurationMs)} • ${formatFileSize(result.outputSizeBytes)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = glass.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTrackPicker) {
        AlertDialog(
            onDismissRequest = { showTrackPicker = false },
            containerColor = glass.surfaceElevated,
            title = { Text("Selecionar faixa para editar") },
            text = {
                LazyColumn(
                    modifier = Modifier.height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(availableTracks, key = { "studio_pick_${it.id}" }) { candidate ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedTrack = candidate
                                    showTrackPicker = false
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MediaArtwork(
                                artworkPath = candidate.artworkPath,
                                contentDescription = null,
                                cornerRadius = 10.dp,
                                maxDecodeSize = 80.dp,
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(candidate.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(candidate.artist, style = MaterialTheme.typography.bodySmall, color = glass.textSecondary, maxLines = 1)
                            }
                            Text(formatDuration(candidate.durationMs), style = MaterialTheme.typography.labelSmall, color = glass.textDisabled)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTrackPicker = false }) {
                    Text("Fechar")
                }
            }
        )
    }

    if (showWriteSettingsPrompt) {
        AlertDialog(
            onDismissRequest = { showWriteSettingsPrompt = false },
            containerColor = glass.surfaceElevated,
            title = { Text("Permitir alteração de toque") },
            text = {
                Text(
                    "O ficheiro foi guardado na pasta Ringtones/Luma Studio. Para definir automaticamente como toque padrão, ative 'Modificar definições do sistema' para o Luma Player."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWriteSettingsPrompt = false
                        runCatching { context.startActivity(LumaStudioEngine.createWriteSettingsIntent(context)) }
                    }
                ) {
                    Text("Abrir Definições")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    val uri = pendingRingtoneUri
                    showWriteSettingsPrompt = false
                    if (uri != null && LumaStudioEngine.applySystemRingtone(context, uri) is RingtoneApplyResult.Success) {
                        onStatusMessage("Toque aplicado com sucesso!")
                    }
                }) {
                    Text("Agora não")
                }
            }
        )
    }
}

@Composable
private fun StudioPresetChip(label: String, onClick: () -> Unit) {
    val glass = LocalLumaGlass.current
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(glass.glassSurface)
            .border(0.8.dp, glass.glassBorder, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = glass.textPrimary)
    }
}

@Composable
private fun FormatInspectorRow(label: String, value: String) {
    val glass = LocalLumaGlass.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = glass.textPrimary)
    }
}
