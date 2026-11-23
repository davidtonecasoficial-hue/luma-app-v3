package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.media.DayListeningStat
import com.example.data.media.LumaInsightsReport
import com.example.data.media.RankedStat
import com.example.data.model.MediaItemEntity
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LocalLumaGlass
import com.example.ui.theme.LumaSectionEntrance
import com.example.ui.theme.MediaArtwork
import com.example.ui.theme.formatDuration

@Composable
fun InsightsScreen(
    report: LumaInsightsReport,
    bottomInsetPadding: Dp,
    onBack: () -> Unit,
    onPlayTrack: (MediaItemEntity, List<MediaItemEntity>) -> Unit
) {
    BackHandler(onBack = onBack)
    val glass = LocalLumaGlass.current
    var showYearlyWrapped by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("insights_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("insights_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Voltar"
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Luma Insights 4.0",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Estatísticas locais, comparações e Luma Wrapped",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 6.dp,
                bottom = bottomInsetPadding + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Luma Wrapped Hero Card (Monthly / Yearly Switcher)
            item {
                LumaSectionEntrance(index = 0) {
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(26.dp),
                        elevated = true,
                        accentGlow = glass.accentPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("insights_wrapped_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = glass.accentPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (showYearlyWrapped) "WRAPPED ANUAL" else "WRAPPED MENSAL",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = glass.accentPrimary
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    WrappedPeriodChip(
                                        label = "Anual",
                                        selected = showYearlyWrapped,
                                        onClick = { showYearlyWrapped = true },
                                        tag = "insights_wrapped_yearly"
                                    )
                                    WrappedPeriodChip(
                                        label = "Mensal",
                                        selected = !showYearlyWrapped,
                                        onClick = { showYearlyWrapped = false },
                                        tag = "insights_wrapped_monthly"
                                    )
                                }
                            }

                            Text(
                                text = if (showYearlyWrapped) {
                                    report.wrappedHeadline
                                } else {
                                    report.monthlyWrappedHeadline.ifBlank { report.wrappedHeadline }
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = glass.textPrimary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Pico: ${report.peakListeningPeriod}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = glass.textSecondary
                                )
                                Text("•", color = glass.textSecondary)
                                Text(
                                    text = if (showYearlyWrapped) {
                                        "${report.annualTotalPlays} plays • ${report.annualMinutes} min"
                                    } else {
                                        "${report.monthlyTotalPlays} plays • ${report.monthlyMinutes} min"
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = glass.accentSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 2x2 Summary KPI Grid
            item {
                LumaSectionEntrance(index = 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            InsightStatTile(
                                icon = Icons.Rounded.Schedule,
                                label = "Tempo total",
                                value = formatDuration(report.totalListeningTimeMs),
                                modifier = Modifier.weight(1f)
                            )
                            InsightStatTile(
                                icon = Icons.Rounded.GraphicEq,
                                label = "Reproduções",
                                value = "${report.totalPlays}",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            InsightStatTile(
                                icon = Icons.Rounded.MusicNote,
                                label = "Faixas únicas",
                                value = "${report.uniqueTracksPlayed}",
                                modifier = Modifier.weight(1f)
                            )
                            InsightStatTile(
                                icon = Icons.Rounded.Headphones,
                                label = "Artistas ouvidos",
                                value = "${report.uniqueArtistsHeard}",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Listening History Comparison Card (This Month vs Last Month, This Week vs Last Week)
            item {
                val cmp = report.comparison
                LiquidGlassSurface(
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("insights_comparison_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Comparação de Histórico",
                            style = MaterialTheme.typography.titleMedium,
                            color = glass.textPrimary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ComparisonDeltaTile(
                                title = "Este Mês vs Anterior",
                                currentLabel = "${cmp.thisMonthPlays} plays • ${cmp.thisMonthMinutes} min",
                                previousLabel = "Anterior: ${cmp.lastMonthPlays} plays",
                                deltaPercent = cmp.monthDeltaPercent,
                                modifier = Modifier.weight(1f)
                            )
                            ComparisonDeltaTile(
                                title = "Esta Semana vs Ant.",
                                currentLabel = "${cmp.thisWeekPlays} plays • ${cmp.thisWeekMinutes} min",
                                previousLabel = "Anterior: ${cmp.lastWeekPlays} plays",
                                deltaPercent = cmp.weekDeltaPercent,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Conclusão: ${cmp.completionRatePercent}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = glass.accentPrimary
                            )
                            Text(
                                text = "Salto cedo: ${cmp.earlySkipRatePercent}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = glass.textSecondary
                            )
                            Text(
                                text = "Rota: ${cmp.topOutputRoute}",
                                style = MaterialTheme.typography.labelLarge,
                                color = glass.accentSecondary
                            )
                        }
                    }
                }
            }

            // Weekly 7-Day Listening Chart
            item {
                LumaSectionEntrance(index = 2) {
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Estatísticas Semanais (7 dias)",
                                style = MaterialTheme.typography.titleMedium,
                                color = glass.textPrimary
                            )
                            WeeklyBarChart(stats = report.weeklyStats)
                        }
                    }
                }
            }

            // Monthly 4-Week & Annual Report
            item {
                LumaSectionEntrance(index = 3) {
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Estatísticas Mensais e Relatório Anual",
                                style = MaterialTheme.typography.titleMedium,
                                color = glass.textPrimary
                            )
                            val maxWeekPlays = (report.monthlyStats.maxOfOrNull { it.plays } ?: 1).coerceAtLeast(1)
                            report.monthlyStats.forEach { week ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = week.weekLabel,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = glass.textPrimary
                                        )
                                        Text(
                                            text = "${week.plays} reprod. • ${week.estimatedMinutes} min",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = glass.textSecondary
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { (week.plays.toFloat() / maxWeekPlays.toFloat()).coerceIn(0.04f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(50)),
                                        color = glass.accentPrimary,
                                        trackColor = glass.textPrimary.copy(alpha = 0.12f),
                                        strokeCap = StrokeCap.Round
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(glass.pillIndicatorBg)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Resumo Anual",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = glass.accentPrimary
                                        )
                                        Text(
                                            text = "${report.annualTotalPlays} reproduções nos últimos 12 meses",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = glass.textSecondary
                                        )
                                    }
                                    Text(
                                        text = "${report.annualMinutes} min",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = glass.accentPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Most Played Songs
            if (report.topSongs.isNotEmpty()) {
                item {
                    Text(
                        text = "Músicas Mais Ouvidas",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                itemsIndexed(report.topSongs, key = { _, song -> "ins_song_${song.id}" }) { idx, song ->
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(18.dp),
                        onClick = { onPlayTrack(song, report.topSongs) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#${idx + 1}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = glass.accentPrimary,
                                modifier = Modifier.width(34.dp)
                            )
                            MediaArtwork(
                                artworkPath = song.artworkPath,
                                contentDescription = song.title,
                                cornerRadius = 10.dp,
                                modifier = Modifier.size(46.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${song.artist} • ${song.playCount}x",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = glass.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Tocar ${song.title}",
                                tint = glass.accentPrimary
                            )
                        }
                    }
                }
            }

            // Ranked Artists, Albums, Genres
            if (report.topArtists.isNotEmpty()) {
                item {
                    RankedStatSection(
                        title = "Artistas Mais Ouvidos",
                        icon = Icons.Rounded.Person,
                        stats = report.topArtists
                    )
                }
            }

            if (report.topAlbums.isNotEmpty()) {
                item {
                    RankedStatSection(
                        title = "Álbuns Mais Ouvidos",
                        icon = Icons.Rounded.Album,
                        stats = report.topAlbums
                    )
                }
            }

            if (report.topGenres.isNotEmpty()) {
                item {
                    RankedStatSection(
                        title = "Géneros em Destaque",
                        icon = Icons.Rounded.Category,
                        stats = report.topGenres
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightStatTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val glass = LocalLumaGlass.current
    LiquidGlassSurface(
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
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
                    imageVector = icon,
                    contentDescription = null,
                    tint = glass.accentPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = glass.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = glass.textSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun WeeklyBarChart(stats: List<DayListeningStat>) {
    val glass = LocalLumaGlass.current
    val maxPlays = (stats.maxOfOrNull { it.plays } ?: 1).coerceAtLeast(1)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val count = stats.size.coerceAtLeast(1)
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.48f).coerceAtMost(28.dp.toPx())
            val maxBarHeight = size.height - 8.dp.toPx()

            stats.forEachIndexed { index, stat ->
                val ratio = (stat.plays.toFloat() / maxPlays.toFloat()).coerceIn(0f, 1f)
                val barHeight = (maxBarHeight * ratio).coerceAtLeast(8.dp.toPx())
                val left = index * slotWidth + (slotWidth - barWidth) / 2f
                val top = size.height - barHeight

                // Track background
                drawRoundRect(
                    color = glass.textPrimary.copy(alpha = 0.08f),
                    topLeft = Offset(left, 4.dp.toPx()),
                    size = Size(barWidth, maxBarHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )

                // Active bar
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(glass.accentPrimary, glass.accentSecondary)),
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            stats.forEach { stat ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stat.dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textSecondary
                    )
                    Text(
                        text = "${stat.plays}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = glass.accentPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun RankedStatSection(
    title: String,
    icon: ImageVector,
    stats: List<RankedStat>
) {
    val glass = LocalLumaGlass.current
    LiquidGlassSurface(
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = glass.accentPrimary, modifier = Modifier.size(18.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = glass.textPrimary
                )
            }

            stats.forEachIndexed { idx, stat ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${idx + 1}.",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = glass.accentPrimary,
                        modifier = Modifier.width(24.dp)
                    )
                    if (stat.artworkPath != null) {
                        MediaArtwork(
                            artworkPath = stat.artworkPath,
                            contentDescription = stat.label,
                            cornerRadius = 8.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stat.label,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stat.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = "${stat.plays}x",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = glass.accentSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun WrappedPeriodChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    val glass = LocalLumaGlass.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) glass.accentPrimary else glass.glassSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 5.dp)
            .testTag(tag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (selected) glass.onAccent else glass.textSecondary
        )
    }
}

@Composable
private fun ComparisonDeltaTile(
    title: String,
    currentLabel: String,
    previousLabel: String,
    deltaPercent: Int,
    modifier: Modifier = Modifier
) {
    val glass = LocalLumaGlass.current
    val deltaText = when {
        deltaPercent > 0 -> "+$deltaPercent%"
        else -> "$deltaPercent%"
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(glass.glassSurface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = glass.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = deltaText,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (deltaPercent >= 0) glass.accentPrimary else glass.error
            )
        }
        Text(currentLabel, style = MaterialTheme.typography.labelLarge, color = glass.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(previousLabel, style = MaterialTheme.typography.bodySmall, color = glass.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
