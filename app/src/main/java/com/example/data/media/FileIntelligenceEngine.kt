package com.example.data.media

import androidx.compose.runtime.Immutable
import com.example.data.model.MediaItemEntity
import com.example.data.model.MediaType
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

@Immutable
data class FormatStorageStat(
    val extension: String,
    val trackCount: Int,
    val totalBytes: Long
)

@Immutable
data class FolderStorageStat(
    val folderName: String,
    val trackCount: Int,
    val totalBytes: Long
)

@Immutable
data class UnsupportedMediaIssue(
    val media: MediaItemEntity,
    val reason: String
)

@Immutable
data class FileIntelligenceReport(
    val totalAudioBytes: Long = 0L,
    val totalVideoBytes: Long = 0L,
    val appCacheBytes: Long = 0L,
    val audioCount: Int = 0,
    val videoCount: Int = 0,
    val formatBreakdown: List<FormatStorageStat> = emptyList(),
    val folderBreakdown: List<FolderStorageStat> = emptyList(),
    val largestFiles: List<MediaItemEntity> = emptyList(),
    val missingMetadataTracks: List<MediaItemEntity> = emptyList(),
    val missingArtworkTracks: List<MediaItemEntity> = emptyList(),
    val inaccessibleTracks: List<MediaItemEntity> = emptyList(),
    val unsupportedFiles: List<UnsupportedMediaIssue> = emptyList(),
    val healthScorePercent: Int = 100
)

object FileIntelligenceEngine {

    private val knownUnsupportedExtensions = setOf("wma", "ra", "rm", "midi", "mid", "ape", "dsf", "dff")

    fun analyze(
        allMedia: List<MediaItemEntity>,
        appCacheBytes: Long,
        deviceCodecs: List<FormatCapability>
    ): FileIntelligenceReport {
        val available = allMedia.filter { it.isAvailable }
        val audio = available.filter { it.mediaType == MediaType.AUDIO }
        val video = available.filter { it.mediaType == MediaType.VIDEO }

        val totalAudioBytes = audio.sumOf { it.sizeBytes.coerceAtLeast(0L) }
        val totalVideoBytes = video.sumOf { it.sizeBytes.coerceAtLeast(0L) }

        val formatBreakdown = available
            .groupBy { item ->
                val ext = item.fileName.substringAfterLast('.', "").uppercase(Locale.ROOT)
                ext.ifBlank { item.mimeType.substringAfter('/', "OUTRO").uppercase(Locale.ROOT) }
            }
            .map { (ext, list) ->
                FormatStorageStat(
                    extension = ext,
                    trackCount = list.size,
                    totalBytes = list.sumOf { it.sizeBytes.coerceAtLeast(0L) }
                )
            }
            .sortedByDescending { it.totalBytes }
            .take(8)

        val folderBreakdown = available
            .groupBy { it.folderName.ifBlank { "Raiz" } }
            .map { (folder, list) ->
                FolderStorageStat(
                    folderName = folder,
                    trackCount = list.size,
                    totalBytes = list.sumOf { it.sizeBytes.coerceAtLeast(0L) }
                )
            }
            .sortedByDescending { it.totalBytes }
            .take(8)

        val largestFiles = available
            .sortedByDescending { it.sizeBytes }
            .take(8)

        val missingMeta = audio.filter {
            it.artist.equals("Artista desconhecido", ignoreCase = true) ||
                it.album.equals("Álbum desconhecido", ignoreCase = true) ||
                it.genre.isBlank()
        }
        val missingArt = audio.filter { it.artworkPath.isNullOrBlank() }
        val inaccessible = allMedia.filter { !it.isAvailable }

        val supportedMimes = deviceCodecs.filter { it.isSupported }.map { it.mimeType.lowercase(Locale.ROOT) }.toSet()
        val unsupported = mutableListOf<UnsupportedMediaIssue>()
        available.forEach { item ->
            val ext = item.fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
            val mime = item.mimeType.lowercase(Locale.ROOT)
            when {
                item.sizeBytes in 1L..511L -> {
                    unsupported.add(UnsupportedMediaIssue(item, "Ficheiro muito pequeno ou truncado (${item.sizeBytes} bytes)"))
                }
                ext in knownUnsupportedExtensions -> {
                    unsupported.add(UnsupportedMediaIssue(item, "Extensão .$ext normalmente não suportada nativamente"))
                }
                supportedMimes.isNotEmpty() && mime.isNotBlank() &&
                    mime != "audio/*" && mime != "video/*" &&
                    mime !in supportedMimes &&
                    !mime.contains("mp4") && !mime.contains("mpeg") && !mime.contains("aac") &&
                    !mime.contains("flac") && !mime.contains("wav") && !mime.contains("x-wav") &&
                    !mime.contains("ogg") && !mime.contains("opus") -> {
                    unsupported.add(UnsupportedMediaIssue(item, "Codec \"$mime\" sem descodificador verificado"))
                }
            }
        }

        val total = allMedia.size.coerceAtLeast(1)
        val issueCount = missingMeta.size + (missingArt.size / 2) + inaccessible.size * 2 + unsupported.size * 2
        val healthScore = (100 - ((issueCount * 100) / (total * 2))).coerceIn(25, 100)

        return FileIntelligenceReport(
            totalAudioBytes = totalAudioBytes,
            totalVideoBytes = totalVideoBytes,
            appCacheBytes = appCacheBytes.coerceAtLeast(0L),
            audioCount = audio.size,
            videoCount = video.size,
            formatBreakdown = formatBreakdown,
            folderBreakdown = folderBreakdown,
            largestFiles = largestFiles,
            missingMetadataTracks = missingMeta,
            missingArtworkTracks = missingArt,
            inaccessibleTracks = inaccessible,
            unsupportedFiles = unsupported,
            healthScorePercent = if (allMedia.isEmpty()) 100 else healthScore
        )
    }

    fun exportReportAsText(report: FileIntelligenceReport): String {
        return buildString {
            appendLine("=== LUMA PLAYER 4.0 — RELATÓRIO DE BIBLIOTECA ===")
            appendLine("Pontuação de Integridade: ${report.healthScorePercent}%")
            appendLine("Faixas Áudio: ${report.audioCount} (${formatBytesReadable(report.totalAudioBytes)})")
            appendLine("Vídeos: ${report.videoCount} (${formatBytesReadable(report.totalVideoBytes)})")
            appendLine("Cache App: ${formatBytesReadable(report.appCacheBytes)}")
            appendLine("Sem metadados: ${report.missingMetadataTracks.size}")
            appendLine("Sem capa: ${report.missingArtworkTracks.size}")
            appendLine("Indisponíveis: ${report.inaccessibleTracks.size}")
            appendLine("Formatos suspeitos/não suportados: ${report.unsupportedFiles.size}")
            appendLine()
            appendLine("-- Formatos --")
            report.formatBreakdown.forEach { fmt ->
                appendLine("${fmt.extension}: ${fmt.trackCount} ficheiro(s) • ${formatBytesReadable(fmt.totalBytes)}")
            }
            appendLine()
            appendLine("-- Pastas --")
            report.folderBreakdown.forEach { folder ->
                appendLine("${folder.folderName}: ${folder.trackCount} ficheiro(s) • ${formatBytesReadable(folder.totalBytes)}")
            }
        }
    }

    fun exportReportAsJson(report: FileIntelligenceReport): String {
        val root = JSONObject().apply {
            put("app", "Luma Player 4.0")
            put("generatedAt", System.currentTimeMillis())
            put("healthScorePercent", report.healthScorePercent)
            put("audioCount", report.audioCount)
            put("videoCount", report.videoCount)
            put("totalAudioBytes", report.totalAudioBytes)
            put("totalVideoBytes", report.totalVideoBytes)
            put("appCacheBytes", report.appCacheBytes)
            put("missingMetadataCount", report.missingMetadataTracks.size)
            put("missingArtworkCount", report.missingArtworkTracks.size)
            put("inaccessibleCount", report.inaccessibleTracks.size)
            put("unsupportedCount", report.unsupportedFiles.size)
            put("formats", JSONArray().apply {
                report.formatBreakdown.forEach { fmt ->
                    put(JSONObject().apply {
                        put("extension", fmt.extension)
                        put("count", fmt.trackCount)
                        put("bytes", fmt.totalBytes)
                    })
                }
            })
            put("folders", JSONArray().apply {
                report.folderBreakdown.forEach { folder ->
                    put(JSONObject().apply {
                        put("name", folder.folderName)
                        put("count", folder.trackCount)
                        put("bytes", folder.totalBytes)
                    })
                }
            })
        }
        return root.toString(2)
    }

    private fun formatBytesReadable(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024.0) String.format(Locale.US, "%.2f GB", mb / 1024.0)
        else String.format(Locale.US, "%.1f MB", mb)
    }
}
