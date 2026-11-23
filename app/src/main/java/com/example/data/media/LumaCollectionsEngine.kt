package com.example.data.media

import androidx.compose.runtime.Immutable
import com.example.data.model.DuplicateGroup
import com.example.data.model.MediaItemEntity
import com.example.data.model.MediaType

@Immutable
data class LibraryHealthReport(
    val totalAudioTracks: Int = 0,
    val totalVideoItems: Int = 0,
    val availableAudioCount: Int = 0,
    val inaccessibleCount: Int = 0,
    val withArtworkCount: Int = 0,
    val withCompleteMetadataCount: Int = 0,
    val withLyricsCount: Int = 0,
    val duplicateGroupCount: Int = 0,
    val duplicateExtraTracksCount: Int = 0,
    val artworkCoveragePercent: Int = 100,
    val metadataCoveragePercent: Int = 100,
    val lyricsCoveragePercent: Int = 0,
    val healthScore: Int = 100,
    val missingArtworkTracks: List<MediaItemEntity> = emptyList(),
    val missingMetadataTracks: List<MediaItemEntity> = emptyList(),
    val inaccessibleTracks: List<MediaItemEntity> = emptyList()
)

object LumaCollectionsEngine {

    fun computeLibraryHealth(
        allMedia: List<MediaItemEntity>,
        duplicateGroups: List<DuplicateGroup>
    ): LibraryHealthReport {
        val audio = allMedia.filter { it.mediaType == MediaType.AUDIO }
        val videos = allMedia.filter { it.mediaType == MediaType.VIDEO }
        val totalAudio = audio.size
        val inaccessible = allMedia.filter { !it.isAvailable }
        if (totalAudio == 0) {
            return LibraryHealthReport(
                totalAudioTracks = 0,
                totalVideoItems = videos.size,
                inaccessibleCount = inaccessible.size,
                inaccessibleTracks = inaccessible
            )
        }

        val missingArt = audio.filter { it.artworkPath.isNullOrBlank() }
        val missingMeta = audio.filter { item ->
            item.artist.equals("Artista desconhecido", true) ||
                item.album.equals("Álbum desconhecido", true) ||
                item.genre.isBlank()
        }
        val withArt = totalAudio - missingArt.size
        val withMeta = totalAudio - missingMeta.size
        val withLyrics = audio.count { !it.embeddedLyrics.isNullOrBlank() }

        val artPct = ((withArt * 100f) / totalAudio).toInt().coerceIn(0, 100)
        val metaPct = ((withMeta * 100f) / totalAudio).toInt().coerceIn(0, 100)
        val lyricsPct = ((withLyrics * 100f) / totalAudio).toInt().coerceIn(0, 100)
        val availablePct = (((totalAudio - audio.count { !it.isAvailable }) * 100f) / totalAudio)
            .toInt()
            .coerceIn(0, 100)

        val extraDuplicates = duplicateGroups.sumOf { (it.items.size - 1).coerceAtLeast(0) }
        val dupPenalty = ((extraDuplicates.toFloat() / totalAudio.coerceAtLeast(1)) * 40f).toInt().coerceIn(0, 20)
        val weightedScore = (artPct * 0.35f + metaPct * 0.40f + availablePct * 0.25f).toInt() - dupPenalty

        return LibraryHealthReport(
            totalAudioTracks = totalAudio,
            totalVideoItems = videos.size,
            availableAudioCount = audio.count { it.isAvailable },
            inaccessibleCount = inaccessible.size,
            withArtworkCount = withArt,
            withCompleteMetadataCount = withMeta,
            withLyricsCount = withLyrics,
            duplicateGroupCount = duplicateGroups.size,
            duplicateExtraTracksCount = extraDuplicates,
            artworkCoveragePercent = artPct,
            metadataCoveragePercent = metaPct,
            lyricsCoveragePercent = lyricsPct,
            healthScore = weightedScore.coerceIn(0, 100),
            missingArtworkTracks = missingArt,
            missingMetadataTracks = missingMeta,
            inaccessibleTracks = inaccessible
        )
    }

    /**
     * Matches tracks missing cover art to verified sibling artwork paths from the same album
     * or parent folder inside the indexed library. Never touches device files.
     */
    fun matchSharedArtworkCandidates(tracks: List<MediaItemEntity>): Map<Long, String> {
        val albumArtwork = tracks
            .filter { !it.artworkPath.isNullOrBlank() && it.album.isNotBlank() && !it.album.equals("Álbum desconhecido", true) }
            .associate { it.album.lowercase().trim() to it.artworkPath!! }

        val folderArtwork = tracks
            .filter { !it.artworkPath.isNullOrBlank() && it.folderPath.isNotBlank() }
            .associate { it.folderPath to it.artworkPath!! }

        val matches = LinkedHashMap<Long, String>()
        tracks.filter { it.mediaType == MediaType.AUDIO && it.artworkPath.isNullOrBlank() }
            .forEach { track ->
                val byAlbum = albumArtwork[track.album.lowercase().trim()]
                val byFolder = if (byAlbum == null) folderArtwork[track.folderPath] else null
                val candidate = byAlbum ?: byFolder
                if (!candidate.isNullOrBlank()) {
                    matches[track.id] = candidate
                }
            }
        return matches
    }

    fun pickRecommendedKeepId(group: DuplicateGroup): Long? {
        return group.items.maxByOrNull { item ->
            var score = 0L
            if (item.isAvailable) score += 10_000L
            if (!item.artworkPath.isNullOrBlank()) score += 500L
            if (!item.embeddedLyrics.isNullOrBlank()) score += 300L
            if (item.isFavorite) score += 400L
            if (!item.artist.equals("Artista desconhecido", true)) score += 200L
            if (!item.album.equals("Álbum desconhecido", true)) score += 200L
            score += item.playCount * 20L
            score += (item.sizeBytes / 1024L).coerceAtMost(2000L)
            score
        }?.id
    }
}
