package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AlbumSummary
import com.example.data.model.ArtistSummary
import com.example.data.model.FolderSummary
import com.example.data.model.GenreSummary
import com.example.data.model.LibraryCategory
import com.example.data.model.LibraryFilter
import com.example.data.model.MediaItemEntity
import com.example.data.model.PlaylistWithTracks
import com.example.data.model.SortOrder
import com.example.data.model.ViewMode
import com.example.player.PlaybackUiState
import com.example.ui.components.TrackListItem
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LocalLumaGlass
import com.example.ui.theme.LumaDropdownMenu
import com.example.ui.theme.LumaSectionEntrance
import com.example.ui.theme.MediaArtwork
import com.example.ui.theme.formatDuration
import com.example.ui.theme.lumaMenuItemColors
import com.example.ui.theme.rememberLumaBounceScale
import com.example.ui.viewmodel.LibraryUiState

@Composable
fun LibraryScreen(
    libraryState: LibraryUiState,
    audioTracks: List<MediaItemEntity>,
    videos: List<MediaItemEntity>,
    favorites: List<MediaItemEntity>,
    albums: List<AlbumSummary>,
    artists: List<ArtistSummary>,
    folders: List<FolderSummary>,
    playlists: List<PlaylistWithTracks>,
    playbackState: PlaybackUiState,
    bottomInsetPadding: Dp,
    onSelectCategory: (LibraryCategory) -> Unit,
    onSortChange: (SortOrder) -> Unit,
    onToggleViewMode: () -> Unit,
    onRequestAutoScan: () -> Unit = {},
    onImportFiles: () -> Unit,
    onImportFolder: () -> Unit,
    onPlayTrack: (MediaItemEntity, List<MediaItemEntity>) -> Unit,
    onPlayVideo: (MediaItemEntity) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onRequestCreatePlaylist: () -> Unit,
    onPlayNext: (MediaItemEntity) -> Unit,
    onAddToQueue: (MediaItemEntity) -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit,
    onSmartMix: (MediaItemEntity) -> Unit,
    onAddToPlaylist: (MediaItemEntity) -> Unit,
    onRemoveFromLibrary: (MediaItemEntity) -> Unit,
    genres: List<GenreSummary> = emptyList(),
    duplicateCount: Int = 0,
    onOpenGenre: (String) -> Unit = {},
    onOpenDuplicatesInspector: () -> Unit = {},
    onEditMetadata: (MediaItemEntity) -> Unit = {},
    activeFilter: LibraryFilter = LibraryFilter.ALL,
    onFilterChange: (LibraryFilter) -> Unit = {},
    missingArtworkCount: Int = 0,
    missingMetadataCount: Int = 0,
    isSelectionMode: Boolean = false,
    selectedTrackIds: Set<Long> = emptySet(),
    onToggleSelectionMode: () -> Unit = {},
    onToggleTrackSelection: (Long) -> Unit = {},
    onClearSelection: () -> Unit = {},
    onOpenBatchEdit: () -> Unit = {},
    onBatchAddToPlaylist: () -> Unit = {},
    onOpenSmartPlaylistDialog: () -> Unit = {},
    onReorganizeLibrary: () -> Unit = {}
) {
    val glass = LocalLumaGlass.current
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("library_screen")
    ) {
        // Header Bar
        LumaSectionEntrance(index = 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Biblioteca",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )

                if (libraryState.selectedCategory == LibraryCategory.SONGS && audioTracks.isNotEmpty()) {
                    IconButton(
                        onClick = onToggleSelectionMode,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("library_select_mode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Checklist,
                            contentDescription = "Seleção múltipla",
                            tint = if (isSelectionMode) glass.accentPrimary else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Box {
                    val sortInteraction = remember { MutableInteractionSource() }
                    val sortBounce = rememberLumaBounceScale(sortInteraction)
                    IconButton(
                        onClick = { sortMenuExpanded = true },
                        interactionSource = sortInteraction,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .graphicsLayer { scaleX = sortBounce; scaleY = sortBounce }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Sort,
                            contentDescription = "Ordenar biblioteca",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    val sortItemColors = lumaMenuItemColors()
                    LumaDropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        SortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = { Text(order.label) },
                                colors = sortItemColors,
                                trailingIcon = {
                                    if (libraryState.sortOrder == order) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, tint = glass.accentPrimary)
                                    }
                                },
                                onClick = {
                                    sortMenuExpanded = false
                                    onSortChange(order)
                                }
                            )
                        }
                    }
                }

                val viewInteraction = remember { MutableInteractionSource() }
                val viewBounce = rememberLumaBounceScale(viewInteraction)
                IconButton(
                    onClick = onToggleViewMode,
                    interactionSource = viewInteraction,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .graphicsLayer { scaleX = viewBounce; scaleY = viewBounce }
                ) {
                    Icon(
                        imageVector = if (libraryState.viewMode == ViewMode.LIST) {
                            Icons.Rounded.GridView
                        } else {
                            Icons.AutoMirrored.Rounded.ViewList
                        },
                        contentDescription = "Alternar lista ou grelha",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                val scanSpin = if (libraryState.isScanning && !glass.reduceMotion) {
                    val inf = rememberInfiniteTransition(label = "libScanSpin")
                    inf.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
                        label = "libScanRot"
                    ).value
                } else 0f
                val scanInteraction = remember { MutableInteractionSource() }
                val scanBounce = rememberLumaBounceScale(scanInteraction)
                IconButton(
                    onClick = onRequestAutoScan,
                    interactionSource = scanInteraction,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .graphicsLayer { scaleX = scanBounce; scaleY = scanBounce }
                        .testTag("library_scan_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Sync,
                        contentDescription = "Varrer dispositivo (MediaStore)",
                        tint = if (libraryState.permissionStatus.hasAnyMediaPermission) glass.accentPrimary else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.graphicsLayer { rotationZ = scanSpin }
                    )
                }

                LiquidGlassSurface(
                    shape = CircleShape,
                    elevated = true,
                    onClick = onImportFiles,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Importar ficheiros",
                            tint = glass.accentPrimary
                        )
                    }
                }
            }
        }

        if (libraryState.isScanning) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 4.dp)
            )
        }

        // Category Chips Row
        LumaSectionEntrance(index = 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                items(LibraryCategory.entries) { category ->
                    val selected = libraryState.selectedCategory == category
                    val chipInteraction = remember { MutableInteractionSource() }
                    val pressBounce = rememberLumaBounceScale(chipInteraction, pressedScale = 0.90f)
                    val selectedScale by animateFloatAsState(
                        targetValue = if (selected && !glass.reduceMotion) 1.05f else 1f,
                        animationSpec = if (glass.reduceMotion) snap() else spring(dampingRatio = 0.54f, stiffness = 480f),
                        label = "catChipScale_${category.name}"
                    )
                    val borderCol by animateColorAsState(
                        targetValue = if (selected) glass.accentPrimary else glass.glassBorder,
                        animationSpec = if (glass.reduceMotion) snap() else tween(180),
                        label = "catChipBorder_${category.name}"
                    )
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                val s = pressBounce * selectedScale
                                scaleX = s
                                scaleY = s
                            }
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (selected) Brush.linearGradient(listOf(glass.accentPrimary, glass.accentSecondary))
                                else Brush.linearGradient(listOf(glass.glassSurface, glass.glassSurface))
                            )
                            .border(
                                width = if (selected) 0.dp else 0.8.dp,
                                color = borderCol,
                                shape = RoundedCornerShape(50)
                            )
                            .clickable(
                                interactionSource = chipInteraction,
                                indication = androidx.compose.material3.ripple(),
                                onClick = { onSelectCategory(category) }
                            )
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                            .testTag("library_cat_${category.name.lowercase()}")
                    ) {
                        Text(
                            text = category.label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (selected) glass.onAccent else glass.textPrimary
                        )
                    }
                }
            }
        }

        // Batch Selection Action Bar
        if (isSelectionMode && libraryState.selectedCategory == LibraryCategory.SONGS) {
            LiquidGlassSurface(
                shape = RoundedCornerShape(18.dp),
                elevated = true,
                accentGlow = glass.accentPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 10.dp)
                    .testTag("library_batch_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedTrackIds.size} selecionada(s)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = glass.accentPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onOpenBatchEdit,
                        enabled = selectedTrackIds.isNotEmpty(),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("batch_edit_button")
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Editar metadados em lote", tint = glass.accentPrimary)
                    }
                    IconButton(
                        onClick = onBatchAddToPlaylist,
                        enabled = selectedTrackIds.isNotEmpty(),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("batch_playlist_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = "Adicionar seleção à playlist", tint = glass.accentPrimary)
                    }
                    IconButton(
                        onClick = onClearSelection,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cancelar seleção")
                    }
                }
            }
        }

        // Advanced Filter Strip for SONGS
        if (libraryState.selectedCategory == LibraryCategory.SONGS) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                items(LibraryFilter.entries) { filter ->
                    val selected = activeFilter == filter
                    val countBadge = when (filter) {
                        LibraryFilter.MISSING_ARTWORK -> if (missingArtworkCount > 0) " ($missingArtworkCount)" else ""
                        LibraryFilter.MISSING_METADATA -> if (missingMetadataCount > 0) " ($missingMetadataCount)" else ""
                        else -> ""
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) glass.pillIndicatorBg else glass.textPrimary.copy(alpha = 0.05f))
                            .border(
                                width = if (selected) 1.2.dp else 0.7.dp,
                                color = if (selected) glass.accentPrimary else glass.glassBorder,
                                shape = RoundedCornerShape(50)
                            )
                            .clickable { onFilterChange(filter) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("library_filter_${filter.name.lowercase()}")
                    ) {
                        Text(
                            text = "${filter.label}$countBadge",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (selected) glass.accentPrimary else glass.textSecondary
                        )
                    }
                }
            }
        }

        // Smart Artist Organization & Maintenance Banner (when duplicates, missing metadata/artwork, or on ARTISTS tab)
        if (duplicateCount > 0) {
            LiquidGlassSurface(
                shape = RoundedCornerShape(16.dp),
                onClick = onOpenDuplicatesInspector,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 8.dp)
                    .testTag("library_duplicates_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        tint = glass.accentPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "$duplicateCount grupo(s) de possíveis duplicados",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Rever",
                        style = MaterialTheme.typography.labelLarge,
                        color = glass.accentPrimary
                    )
                }
            }
        }

        if ((libraryState.selectedCategory == LibraryCategory.ARTISTS || missingMetadataCount > 0) &&
            (artists.isNotEmpty() || audioTracks.isNotEmpty())
        ) {
            LiquidGlassSurface(
                shape = RoundedCornerShape(16.dp),
                onClick = onReorganizeLibrary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 10.dp)
                    .testTag("library_reorganize_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoFixHigh,
                        contentDescription = null,
                        tint = glass.accentPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Unificar cantores e organizar tags",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = glass.textPrimary
                        )
                        if (missingMetadataCount > 0 || missingArtworkCount > 0) {
                            Text(
                                text = "$missingMetadataCount sem metadados • $missingArtworkCount sem capa",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.textSecondary
                            )
                        }
                    }
                    Text(
                        text = "Organizar",
                        style = MaterialTheme.typography.labelLarge,
                        color = glass.accentPrimary
                    )
                }
            }
        }

        // Category Content
        val bottomPad = bottomInsetPadding + 20.dp
        when (libraryState.selectedCategory) {
            LibraryCategory.SONGS -> MediaItemsCollection(
                items = audioTracks,
                emptyTitle = if (activeFilter == LibraryFilter.ALL) "Sem músicas na biblioteca" else "Nenhuma faixa neste filtro",
                emptyBody = if (activeFilter == LibraryFilter.ALL) {
                    "Importe faixas MP3, M4A, FLAC, WAV, OGG ou Opus do seu dispositivo."
                } else {
                    "Experimente selecionar \"Tudo\" para ver todas as suas faixas."
                },
                viewMode = libraryState.viewMode,
                currentTrackId = playbackState.currentTrack?.id,
                bottomPad = bottomPad,
                isSelectionMode = isSelectionMode,
                selectedTrackIds = selectedTrackIds,
                onToggleTrackSelection = onToggleTrackSelection,
                onImportFiles = onImportFiles,
                onImportFolder = onImportFolder,
                onItemClick = { onPlayTrack(it, audioTracks) },
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onToggleFavorite = onToggleFavorite,
                onSmartMix = onSmartMix,
                onAddToPlaylist = onAddToPlaylist,
                onEditMetadata = onEditMetadata,
                onRemoveFromLibrary = onRemoveFromLibrary
            )

            LibraryCategory.FAVORITES -> MediaItemsCollection(
                items = favorites,
                emptyTitle = "Sem favoritos ainda",
                emptyBody = "Toque no ícone de coração em qualquer música para a guardar aqui.",
                viewMode = libraryState.viewMode,
                currentTrackId = playbackState.currentTrack?.id,
                bottomPad = bottomPad,
                onImportFiles = onImportFiles,
                onImportFolder = onImportFolder,
                onItemClick = { onPlayTrack(it, favorites) },
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onToggleFavorite = onToggleFavorite,
                onSmartMix = onSmartMix,
                onAddToPlaylist = onAddToPlaylist,
                onEditMetadata = onEditMetadata,
                onRemoveFromLibrary = onRemoveFromLibrary
            )

            LibraryCategory.VIDEOS -> MediaItemsCollection(
                items = videos,
                emptyTitle = "Sem vídeos na biblioteca",
                emptyBody = "Importe ficheiros MP4, MKV, WebM ou MOV para reproduzir no leitor de vídeo.",
                viewMode = libraryState.viewMode,
                currentTrackId = null,
                bottomPad = bottomPad,
                onImportFiles = onImportFiles,
                onImportFolder = onImportFolder,
                onItemClick = onPlayVideo,
                onPlayNext = {},
                onAddToQueue = {},
                onToggleFavorite = {},
                onSmartMix = {},
                onAddToPlaylist = {},
                onEditMetadata = onEditMetadata,
                onRemoveFromLibrary = onRemoveFromLibrary
            )

            LibraryCategory.ALBUMS -> {
                if (albums.isEmpty()) {
                    EmptyLibraryCard("Sem álbuns indexados", "Importe músicas com metadados ou capas incorporadas.", onImportFiles, onImportFolder)
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 156.dp),
                        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = bottomPad),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(albums, key = { it.name }) { album ->
                            LiquidGlassSurface(
                                shape = RoundedCornerShape(22.dp),
                                onClick = { onOpenAlbum(album.name) }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    MediaArtwork(
                                        artworkPath = album.artworkPath,
                                        contentDescription = album.name,
                                        cornerRadius = 16.dp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(132.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
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
                            }
                        }
                    }
                }
            }

            LibraryCategory.ARTISTS -> {
                if (artists.isEmpty()) {
                    EmptyLibraryCard("Sem artistas indexados", "Importe músicas para organizar automaticamente por artista.", onImportFiles, onImportFolder)
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = bottomPad),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(artists, key = { it.name }) { artist ->
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
                                        modifier = Modifier.size(52.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = artist.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${artist.trackCount} música(s) • ${artist.albumCount} álbum(ns)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = null,
                                        tint = glass.accentPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            LibraryCategory.GENRES -> {
                if (genres.isEmpty()) {
                    EmptyLibraryCard("Sem géneros indexados", "Importe músicas ou edite metadados para agrupar por género.", onImportFiles, onImportFolder)
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 156.dp),
                        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = bottomPad),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(genres, key = { it.name }) { genre ->
                            LiquidGlassSurface(
                                shape = RoundedCornerShape(20.dp),
                                onClick = { onOpenGenre(genre.name) },
                                modifier = Modifier.testTag("genre_card_${genre.name}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(glass.pillIndicatorBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Category,
                                            contentDescription = null,
                                            tint = glass.accentPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = genre.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${genre.trackCount} faixa(s)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            LibraryCategory.FOLDERS -> {
                if (folders.isEmpty()) {
                    EmptyLibraryCard("Sem pastas importadas", "Selecione uma pasta local para indexar todas as músicas e vídeos.", onImportFiles, onImportFolder)
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = bottomPad),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(folders, key = { it.name }) { folder ->
                            LiquidGlassSurface(
                                shape = RoundedCornerShape(18.dp),
                                onClick = { onOpenFolder(folder.name) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(glass.pillIndicatorBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Folder,
                                            contentDescription = null,
                                            tint = glass.accentPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = folder.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${folder.itemCount} ficheiro(s)",
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

            LibraryCategory.PLAYLISTS -> {
                LazyColumn(
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = bottomPad),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        LiquidGlassSurface(
                            shape = RoundedCornerShape(20.dp),
                            elevated = true,
                            onClick = onRequestCreatePlaylist,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_playlist_button")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(glass.pillIndicatorBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                        contentDescription = null,
                                        tint = glass.accentPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Criar nova playlist",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Organize a sua coleção local",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    item {
                        LiquidGlassSurface(
                            shape = RoundedCornerShape(20.dp),
                            onClick = onOpenSmartPlaylistDialog,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_smart_playlist_button")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(glass.pillIndicatorBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = glass.accentSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Gerar Playlist Inteligente",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Mais tocadas, joias escondidas, energia ou noite",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (playlists.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Ainda não criou nenhuma playlist.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(playlists, key = { it.playlist.id }) { pl ->
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
                                    MediaArtwork(
                                        artworkPath = pl.tracks.firstOrNull { it.artworkPath != null }?.artworkPath,
                                        contentDescription = pl.playlist.name,
                                        cornerRadius = 12.dp,
                                        modifier = Modifier.size(52.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pl.playlist.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${pl.tracks.size} faixa(s)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(glass.pillIndicatorBg)
                                            .clickable(enabled = pl.tracks.isNotEmpty()) {
                                                pl.tracks.firstOrNull()?.let { first ->
                                                    onPlayTrack(first, pl.tracks)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = "Reproduzir playlist",
                                            tint = glass.accentPrimary
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

@Composable
private fun MediaItemsCollection(
    items: List<MediaItemEntity>,
    emptyTitle: String,
    emptyBody: String,
    viewMode: ViewMode,
    currentTrackId: Long?,
    bottomPad: Dp,
    isSelectionMode: Boolean = false,
    selectedTrackIds: Set<Long> = emptySet(),
    onToggleTrackSelection: (Long) -> Unit = {},
    onImportFiles: () -> Unit,
    onImportFolder: () -> Unit,
    onItemClick: (MediaItemEntity) -> Unit,
    onPlayNext: (MediaItemEntity) -> Unit,
    onAddToQueue: (MediaItemEntity) -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit,
    onSmartMix: (MediaItemEntity) -> Unit,
    onAddToPlaylist: (MediaItemEntity) -> Unit,
    onEditMetadata: (MediaItemEntity) -> Unit,
    onRemoveFromLibrary: (MediaItemEntity) -> Unit
) {
    val glass = LocalLumaGlass.current
    if (items.isEmpty()) {
        EmptyLibraryCard(emptyTitle, emptyBody, onImportFiles, onImportFolder)
        return
    }

    if (viewMode == ViewMode.LIST) {
        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = bottomPad),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items, key = { it.id }) { item ->
                if (isSelectionMode) {
                    val selected = item.id in selectedTrackIds
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onToggleTrackSelection(item.id) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = if (selected) "Desmarcar" else "Selecionar",
                                tint = if (selected) glass.accentPrimary else glass.textSecondary
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            TrackListItem(
                                item = item,
                                isPlayingNow = currentTrackId == item.id,
                                onPlay = { onToggleTrackSelection(item.id) }
                            )
                        }
                    }
                } else {
                    TrackListItem(
                        item = item,
                        isPlayingNow = currentTrackId == item.id,
                        onPlay = { onItemClick(item) },
                        onPlayNext = { onPlayNext(item) },
                        onAddToQueue = { onAddToQueue(item) },
                        onToggleFavorite = { onToggleFavorite(item) },
                        onSmartMix = { onSmartMix(item) },
                        onAddToPlaylist = { onAddToPlaylist(item) },
                        onEditMetadata = { onEditMetadata(item) },
                        onRemoveFromLibrary = { onRemoveFromLibrary(item) }
                    )
                }
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 156.dp),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = bottomPad),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.id }) { item ->
                val selected = isSelectionMode && item.id in selectedTrackIds
                LiquidGlassSurface(
                    shape = RoundedCornerShape(20.dp),
                    elevated = currentTrackId == item.id || selected,
                    accentGlow = if (selected) glass.accentPrimary else null,
                    onClick = {
                        if (isSelectionMode) onToggleTrackSelection(item.id) else onItemClick(item)
                    }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        MediaArtwork(
                            artworkPath = item.artworkPath,
                            contentDescription = item.title,
                            cornerRadius = 14.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(124.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.artist} • ${formatDuration(item.durationMs)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryCard(
    title: String,
    body: String,
    onImportFiles: () -> Unit,
    onImportFolder: () -> Unit
) {
    val glass = LocalLumaGlass.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassSurface(shape = RoundedCornerShape(24.dp), elevated = true) {
            Column(
                modifier = Modifier.padding(20.dp),
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
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onImportFiles,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = glass.accentPrimary,
                            contentColor = glass.onAccent
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Importar")
                    }
                    OutlinedButton(
                        onClick = onImportFolder,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Pasta")
                    }
                }
            }
        }
    }
}
