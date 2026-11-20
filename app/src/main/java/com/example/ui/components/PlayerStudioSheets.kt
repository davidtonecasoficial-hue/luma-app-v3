package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatAlignLeft
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.FormatAlignCenter
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.media.LrcParser
import com.example.data.media.LyricLine
import com.example.data.model.AppSettings
import com.example.data.model.CustomAudioPreset
import com.example.data.model.MediaItemEntity
import com.example.data.model.ReverbEffectPreset
import com.example.data.model.VisualizerPreset
import com.example.data.model.VisualizerStyle
import com.example.player.PlaybackController
import com.example.player.PlaybackUiState
import com.example.ui.theme.LiquidGlassBackground
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LocalLumaGlass
import com.example.ui.theme.LumaAnimatedSwitch
import com.example.ui.theme.formatDuration

@Composable
fun StudioEqualizerSheetContent(
    playbackState: PlaybackUiState,
    settings: AppSettings,
    customPresets: List<CustomAudioPreset>,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectSystemPreset: (Int) -> Unit,
    onStudioBandChange: (Int, Int) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onSelectReverb: (ReverbEffectPreset) -> Unit,
    onStereoBalanceChange: (Float) -> Unit,
    onToggleVolumeNormalization: (Boolean) -> Unit,
    onApplyCustomPreset: (CustomAudioPreset) -> Unit,
    onSaveCustomPreset: (CustomAudioPreset) -> Unit,
    onDeleteCustomPreset: (String) -> Unit,
    onSelectVisualizerStyle: (VisualizerStyle) -> Unit,
    onSelectVisualizerPreset: (VisualizerPreset) -> Unit,
    onToggleReactiveLighting: (Boolean) -> Unit
) {
    val eq = playbackState.equalizer
    val glass = LocalLumaGlass.current
    var showSavePresetDialog by remember { mutableStateOf(false) }

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
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Equalizer, contentDescription = null, tint = glass.accentPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Estúdio de Áudio 10 Bandas", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (eq.isAvailable) {
                LumaAnimatedSwitch(
                    checked = eq.enabled,
                    onCheckedChange = onToggleEnabled,
                    testTag = "eq_toggle_switch"
                )
            }
        }

        // Custom Audio Presets Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Presets Personalizados", style = MaterialTheme.typography.labelLarge, color = glass.accentPrimary)
            TextButton(onClick = { showSavePresetDialog = true }) {
                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Guardar atual")
            }
        }
        if (customPresets.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                customPresets.forEach { preset ->
                    FilterChip(
                        selected = false,
                        onClick = { onApplyCustomPreset(preset) },
                        label = { Text(preset.name, maxLines = 1) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Apagar ${preset.name}",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onDeleteCustomPreset(preset.name) }
                            )
                        },
                        colors = chipColors
                    )
                }
            }
        }

        if (!eq.isAvailable) {
            Text(
                text = "Inicie a reprodução de uma faixa para ativar os efeitos de hardware DSP (10 bandas, Virtualizer e Reverb). Os controlos abaixo continuam configuráveis.",
                style = MaterialTheme.typography.bodyMedium,
                color = glass.textSecondary
            )
        } else if (eq.presets.isNotEmpty()) {
            Text("Perfis do Sistema", style = MaterialTheme.typography.labelLarge, color = glass.textSecondary)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                eq.presets.forEachIndexed { idx, presetName ->
                    FilterChip(
                        selected = eq.selectedPresetIndex == idx,
                        onClick = { onSelectSystemPreset(idx) },
                        label = { Text(presetName, maxLines = 1) },
                        colors = chipColors
                    )
                }
            }
        }

        // 10-Band Studio Sliders mapped onto hardware bands
        Text("Curva de 10 Bandas (31Hz — 16kHz)", style = MaterialTheme.typography.labelLarge, color = glass.accentPrimary)
        PlaybackController.STUDIO_BAND_FREQS_HZ.forEachIndexed { idx, freqHz ->
            val levelMb = eq.studioTenBandsMb.getOrElse(idx) { 0 }
            val freqLabel = if (freqHz >= 1000) "${freqHz / 1000} kHz" else "$freqHz Hz"
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(freqLabel, style = MaterialTheme.typography.labelLarge)
                    Text("${levelMb / 100} dB", style = MaterialTheme.typography.labelSmall, color = glass.textSecondary)
                }
                Slider(
                    value = levelMb.toFloat(),
                    onValueChange = { onStudioBandChange(idx, it.toInt()) },
                    valueRange = eq.minLevel.toFloat()..eq.maxLevel.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = glass.accentPrimary,
                        activeTrackColor = glass.accentPrimary
                    )
                )
            }
        }

        HorizontalDivider(color = glass.divider)
        Text("Espacialidade e Dinâmica", style = MaterialTheme.typography.labelLarge, color = glass.accentPrimary)

        if (eq.bassBoostSupported) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Reforço de Graves", style = MaterialTheme.typography.labelLarge)
                    Text("${eq.bassBoostStrength / 10}%", style = MaterialTheme.typography.labelSmall)
                }
                Slider(
                    value = eq.bassBoostStrength.toFloat(),
                    onValueChange = { onBassBoostChange(it.toInt()) },
                    valueRange = 0f..1000f
                )
            }
        }

        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Virtualizador 3D", style = MaterialTheme.typography.labelLarge)
                Text("${eq.virtualizerStrength / 10}%", style = MaterialTheme.typography.labelSmall)
            }
            Slider(
                value = eq.virtualizerStrength.toFloat(),
                onValueChange = { onVirtualizerChange(it.toInt()) },
                valueRange = 0f..1000f
            )
        }

        // Stereo Balance (-1 Left .. 0 Center .. +1 Right)
        Column {
            val balLabel = when {
                eq.stereoBalance < -0.05f -> "Esq. ${(-eq.stereoBalance * 100).toInt()}%"
                eq.stereoBalance > 0.05f -> "Dir. ${(eq.stereoBalance * 100).toInt()}%"
                else -> "Centrado"
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Balanço Estéreo (L / R)", style = MaterialTheme.typography.labelLarge)
                Text(balLabel, style = MaterialTheme.typography.labelSmall, color = glass.accentPrimary)
            }
            Slider(
                value = eq.stereoBalance,
                onValueChange = onStereoBalanceChange,
                valueRange = -1f..1f
            )
        }

        Text("Efeito Reverb", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReverbEffectPreset.entries.forEach { rev ->
                FilterChip(
                    selected = eq.reverbPreset == rev,
                    onClick = { onSelectReverb(rev) },
                    label = { Text(rev.label) },
                    colors = chipColors
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Normalização de Volume", style = MaterialTheme.typography.titleSmall)
                Text("Nivela o ganho entre faixas suaves e intensas", style = MaterialTheme.typography.bodyMedium, color = glass.textSecondary)
            }
            LumaAnimatedSwitch(
                checked = eq.volumeNormalizationEnabled,
                onCheckedChange = onToggleVolumeNormalization
            )
        }

        HorizontalDivider(color = glass.divider)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = glass.accentPrimary, modifier = Modifier.size(18.dp))
            Text("Luma Spectrum Studio", style = MaterialTheme.typography.labelLarge, color = glass.accentPrimary)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VisualizerStyle.entries.forEach { st ->
                FilterChip(
                    selected = settings.visualizerStyle == st,
                    onClick = { onSelectVisualizerStyle(st) },
                    label = { Text(st.label) },
                    colors = chipColors
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VisualizerPreset.entries.forEach { pr ->
                FilterChip(
                    selected = settings.visualizerPreset == pr,
                    onClick = { onSelectVisualizerPreset(pr) },
                    label = { Text(pr.label) },
                    colors = chipColors
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Iluminação reativa à música", style = MaterialTheme.typography.titleSmall)
            LumaAnimatedSwitch(
                checked = settings.reactiveLightingEnabled,
                onCheckedChange = onToggleReactiveLighting
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showSavePresetDialog) {
        var presetName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            containerColor = glass.surfaceElevated,
            title = { Text("Guardar Preset de Áudio") },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    singleLine = true,
                    label = { Text("Nome do preset") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = presetName.trim()
                        if (trimmed.isNotEmpty()) {
                            onSaveCustomPreset(
                                CustomAudioPreset(
                                    name = trimmed,
                                    bandLevelsMb = eq.studioTenBandsMb,
                                    bassStrength = eq.bassBoostStrength,
                                    virtualizerStrength = eq.virtualizerStrength,
                                    reverbPreset = eq.reverbPreset,
                                    loudnessGainMb = if (eq.volumeNormalizationEnabled) 450 else 0,
                                    stereoBalance = eq.stereoBalance
                                )
                            )
                            showSavePresetDialog = false
                        }
                    },
                    enabled = presetName.isNotBlank()
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun LyricsStudioSheetContent(
    track: MediaItemEntity,
    positionMs: Long,
    settings: AppSettings,
    onSeekTo: (Long) -> Unit,
    onImportLyrics: () -> Unit,
    onClearLyrics: () -> Unit,
    onSaveLyricsAndOffset: (Long, String?, Long) -> Unit,
    onUpdateFontScale: (Float) -> Unit,
    onToggleCentered: (Boolean) -> Unit,
    onOpenFullScreen: () -> Unit
) {
    val glass = LocalLumaGlass.current
    val parsed = remember(track.embeddedLyrics) { LrcParser.parse(track.embeddedLyrics) }
    val activeIdx = remember(parsed, positionMs, track.lyricsOffsetMs) {
        if (parsed.isTimed) LrcParser.findActiveLineIndex(parsed.lines, positionMs, track.lyricsOffsetMs) else -1
    }
    val listState = rememberLazyListState()
    var manualSyncMode by remember { mutableStateOf(false) }
    var syncCursor by remember(track.id) { mutableIntStateOf(0) }
    val editableLines = remember(track.id, track.embeddedLyrics) {
        mutableStateListOf<LyricLine>().apply { addAll(parsed.lines) }
    }

    LaunchedEffect(activeIdx) {
        if (!manualSyncMode && parsed.isTimed && activeIdx >= 0) {
            listState.animateScrollToItem((activeIdx - 2).coerceAtLeast(0))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lyrics, contentDescription = null, tint = glass.accentPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Luma Lyrics Studio", style = MaterialTheme.typography.titleLarge)
                val subtitle = buildString {
                    append(if (parsed.isTimed) "Karaoke LRC" else "Texto estático")
                    if (parsed.hasTranslations) append(" • Bilingue")
                    if (track.lyricsOffsetMs != 0L) append(" • Ajuste ${track.lyricsOffsetMs}ms")
                }
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = glass.accentPrimary)
            }
            if (parsed.lines.isNotEmpty()) {
                IconButton(onClick = onOpenFullScreen, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Rounded.Fullscreen, contentDescription = "Ecrã inteiro", tint = glass.textPrimary)
                }
            }
            OutlinedButton(onClick = onImportLyrics) {
                Icon(Icons.Rounded.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(".lrc")
            }
        }

        // Offset + Typography Toolbar
        if (parsed.lines.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(onClick = {
                    onSaveLyricsAndOffset(track.id, track.embeddedLyrics, track.lyricsOffsetMs - 500L)
                }) { Text("-0.5s") }
                OutlinedButton(onClick = {
                    onSaveLyricsAndOffset(track.id, track.embeddedLyrics, track.lyricsOffsetMs + 500L)
                }) { Text("+0.5s") }
                if (track.lyricsOffsetMs != 0L) {
                    IconButton(onClick = { onSaveLyricsAndOffset(track.id, track.embeddedLyrics, 0L) }) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = "Repor ajuste", modifier = Modifier.size(18.dp))
                    }
                }
                IconButton(onClick = { onUpdateFontScale((settings.lyricsFontScale - 0.12f).coerceIn(0.8f, 1.5f)) }) {
                    Icon(Icons.Rounded.ZoomOut, contentDescription = "Diminuir letra", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { onUpdateFontScale((settings.lyricsFontScale + 0.12f).coerceIn(0.8f, 1.5f)) }) {
                    Icon(Icons.Rounded.ZoomIn, contentDescription = "Aumentar letra", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { onToggleCentered(!settings.lyricsCentered) }) {
                    Icon(
                        imageVector = if (settings.lyricsCentered) Icons.Rounded.FormatAlignCenter else Icons.AutoMirrored.Rounded.FormatAlignLeft,
                        contentDescription = "Alinhamento",
                        tint = glass.accentPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                FilterChip(
                    selected = manualSyncMode,
                    onClick = {
                        manualSyncMode = !manualSyncMode
                        syncCursor = 0
                    },
                    leadingIcon = { Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(15.dp)) },
                    label = { Text("Sincronizar") }
                )
            }
        }

        if (manualSyncMode && editableLines.isNotEmpty()) {
            LiquidGlassSurface(shape = RoundedCornerShape(16.dp), elevated = true) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Modo Sincronia Manual: toque no botão à medida que ouve cada verso.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = glass.textSecondary
                    )
                    val targetLine = editableLines.getOrNull(syncCursor)
                    Text(
                        text = targetLine?.text ?: "Todas as linhas marcadas!",
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.accentPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (syncCursor in editableLines.indices) {
                                    editableLines[syncCursor] = editableLines[syncCursor].copy(timeMs = positionMs)
                                    if (syncCursor < editableLines.lastIndex) syncCursor++
                                }
                            },
                            enabled = syncCursor in editableLines.indices,
                            colors = ButtonDefaults.buttonColors(containerColor = glass.accentPrimary, contentColor = glass.onAccent)
                        ) {
                            Text("Marcar (${formatDuration(positionMs)})")
                        }
                        OutlinedButton(onClick = {
                            val serialized = LrcParser.serializeLrc(editableLines)
                            onSaveLyricsAndOffset(track.id, serialized, 0L)
                            manualSyncMode = false
                        }) {
                            Text("Guardar LRC")
                        }
                    }
                }
            }
        }

        if (parsed.lines.isEmpty()) {
            Text(
                text = "Este ficheiro não contém letras. Carregue um ficheiro .lrc ou .txt — linhas bilingues com \" | \" mostram tradução automática.",
                style = MaterialTheme.typography.bodyLarge,
                color = glass.textSecondary
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(parsed.lines) { idx, line ->
                    val isCurrent = parsed.isTimed && idx == activeIdx
                    val lineBg by animateColorAsState(
                        targetValue = if (isCurrent) glass.pillIndicatorBg else Color.Transparent,
                        animationSpec = tween(220),
                        label = "lyricLineBg"
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(lineBg)
                            .then(
                                if (line.timeMs != null) Modifier.clickable { onSeekTo((line.timeMs - track.lyricsOffsetMs).coerceAtLeast(0L)) }
                                else Modifier
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = if (settings.lyricsCentered) Alignment.CenterHorizontally else Alignment.Start
                    ) {
                        Text(
                            text = line.text,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = (if (isCurrent) 18f else 15f).sp * settings.lyricsFontScale,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isCurrent) glass.accentPrimary else glass.textPrimary,
                            textAlign = if (settings.lyricsCentered) TextAlign.Center else TextAlign.Start
                        )
                        if (!line.translatedText.isNullOrBlank()) {
                            Text(
                                text = line.translatedText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp * settings.lyricsFontScale
                                ),
                                color = glass.textSecondary,
                                textAlign = if (settings.lyricsCentered) TextAlign.Center else TextAlign.Start
                            )
                        }
                    }
                }
            }
            TextButton(onClick = onClearLyrics, modifier = Modifier.align(Alignment.End)) {
                Text("Remover letra associada", color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
fun FullScreenLyricsDialog(
    track: MediaItemEntity,
    positionMs: Long,
    settings: AppSettings,
    onSeekTo: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalLumaGlass.current
    val parsed = remember(track.embeddedLyrics) { LrcParser.parse(track.embeddedLyrics) }
    val activeIdx = remember(parsed, positionMs, track.lyricsOffsetMs) {
        if (parsed.isTimed) LrcParser.findActiveLineIndex(parsed.lines, positionMs, track.lyricsOffsetMs) else -1
    }
    val listState = rememberLazyListState()

    LaunchedEffect(activeIdx) {
        if (parsed.isTimed && activeIdx >= 0) {
            listState.animateScrollToItem((activeIdx - 2).coerceAtLeast(0))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        LiquidGlassBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(track.title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = glass.textSecondary, maxLines = 1)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Rounded.Close, contentDescription = "Fechar ecrã inteiro", tint = glass.textPrimary)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(parsed.lines) { idx, line ->
                        val isCurrent = parsed.isTimed && idx == activeIdx
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isCurrent) glass.pillIndicatorBg else Color.Transparent)
                                .then(
                                    if (line.timeMs != null) Modifier.clickable { onSeekTo((line.timeMs - track.lyricsOffsetMs).coerceAtLeast(0L)) }
                                    else Modifier
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = if (settings.lyricsCentered) Alignment.CenterHorizontally else Alignment.Start
                        ) {
                            Text(
                                text = line.text,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = (if (isCurrent) 24f else 18f).sp * settings.lyricsFontScale,
                                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium
                                ),
                                color = if (isCurrent) glass.accentPrimary else glass.textPrimary.copy(alpha = 0.72f),
                                textAlign = if (settings.lyricsCentered) TextAlign.Center else TextAlign.Start
                            )
                            if (!line.translatedText.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = line.translatedText,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp * settings.lyricsFontScale),
                                    color = glass.textSecondary,
                                    textAlign = if (settings.lyricsCentered) TextAlign.Center else TextAlign.Start
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
