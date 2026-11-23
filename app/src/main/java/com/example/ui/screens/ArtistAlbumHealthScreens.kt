package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.media.LibraryHealthReport
import com.example.data.model.AlbumSummary
import com.example.data.model.ArtistSummary
import com.example.data.model.CustomCollectionWithTracks
import com.example.data.model.MediaItemEntity
import com.example.player.PlaybackUiState
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.CreateEditCollectionDialog
import com.example.ui.components.TrackListItem
import com.example.ui.components.collectionAccentColor
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LocalLumaGlass
import com.example.ui.theme.MediaArtwork
import com.example.ui.theme.formatDuration

@Composable
fun ArtistDetailScreen(
    artistName: String,
    artist: ArtistSummary?,
    artistAlbums: List<AlbumSummary>,
    relatedArtists: List<ArtistSummary>,
    playbackState: PlaybackUiState,
    bottomInsetPadding: Dp,
    onBack: () -> Unit,
    onPlayAll: (List<MediaItemEntity>) -> Unit,
    onShuffleAll: (List<MediaItemEntity>) -> Unit,
    onPlayTrack: (MediaItemEntity, List<MediaItemEntity>) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenRelatedArtist: (String) -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit,
    onPlayNext: (MediaItemEntity) -> Unit,
    onAddToQueue: (MediaItemEntity) -> Unit,
    onSmartMix: (MediaItemEntity) -> Unit,
    onAddToPlaylist: (MediaItemEntity) -> Unit,
    onAddToCollection: (MediaItemEntity) -> Unit,
    onEditMetadata: (MediaItemEntity) -> Unit,
    onOpenInStudio: (MediaItemEntity) -> Unit,
    onRemoveFromLibrary: (MediaItemEntity) -> Unit
) {
    BackHandler { onBack() }
    val glass = LocalLumaGlass.current
    val tracks = artist?.tracks.orEmpty()
    val topTracks = remember(tracks) {
        tracks.sortedWith(compareByDescending<MediaItemEntity> { it.playCount }.thenByDescending { it.isFavorite })
            .take(5)
    }
    val totalPlays = remember(tracks) { tracks.sumOf { it.playCount } }
    val totalDuration = remember(tracks) { tracks.sumOf { it.durationMs } }
    val genresLabel = remember(tracks) {
        tracks.mapNotNull { it.genre.trim().takeIf(String::isNotBlank) }.distinct().take(3).joinToString(" • ")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("artist_detail_screen"),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = bottomInsetPadding + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Artista",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            LiquidGlassSurface(shape = RoundedCornerShape(26.dp), elevated = true, accentGlow = glass.accentPrimary) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MediaArtwork(
                            artworkPath = artist?.artworkPath,
                            contentDescription = artistName,
                            cornerRadius = 46.dp,
                            modifier = Modifier.size(92.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = artist?.name ?: artistName,
                                style = MaterialTheme.typography.headlineMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${tracks.size} faixa(s) • ${artistAlbums.size} álbum(ns) • ${formatDuration(totalDuration)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = glass.textSecondary
                            )
                            if (genresLabel.isNotBlank() || totalPlays > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = listOfNotNull(
                                        genresLabel.takeIf { it.isNotBlank() },
                                        if (totalPlays > 0) "$totalPlays reproduções" else null
                                    ).joinToString(" • "),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = glass.accentPrimary
                                )
                            }
                        }
                    }
                    if (tracks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { onPlayAll(tracks) },
                                colors = ButtonDefaults.buttonColors(containerColor = glass.accentPrimary, contentColor = glass.onAccent),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tocar Artista")
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

        if (artistAlbums.isNotEmpty()) {
            item {
                Text("Álbuns & Lançamentos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(artistAlbums, key = { it.name }) { album ->
                        LiquidGlassSurface(
                            shape = RoundedCornerShape(18.dp),
                            onClick = { onOpenAlbum(album.name) },
                            modifier = Modifier.width(150.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                MediaArtwork(
                                    artworkPath = album.artworkPath,
                                    contentDescription = album.name,
                                    cornerRadius = 14.dp,
                                    maxDecodeSize = 180.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(album.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = "${album.trackCount} faixa(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        if (topTracks.isNotEmpty() && tracks.size > 3) {
            item {
                Text("Destaques do Artista", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
            }
            itemsIndexed(topTracks, key = { _, t -> "artist_top_${t.id}" }) { index, track ->
                LiquidGlassSurface(
                    shape = RoundedCornerShape(16.dp),
                    onClick = { onPlayTrack(track, topTracks) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = glass.accentPrimary,
                            modifier = Modifier.width(24.dp)
                        )
                        MediaArtwork(
                            artworkPath = track.artworkPath,
                            contentDescription = track.title,
                            cornerRadius = 10.dp,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = "${track.album} • ${formatDuration(track.durationMs)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (track.playCount > 0) {
                            Text(
                                text = "${track.playCount}x",
                                style = MaterialTheme.typography.labelMedium,
                                color = glass.accentPrimary
                            )
                        }
                    }
                }
            }
        }

        if (relatedArtists.isNotEmpty()) {
            item {
                Text("Artistas Semelhantes", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(relatedArtists, key = { it.name }) { rel ->
                        LiquidGlassSurface(
                            shape = RoundedCornerShape(18.dp),
                            onClick = { onOpenRelatedArtist(rel.name) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MediaArtwork(
                                    artworkPath = rel.artworkPath,
                                    contentDescription = rel.name,
                                    cornerRadius = 18.dp,
                                    modifier = Modifier.size(34.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(rel.name, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("Todas as Músicas (${tracks.size})", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
        }

        items(tracks, key = { "artist_all_${it.id}" }) { track ->
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
                onRemoveFromLibrary = { onRemoveFromLibrary(track) },
                extraMenuActions = { close ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Adicionar a Coleção") },
                        leadingIcon = { Icon(Icons.Rounded.BookmarkAdd, contentDescription = null) },
                        onClick = { close(); onAddToCollection(track) }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Abrir no Luma Studio") },
                        leadingIcon = { Icon(Icons.Rounded.GraphicEq, contentDescription = null) },
                        onClick = { close(); onOpenInStudio(track) }
                    )
                }
            )
        }
    }
}

@Composable
fun AlbumDetailScreen(
    albumName: String,
    album: AlbumSummary?,
    moreByArtist: List<AlbumSummary>,
    playbackState: PlaybackUiState,
    bottomInsetPadding: Dp,
    onBack: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onPlayAll: (List<MediaItemEntity>) -> Unit,
    onShuffleAll: (List<MediaItemEntity>) -> Unit,
    onPlayTrack: (MediaItemEntity, List<MediaItemEntity>) -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit,
    onPlayNext: (MediaItemEntity) -> Unit,
    onAddToQueue: (MediaItemEntity) -> Unit,
    onSmartMix: (MediaItemEntity) -> Unit,
    onAddToPlaylist: (MediaItemEntity) -> Unit,
    onAddToCollection: (MediaItemEntity) -> Unit,
    onEditMetadata: (MediaItemEntity) -> Unit,
    onOpenInStudio: (MediaItemEntity) -> Unit,
    onRemoveFromLibrary: (MediaItemEntity) -> Unit
) {
    BackHandler { onBack() }
    val glass = LocalLumaGlass.current
    val tracks = album?.tracks.orEmpty()
    val releaseYear = remember(tracks) { tracks.mapNotNull { it.releaseYear.takeIf { y -> y > 0 } }.maxOrNull() }
    val genresLabel = remember(tracks) {
        tracks.mapNotNull { it.genre.trim().takeIf(String::isNotBlank) }.distinct().take(2).joinToString(" • ")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("album_detail_screen"),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = bottomInsetPadding + 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("Álbum", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            }
        }

        item {
            LiquidGlassSurface(shape = RoundedCornerShape(26.dp), elevated = true, accentGlow = glass.accentPrimary) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MediaArtwork(
                            artworkPath = album?.artworkPath,
                            contentDescription = albumName,
                            cornerRadius = 20.dp,
                            modifier = Modifier.size(104.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = album?.name ?: albumName,
                                style = MaterialTheme.typography.headlineMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (!album?.artist.isNullOrBlank()) {
                                Text(
                                    text = album!!.artist,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = glass.accentPrimary,
                                    modifier = Modifier.clickable { onOpenArtist(album.artist) }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val metaBits = buildList {
                                if (releaseYear != null) add(releaseYear.toString())
                                if (genresLabel.isNotBlank()) add(genresLabel)
                                add("${tracks.size} faixa(s)")
                                add(formatDuration(album?.totalDurationMs ?: tracks.sumOf { it.durationMs }))
                            }
                            Text(
                                text = metaBits.joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary
                            )
                        }
                    }

                    if (tracks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { onPlayAll(tracks) },
                                colors = ButtonDefaults.buttonColors(containerColor = glass.accentPrimary, contentColor = glass.onAccent),
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

        itemsIndexed(tracks, key = { _, t -> "alb_trk_${t.id}" }) { index, track ->
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
                onRemoveFromLibrary = { onRemoveFromLibrary(track) },
                leadingLabel = if (track.trackNumber > 0) track.trackNumber.toString() else "${index + 1}",
                extraMenuActions = { close ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Adicionar a Coleção") },
                        leadingIcon = { Icon(Icons.Rounded.BookmarkAdd, contentDescription = null) },
                        onClick = { close(); onAddToCollection(track) }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Abrir no Luma Studio") },
                        leadingIcon = { Icon(Icons.Rounded.GraphicEq, contentDescription = null) },
                        onClick = { close(); onOpenInStudio(track) }
                    )
                }
            )
        }

        if (moreByArtist.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Mais de ${album?.artist ?: "Artista"}", style = MaterialTheme.typography.titleMedium)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(moreByArtist, key = { "more_alb_${it.name}" }) { other ->
                        LiquidGlassSurface(
                            shape = RoundedCornerShape(18.dp),
                            onClick = { onOpenAlbum(other.name) },
                            modifier = Modifier.width(142.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                MediaArtwork(
                                    artworkPath = other.artworkPath,
                                    contentDescription = other.name,
                                    cornerRadius = 12.dp,
                                    maxDecodeSize = 160.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(other.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${other.trackCount} faixa(s)", style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomCollectionDetailScreen(
    collection: CustomCollectionWithTracks,
    playbackState: PlaybackUiState,
    bottomInsetPadding: Dp,
    onBack: () -> Unit,
    onPlayAll: (List<MediaItemEntity>) -> Unit,
    onShuffleAll: (List<MediaItemEntity>) -> Unit,
    onPlayTrack: (MediaItemEntity, List<MediaItemEntity>) -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit,
    onTogglePinned: (Boolean) -> Unit,
    onEditCollectionMeta: (name: String, description: String, accentTag: String) -> Unit,
    onDeleteCollection: () -> Unit,
    onRemoveTrack: (Long) -> Unit,
    onEditMetadata: (MediaItemEntity) -> Unit
) {
    val accentColor = collectionAccentColor(collection.collection.accentTag)
    var showEdit by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showEdit) {
        CreateEditCollectionDialog(
            initialName = collection.collection.name,
            initialDescription = collection.collection.description,
            initialAccentTag = collection.collection.accentTag,
            dialogTitle = "Editar Coleção",
            confirmLabel = "Guardar",
            onDismiss = { showEdit = false },
            onConfirm = { name, desc, tag ->
                showEdit = false
                onEditCollectionMeta(name, desc, tag)
            }
        )
    }

    if (showDeleteConfirm) {
        ConfirmationDialog(
            title = "Eliminar coleção",
            message = "Pretende remover a coleção \"${collection.collection.name}\"? As músicas originais continuam intactas no dispositivo.",
            confirmLabel = "Eliminar",
            onConfirm = {
                showDeleteConfirm = false
                onDeleteCollection()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    val subtitle = buildString {
        if (collection.collection.description.isNotBlank()) {
            append(collection.collection.description)
            append(" • ")
        }
        append("${collection.tracks.size} faixa(s) • ${formatDuration(collection.tracks.sumOf { it.durationMs })}")
    }

    CollectionDetailScreen(
        title = collection.collection.name,
        subtitle = subtitle,
        artworkPath = collection.tracks.firstOrNull { it.artworkPath != null }?.artworkPath,
        tracks = collection.tracks,
        playbackState = playbackState,
        bottomInsetPadding = bottomInsetPadding,
        onBack = onBack,
        onPlayAll = onPlayAll,
        onShuffleAll = onShuffleAll,
        onPlayTrack = onPlayTrack,
        onToggleFavorite = onToggleFavorite,
        onPlayNext = {},
        onAddToQueue = {},
        onSmartMix = {},
        onAddToPlaylist = {},
        onEditMetadata = onEditMetadata,
        onRemoveFromLibrary = { track -> onRemoveTrack(track.id) },
        extraHeaderActions = {
            IconButton(onClick = { onTogglePinned(!collection.collection.isPinned) }) {
                Icon(
                    imageVector = Icons.Rounded.PushPin,
                    contentDescription = if (collection.collection.isPinned) "Desafixar coleção" else "Fixar coleção",
                    tint = if (collection.collection.isPinned) accentColor else MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(onClick = { showEdit = true }) {
                Icon(Icons.Rounded.Edit, contentDescription = "Editar coleção")
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar coleção", tint = MaterialTheme.colorScheme.error)
            }
        }
    )
}

@Composable
fun LibraryHealthScreen(
    health: LibraryHealthReport,
    bottomInsetPadding: Dp,
    onBack: () -> Unit,
    onReorganizeTags: () -> Unit,
    onOpenArtworkScanner: () -> Unit,
    onOpenDuplicatesInspector: () -> Unit,
    onOpenFileIntelligence: () -> Unit,
    onCleanInaccessibleIndex: () -> Unit
) {
    BackHandler { onBack() }
    val glass = LocalLumaGlass.current
    var showCleanConfirm by remember { mutableStateOf(false) }

    if (showCleanConfirm) {
        ConfirmationDialog(
            title = "Limpar registos indisponíveis",
            message = "Remover da biblioteca os ${health.inaccessibleCount} atalho(s) cujo ficheiro já não se encontra acessível? Nenhum ficheiro do dispositivo será apagado.",
            confirmLabel = "Limpar índices",
            onConfirm = {
                showCleanConfirm = false
                onCleanInaccessibleIndex()
            },
            onDismiss = { showCleanConfirm = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("library_health_screen"),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = bottomInsetPadding + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("Saúde da Biblioteca", style = MaterialTheme.typography.headlineMedium)
            }
        }

        item {
            LiquidGlassSurface(shape = RoundedCornerShape(24.dp), elevated = true, accentGlow = glass.accentPrimary) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(glass.pillIndicatorBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.HealthAndSafety, contentDescription = null, tint = glass.accentPrimary, modifier = Modifier.size(30.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Índice Geral de Saúde", style = MaterialTheme.typography.labelLarge, color = glass.textSecondary)
                            Text(
                                text = "${health.healthScore}%",
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                                color = glass.accentPrimary
                            )
                            Text(
                                text = "${health.totalAudioTracks} músicas • ${health.totalVideoItems} vídeos",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary
                            )
                        }
                    }

                    HealthBarRow("Capas associadas", health.artworkCoveragePercent)
                    HealthBarRow("Metadados completos", health.metadataCoveragePercent)
                    HealthBarRow("Letras guardadas", health.lyricsCoveragePercent)
                }
            }
        }

        item {
            LiquidGlassSurface(
                shape = RoundedCornerShape(20.dp),
                onClick = onOpenArtworkScanner,
                modifier = Modifier.testTag("health_artwork_scanner_card")
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ImageSearch, contentDescription = null, tint = glass.accentPrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Scanner de Capas em Falta", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (health.missingArtworkTracks.isEmpty()) "Todas as faixas têm capa." else "${health.missingArtworkTracks.size} música(s) sem capa",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                    }
                    Text("Abrir", style = MaterialTheme.typography.labelLarge, color = glass.accentPrimary)
                }
            }
        }

        item {
            LiquidGlassSurface(
                shape = RoundedCornerShape(20.dp),
                onClick = onReorganizeTags
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, tint = glass.accentPrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Normalizar Metadados & Artistas", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (health.missingMetadataTracks.isEmpty()) "Tags bem estruturadas." else "${health.missingMetadataTracks.size} faixa(s) com campos incompletos",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                    }
                    Text("Organizar", style = MaterialTheme.typography.labelLarge, color = glass.accentPrimary)
                }
            }
        }

        item {
            LiquidGlassSurface(
                shape = RoundedCornerShape(20.dp),
                onClick = onOpenDuplicatesInspector
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = glass.accentPrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Detetor de Duplicados", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (health.duplicateGroupCount == 0) "Sem duplicados detetados." else "${health.duplicateGroupCount} grupo(s) (${health.duplicateExtraTracksCount} extra)",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                    }
                    Text("Rever", style = MaterialTheme.typography.labelLarge, color = glass.accentPrimary)
                }
            }
        }

        if (health.inaccessibleCount > 0) {
            item {
                LiquidGlassSurface(
                    shape = RoundedCornerShape(20.dp),
                    onClick = { showCleanConfirm = true }
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.FolderOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ficheiros Inacessíveis", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${health.inaccessibleCount} atalho(s) não encontrados no armazenamento.",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary
                            )
                        }
                        Text("Limpar", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onOpenFileIntelligence,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Abrir Inteligência de Ficheiros & Armazenamento")
            }
        }
    }
}

@Composable
private fun HealthBarRow(label: String, percent: Int) {
    val glass = LocalLumaGlass.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
            Text("$percent%", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = glass.textPrimary)
        }
        LinearProgressIndicator(
            progress = { (percent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color = glass.accentPrimary,
            trackColor = glass.textPrimary.copy(alpha = 0.09f)
        )
    }
}
