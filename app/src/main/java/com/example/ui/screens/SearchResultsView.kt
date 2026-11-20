package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.media.SearchResultsBundle
import com.example.data.model.MediaItemEntity
import com.example.data.model.SearchFilter
import com.example.player.PlaybackUiState
import com.example.ui.components.TrackListItem
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LocalLumaGlass
import com.example.ui.theme.MediaArtwork
import com.example.ui.theme.formatDuration

@Composable
fun SearchResultsView(
    query: String,
    activeFilter: SearchFilter,
    results: SearchResultsBundle,
    allMediaCount: Int,
    isScanning: Boolean,
    errorMessage: String?,
    playbackState: PlaybackUiState,
    bottomInsetPadding: Dp,
    onRetryRefresh: () -> Unit,
    onImportFiles: () -> Unit,
    onSelectFilter: (SearchFilter) -> Unit,
    onPlaySong: (MediaItemEntity, List<MediaItemEntity>) -> Unit,
    onPlayVideo: (MediaItemEntity) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onPlayNext: (MediaItemEntity) -> Unit,
    onAddToQueue: (MediaItemEntity) -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit,
    onSmartMix: (MediaItemEntity) -> Unit,
    onAddToPlaylist: (MediaItemEntity) -> Unit,
    onRemoveFromLibrary: (MediaItemEntity) -> Unit,
    onEditMetadata: (MediaItemEntity) -> Unit = {}
) {
    val glass = LocalLumaGlass.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("search_results_view")
    ) {
        // Search Header with Active Filter badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pesquisa",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Filtro: ${activeFilter.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = glass.accentPrimary
                )
            }
            if (isScanning) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = glass.accentPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("A atualizar...", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // State 1: Error state with recovery action
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                LiquidGlassSurface(
                    shape = RoundedCornerShape(22.dp),
                    elevated = true,
                    accentGlow = MaterialTheme.colorScheme.error
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onRetryRefresh,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = glass.accentPrimary,
                                contentColor = glass.onAccent
                            )
                        ) {
                            Text("Verificar biblioteca")
                        }
                    }
                }
            }
        }

        // State 2: Empty Library
        if (allMediaCount == 0 && !isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = bottomInsetPadding),
                contentAlignment = Alignment.Center
            ) {
                LiquidGlassSurface(shape = RoundedCornerShape(24.dp), elevated = true) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_empty_library),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(18.dp))
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Biblioteca vazia",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ainda não foram encontrados ficheiros multimédia locais. Importe músicas ou vídeos para pesquisar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onImportFiles,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = glass.accentPrimary,
                                contentColor = glass.onAccent
                            )
                        ) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Importar ficheiros")
                        }
                    }
                }
            }
            return
        }

        // State 3: Initial empty query state
        if (query.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = bottomInsetPadding),
                contentAlignment = Alignment.Center
            ) {
                LiquidGlassSurface(shape = RoundedCornerShape(24.dp)) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(glass.pillIndicatorBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = glass.accentPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Pesquise na sua biblioteca",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Encontre instantaneamente $allMediaCount ficheiro(s) por título, artista, álbum, pasta ou nome de vídeo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Dica: toque no botão circular à esquerda do campo para filtrar.",
                            style = MaterialTheme.typography.labelMedium,
                            color = glass.accentPrimary
                        )
                    }
                }
            }
            return
        }

        // State 4: No matches
        if (results.isEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = bottomInsetPadding),
                contentAlignment = Alignment.Center
            ) {
                LiquidGlassSurface(shape = RoundedCornerShape(24.dp)) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Sem resultados",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Não foram encontradas correspondências para \"$query\" em ${activeFilter.label}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (activeFilter != SearchFilter.ALL) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onSelectFilter(SearchFilter.ALL) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = glass.accentPrimary,
                                    contentColor = glass.onAccent
                                )
                            ) {
                                Text("Pesquisar em Tudo")
                            }
                        }
                    }
                }
            }
            return
        }

        // State 5: Organized Offline Results (Songs, Albums, Artists, Videos, Playlists)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 4.dp,
                bottom = bottomInsetPadding + 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (results.songs.isNotEmpty()) {
                item {
                    SearchGroupHeader("Músicas (${results.songs.size})")
                }
                items(results.songs, key = { "s_${it.id}" }) { song ->
                    TrackListItem(
                        item = song,
                        isPlayingNow = playbackState.currentTrack?.id == song.id,
                        onPlay = { onPlaySong(song, results.songs) },
                        onPlayNext = { onPlayNext(song) },
                        onAddToQueue = { onAddToQueue(song) },
                        onToggleFavorite = { onToggleFavorite(song) },
                        onSmartMix = { onSmartMix(song) },
                        onAddToPlaylist = { onAddToPlaylist(song) },
                        onEditMetadata = { onEditMetadata(song) },
                        onRemoveFromLibrary = { onRemoveFromLibrary(song) }
                    )
                }
            }

            if (results.albums.isNotEmpty()) {
                item {
                    SearchGroupHeader("Álbuns (${results.albums.size})")
                }
                items(results.albums, key = { "al_${it.name}" }) { album ->
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(18.dp),
                        onClick = { onOpenAlbum(album.name) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MediaArtwork(
                                artworkPath = album.artworkPath,
                                contentDescription = album.name,
                                cornerRadius = 12.dp,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = album.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${album.artist} • ${album.trackCount} faixa(s)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(Icons.Rounded.Album, contentDescription = null, tint = glass.accentPrimary)
                        }
                    }
                }
            }

            if (results.artists.isNotEmpty()) {
                item {
                    SearchGroupHeader("Artistas (${results.artists.size})")
                }
                items(results.artists, key = { "ar_${it.name}" }) { artist ->
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(18.dp),
                        onClick = { onOpenArtist(artist.name) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MediaArtwork(
                                artworkPath = artist.artworkPath,
                                contentDescription = artist.name,
                                cornerRadius = 26.dp,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = artist.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${artist.trackCount} música(s) • ${artist.albumCount} álbum(ns)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = glass.accentPrimary)
                        }
                    }
                }
            }

            if (results.videos.isNotEmpty()) {
                item {
                    SearchGroupHeader("Vídeos (${results.videos.size})")
                }
                items(results.videos, key = { "vid_${it.id}" }) { video ->
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(18.dp),
                        onClick = { onPlayVideo(video) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MediaArtwork(
                                artworkPath = video.artworkPath,
                                contentDescription = video.title,
                                cornerRadius = 12.dp,
                                modifier = Modifier.size(width = 76.dp, height = 50.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = video.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val res = if (video.videoWidth > 0) "${video.videoWidth}x${video.videoHeight} • " else ""
                                Text(
                                    text = "$res${formatDuration(video.durationMs)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Rounded.VideoLibrary, contentDescription = null, tint = glass.accentPrimary)
                        }
                    }
                }
            }

            if (results.playlists.isNotEmpty()) {
                item {
                    SearchGroupHeader("Playlists (${results.playlists.size})")
                }
                items(results.playlists, key = { "pl_${it.playlist.id}" }) { pl ->
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(18.dp),
                        onClick = { onOpenPlaylist(pl.playlist.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = glass.accentPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = pl.playlist.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${pl.tracks.size} faixa(s)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchGroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}
