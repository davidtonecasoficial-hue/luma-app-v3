package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.config.AppConfig
import com.example.data.media.FormatCapability
import com.example.data.model.AlbumSummary
import com.example.data.model.ArtistSummary
import com.example.data.model.FolderSummary
import com.example.data.model.HistoryWithMedia
import com.example.data.model.MediaItemEntity
import com.example.data.model.MoodType
import com.example.data.model.PlaylistWithTracks
import com.example.player.PlaybackUiState
import com.example.ui.components.AnimatedLumaMonogramLogo
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.TrackListItem
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LocalLumaGlass
import com.example.ui.theme.MediaArtwork
import com.example.ui.theme.formatDuration
import java.text.DateFormat
import java.util.Date

@Composable
fun CollectionDetailScreen(
    title: String,
    subtitle: String,
    artworkPath: String?,
    tracks: List<MediaItemEntity>,
    playbackState: PlaybackUiState,
    bottomInsetPadding: Dp,
    onBack: () -> Unit,
    onPlayAll: (List<MediaItemEntity>) -> Unit,
    onShuffleAll: (List<MediaItemEntity>) -> Unit,
    onPlayTrack: (MediaItemEntity, List<MediaItemEntity>) -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit,
    onPlayNext: (MediaItemEntity) -> Unit,
    onAddToQueue: (MediaItemEntity) -> Unit,
    onSmartMix: (MediaItemEntity) -> Unit,
    onAddToPlaylist: (MediaItemEntity) -> Unit,
    onRemoveFromLibrary: (MediaItemEntity) -> Unit,
    onEditMetadata: (MediaItemEntity) -> Unit = {},
    reorderingEnabled: Boolean = false,
    onMoveTrack: ((Int, Int) -> Unit)? = null,
    extraHeaderActions: @Composable (() -> Unit)? = null
) {
    BackHandler { onBack() }
    val glass = LocalLumaGlass.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = 10.dp,
            bottom = bottomInsetPadding + 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                extraHeaderActions?.invoke()
            }
        }

        item {
            LiquidGlassSurface(shape = RoundedCornerShape(24.dp), elevated = true) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MediaArtwork(
                            artworkPath = artworkPath,
                            contentDescription = title,
                            cornerRadius = 18.dp,
                            modifier = Modifier.size(84.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (tracks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onPlayAll(tracks) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = glass.accentPrimary,
                                    contentColor = glass.onAccent
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reproduzir")
                            }
                            OutlinedButton(
                                onClick = { onShuffleAll(tracks) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Aleatório")
                            }
                        }
                    }
                }
            }
        }

        if (tracks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sem faixas disponíveis nesta coleção.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (reorderingEnabled && onMoveTrack != null) {
            itemsIndexed(tracks, key = { _, track -> "reorder_${track.id}" }) { index, track ->
                LiquidGlassSurface(shape = RoundedCornerShape(18.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            color = glass.accentPrimary,
                            modifier = Modifier.width(26.dp)
                        )
                        MediaArtwork(
                            artworkPath = track.artworkPath,
                            contentDescription = track.title,
                            cornerRadius = 10.dp,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { onMoveTrack(index, index - 1) },
                            enabled = index > 0,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("playlist_move_up_$index")
                        ) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = "Mover para cima")
                        }
                        IconButton(
                            onClick = { onMoveTrack(index, index + 1) },
                            enabled = index < tracks.lastIndex,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("playlist_move_down_$index")
                        ) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = "Mover para baixo")
                        }
                        IconButton(
                            onClick = { onRemoveFromLibrary(track) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = "Remover da playlist",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        } else {
            items(tracks, key = { "det_${it.id}" }) { track ->
                TrackListItem(
                    item = track,
                    isPlayingNow = playbackState.currentTrack?.id == track.id,
                    onPlay = { onPlayTrack(track, tracks) },
                    onPlayNext = { onPlayNext(track) },
                    onAddToQueue = { onAddToQueue(track) },
                    onToggleFavorite = { onToggleFavorite(track) },
                    onSmartMix = { onSmartMix(track) },
                    onAddToPlaylist = { onAddToPlaylist(track) },
                    onEditMetadata = { onEditMetadata(track) },
                    onRemoveFromLibrary = { onRemoveFromLibrary(track) }
                )
            }
        }
    }
}

@Composable
fun PlaylistDetailScreen(
    playlist: PlaylistWithTracks,
    playbackState: PlaybackUiState,
    bottomInsetPadding: Dp,
    onBack: () -> Unit,
    onPlayAll: (List<MediaItemEntity>) -> Unit,
    onShuffleAll: (List<MediaItemEntity>) -> Unit,
    onPlayTrack: (MediaItemEntity, List<MediaItemEntity>) -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit,
    onRenamePlaylist: (String) -> Unit,
    onDeletePlaylist: () -> Unit,
    onRemoveTrackFromPlaylist: (Long) -> Unit,
    onMoveTrack: (Int, Int) -> Unit = { _, _ -> },
    onEditMetadata: (MediaItemEntity) -> Unit = {}
) {
    var showRename by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isReordering by remember { mutableStateOf(false) }

    if (showRename) {
        CreatePlaylistDialog(
            initialName = playlist.playlist.name,
            title = "Renomear Playlist",
            confirmLabel = "Guardar",
            onDismiss = { showRename = false },
            onConfirm = { newName ->
                showRename = false
                onRenamePlaylist(newName)
            }
        )
    }

    if (showDeleteConfirm) {
        ConfirmationDialog(
            title = "Eliminar playlist",
            message = "Pretende eliminar a playlist \"${playlist.playlist.name}\"? As músicas da biblioteca não serão apagadas.",
            confirmLabel = "Eliminar",
            onConfirm = {
                showDeleteConfirm = false
                onDeletePlaylist()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    CollectionDetailScreen(
        title = playlist.playlist.name,
        subtitle = "${playlist.tracks.size} faixa(s) • ${formatDuration(playlist.tracks.sumOf { it.durationMs })}",
        artworkPath = playlist.tracks.firstOrNull { it.artworkPath != null }?.artworkPath,
        tracks = playlist.tracks,
        playbackState = playbackState,
        bottomInsetPadding = bottomInsetPadding,
        onBack = {
            if (isReordering) isReordering = false else onBack()
        },
        onPlayAll = onPlayAll,
        onShuffleAll = onShuffleAll,
        onPlayTrack = onPlayTrack,
        onToggleFavorite = onToggleFavorite,
        onPlayNext = {},
        onAddToQueue = {},
        onSmartMix = {},
        onAddToPlaylist = {},
        onEditMetadata = onEditMetadata,
        onRemoveFromLibrary = { track -> onRemoveTrackFromPlaylist(track.id) },
        reorderingEnabled = isReordering,
        onMoveTrack = onMoveTrack,
        extraHeaderActions = {
            if (playlist.tracks.size > 1) {
                IconButton(
                    onClick = { isReordering = !isReordering },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("playlist_reorder_toggle")
                ) {
                    Icon(
                        imageVector = if (isReordering) Icons.Rounded.Check else Icons.Rounded.SwapVert,
                        contentDescription = if (isReordering) "Concluir ordenação" else "Reordenar faixas",
                        tint = if (isReordering) LocalLumaGlass.current.accentPrimary else MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            IconButton(onClick = { showRename = true }) {
                Icon(Icons.Rounded.Edit, contentDescription = "Renomear playlist")
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar playlist", tint = MaterialTheme.colorScheme.error)
            }
        }
    )
}

@Composable
fun HistoryScreen(
    history: List<HistoryWithMedia>,
    bottomInsetPadding: Dp,
    onBack: () -> Unit,
    onPlayMedia: (MediaItemEntity) -> Unit,
    onDeleteEntry: (Long) -> Unit,
    onClearAll: () -> Unit
) {
    BackHandler { onBack() }
    var showClearConfirm by remember { mutableStateOf(false) }

    if (showClearConfirm) {
        ConfirmationDialog(
            title = "Limpar histórico",
            message = "Apagar todas as entradas do histórico de reprodução?",
            confirmLabel = "Limpar",
            onConfirm = {
                showClearConfirm = false
                onClearAll()
            },
            onDismiss = { showClearConfirm = false }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = 10.dp,
            bottom = bottomInsetPadding + 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                }
                Text("Histórico", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                if (history.isNotEmpty()) {
                    TextButton(onClick = { showClearConfirm = true }) {
                        Text("Limpar tudo", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        if (history.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhuma reprodução registada ainda.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            items(history, key = { it.history.id }) { entry ->
                val media = entry.media
                if (media != null) {
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(18.dp),
                        onClick = { onPlayMedia(media) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MediaArtwork(
                                artworkPath = media.artworkPath,
                                contentDescription = media.title,
                                cornerRadius = 12.dp,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(media.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = "${media.artist} • ${formatter.format(Date(entry.history.playedAt))}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { onDeleteEntry(entry.history.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remover do histórico")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SupportedFormatsScreen(
    codecs: List<FormatCapability>,
    bottomInsetPadding: Dp,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val glass = LocalLumaGlass.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = 10.dp,
            bottom = bottomInsetPadding + 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                }
                Column {
                    Text("Formatos Suportados", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "Verificado diretamente nos descodificadores do dispositivo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(codecs, key = { "${it.isVideo}_${it.mimeType}" }) { cap ->
            LiquidGlassSurface(shape = RoundedCornerShape(18.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (cap.isSupported) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = if (cap.isSupported) glass.success else glass.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${cap.formatName} (${if (cap.isVideo) "Vídeo" else "Áudio"})",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = cap.note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (cap.decoderNames.isNotEmpty()) {
                            Text(
                                text = cap.decoderNames.joinToString(", "),
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.accentPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AboutScreen(
    bottomInsetPadding: Dp,
    onBack: () -> Unit,
    onOpenSupportedFormats: () -> Unit
) {
    BackHandler { onBack() }
    val glass = LocalLumaGlass.current
    val strings = com.example.ui.theme.LocalLumaStrings.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("about_screen"),
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = 10.dp,
            bottom = bottomInsetPadding + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.aboutTitle, style = MaterialTheme.typography.headlineMedium)
            }
        }

        // Identidade e Versão da Aplicação
        item {
            LiquidGlassSurface(
                shape = RoundedCornerShape(24.dp),
                elevated = true,
                accentGlow = glass.accentPrimary
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedLumaMonogramLogo(
                        medallionSize = 84.dp,
                        reduceMotion = glass.reduceMotion
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = AppConfig.APP_NAME,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${strings.aboutVersionPrefix} ${AppConfig.APP_VERSION}",
                        style = MaterialTheme.typography.labelLarge,
                        color = glass.accentPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Feito para transformar a sua biblioteca de música e vídeo numa experiência leve, envolvente e sem complicações.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Informações do Programador e Desenvolvimento
        item {
            LiquidGlassSurface(shape = RoundedCornerShape(22.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = strings.aboutDeveloperCardTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.accentPrimary
                    )
                    AboutInfoRow("Autor & Créditos", strings.aboutDeveloperCredit)
                    AboutInfoRow("Tecnologia", "Android Nativo (Kotlin, Jetpack Compose, Media3)")
                    AboutInfoRow("Armazenamento & Preferências", "Room Database + DataStore")
                    AboutInfoRow("Versão Atual", AppConfig.APP_VERSION)
                }
            }
        }

        // Privacidade e Letras
        item {
            LiquidGlassSurface(shape = RoundedCornerShape(22.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Privacidade e Controlo",
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.accentPrimary
                    )
                    Text(
                        text = "As suas músicas, vídeos, listas de reprodução e ajustes ficam guardados com segurança no seu dispositivo. A ligação à internet é usada apenas quando opta por pesquisar letras de músicas online.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Para organizar a sua coleção automaticamente, o Luma utiliza apenas as permissões essenciais de áudio e vídeo do sistema.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Bibliotecas, Licenças e Compatibilidade
        item {
            LiquidGlassSurface(shape = RoundedCornerShape(22.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = strings.aboutLibrariesTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.accentPrimary
                    )
                    AboutInfoRow("AndroidX Media3 / ExoPlayer", "Licença Apache 2.0")
                    AboutInfoRow("Jetpack Compose & Material 3", "Licença Apache 2.0")
                    AboutInfoRow("AndroidX Room & DataStore", "Licença Apache 2.0")
                    AboutInfoRow("Coil Image Loading", "Licença Apache 2.0")
                    AboutInfoRow("Kotlin Coroutines", "Licença Apache 2.0")
                    AboutInfoRow("Fontes Outfit, Plus Jakarta Sans & JetBrains Mono", "SIL Open Font License")
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = onOpenSupportedFormats,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("about_supported_formats_button")
                    ) {
                        Text("Ver formatos suportados pelo dispositivo")
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutInfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

