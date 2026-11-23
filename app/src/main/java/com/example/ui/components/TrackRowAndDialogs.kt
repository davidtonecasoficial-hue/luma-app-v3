package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.example.data.media.ArtistNormalizer
import com.example.data.media.SmartPlaylistRule
import com.example.data.model.DuplicateGroup
import com.example.data.model.MediaItemEntity
import com.example.data.model.MediaType
import com.example.data.model.PlaylistWithTracks
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LocalLumaGlass
import com.example.ui.theme.LumaDropdownMenu
import com.example.ui.theme.MediaArtwork
import com.example.ui.theme.formatDuration
import com.example.ui.theme.formatFileSize
import com.example.ui.theme.lumaMenuItemColors

@Composable
fun TrackListItem(
    item: MediaItemEntity,
    isPlayingNow: Boolean,
    onPlay: () -> Unit,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onSmartMix: (() -> Unit)? = null,
    onEditMetadata: (() -> Unit)? = null,
    onRemoveFromLibrary: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    leadingLabel: String? = null,
    extraMenuActions: (@Composable (closeMenu: () -> Unit) -> Unit)? = null
) {
    val glass = LocalLumaGlass.current
    var menuExpanded by remember { mutableStateOf(false) }

    LiquidGlassSurface(
        shape = RoundedCornerShape(18.dp),
        elevated = isPlayingNow,
        accentGlow = if (isPlayingNow) glass.accentPrimary else null,
        onClick = onPlay,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (item.isAvailable) 1f else 0.56f }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!leadingLabel.isNullOrBlank()) {
                Text(
                    text = leadingLabel,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isPlayingNow) glass.accentPrimary else glass.textSecondary,
                    modifier = Modifier.width(24.dp)
                )
            }
            Box(contentAlignment = Alignment.Center) {
                MediaArtwork(
                    artworkPath = item.artworkPath,
                    contentDescription = item.title,
                    cornerRadius = 12.dp,
                    modifier = Modifier.size(52.dp)
                )
                if (item.mediaType == MediaType.VIDEO) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(3.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VideoFile,
                            contentDescription = "Vídeo",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                } else if (isPlayingNow) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.50f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = "A reproduzir",
                            tint = if (glass.isMonochrome) Color.White else glass.accentPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isPlayingNow) glass.accentPrimary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val subtitle = remember(
                    item.id,
                    item.artist,
                    item.album,
                    item.mediaType,
                    item.videoWidth,
                    item.videoHeight,
                    item.isAvailable
                ) {
                    val base = buildString {
                        append(item.artist)
                        if (item.mediaType == MediaType.AUDIO && item.album.isNotBlank()) {
                            append(" • ")
                            append(item.album)
                        } else if (item.mediaType == MediaType.VIDEO && item.videoWidth > 0) {
                            append(" • ${item.videoWidth}x${item.videoHeight}")
                        }
                    }
                    if (item.isAvailable) base else "Ficheiro indisponível • $base"
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.isAvailable) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val formattedDuration = remember(item.durationMs) { formatDuration(item.durationMs) }
            Text(
                text = formattedDuration,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            if (onToggleFavorite != null && item.mediaType == MediaType.AUDIO) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (item.isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                        tint = if (item.isFavorite) glass.accentSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Opções para ${item.title}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val itemColors = lumaMenuItemColors()
                LumaDropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Reproduzir") },
                        leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                        colors = itemColors,
                        onClick = {
                            menuExpanded = false
                            onPlay()
                        }
                    )
                    if (onPlayNext != null && item.mediaType == MediaType.AUDIO) {
                        DropdownMenuItem(
                            text = { Text("Reproduzir a seguir") },
                            leadingIcon = { Icon(Icons.Rounded.PlaylistPlay, contentDescription = null) },
                            colors = itemColors,
                            onClick = {
                                menuExpanded = false
                                onPlayNext()
                            }
                        )
                    }
                    if (onAddToQueue != null && item.mediaType == MediaType.AUDIO) {
                        DropdownMenuItem(
                            text = { Text("Adicionar à fila") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null) },
                            colors = itemColors,
                            onClick = {
                                menuExpanded = false
                                onAddToQueue()
                            }
                        )
                    }
                    if (onSmartMix != null && item.mediaType == MediaType.AUDIO) {
                        DropdownMenuItem(
                            text = { Text("Smart Mix local") },
                            leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                            colors = itemColors,
                            onClick = {
                                menuExpanded = false
                                onSmartMix()
                            }
                        )
                    }
                    if (onAddToPlaylist != null && item.mediaType == MediaType.AUDIO) {
                        DropdownMenuItem(
                            text = { Text("Adicionar a playlist") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null) },
                            colors = itemColors,
                            onClick = {
                                menuExpanded = false
                                onAddToPlaylist()
                            }
                        )
                    }
                    if (onEditMetadata != null) {
                        DropdownMenuItem(
                            text = { Text("Detalhes e metadados") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            colors = itemColors,
                            onClick = {
                                menuExpanded = false
                                onEditMetadata()
                            }
                        )
                    }
                    extraMenuActions?.invoke { menuExpanded = false }
                    if (onRemoveFromLibrary != null) {
                        DropdownMenuItem(
                            text = { Text("Remover da biblioteca", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.error,
                                leadingIconColor = MaterialTheme.colorScheme.error
                            ),
                            onClick = {
                                menuExpanded = false
                                onRemoveFromLibrary()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreatePlaylistDialog(
    initialName: String = "",
    title: String = "Nova Playlist",
    confirmLabel: String = "Criar",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val glass = LocalLumaGlass.current
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = glass.surfaceElevated,
        titleContentColor = glass.textPrimary,
        textContentColor = glass.textSecondary,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Nome da playlist") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun PlaylistPickerDialog(
    playlists: List<PlaylistWithTracks>,
    onSelectPlaylist: (Long) -> Unit,
    onCreateNew: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalLumaGlass.current
    var showNewField by remember { mutableStateOf(playlists.isEmpty()) }
    var newName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = glass.surfaceElevated,
        titleContentColor = glass.textPrimary,
        textContentColor = glass.textSecondary,
        title = { Text("Adicionar à Playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (playlists.isNotEmpty() && !showNewField) {
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(playlists, key = { it.playlist.id }) { pl ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectPlaylist(pl.playlist.id) }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = pl.playlist.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${pl.tracks.size} faixas",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    TextButton(onClick = { showNewField = true }) {
                        Text("+ Criar nova playlist")
                    }
                } else {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        singleLine = true,
                        label = { Text("Nome da nova playlist") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (showNewField) {
                TextButton(
                    onClick = { if (newName.isNotBlank()) onCreateNew(newName.trim()) },
                    enabled = newName.isNotBlank()
                ) {
                    Text("Criar e adicionar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String = "Confirmar",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalLumaGlass.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = glass.surfaceElevated,
        titleContentColor = glass.textPrimary,
        textContentColor = glass.textSecondary,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun StatusErrorBanner(
    message: String,
    isError: Boolean,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalLumaGlass.current
    LiquidGlassSurface(
        shape = RoundedCornerShape(18.dp),
        elevated = true,
        accentGlow = if (isError) MaterialTheme.colorScheme.error else glass.accentPrimary,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = if (isError) MaterialTheme.colorScheme.error else glass.accentPrimary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(actionLabel)
                }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Fechar aviso",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun MediaMetadataDialog(
    item: MediaItemEntity,
    onSave: (title: String, artist: String, album: String, genre: String) -> Unit = { _, _, _, _ -> },
    onSaveExtended: ((title: String, artist: String, album: String, genre: String, releaseYear: Int, trackNumber: Int) -> Unit)? = null,
    onPickArtwork: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val glass = LocalLumaGlass.current
    var title by remember(item.id) { mutableStateOf(item.title) }
    var artist by remember(item.id) { mutableStateOf(item.artist) }
    var album by remember(item.id) { mutableStateOf(item.album) }
    var genre by remember(item.id) { mutableStateOf(item.genre) }
    var releaseYearText by remember(item.id) {
        mutableStateOf(if (item.releaseYear > 0) item.releaseYear.toString() else "")
    }
    var trackNumberText by remember(item.id) {
        mutableStateOf(if (item.trackNumber > 0) item.trackNumber.toString() else "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = glass.surfaceElevated,
        titleContentColor = glass.textPrimary,
        textContentColor = glass.textSecondary,
        title = { Text("Luma Metadata Editor") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MediaArtwork(
                        artworkPath = item.artworkPath,
                        contentDescription = item.title,
                        cornerRadius = 14.dp,
                        modifier = Modifier.size(62.dp)
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (onPickArtwork != null) {
                            OutlinedButton(
                                onClick = onPickArtwork,
                                modifier = Modifier.fillMaxWidth().testTag("metadata_pick_artwork_button")
                            ) {
                                Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Alterar Capa")
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                val parsed = ArtistNormalizer.parseFilenameMetadata(item.fileName)
                                if (parsed.title.isNotBlank()) title = parsed.title
                                val detectedArtist = ArtistNormalizer.normalizePrimaryArtist(
                                    ArtistNormalizer.cleanDisplayTag(artist).ifBlank { parsed.artist },
                                    item.fileName
                                )
                                if (detectedArtist != "Artista desconhecido") artist = detectedArtist
                                if (parsed.releaseYear > 0 && releaseYearText.isBlank()) {
                                    releaseYearText = parsed.releaseYear.toString()
                                }
                                if (parsed.trackNumber > 0 && trackNumberText.isBlank()) {
                                    trackNumberText = parsed.trackNumber.toString()
                                }
                                if (genre.isBlank()) {
                                    genre = ArtistNormalizer.inferGenre(title, artist, album, item.folderName, item.durationMs)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("metadata_auto_detect_button")
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Detetar Auto")
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    singleLine = true,
                    label = { Text("Artista") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    singleLine = true,
                    label = { Text("Álbum") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    singleLine = true,
                    label = { Text("Género") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = releaseYearText,
                        onValueChange = { releaseYearText = it.filter { ch -> ch.isDigit() }.take(4) },
                        singleLine = true,
                        label = { Text("Ano") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = trackNumberText,
                        onValueChange = { trackNumberText = it.filter { ch -> ch.isDigit() }.take(3) },
                        singleLine = true,
                        label = { Text("Nº Faixa") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                LiquidGlassSurface(shape = RoundedCornerShape(14.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Info,
                                contentDescription = null,
                                tint = LocalLumaGlass.current.accentPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Localização e Ficheiro", style = MaterialTheme.typography.labelLarge)
                        }
                        Text(
                            text = "Ficheiro: ${item.fileName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Pasta: ${item.folderPath.ifBlank { item.folderName }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tamanho: ${formatFileSize(item.sizeBytes)} • Duração: ${formatDuration(item.durationMs)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (item.mimeType.isNotBlank()) {
                            Text(
                                text = "Formato: ${item.mimeType}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val y = releaseYearText.toIntOrNull() ?: 0
                    val tn = trackNumberText.toIntOrNull() ?: 0
                    if (onSaveExtended != null) {
                        onSaveExtended(title, artist, album, genre, y, tn)
                    } else {
                        onSave(title, artist, album, genre)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun BatchMetadataDialog(
    selectedCount: Int,
    onApply: (artist: String?, album: String?, genre: String?, year: Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalLumaGlass.current
    var artist by remember { mutableStateOf("") }
    var album by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var yearText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = glass.surfaceElevated,
        titleContentColor = glass.textPrimary,
        textContentColor = glass.textSecondary,
        title = { Text("Edição em Lote ($selectedCount faixas)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Apenas os campos preenchidos serão atualizados em todas as faixas selecionadas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = glass.textSecondary
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    singleLine = true,
                    label = { Text("Artista (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    singleLine = true,
                    label = { Text("Álbum (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    singleLine = true,
                    label = { Text("Género (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = yearText,
                    onValueChange = { yearText = it.filter { c -> c.isDigit() }.take(4) },
                    singleLine = true,
                    label = { Text("Ano (opcional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApply(
                        artist.trim().takeIf { it.isNotEmpty() },
                        album.trim().takeIf { it.isNotEmpty() },
                        genre.trim().takeIf { it.isNotEmpty() },
                        yearText.toIntOrNull()
                    )
                },
                enabled = artist.isNotBlank() || album.isNotBlank() || genre.isNotBlank() || yearText.isNotBlank()
            ) {
                Text("Aplicar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun SmartPlaylistDialog(
    onSelectRule: (SmartPlaylistRule) -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalLumaGlass.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = glass.surfaceElevated,
        titleContentColor = glass.textPrimary,
        textContentColor = glass.textSecondary,
        title = { Text("Gerador Inteligente") },
        text = {
            LazyColumn(
                modifier = Modifier.height(310.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SmartPlaylistRule.entries, key = { it.name }) { rule ->
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(14.dp),
                        onClick = { onSelectRule(rule) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = rule.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = glass.accentPrimary
                            )
                            Text(
                                text = rule.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = glass.textSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}

@Composable
fun DuplicatesInspectorDialog(
    groups: List<DuplicateGroup>,
    onRemoveIndexEntry: (MediaItemEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalLumaGlass.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = glass.surfaceElevated,
        titleContentColor = glass.textPrimary,
        textContentColor = glass.textSecondary,
        title = { Text("Duplicados Detetados (${groups.size})") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "A remoção aqui limpa apenas a entrada duplicada na lista da biblioteca. Nenhum ficheiro original é apagado do dispositivo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (groups.isEmpty()) {
                    Text(
                        text = "Nenhum duplicado encontrado na biblioteca local.",
                        style = MaterialTheme.typography.titleSmall
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(groups, key = { "${it.titleLabel}_${it.items.size}" }) { group ->
                            LiquidGlassSurface(shape = RoundedCornerShape(14.dp)) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = group.titleLabel,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${group.reasonLabel} • ${group.items.size} cópias",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LocalLumaGlass.current.accentPrimary
                                    )
                                    val keepId = com.example.data.media.LumaCollectionsEngine.pickRecommendedKeepId(group)
                                    group.items.forEachIndexed { idx, copy ->
                                        val isRecommendedKeep = if (keepId != null) copy.id == keepId else idx == 0
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = copy.folderPath.ifBlank { copy.folderName } +
                                                        if (isRecommendedKeep) " • Melhor qualidade" else "",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (isRecommendedKeep) glass.accentPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${formatFileSize(copy.sizeBytes)} • ${formatDuration(copy.durationMs)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (!isRecommendedKeep) {
                                                IconButton(onClick = { onRemoveIndexEntry(copy) }) {
                                                    Icon(
                                                        Icons.Rounded.DeleteOutline,
                                                        contentDescription = "Remover duplicado da lista",
                                                        tint = MaterialTheme.colorScheme.error
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}

