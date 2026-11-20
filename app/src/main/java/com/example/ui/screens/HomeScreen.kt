package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.media.MediaPermissionStatus
import com.example.data.media.SmartPlaylistRule
import com.example.data.model.AppSettings
import com.example.data.model.ArtistSummary
import com.example.data.model.HistoryWithMedia
import com.example.data.model.HomeSectionType
import com.example.data.model.LibraryCategory
import com.example.data.model.MediaItemEntity
import com.example.data.model.MoodType
import com.example.data.model.PlaylistWithTracks
import com.example.data.model.SurfaceMaterial
import com.example.player.PlaybackUiState
import com.example.ui.components.TrackListItem
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LocalLumaGlass
import com.example.ui.theme.LumaSectionEntrance
import com.example.ui.theme.MediaArtwork
import com.example.ui.theme.SculptedCircleButton
import com.example.ui.theme.formatDuration
import com.example.ui.theme.rememberLumaBounceScale
import java.util.Calendar

@Composable
fun HomeScreen(
    allMedia: List<MediaItemEntity>,
    audioTracks: List<MediaItemEntity>,
    continueListening: List<MediaItemEntity>,
    favorites: List<MediaItemEntity>,
    playlists: List<PlaylistWithTracks>,
    history: List<HistoryWithMedia>,
    playbackState: PlaybackUiState,
    permissionStatus: MediaPermissionStatus = MediaPermissionStatus(),
    isScanning: Boolean = false,
    bottomInsetPadding: Dp,
    onRequestAutoScan: () -> Unit = {},
    onImportFiles: () -> Unit,
    onImportFolder: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenCategory: (LibraryCategory) -> Unit,
    onOpenMood: (MoodType) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onPlayTrack: (MediaItemEntity, List<MediaItemEntity>) -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit,
    onExpandPlayer: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onPlayNext: (MediaItemEntity) -> Unit,
    onAddToQueue: (MediaItemEntity) -> Unit,
    onSmartMix: (MediaItemEntity) -> Unit,
    onAddToPlaylist: (MediaItemEntity) -> Unit,
    onRemoveFromLibrary: (MediaItemEntity) -> Unit,
    onEditMetadata: (MediaItemEntity) -> Unit = {},
    settings: AppSettings = AppSettings(),
    recommendedTracks: List<MediaItemEntity> = emptyList(),
    mostPlayedTracks: List<MediaItemEntity> = emptyList(),
    recentArtists: List<ArtistSummary> = emptyList(),
    videos: List<MediaItemEntity> = emptyList(),
    onOpenInsights: () -> Unit = {},
    onOpenArtist: (String) -> Unit = {},
    onOpenVideo: (MediaItemEntity) -> Unit = {},
    onCreateSmartPlaylist: (SmartPlaylistRule) -> Unit = {}
) {
    val glass = LocalLumaGlass.current
    val greeting = rememberContextualGreeting()
    val orderedSections = remember(settings.homeSectionOrder, settings.hiddenHomeSections) {
        HomeSectionType.parseOrder(settings.homeSectionOrder)
            .filter { it.key !in settings.hiddenHomeSections }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = 12.dp,
            bottom = bottomInsetPadding + 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header Row
        item {
            LumaSectionEntrance(index = 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    LiquidGlassSurface(
                        shape = CircleShape,
                        onClick = onOpenInsights,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("home_insights_button")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Rounded.BarChart,
                                contentDescription = "Luma Insights",
                                tint = glass.accentPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    LiquidGlassSurface(
                        shape = CircleShape,
                        onClick = onOpenHistory,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = "Histórico de reprodução",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    LiquidGlassSurface(
                        shape = CircleShape,
                        elevated = true,
                        accentGlow = glass.accentPrimary,
                        onClick = onImportFiles,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("home_import_button")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Importar músicas ou vídeos",
                                tint = glass.accentPrimary
                            )
                        }
                    }
                }
            }
        }

        // Empty State when no local media is indexed yet
        if (allMedia.isEmpty()) {
            item {
                LumaSectionEntrance(index = 1) {
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(28.dp),
                        elevated = true,
                        accentGlow = glass.accentPrimary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_empty_library),
                                contentDescription = "Ilustração de biblioteca vazia",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(190.dp)
                                    .clip(RoundedCornerShape(20.dp))
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "A sua biblioteca local está pronta",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Ative a varredura automática via MediaStore para encontrar músicas e vídeos do dispositivo, ou importe ficheiros manualmente.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isScanning) {
                                Spacer(modifier = Modifier.height(10.dp))
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            val scanSpin = if (isScanning && !glass.reduceMotion) {
                                val inf = rememberInfiniteTransition(label = "homeScanSpin")
                                inf.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
                                    label = "homeScanRot"
                                ).value
                            } else 0f
                            Button(
                                onClick = onRequestAutoScan,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = glass.accentPrimary,
                                    contentColor = glass.onAccent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("home_auto_scan_button")
                            ) {
                                Icon(
                                    Icons.Rounded.Sync,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .graphicsLayer { rotationZ = scanSpin }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (permissionStatus.hasAllMediaPermissions) "Varrer biblioteca automaticamente"
                                    else "Permitir e varrer biblioteca"
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onImportFiles,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ficheiros")
                                }
                                OutlinedButton(
                                    onClick = onImportFolder,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pasta")
                                }
                            }
                        }
                    }
                }
            }
        } else if (!permissionStatus.hasAnyMediaPermission) {
            item {
                LiquidGlassSurface(
                    shape = RoundedCornerShape(22.dp),
                    onClick = onRequestAutoScan,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Sync,
                            contentDescription = null,
                            tint = glass.accentPrimary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Varredura automática (MediaStore)", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Toque para permitir leitura de áudio e vídeo do dispositivo.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Spotlight Card for Active Playback
        playbackState.currentTrack?.let { current ->
            item {
                LumaSectionEntrance(index = 1) {
                    NowPlayingSpotlightCard(
                        track = current,
                        playbackState = playbackState,
                        onExpand = onExpandPlayer,
                        onTogglePlayPause = onTogglePlayPause,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onToggleFavorite = { onToggleFavorite(current) }
                    )
                }
            }
        }

        // Quick Shortcuts Row (Álbuns, Artistas, Pastas, Vídeos, Insights)
        item {
            LumaSectionEntrance(index = 2) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        ShortcutGlassChip(
                            icon = Icons.Rounded.Album,
                            label = "Álbuns",
                            onClick = { onOpenCategory(LibraryCategory.ALBUMS) }
                        )
                    }
                    item {
                        ShortcutGlassChip(
                            icon = Icons.Rounded.Person,
                            label = "Artistas",
                            onClick = { onOpenCategory(LibraryCategory.ARTISTS) }
                        )
                    }
                    item {
                        ShortcutGlassChip(
                            icon = Icons.Rounded.Folder,
                            label = "Pastas",
                            onClick = { onOpenCategory(LibraryCategory.FOLDERS) }
                        )
                    }
                    item {
                        ShortcutGlassChip(
                            icon = Icons.Rounded.VideoLibrary,
                            label = "Vídeos",
                            onClick = { onOpenCategory(LibraryCategory.VIDEOS) }
                        )
                    }
                    item {
                        ShortcutGlassChip(
                            icon = Icons.Rounded.BarChart,
                            label = "Insights",
                            onClick = onOpenInsights
                        )
                    }
                }
            }
        }

        // Dynamic Customizable Home Sections
        orderedSections.forEachIndexed { sectionIndex, section ->
            when (section) {
                HomeSectionType.FOR_YOU -> {
                    if (recommendedTracks.isNotEmpty() || audioTracks.isNotEmpty()) {
                        item(key = "sec_for_you") {
                            LumaSectionEntrance(index = 2 + sectionIndex) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    SectionHeader(
                                        title = "Para Ti & Insights",
                                        actionText = "Ver Insights",
                                        onAction = onOpenInsights
                                    )
                                    if (recommendedTracks.isNotEmpty()) {
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            items(recommendedTracks, key = { "foryou_${it.id}" }) { item ->
                                                LiquidGlassSurface(
                                                    shape = RoundedCornerShape(18.dp),
                                                    onClick = { onPlayTrack(item, recommendedTracks) },
                                                    modifier = Modifier.width(152.dp)
                                                ) {
                                                    Column(modifier = Modifier.padding(10.dp)) {
                                                        MediaArtwork(
                                                            artworkPath = item.artworkPath,
                                                            contentDescription = item.title,
                                                            cornerRadius = 14.dp,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(104.dp)
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Text(
                                                            text = item.title,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = item.artist,
                                                            style = MaterialTheme.typography.bodySmall,
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
                            }
                        }
                    }
                }

                HomeSectionType.CONTINUE_LISTENING -> {
                    if (continueListening.isNotEmpty()) {
                        item(key = "sec_continue") {
                            LumaSectionEntrance(index = 3 + sectionIndex) {
                                Column {
                                    SectionHeader(title = "Continuar a ouvir")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items(continueListening, key = { "cont_${it.id}" }) { item ->
                                            val ratio = if (item.durationMs > 0) {
                                                (item.lastPositionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
                                            } else 0f
                                            LiquidGlassSurface(
                                                shape = RoundedCornerShape(20.dp),
                                                onClick = { onPlayTrack(item, audioTracks) },
                                                modifier = Modifier.width(164.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    MediaArtwork(
                                                        artworkPath = item.artworkPath,
                                                        contentDescription = item.title,
                                                        cornerRadius = 14.dp,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(110.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = item.title,
                                                        style = MaterialTheme.typography.titleSmall,
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
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    LinearProgressIndicator(
                                                        progress = { ratio },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(3.dp)
                                                            .clip(RoundedCornerShape(50)),
                                                        color = glass.accentPrimary,
                                                        trackColor = glass.textPrimary.copy(alpha = 0.14f)
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

                HomeSectionType.SMART_MIXES -> {
                    if (audioTracks.isNotEmpty()) {
                        item(key = "sec_smart_mixes") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SectionHeader(
                                    title = "Playlists Inteligentes",
                                    actionText = "Playlists",
                                    onAction = { onOpenCategory(LibraryCategory.PLAYLISTS) }
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(SmartPlaylistRule.entries) { rule ->
                                        LiquidGlassSurface(
                                            shape = RoundedCornerShape(20.dp),
                                            onClick = { onCreateSmartPlaylist(rule) },
                                            modifier = Modifier.width(168.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(CircleShape)
                                                        .background(glass.pillIndicatorBg),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.AutoAwesome,
                                                        contentDescription = null,
                                                        tint = glass.accentPrimary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Text(
                                                    text = rule.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = rule.subtitle,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = glass.textSecondary,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HomeSectionType.MOODS -> {
                    if (audioTracks.isNotEmpty()) {
                        item(key = "sec_moods") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Coleções de Humor",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(MoodType.entries) { mood ->
                                        val icon = when (mood) {
                                            MoodType.CHILL -> Icons.Rounded.Spa
                                            MoodType.FOCUS -> Icons.Rounded.SelfImprovement
                                            MoodType.WORKOUT -> Icons.Rounded.FitnessCenter
                                            MoodType.ROAD_TRIP -> Icons.Rounded.DirectionsCar
                                        }
                                        LiquidGlassSurface(
                                            shape = RoundedCornerShape(20.dp),
                                            onClick = { onOpenMood(mood) },
                                            modifier = Modifier.width(156.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(glass.pillIndicatorBg),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = null,
                                                        tint = glass.accentPrimary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(
                                                    text = mood.label,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = mood.subtitle,
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
                    }
                }

                HomeSectionType.MOST_PLAYED -> {
                    if (mostPlayedTracks.isNotEmpty()) {
                        item(key = "sec_most_played") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SectionHeader(
                                    title = "Mais ouvidas",
                                    actionText = "Insights",
                                    onAction = onOpenInsights
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(mostPlayedTracks.take(10), key = { "mp_${it.id}" }) { track ->
                                        LiquidGlassSurface(
                                            shape = RoundedCornerShape(18.dp),
                                            onClick = { onPlayTrack(track, mostPlayedTracks) },
                                            modifier = Modifier.width(148.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                MediaArtwork(
                                                    artworkPath = track.artworkPath,
                                                    contentDescription = track.title,
                                                    cornerRadius = 12.dp,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(98.dp)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = track.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${track.playCount}x • ${track.artist}",
                                                    style = MaterialTheme.typography.bodySmall,
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
                }

                HomeSectionType.RECENT_ARTISTS -> {
                    if (recentArtists.isNotEmpty()) {
                        item(key = "sec_artists") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SectionHeader(
                                    title = "Artistas",
                                    actionText = "Todos",
                                    onAction = { onOpenCategory(LibraryCategory.ARTISTS) }
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(recentArtists.take(12), key = { "art_${it.name}" }) { artist ->
                                        LiquidGlassSurface(
                                            shape = RoundedCornerShape(20.dp),
                                            onClick = { onOpenArtist(artist.name) },
                                            modifier = Modifier.width(122.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                MediaArtwork(
                                                    artworkPath = artist.artworkPath,
                                                    contentDescription = artist.name,
                                                    cornerRadius = 36.dp,
                                                    modifier = Modifier.size(72.dp)
                                                )
                                                Text(
                                                    text = artist.name,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center
                                                )
                                                Text(
                                                    text = "${artist.trackCount} faixas",
                                                    style = MaterialTheme.typography.labelSmall,
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

                HomeSectionType.VIDEOS -> {
                    if (videos.isNotEmpty()) {
                        item(key = "sec_videos") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SectionHeader(
                                    title = "Vídeos locais",
                                    actionText = "Ver todos",
                                    onAction = { onOpenCategory(LibraryCategory.VIDEOS) }
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(videos.take(8), key = { "vid_${it.id}" }) { vid ->
                                        LiquidGlassSurface(
                                            shape = RoundedCornerShape(18.dp),
                                            onClick = { onOpenVideo(vid) },
                                            modifier = Modifier.width(168.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                MediaArtwork(
                                                    artworkPath = vid.artworkPath,
                                                    contentDescription = vid.title,
                                                    cornerRadius = 12.dp,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(94.dp)
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = vid.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = formatDuration(vid.durationMs),
                                                    style = MaterialTheme.typography.labelSmall,
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

                HomeSectionType.QUICK_PLAYLISTS -> {
                    if (playlists.isNotEmpty()) {
                        item(key = "sec_playlists") {
                            Column {
                                SectionHeader(
                                    title = "Playlists recentes",
                                    actionText = "Ver todas",
                                    onAction = { onOpenCategory(LibraryCategory.PLAYLISTS) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(playlists, key = { "pl_${it.playlist.id}" }) { pl ->
                                        LiquidGlassSurface(
                                            shape = RoundedCornerShape(20.dp),
                                            onClick = { onOpenPlaylist(pl.playlist.id) },
                                            modifier = Modifier.width(160.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                MediaArtwork(
                                                    artworkPath = pl.tracks.firstOrNull { it.artworkPath != null }?.artworkPath,
                                                    contentDescription = pl.playlist.name,
                                                    cornerRadius = 14.dp,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(104.dp)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = pl.playlist.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
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
                    if (favorites.isNotEmpty()) {
                        item(key = "sec_favorites") {
                            Column {
                                SectionHeader(
                                    title = "Favoritos",
                                    actionText = "Abrir",
                                    onAction = { onOpenCategory(LibraryCategory.FAVORITES) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(favorites.take(10), key = { "fav_${it.id}" }) { fav ->
                                        LiquidGlassSurface(
                                            shape = RoundedCornerShape(18.dp),
                                            onClick = { onPlayTrack(fav, favorites) },
                                            modifier = Modifier.width(148.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                MediaArtwork(
                                                    artworkPath = fav.artworkPath,
                                                    contentDescription = fav.title,
                                                    cornerRadius = 12.dp,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(100.dp)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = fav.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = fav.artist,
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
                    }
                }

                HomeSectionType.RECENTLY_ADDED -> {
                    val recentItems = history.mapNotNull { it.media }.distinctBy { it.id }.take(5)
                    if (recentItems.isNotEmpty()) {
                        item(key = "sec_recent_hist_header") {
                            SectionHeader(
                                title = "Reproduzido recentemente",
                                actionText = "Histórico",
                                onAction = onOpenHistory
                            )
                        }
                        items(recentItems, key = { "rec_${it.id}" }) { item ->
                            TrackListItem(
                                item = item,
                                isPlayingNow = playbackState.currentTrack?.id == item.id,
                                onPlay = { onPlayTrack(item, audioTracks) },
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

                    if (allMedia.isNotEmpty()) {
                        item(key = "sec_added_header") {
                            SectionHeader(
                                title = "Adicionados recentemente",
                                actionText = "Biblioteca",
                                onAction = { onOpenCategory(LibraryCategory.SONGS) }
                            )
                        }
                        items(allMedia.take(8), key = { "add_${it.id}" }) { item ->
                            TrackListItem(
                                item = item,
                                isPlayingNow = playbackState.currentTrack?.id == item.id,
                                onPlay = { onPlayTrack(item, audioTracks) },
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
            }
        }
    }
}

@Composable
private fun NowPlayingSpotlightCard(
    track: MediaItemEntity,
    playbackState: PlaybackUiState,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val glass = LocalLumaGlass.current
    val progress = if (playbackState.durationMs > 0L) {
        (playbackState.positionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    LiquidGlassSurface(
        shape = RoundedCornerShape(26.dp),
        elevated = true,
        accentGlow = glass.accentPrimary,
        onClick = onExpand,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MediaArtwork(
                    artworkPath = track.artworkPath,
                    contentDescription = track.title,
                    cornerRadius = 18.dp,
                    modifier = Modifier.size(82.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = glass.accentPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "A REPRODUZIR AGORA",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.accentPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${track.artist} • ${track.album}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (track.isFavorite) glass.accentSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(50)),
                color = glass.accentPrimary,
                trackColor = glass.textPrimary.copy(alpha = 0.14f),
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(playbackState.positionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val prevInteraction = remember { MutableInteractionSource() }
                    val prevScale = rememberLumaBounceScale(prevInteraction)
                    IconButton(
                        onClick = onPrevious,
                        interactionSource = prevInteraction,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .graphicsLayer { scaleX = prevScale; scaleY = prevScale }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Anterior",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (glass.isNeumorphic) {
                        SculptedCircleButton(
                            onClick = onTogglePlayPause,
                            emphasized = true,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (playbackState.isPlaying) "Pausar" else "Reproduzir",
                                tint = if (glass.surfaceMaterial == SurfaceMaterial.NEUMORPHIC_SOFT) glass.accentPrimary else glass.textPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    } else {
                        val playInteraction = remember { MutableInteractionSource() }
                        val playScale = rememberLumaBounceScale(playInteraction, pressedScale = 0.88f)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .graphicsLayer { scaleX = playScale; scaleY = playScale }
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(glass.accentPrimary, glass.accentSecondary)))
                                .clickable(
                                    interactionSource = playInteraction,
                                    indication = androidx.compose.material3.ripple(),
                                    onClick = onTogglePlayPause
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (playbackState.isPlaying) "Pausar" else "Reproduzir",
                                tint = glass.onAccent,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    val nextInteraction = remember { MutableInteractionSource() }
                    val nextScale = rememberLumaBounceScale(nextInteraction)
                    IconButton(
                        onClick = onNext,
                        interactionSource = nextInteraction,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .graphicsLayer { scaleX = nextScale; scaleY = nextScale }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Seguinte",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Text(
                    text = formatDuration(playbackState.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ShortcutGlassChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val glass = LocalLumaGlass.current
    LiquidGlassSurface(
        shape = RoundedCornerShape(18.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = glass.accentPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (actionText != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionText)
            }
        }
    }
}

private fun rememberContextualGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Bom dia"
        in 12..18 -> "Boa tarde"
        else -> "Boa noite"
    }
}
