package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.media.FileIntelligenceReport
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LocalLumaGlass
import com.example.ui.theme.formatFileSize

@Composable
fun FileIntelligenceScreen(
    report: FileIntelligenceReport,
    bottomInsetPadding: Dp,
    onBack: () -> Unit,
    onRefreshAvailability: () -> Unit,
    onCleanInaccessibleIndex: () -> Unit,
    onReorganizeTags: () -> Unit,
    onClearAppCache: () -> Unit,
    onOpenSupportedFormats: () -> Unit,
    onExportReportTxt: () -> Unit,
    onExportReportJson: () -> Unit,
    onExportFullBackup: () -> Unit,
    onImportFullBackup: () -> Unit
) {
    BackHandler { onBack() }
    val glass = LocalLumaGlass.current
    val totalStorage = (report.totalAudioBytes + report.totalVideoBytes + report.appCacheBytes).coerceAtLeast(1L)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("file_intelligence_screen"),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = bottomInsetPadding + 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("file_intel_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Luma File Intelligence", style = MaterialTheme.typography.headlineMedium, color = glass.textPrimary)
                    Text(
                        "Armazenamento, formatos, segurança e relatórios",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                }
            }
        }

        // Storage Usage Analysis Hero Card
        item {
            LiquidGlassSurface(
                shape = RoundedCornerShape(26.dp),
                elevated = true,
                accentGlow = glass.accentPrimary,
                modifier = Modifier.testTag("file_intel_storage_card")
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(Brush.linearGradient(listOf(glass.accentPrimary, glass.accentSecondary))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Storage, contentDescription = null, tint = glass.onAccent)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Uso de Armazenamento", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${formatFileSize(report.totalAudioBytes + report.totalVideoBytes)} em média • Integridade ${report.healthScorePercent}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary
                            )
                        }
                    }

                    StorageProgressLine(
                        label = "Música (${report.audioCount} faixas)",
                        bytes = report.totalAudioBytes,
                        fraction = report.totalAudioBytes.toFloat() / totalStorage.toFloat(),
                        color = glass.accentPrimary
                    )
                    StorageProgressLine(
                        label = "Vídeo (${report.videoCount} vídeos)",
                        bytes = report.totalVideoBytes,
                        fraction = report.totalVideoBytes.toFloat() / totalStorage.toFloat(),
                        color = glass.accentSecondary
                    )
                    StorageProgressLine(
                        label = "Cache interna da app",
                        bytes = report.appCacheBytes,
                        fraction = report.appCacheBytes.toFloat() / totalStorage.toFloat(),
                        color = glass.textSecondary
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onClearAppCache, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Limpar Cache")
                        }
                        OutlinedButton(onClick = onRefreshAvailability, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verificar Tudo")
                        }
                    }
                }
            }
        }

        // Format Breakdown
        if (report.formatBreakdown.isNotEmpty()) {
            item {
                LiquidGlassSurface(shape = RoundedCornerShape(22.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Distribuição por Formato", style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = onOpenSupportedFormats) {
                                Text("Codecs")
                            }
                        }
                        report.formatBreakdown.forEach { fmt ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${fmt.extension} (${fmt.trackCount})", style = MaterialTheme.typography.bodyMedium)
                                Text(formatFileSize(fmt.totalBytes), style = MaterialTheme.typography.labelLarge, color = glass.accentPrimary)
                            }
                        }
                    }
                }
            }
        }

        // Unsupported Format & File Diagnostics
        item {
            LiquidGlassSurface(shape = RoundedCornerShape(22.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Diagnóstico & Segurança de Ficheiros", style = MaterialTheme.typography.titleMedium)
                    DiagnosticLine("Faixas sem metadados completos", report.missingMetadataTracks.size)
                    DiagnosticLine("Faixas sem capa", report.missingArtworkTracks.size)
                    DiagnosticLine("Registos inacessíveis / movidos", report.inaccessibleTracks.size)
                    DiagnosticLine("Formatos suspeitos / não suportados", report.unsupportedFiles.size)

                    report.unsupportedFiles.take(4).forEach { issue ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = glass.error, modifier = Modifier.size(16.dp))
                            Text(
                                "${issue.media.fileName}: ${issue.reason}",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onReorganizeTags,
                            colors = ButtonDefaults.buttonColors(containerColor = glass.accentPrimary, contentColor = glass.onAccent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Organizar Tags")
                        }
                        OutlinedButton(
                            onClick = onCleanInaccessibleIndex,
                            enabled = report.inaccessibleTracks.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Limpar Índice (${report.inaccessibleTracks.size})")
                        }
                    }
                }
            }
        }

        // Top Folders by Size
        if (report.folderBreakdown.isNotEmpty()) {
            item {
                Text("Maiores Pastas Locais", style = MaterialTheme.typography.titleMedium)
            }
            items(report.folderBreakdown, key = { "fi_folder_${it.folderName}" }) { folder ->
                LiquidGlassSurface(shape = RoundedCornerShape(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Folder, contentDescription = null, tint = glass.accentPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(folder.folderName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${folder.trackCount} ficheiro(s)", style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
                        }
                        Text(formatFileSize(folder.totalBytes), style = MaterialTheme.typography.labelLarge, color = glass.accentSecondary)
                    }
                }
            }
        }

        // Exportable Reports & Metadata Backup / Restoration
        item {
            LiquidGlassSurface(shape = RoundedCornerShape(24.dp), elevated = true) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Relatórios & Backup de Metadados", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Exporte diagnósticos em TXT ou JSON e faça backup de playlists, coleções, letras e metadados sem alterar ficheiros originais.",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onExportReportTxt,
                            modifier = Modifier.weight(1f).testTag("export_report_txt_button")
                        ) {
                            Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Relatório TXT")
                        }
                        OutlinedButton(
                            onClick = onExportReportJson,
                            modifier = Modifier.weight(1f).testTag("export_report_json_button")
                        ) {
                            Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Relatório JSON")
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onExportFullBackup,
                            colors = ButtonDefaults.buttonColors(containerColor = glass.accentPrimary, contentColor = glass.onAccent),
                            modifier = Modifier.weight(1f).testTag("file_intel_backup_button")
                        ) {
                            Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Backup v2")
                        }
                        OutlinedButton(
                            onClick = onImportFullBackup,
                            modifier = Modifier.weight(1f).testTag("file_intel_restore_button")
                        ) {
                            Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restaurar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageProgressLine(
    label: String,
    bytes: Long,
    fraction: Float,
    color: androidx.compose.ui.graphics.Color
) {
    val glass = LocalLumaGlass.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
            Text(formatFileSize(bytes), style = MaterialTheme.typography.labelMedium, color = glass.textPrimary)
        }
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0.02f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color = color,
            trackColor = glass.textPrimary.copy(alpha = 0.10f),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
private fun DiagnosticLine(label: String, count: Int) {
    val glass = LocalLumaGlass.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = if (count > 0) glass.accentSecondary else glass.accentPrimary
        )
    }
}
