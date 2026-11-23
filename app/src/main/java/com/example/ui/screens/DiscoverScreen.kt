package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.media.LumaIntelligenceSnapshot
import com.example.data.media.LumaMoodCategory
import com.example.data.model.MediaItemEntity
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LocalLumaGlass
import com.example.ui.theme.LocalLumaStrings
import com.example.ui.theme.MediaArtwork
import com.example.ui.theme.formatDuration

@Composable
fun DiscoverScreen(
    snapshot: LumaIntelligenceSnapshot,
    currentTrack: MediaItemEntity?,
    audioCount: Int,
    bottomInsetPadding: Dp,
    onStartAutoDj: (MediaItemEntity?) -> Unit,
    onPlayTrack: (MediaItemEntity, List<MediaItemEntity>) -> Unit,
    onOpenSimilarTracks: (MediaItemEntity) -> Unit,
    onOpenStudio: () -> Unit,
    onOpenConnect: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenLibraryHealth: () -> Unit,
    onOpenFileIntelligence: () -> Unit
) {
    val glass = LocalLumaGlass.current
    val strings = LocalLumaStrings.current
    var selectedMood by remember(snapshot.activeMoodHint) { mutableStateOf(snapshot.activeMoodHint) }
    val activeMoodTracks = snapshot.moodBuckets[selectedMood].orEmpty()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("discover_screen"),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = bottomInsetPadding + 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = strings.discoverTitle,
                    style = MaterialTheme.typography.headlineLarge,
                    color = glass.textPrimary
                )
                Text(
                    text = strings.discoverSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = glass.textSecondary
                )
            }
        }

        // Auto DJ Hero Card
        item {
            LiquidGlassSurface(
                shape = RoundedCornerShape(26.dp),
                elevated = true,
                accentGlow = glass.accentPrimary,
                modifier = Modifier.testTag("discover_autodj_card")
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(Brush.linearGradient(listOf(glass.accentPrimary, glass.accentSecondary))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = glass.onAccent)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.autoDjLabel, style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "Mix contínuo por humor, ritmo e transição harmónica local",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary
                            )
                        }
                    }
                    Button(
                        onClick = { onStartAutoDj(currentTrack ?: snapshot.smartRecommendations.firstOrNull()) },
                        enabled = audioCount > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = glass.accentPrimary, contentColor = glass.onAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("start_auto_dj_button")
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (currentTrack != null) "Iniciar Auto DJ com \"${currentTrack.title}\""
                            else "Iniciar Sessão Auto DJ"
                        )
                    }
                }
            }
        }

        // Luma 4.0 Feature Suite Launchpad
        item {
            Text("Estúdio & Ferramentas Luma 4.0", style = MaterialTheme.typography.titleMedium)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DiscoverToolTile(
                        title = strings.lumaStudioLabel,
                        subtitle = "Cortar, Fade & Toques",
                        icon = Icons.Rounded.GraphicEq,
                        onClick = onOpenStudio,
                        modifier = Modifier.weight(1f).testTag("discover_open_studio")
                    )
                    DiscoverToolTile(
                        title = strings.lumaConnectLabel,
                        subtitle = "Bluetooth, Auto & Saída",
                        icon = Icons.Rounded.BluetoothAudio,
                        onClick = onOpenConnect,
                        modifier = Modifier.weight(1f).testTag("discover_open_connect")
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DiscoverToolTile(
                        title = "Insights 4.0",
                        subtitle = "Wrapped & Comparações",
                        icon = Icons.Rounded.Insights,
                        onClick = onOpenInsights,
                        modifier = Modifier.weight(1f).testTag("discover_open_insights")
                    )
                    DiscoverToolTile(
                        title = strings.fileIntelligenceLabel,
                        subtitle = "Armazenamento & Tags",
                        icon = Icons.Rounded.FolderOpen,
                        onClick = onOpenFileIntelligence,
                        modifier = Modifier.weight(1f).testTag("discover_open_file_intel")
                    )
                }
                LiquidGlassSurface(
                    shape = RoundedCornerShape(18.dp),
                    onClick = onOpenLibraryHealth,
                    modifier = Modifier.fillMaxWidth().testTag("discover_open_health")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.HealthAndSafety, contentDescription = null, tint = glass.accentPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.libraryHealthLabel, style = MaterialTheme.typography.titleSmall)
                            Text("Capas em falta, metadados e duplicados", style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
                        }
                        Text("Abrir", style = MaterialTheme.typography.labelLarge, color = glass.accentPrimary)
                    }
                }
            }
        }

        // Offline Listening Pattern Analysis
        item {
            LiquidGlassSurface(shape = RoundedCornerShape(22.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Psychology, contentDescription = null, tint = glass.accentPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Padrão de Escuta Offline", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = snapshot.patternReport.habitSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = glass.textSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PatternChip("Pico: ${snapshot.patternReport.peakWindowLabel}", Modifier.weight(1f))
                        PatternChip("Conclusão: ${snapshot.patternReport.completionRatePercent}%", Modifier.weight(1f))
                        PatternChip(snapshot.patternReport.topRouteLabel, Modifier.weight(1f))
                    }
                }
            }
        }

        // Mood Classifier Selector
        item {
            Text("Classificação por Humor (Offline)", style = MaterialTheme.typography.titleMedium)
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(LumaMoodCategory.entries, key = { it.name }) { mood ->
                    val isSelected = selectedMood == mood
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) glass.accentPrimary else glass.glassSurface)
                            .border(
                                width = if (isSelected) 0.dp else 0.8.dp,
                                color = glass.glassBorder,
                                shape = RoundedCornerShape(50)
                            )
                            .clickable { selectedMood = mood }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                            .testTag("discover_mood_${mood.name.lowercase()}")
                    ) {
                        Text(
                            text = mood.title,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) glass.onAccent else glass.textPrimary
                        )
                    }
                }
            }
        }

        if (activeMoodTracks.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(activeMoodTracks, key = { "mood_card_${it.id}" }) { track ->
                        LiquidGlassSurface(
                            shape = RoundedCornerShape(18.dp),
                            onClick = { onPlayTrack(track, activeMoodTracks) },
                            modifier = Modifier.width(156.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                MediaArtwork(
                                    artworkPath = track.artworkPath,
                                    contentDescription = track.title,
                                    cornerRadius = 14.dp,
                                    maxDecodeSize = 160.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(112.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = "${track.artist} • ${formatDuration(track.durationMs)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Personalized Recommendations + Similar Tracks quick action
        if (snapshot.smartRecommendations.isNotEmpty()) {
            item {
                Text("Sugestões Inteligentes Para Ti", style = MaterialTheme.typography.titleMedium)
            }
            items(snapshot.smartRecommendations.take(6), key = { "smart_rec_${it.id}" }) { track ->
                LiquidGlassSurface(
                    shape = RoundedCornerShape(18.dp),
                    onClick = { onPlayTrack(track, snapshot.smartRecommendations) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MediaArtwork(
                            artworkPath = track.artworkPath,
                            contentDescription = track.title,
                            cornerRadius = 12.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = "${track.artist} • ${track.genre.ifBlank { "Biblioteca Local" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(glass.pillIndicatorBg)
                                .clickable { onOpenSimilarTracks(track) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Semelhantes",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = glass.accentPrimary
                            )
                        }
                    }
                }
            }
        }

        // Automatic Categorization Buckets
        if (snapshot.autoCategories.isNotEmpty()) {
            item {
                Text("Categorização Automática", style = MaterialTheme.typography.titleMedium)
            }
            items(snapshot.autoCategories, key = { it.title }) { bucket ->
                LiquidGlassSurface(
                    shape = RoundedCornerShape(20.dp),
                    onClick = { bucket.tracks.firstOrNull()?.let { onPlayTrack(it, bucket.tracks) } }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(glass.pillIndicatorBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Equalizer, contentDescription = null, tint = glass.accentPrimary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(bucket.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${bucket.subtitle} • ${bucket.tracks.size} faixa(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary
                            )
                        }
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Reproduzir categoria", tint = glass.accentPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverToolTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalLumaGlass.current
    LiquidGlassSurface(
        shape = RoundedCornerShape(20.dp),
        onClick = onClick,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(glass.pillIndicatorBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = glass.accentPrimary, modifier = Modifier.size(20.dp))
            }
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = glass.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PatternChip(label: String, modifier: Modifier = Modifier) {
    val glass = LocalLumaGlass.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(glass.pillIndicatorBg)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = glass.accentPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
