package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Fireplace
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.media.LrcParser
import com.example.data.model.AmbientSoundType
import com.example.data.model.MediaItemEntity
import com.example.data.model.TrackBookmark
import com.example.player.AmbientMixerState
import com.example.player.PlaybackUiState
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LocalLumaGlass
import com.example.ui.theme.LocalLumaStrings
import com.example.ui.theme.formatDuration
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 1. A-B Phrase Loop Studio Sheet: mark Point A, Point B, nudge ±500ms, toggle, or clear.
 */
@Composable
fun AbLoopSheetContent(
    playbackState: PlaybackUiState,
    onSetPointA: () -> Unit,
    onSetPointB: () -> Unit,
    onNudgePoint: (isPointA: Boolean, deltaMs: Long) -> Unit,
    onToggleActive: () -> Unit,
    onSeekToA: () -> Unit,
    onClear: () -> Unit
) {
    val glass = LocalLumaGlass.current
    val strings = LocalLumaStrings.current
    val dur = playbackState.durationMs.coerceAtLeast(1L)
    val pointA = playbackState.loopStartMs
    val pointB = playbackState.loopEndMs

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("ab_loop_sheet"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Loop, contentDescription = null, tint = glass.accentPrimary)
            Column(modifier = Modifier.weight(1f)) {
                Text(strings.abLoopTitle, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = if (playbackState.isAbLoopActive && pointA != null && pointB != null) {
                        "Ativo: ${formatDuration(pointA)} → ${formatDuration(pointB)}"
                    } else {
                        "Marca os pontos A e B para repetir um trecho"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.accentPrimary
                )
            }
        }

        // Visual A-B Progress Range Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(glass.textPrimary.copy(alpha = 0.12f))
        ) {
            if (pointA != null) {
                val startFrac = (pointA.toFloat() / dur.toFloat()).coerceIn(0f, 0.96f)
                val endFrac = ((pointB ?: (pointA + 3000L)).toFloat() / dur.toFloat()).coerceIn(startFrac + 0.04f, 1f)
                Row(modifier = Modifier.fillMaxWidth()) {
                    if (startFrac > 0.01f) Spacer(modifier = Modifier.weight(startFrac))
                    Box(
                        modifier = Modifier
                            .weight((endFrac - startFrac).coerceAtLeast(0.04f))
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(if (playbackState.isAbLoopActive) glass.accentPrimary else glass.accentSecondary)
                    )
                    val tail = (1f - endFrac).coerceAtLeast(0.001f)
                    if (tail > 0.01f) Spacer(modifier = Modifier.weight(tail))
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onSetPointA,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (pointA != null) glass.accentPrimary else glass.surface,
                    contentColor = if (pointA != null) glass.onAccent else glass.textPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ab_set_a_button")
            ) {
                Text(if (pointA != null) "A: ${formatDuration(pointA)}" else "Marcar Ponto A")
            }
            Button(
                onClick = onSetPointB,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (pointB != null) glass.accentPrimary else glass.surface,
                    contentColor = if (pointB != null) glass.onAccent else glass.textPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ab_set_b_button")
            ) {
                Text(if (pointB != null) "B: ${formatDuration(pointB)}" else "Marcar Ponto B")
            }
        }

        if (pointA != null || pointB != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ajuste fino A", style = MaterialTheme.typography.labelMedium, color = glass.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { onNudgePoint(true, -500L) }) { Text("-0.5s") }
                    OutlinedButton(onClick = { onNudgePoint(true, 500L) }) { Text("+0.5s") }
                }
            }
            if (pointB != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ajuste fino B", style = MaterialTheme.typography.labelMedium, color = glass.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { onNudgePoint(false, -500L) }) { Text("-0.5s") }
                        OutlinedButton(onClick = { onNudgePoint(false, 500L) }) { Text("+0.5s") }
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (pointA != null && pointB != null) {
                OutlinedButton(
                    onClick = onToggleActive,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Repeat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (playbackState.isAbLoopActive) "Pausar Loop" else "Ativar Loop")
                }
                OutlinedButton(onClick = onSeekToA) {
                    Text("Ir para A")
                }
            }
            if (pointA != null || pointB != null) {
                TextButton(onClick = onClear) {
                    Text("Limpar", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
    }
}

/**
 * 2. DJ Speed & Pitch Studio Sheet: independent tempo + pitch semitones (-6..+6) + vibe presets.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpeedAndPitchSheetContent(
    currentSpeed: Float,
    currentPitchSemitones: Float,
    onSelectSpeed: (Float) -> Unit,
    onSelectPitchSemitones: (Float) -> Unit
) {
    val glass = LocalLumaGlass.current
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = glass.pillIndicatorBg,
        selectedLabelColor = glass.accentPrimary,
        containerColor = glass.surface,
        labelColor = glass.textPrimary
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("speed_pitch_sheet"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Tune, contentDescription = null, tint = glass.accentPrimary)
                Text("Velocidade & Tom (DJ)", style = MaterialTheme.typography.titleLarge)
            }
            TextButton(onClick = {
                onSelectSpeed(1.0f)
                onSelectPitchSemitones(0f)
            }) {
                Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Normal")
            }
        }

        // Quick Vibe Presets
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = kotlin.math.abs(currentSpeed - 1.0f) < 0.03f && kotlin.math.abs(currentPitchSemitones) < 0.2f,
                onClick = { onSelectSpeed(1.0f); onSelectPitchSemitones(0f) },
                label = { Text("Original") },
                colors = chipColors
            )
            FilterChip(
                selected = kotlin.math.abs(currentSpeed - 0.85f) < 0.03f && kotlin.math.abs(currentPitchSemitones + 2f) < 0.3f,
                onClick = { onSelectSpeed(0.85f); onSelectPitchSemitones(-2f) },
                label = { Text("Warm Lo-Fi") },
                colors = chipColors,
                modifier = Modifier.testTag("preset_warm_lofi")
            )
            FilterChip(
                selected = kotlin.math.abs(currentSpeed - 1.2f) < 0.03f && kotlin.math.abs(currentPitchSemitones - 2.5f) < 0.4f,
                onClick = { onSelectSpeed(1.2f); onSelectPitchSemitones(2.5f) },
                label = { Text("Nightcore") },
                colors = chipColors
            )
            FilterChip(
                selected = kotlin.math.abs(currentSpeed - 1.0f) < 0.03f && kotlin.math.abs(currentPitchSemitones + 3f) < 0.3f,
                onClick = { onSelectSpeed(1.0f); onSelectPitchSemitones(-3f) },
                label = { Text("Deep Vocal") },
                colors = chipColors
            )
        }

        // Tempo Speed Chips + Slider
        Text(
            text = "Velocidade: ${String.format("%.2fx", currentSpeed)}",
            style = MaterialTheme.typography.titleSmall
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(0.75f, 0.9f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                FilterChip(
                    selected = kotlin.math.abs(currentSpeed - speed) < 0.04f,
                    onClick = { onSelectSpeed(speed) },
                    label = { Text("${speed}x") },
                    colors = chipColors
                )
            }
        }

        // Pitch Semitone Slider (-6 to +6)
        val roundedPitch = (currentPitchSemitones * 2f).roundToInt() / 2f
        val pitchLabel = when {
            roundedPitch > 0f -> "+$roundedPitch st"
            roundedPitch < 0f -> "$roundedPitch st"
            else -> "0 st (Natural)"
        }
        Text(
            text = "Tom (Pitch): $pitchLabel",
            style = MaterialTheme.typography.titleSmall
        )
        Slider(
            value = currentPitchSemitones.coerceIn(-6f, 6f),
            onValueChange = { raw ->
                val snapped = (raw * 2f).roundToInt() / 2f
                onSelectPitchSemitones(snapped)
            },
            valueRange = -6f..6f,
            colors = SliderDefaults.colors(
                thumbColor = glass.accentPrimary,
                activeTrackColor = glass.accentPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pitch_semitone_slider")
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

/**
 * 3. Timestamp Moments (Bookmarks) Sheet: pin favorite moments in any song/video & jump instantly.
 */
@Composable
fun BookmarksSheetContent(
    track: MediaItemEntity,
    positionMs: Long,
    bookmarks: List<TrackBookmark>,
    onAddBookmark: (label: String) -> Unit,
    onJumpToBookmark: (Long) -> Unit,
    onDeleteBookmark: (Long) -> Unit
) {
    val glass = LocalLumaGlass.current
    val strings = LocalLumaStrings.current
    var labelInput by remember(track.id) { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("bookmarks_sheet"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Bookmark, contentDescription = null, tint = glass.accentPrimary)
            Column(modifier = Modifier.weight(1f)) {
                Text(strings.bookmarksTitle, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "${track.title} • ${formatDuration(positionMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = labelInput,
                onValueChange = { labelInput = it.take(36) },
                singleLine = true,
                label = { Text("Etiqueta (ex: Refrão, Solo)") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("bookmark_label_input")
            )
            Button(
                onClick = {
                    val resolved = labelInput.trim().ifBlank { "Momento ${formatDuration(positionMs)}" }
                    onAddBookmark(resolved)
                    labelInput = ""
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = glass.accentPrimary,
                    contentColor = glass.onAccent
                ),
                modifier = Modifier.testTag("bookmark_add_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Marcar")
            }
        }

        if (bookmarks.isEmpty()) {
            Text(
                text = "Ainda sem momentos marcados nesta faixa. Toca em Marcar para guardar esta posição.",
                style = MaterialTheme.typography.bodyMedium,
                color = glass.textSecondary
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(bookmarks, key = { it.id }) { bm ->
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(14.dp),
                        onClick = { onJumpToBookmark(bm.positionMs) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = glass.accentPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(bm.label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(formatDuration(bm.positionMs), style = MaterialTheme.typography.labelSmall, color = glass.accentPrimary)
                            }
                            IconButton(
                                onClick = { onDeleteBookmark(bm.id) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Apagar momento", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
    }
}

/**
 * 4. Ambient Soundscapes Mixer Sheet: layer procedural Rain, Ocean, Hearth or Pink Noise.
 */
@Composable
fun AmbientMixerSheetContent(
    ambientState: AmbientMixerState,
    onSelectSound: (AmbientSoundType) -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    val glass = LocalLumaGlass.current
    val strings = LocalLumaStrings.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("ambient_mixer_sheet"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Spa, contentDescription = null, tint = glass.accentPrimary)
            Column(modifier = Modifier.weight(1f)) {
                Text(strings.ambientTitle, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = if (ambientState.isPlaying) "${ambientState.soundType.label} ativo" else "Mistura um som relaxante por baixo da música",
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.accentPrimary
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AmbientSoundType.entries.forEach { sound ->
                val selected = ambientState.soundType == sound
                val icon = when (sound) {
                    AmbientSoundType.OFF -> Icons.Rounded.Close
                    AmbientSoundType.WARM_RAIN -> Icons.Rounded.WaterDrop
                    AmbientSoundType.OCEAN_WAVES -> Icons.Rounded.Waves
                    AmbientSoundType.CRACKLING_HEARTH -> Icons.Rounded.Fireplace
                    AmbientSoundType.FOCUS_PINK -> Icons.Rounded.GraphicEq
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) glass.pillIndicatorBg else glass.textPrimary.copy(alpha = 0.05f))
                        .border(
                            width = if (selected) 1.4.dp else 0.8.dp,
                            color = if (selected) glass.accentPrimary else glass.glassBorder,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelectSound(sound) }
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                        .testTag("ambient_option_${sound.name.lowercase()}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = null, tint = if (selected) glass.accentPrimary else glass.textSecondary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = sound.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (selected) glass.accentPrimary else glass.textPrimary
                        )
                        Text(
                            text = sound.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Volume (${(ambientState.volume * 100).roundToInt()}%)",
                style = MaterialTheme.typography.labelLarge
            )
            Slider(
                value = ambientState.volume,
                onValueChange = onVolumeChange,
                colors = SliderDefaults.colors(
                    thumbColor = glass.accentPrimary,
                    activeTrackColor = glass.accentPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ambient_volume_slider")
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
    }
}

/**
 * 7. Shareable Luma Story Card Preview & PNG Exporter Dialog.
 */
@Composable
fun StoryCardPreviewDialog(
    track: MediaItemEntity,
    onDismiss: () -> Unit
) {
    val glass = LocalLumaGlass.current
    val strings = LocalLumaStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val parsedLyrics = remember(track.embeddedLyrics) { LrcParser.parse(track.embeddedLyrics) }
    var selectedQuote by remember(track.id) {
        mutableStateOf(parsedLyrics.lines.firstOrNull { it.text.isNotBlank() }?.text ?: "")
    }
    var customQuote by remember(track.id) { mutableStateOf("") }
    var previewBitmap by remember(track.id) { mutableStateOf<Bitmap?>(null) }
    var isSharing by remember { mutableStateOf(false) }

    val activeQuote = customQuote.ifBlank { selectedQuote }

    LaunchedEffect(track.id, track.artworkPath, activeQuote, glass.accentPrimary, glass.isDark) {
        if (customQuote.isNotEmpty()) {
            kotlinx.coroutines.delay(180L)
        }
        val rendered = runCatching {
            val artBmp = StoryCardExporter.loadTrackArtworkBitmap(context, track.artworkPath)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                try {
                    StoryCardExporter.renderStoryCardBitmap(
                        track = track,
                        artworkBitmap = artBmp,
                        quoteLine = activeQuote,
                        accentPrimary = glass.accentPrimary,
                        accentSecondary = glass.accentSecondary,
                        isDark = glass.isDark
                    )
                } finally {
                    runCatching { if (artBmp != null && !artBmp.isRecycled) artBmp.recycle() }
                }
            }
        }.getOrNull()
        if (rendered != null) {
            previewBitmap = rendered
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = glass.surfaceElevated,
        title = { Text(strings.shareCardTitle) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("story_card_dialog"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val bmp = previewBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = track.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.8f)
                            .clip(RoundedCornerShape(18.dp))
                    )
                }
                if (parsedLyrics.lines.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        parsedLyrics.lines.filter { it.text.isNotBlank() }.take(6).forEach { line ->
                            FilterChip(
                                selected = selectedQuote == line.text && customQuote.isBlank(),
                                onClick = {
                                    customQuote = ""
                                    selectedQuote = if (selectedQuote == line.text) "" else line.text
                                },
                                label = { Text(line.text.take(22)) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = customQuote,
                    onValueChange = { customQuote = it.take(90) },
                    singleLine = true,
                    label = { Text("Frase ou verso opcional") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bmp = previewBitmap ?: return@Button
                    isSharing = true
                    scope.launch {
                        StoryCardExporter.saveAndShareCard(context, bmp, track.title)
                        isSharing = false
                        onDismiss()
                    }
                },
                enabled = previewBitmap != null && !isSharing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = glass.accentPrimary,
                    contentColor = glass.onAccent
                ),
                modifier = Modifier.testTag("story_card_share_button")
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Partilhar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancelLabel) }
        }
    )
}
