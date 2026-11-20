package com.example.data.media

import android.net.Uri
import com.example.data.local.HistoryDao
import com.example.data.local.MediaDao
import com.example.data.local.PlaylistDao
import com.example.data.local.QueueDao
import com.example.data.model.AlbumSummary
import com.example.data.model.ArtistSummary
import com.example.data.model.DuplicateGroup
import com.example.data.model.FolderSummary
import com.example.data.model.LibraryFilter
import com.example.data.model.GenreSummary
import com.example.data.model.HistoryWithMedia
import com.example.data.model.MediaItemEntity
import com.example.data.model.MediaType
import com.example.data.model.MoodType
import com.example.data.model.PlaybackHistoryEntity
import com.example.data.model.PlaylistEntity
import com.example.data.model.PlaylistTrackCrossRef
import com.example.data.model.PlaylistWithTracks
import com.example.data.model.SearchFilter
import com.example.data.model.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class SearchResultsBundle(
    val songs: List<MediaItemEntity> = emptyList(),
    val albums: List<AlbumSummary> = emptyList(),
    val artists: List<ArtistSummary> = emptyList(),
    val videos: List<MediaItemEntity> = emptyList(),
    val playlists: List<PlaylistWithTracks> = emptyList()
) {
    val isEmpty: Boolean
        get() = songs.isEmpty() && albums.isEmpty() && artists.isEmpty() && videos.isEmpty() && playlists.isEmpty()
}

class MediaRepository(
    private val mediaDao: MediaDao,
    private val playlistDao: PlaylistDao,
    private val historyDao: HistoryDao,
    private val queueDao: QueueDao,
    private val scanner: MediaScanner
) {
    val allMedia: Flow<List<MediaItemEntity>> = mediaDao.observeAllMedia()
    val audioTracks: Flow<List<MediaItemEntity>> = mediaDao.observeByType(MediaType.AUDIO)
    val videos: Flow<List<MediaItemEntity>> = mediaDao.observeByType(MediaType.VIDEO)
    val favorites: Flow<List<MediaItemEntity>> = mediaDao.observeFavorites()
    val continueListening: Flow<List<MediaItemEntity>> = mediaDao.observeContinueListening()
    val mostPlayed: Flow<List<MediaItemEntity>> = mediaDao.observeMostPlayed()
    val playlists: Flow<List<PlaylistWithTracks>> = combine(
        playlistDao.observePlaylistsWithTracks(),
        playlistDao.observeAllTrackRefs()
    ) { rawPlaylists, allRefs ->
        val refsByPlaylist = allRefs.groupBy { it.playlistId }
        rawPlaylists.map { pl ->
            val posMap = refsByPlaylist[pl.playlist.id]
                ?.associate { it.mediaId to it.position }
                .orEmpty()
            pl.copy(tracks = pl.tracks.sortedBy { posMap[it.id] ?: Int.MAX_VALUE })
        }
    }
    val history: Flow<List<HistoryWithMedia>> = historyDao.observeHistory()
    val insightsHistory: Flow<List<HistoryWithMedia>> = historyDao.observeInsightsHistory()

    val albums: Flow<List<AlbumSummary>> = audioTracks.map { tracks ->
        tracks.groupBy { it.album.ifBlank { "Álbum desconhecido" } }
            .map { (albumName, albumTracks) ->
                AlbumSummary(
                    name = albumName,
                    artist = ArtistNormalizer.pickBestDisplayName(albumTracks),
                    artworkPath = albumTracks.firstOrNull { it.artworkPath != null }?.artworkPath,
                    trackCount = albumTracks.size,
                    totalDurationMs = albumTracks.sumOf { it.durationMs },
                    tracks = albumTracks.sortedWith(compareBy<MediaItemEntity> {
                        if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE
                    }.thenBy { it.title.lowercase() })
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    val artists: Flow<List<ArtistSummary>> = audioTracks.map { tracks ->
        tracks.groupBy { ArtistNormalizer.artistGroupingKey(it.artist, it.fileName) }
            .values
            .map { artistTracks ->
                val unifiedName = ArtistNormalizer.pickBestDisplayName(artistTracks)
                ArtistSummary(
                    name = unifiedName,
                    trackCount = artistTracks.size,
                    albumCount = artistTracks.map { it.album.ifBlank { "Álbum desconhecido" } }.distinct().size,
                    artworkPath = artistTracks.firstOrNull { it.artworkPath != null }?.artworkPath,
                    tracks = artistTracks.sortedWith(
                        compareBy<MediaItemEntity> { it.album.lowercase() }
                            .thenBy { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
                            .thenBy { it.title.lowercase() }
                    )
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    val folders: Flow<List<FolderSummary>> = allMedia.map { items ->
        items.groupBy { it.folderName.ifBlank { "Importados" } }
            .map { (folderName, folderItems) ->
                FolderSummary(
                    name = folderName,
                    path = folderItems.firstOrNull()?.folderPath.orEmpty(),
                    itemCount = folderItems.size,
                    tracks = folderItems
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    val genres: Flow<List<GenreSummary>> = audioTracks.map { tracks ->
        tracks.filter { it.genre.isNotBlank() }
            .groupBy { it.genre.trim() }
            .map { (genreName, genreTracks) ->
                GenreSummary(
                    name = genreName,
                    trackCount = genreTracks.size,
                    artworkPath = genreTracks.firstOrNull { it.artworkPath != null }?.artworkPath,
                    tracks = genreTracks
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    fun searchLocal(queryFlow: Flow<String>, filterFlow: Flow<SearchFilter>): Flow<SearchResultsBundle> {
        return combine(allMedia, albums, artists, playlists, queryFlow, filterFlow) { values ->
            @Suppress("UNCHECKED_CAST")
            val media = values[0] as List<MediaItemEntity>
            @Suppress("UNCHECKED_CAST")
            val allAlbums = values[1] as List<AlbumSummary>
            @Suppress("UNCHECKED_CAST")
            val allArtists = values[2] as List<ArtistSummary>
            @Suppress("UNCHECKED_CAST")
            val allPlaylists = values[3] as List<PlaylistWithTracks>
            val rawQuery = values[4] as String
            val filter = values[5] as SearchFilter

            val q = rawQuery.trim().lowercase()
            if (q.isEmpty()) {
                return@combine SearchResultsBundle()
            }

            val matchedSongs = if (filter == SearchFilter.ALL || filter == SearchFilter.SONGS) {
                media.filter { item ->
                    item.mediaType == MediaType.AUDIO && (
                        item.title.lowercase().contains(q) ||
                            item.artist.lowercase().contains(q) ||
                            item.album.lowercase().contains(q) ||
                            item.fileName.lowercase().contains(q) ||
                            item.folderName.lowercase().contains(q)
                        )
                }
            } else emptyList()

            val matchedAlbums = if (filter == SearchFilter.ALL || filter == SearchFilter.ALBUMS) {
                allAlbums.filter { album ->
                    album.name.lowercase().contains(q) || album.artist.lowercase().contains(q)
                }
            } else emptyList()

            val matchedArtists = if (filter == SearchFilter.ALL || filter == SearchFilter.ARTISTS) {
                allArtists.filter { artist ->
                    artist.name.lowercase().contains(q)
                }
            } else emptyList()

            val matchedVideos = if (filter == SearchFilter.ALL || filter == SearchFilter.VIDEOS) {
                media.filter { item ->
                    item.mediaType == MediaType.VIDEO && (
                        item.title.lowercase().contains(q) ||
                            item.fileName.lowercase().contains(q) ||
                            item.folderName.lowercase().contains(q)
                        )
                }
            } else emptyList()

            val matchedPlaylists = if (filter == SearchFilter.ALL) {
                allPlaylists.filter { pl ->
                    pl.playlist.name.lowercase().contains(q) ||
                        pl.tracks.any { it.title.lowercase().contains(q) }
                }
            } else emptyList()

            SearchResultsBundle(
                songs = matchedSongs,
                albums = matchedAlbums,
                artists = matchedArtists,
                videos = matchedVideos,
                playlists = matchedPlaylists
            )
        }
    }

    suspend fun importUris(
        uris: List<Uri>,
        lrcSidecars: Map<String, Uri> = emptyMap()
    ): ImportSummary {
        var imported = 0
        var duplicates = 0
        var failed = 0
        val messages = mutableListOf<String>()

        for (uri in uris) {
            val uriStr = uri.toString()
            if (mediaDao.getByUri(uriStr) != null) {
                duplicates++
                continue
            }

            val extracted = scanner.extractMediaItem(uri)
            extracted.onSuccess { entity ->
                val dupByNameSize = if (entity.sizeBytes > 0L) {
                    mediaDao.findDuplicate(entity.fileName, entity.sizeBytes)
                } else null

                if (dupByNameSize != null) {
                    duplicates++
                    messages.add("Duplicado detetado: ${entity.fileName}")
                } else {
                    val baseName = entity.fileName.substringBeforeLast('.', entity.fileName).lowercase().trim()
                    val matchedLrcUri = lrcSidecars[baseName]
                    val autoLyrics = if (matchedLrcUri != null) {
                        scanner.readLyricsFromUri(matchedLrcUri).getOrNull()
                    } else null
                    val finalEntity = if (autoLyrics != null) {
                        entity.copy(embeddedLyrics = autoLyrics)
                    } else {
                        entity
                    }
                    mediaDao.insert(finalEntity)
                    imported++
                }
            }.onFailure { err ->
                failed++
                messages.add(err.message ?: "Erro ao ler ficheiro")
            }
        }

        return ImportSummary(imported, duplicates, failed, messages)
    }

    suspend fun importFolderTree(treeUri: Uri): ImportSummary {
        val (uris, lrcSidecars) = scanner.collectMediaAndLrcFromTree(treeUri)
        if (uris.isEmpty()) {
            return ImportSummary(0, 0, 0, listOf("Nenhum ficheiro de áudio ou vídeo encontrado nesta pasta."))
        }
        return importUris(uris, lrcSidecars)
    }

    suspend fun syncFromMediaStore(
        scanAudio: Boolean,
        scanVideo: Boolean
    ): MediaStoreScanSummary {
        if (!scanAudio && !scanVideo) {
            return MediaStoreScanSummary(totalScanned = 0, addedCount = 0, updatedCount = 0, missingCount = 0)
        }
        val discovered = scanner.scanMediaStore(scanAudio = scanAudio, scanVideo = scanVideo)
        val existingAll = mediaDao.getAllOnce()
        val existingByUri = existingAll.associateBy { it.uri }
        val discoveredUris = discovered.map { it.uri }.toHashSet()

        var addedCount = 0
        var updatedCount = 0
        var missingCount = 0

        for (item in discovered) {
            val existing = existingByUri[item.uri]
            if (existing != null) {
                val merged = existing.copy(
                    title = item.title.ifBlank { existing.title },
                    artist = item.artist.ifBlank { existing.artist },
                    album = item.album.ifBlank { existing.album },
                    genre = item.genre.ifBlank { existing.genre },
                    durationMs = if (item.durationMs > 0L) item.durationMs else existing.durationMs,
                    folderName = item.folderName.ifBlank { existing.folderName },
                    folderPath = item.folderPath.ifBlank { existing.folderPath },
                    mimeType = item.mimeType.ifBlank { existing.mimeType },
                    sizeBytes = if (item.sizeBytes > 0L) item.sizeBytes else existing.sizeBytes,
                    videoWidth = if (item.videoWidth > 0) item.videoWidth else existing.videoWidth,
                    videoHeight = if (item.videoHeight > 0) item.videoHeight else existing.videoHeight,
                    artworkPath = item.artworkPath ?: existing.artworkPath,
                    isAvailable = true
                )
                if (merged != existing) {
                    mediaDao.update(merged)
                    updatedCount++
                }
            } else {
                val dupByNameSize = if (item.sizeBytes > 0L) {
                    mediaDao.findDuplicate(item.fileName, item.sizeBytes)
                } else null
                if (dupByNameSize == null) {
                    mediaDao.insert(item)
                    addedCount++
                }
            }
        }

        for (existing in existingAll) {
            val isManagedMediaStoreUri = existing.uri.startsWith("content://media/")
            val typeWasScanned = (existing.mediaType == MediaType.AUDIO && scanAudio) ||
                (existing.mediaType == MediaType.VIDEO && scanVideo)
            if (isManagedMediaStoreUri && typeWasScanned && existing.uri !in discoveredUris) {
                if (existing.isAvailable) {
                    mediaDao.setAvailable(existing.id, false)
                }
                missingCount++
            }
        }

        return MediaStoreScanSummary(
            totalScanned = discovered.size,
            addedCount = addedCount,
            updatedCount = updatedCount,
            missingCount = missingCount
        )
    }

    suspend fun refreshAvailability(): Int {
        val all = mediaDao.getAllOnce()
        var unavailableCount = 0
        for (item in all) {
            val ok = scanner.isUriAccessible(item.uri)
            if (item.isAvailable != ok) {
                mediaDao.setAvailable(item.id, ok)
            }
            if (!ok) unavailableCount++
        }
        return unavailableCount
    }

    suspend fun markUnavailable(id: Long) = mediaDao.setAvailable(id, false)

    suspend fun getMediaById(id: Long): MediaItemEntity? = mediaDao.getById(id)

    suspend fun toggleFavorite(item: MediaItemEntity) {
        mediaDao.setFavorite(item.id, !item.isFavorite)
    }

    suspend fun recordPlaybackStarted(mediaId: Long, pauseHistory: Boolean = false) {
        if (pauseHistory) return
        val now = System.currentTimeMillis()
        mediaDao.incrementPlayCount(mediaId, now)
        historyDao.insert(PlaybackHistoryEntity(mediaId = mediaId, playedAt = now))
    }

    suspend fun savePlaybackPosition(mediaId: Long, positionMs: Long) {
        mediaDao.updatePosition(mediaId, positionMs, System.currentTimeMillis())
    }

    suspend fun deleteMediaFromLibrary(mediaId: Long) {
        mediaDao.deleteById(mediaId)
    }

    suspend fun createPlaylist(name: String, initialTrackIds: List<Long> = emptyList()): Long {
        val now = System.currentTimeMillis()
        val id = playlistDao.insertPlaylist(
            PlaylistEntity(name = name.trim(), createdAt = now, updatedAt = now)
        )
        initialTrackIds.forEachIndexed { index, mediaId ->
            playlistDao.insertTrackRef(
                PlaylistTrackCrossRef(playlistId = id, mediaId = mediaId, position = index)
            )
        }
        return id
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) {
        playlistDao.renamePlaylist(playlistId, newName.trim(), System.currentTimeMillis())
    }

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, mediaId: Long) {
        val existing = playlistDao.getTrackRefs(playlistId)
        val nextPos = (existing.maxOfOrNull { it.position } ?: -1) + 1
        playlistDao.insertTrackRef(
            PlaylistTrackCrossRef(playlistId = playlistId, mediaId = mediaId, position = nextPos)
        )
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, mediaId: Long) {
        playlistDao.removeTrackFromPlaylist(playlistId, mediaId)
        playlistDao.touchPlaylist(playlistId, System.currentTimeMillis())
    }

    suspend fun movePlaylistTrack(playlistId: Long, fromIndex: Int, toIndex: Int) {
        val refs = playlistDao.getTrackRefs(playlistId).toMutableList()
        if (fromIndex !in refs.indices || toIndex !in refs.indices || fromIndex == toIndex) return
        val moved = refs.removeAt(fromIndex)
        refs.add(toIndex, moved)
        val reindexed = refs.mapIndexed { idx, ref -> ref.copy(position = idx) }
        playlistDao.insertTrackRefs(reindexed)
        playlistDao.touchPlaylist(playlistId, System.currentTimeMillis())
    }

    suspend fun updateTrackMetadata(
        mediaId: Long,
        title: String,
        artist: String,
        album: String,
        genre: String,
        releaseYear: Int = 0,
        trackNumber: Int = 0
    ) {
        mediaDao.updateMetadata(
            id = mediaId,
            title = title.trim().ifBlank { "Sem título" },
            artist = ArtistNormalizer.normalizePrimaryArtist(artist, "").takeIf { it != "Artista desconhecido" }
                ?: artist.trim().ifBlank { "Artista desconhecido" },
            album = album.trim().ifBlank { "Álbum desconhecido" },
            genre = genre.trim(),
            releaseYear = releaseYear.coerceAtLeast(0),
            trackNumber = trackNumber.coerceAtLeast(0)
        )
    }

    suspend fun batchUpdateMetadata(
        mediaIds: Set<Long>,
        artist: String?,
        album: String?,
        genre: String?,
        releaseYear: Int? = null
    ): Int {
        var count = 0
        for (id in mediaIds) {
            val item = mediaDao.getById(id) ?: continue
            val newArtist = artist?.trim()?.takeIf { it.isNotEmpty() } ?: item.artist
            val newAlbum = album?.trim()?.takeIf { it.isNotEmpty() } ?: item.album
            val newGenre = genre?.trim()?.takeIf { it.isNotEmpty() } ?: item.genre
            val newYear = releaseYear?.takeIf { it > 0 } ?: item.releaseYear
            mediaDao.updateMetadata(
                id = id,
                title = item.title,
                artist = ArtistNormalizer.normalizePrimaryArtist(newArtist, item.fileName),
                album = newAlbum,
                genre = newGenre,
                releaseYear = newYear,
                trackNumber = item.trackNumber
            )
            count++
        }
        return count
    }

    suspend fun updateTrackArtwork(mediaId: Long, artworkPath: String?) {
        mediaDao.updateArtwork(mediaId, artworkPath)
    }

    suspend fun updateTrackLyrics(mediaId: Long, lyrics: String?) {
        mediaDao.updateLyrics(mediaId, lyrics)
    }

    suspend fun updateTrackLyricsAndOffset(mediaId: Long, lyrics: String?, offsetMs: Long) {
        mediaDao.updateLyricsAndOffset(mediaId, lyrics, offsetMs)
    }

    fun findMissingArtworkItems(items: List<MediaItemEntity>): List<MediaItemEntity> {
        return items.filter { it.mediaType == MediaType.AUDIO && it.artworkPath.isNullOrBlank() }
    }

    fun findMissingMetadataItems(items: List<MediaItemEntity>): List<MediaItemEntity> {
        return items.filter { item ->
            item.mediaType == MediaType.AUDIO && (
                item.artist.equals("Artista desconhecido", true) ||
                    item.album.equals("Álbum desconhecido", true) ||
                    item.genre.isBlank()
                )
        }
    }

    suspend fun reorganizeLibraryWithSmartArtistScan(): Int {
        val all = mediaDao.getAllOnce()
        var updated = 0
        for (item in all) {
            if (item.mediaType != MediaType.AUDIO) continue
            val enriched = ArtistNormalizer.enrichEntityMetadata(item)
            if (enriched != item) {
                mediaDao.update(enriched)
                updated++
            }
        }
        return updated
    }

    fun findPotentialDuplicates(items: List<MediaItemEntity>): List<DuplicateGroup> {
        val groups = mutableListOf<DuplicateGroup>()
        val seenIds = mutableSetOf<Long>()

        items.filter { it.sizeBytes > 0L }
            .groupBy { "${it.fileName.lowercase().trim()}|${it.sizeBytes}" }
            .values
            .filter { it.size > 1 }
            .forEach { matches ->
                matches.forEach { seenIds.add(it.id) }
                val first = matches.first()
                groups.add(
                    DuplicateGroup(
                        titleLabel = first.fileName,
                        reasonLabel = "Mesmo nome e tamanho",
                        items = matches
                    )
                )
            }

        items.filter { it.id !in seenIds && it.title.isNotBlank() && it.durationMs > 0L }
            .groupBy {
                val bucketSec = (it.durationMs / 3000L)
                "${it.title.lowercase().trim()}|${it.artist.lowercase().trim()}|$bucketSec"
            }
            .values
            .filter { it.size > 1 }
            .forEach { matches ->
                val first = matches.first()
                groups.add(
                    DuplicateGroup(
                        titleLabel = "${first.title} • ${first.artist}",
                        reasonLabel = "Mesmo título, artista e duração",
                        items = matches
                    )
                )
            }

        return groups
    }

    fun observePlaylist(playlistId: Long): Flow<PlaylistWithTracks?> = combine(
        playlistDao.observePlaylistWithTracks(playlistId),
        playlistDao.observeTrackRefs(playlistId)
    ) { playlist, refs ->
        if (playlist == null) return@combine null
        val posMap = refs.associate { it.mediaId to it.position }
        playlist.copy(tracks = playlist.tracks.sortedBy { posMap[it.id] ?: Int.MAX_VALUE })
    }

    suspend fun deleteHistoryEntry(historyId: Long) = historyDao.deleteEntry(historyId)

    suspend fun clearHistory() = historyDao.clearAll()

    suspend fun resetStats() = mediaDao.resetStats()

    suspend fun saveQueue(mediaIds: List<Long>) = queueDao.replaceQueue(mediaIds)

    suspend fun loadSavedQueue(): List<MediaItemEntity> {
        val refs = queueDao.getQueueOnce()
        return refs.mapNotNull { mediaDao.getById(it.mediaId) }
    }

    fun buildSmartMix(seed: MediaItemEntity, allAudio: List<MediaItemEntity>): List<MediaItemEntity> {
        val available = allAudio.filter { it.isAvailable && it.mediaType == MediaType.AUDIO }
        if (available.isEmpty()) return emptyList()

        val scored = available.sortedByDescending { candidate ->
            var score = 0
            if (candidate.id == seed.id) score += 1000
            if (candidate.artist.equals(seed.artist, ignoreCase = true)) score += 30
            if (candidate.album.equals(seed.album, ignoreCase = true)) score += 20
            if (seed.genre.isNotBlank() && candidate.genre.equals(seed.genre, ignoreCase = true)) score += 25
            if (candidate.isFavorite) score += 10
            score
        }
        return scored.take(20)
    }

    fun buildMoodCollection(mood: MoodType, allAudio: List<MediaItemEntity>): List<MediaItemEntity> {
        val audio = allAudio.filter { it.isAvailable && it.mediaType == MediaType.AUDIO }
        if (audio.isEmpty()) return emptyList()

        val filtered = when (mood) {
            MoodType.CHILL -> audio.filter {
                it.genre.contains("chill", true) ||
                    it.genre.contains("ambient", true) ||
                    it.genre.contains("acoustic", true) ||
                    it.genre.contains("jazz", true) ||
                    it.durationMs >= 210_000L
            }
            MoodType.FOCUS -> audio.filter {
                it.genre.contains("classic", true) ||
                    it.genre.contains("instrumental", true) ||
                    it.genre.contains("lo-fi", true) ||
                    it.genre.contains("piano", true) ||
                    it.durationMs in 150_000L..320_000L
            }
            MoodType.WORKOUT -> audio.filter {
                it.genre.contains("pop", true) ||
                    it.genre.contains("rock", true) ||
                    it.genre.contains("electronic", true) ||
                    it.genre.contains("hip", true) ||
                    it.durationMs in 120_000L..230_000L
            }
            MoodType.ROAD_TRIP -> audio.filter {
                it.isFavorite || it.playCount > 0 || it.durationMs >= 160_000L
            }
        }
        return if (filtered.isNotEmpty()) filtered.take(20) else audio.take(15)
    }

    fun filterMedia(
        items: List<MediaItemEntity>,
        filter: LibraryFilter,
        excludedFolders: Set<String>
    ): List<MediaItemEntity> {
        val visible = if (excludedFolders.isEmpty()) {
            items
        } else {
            items.filter { item ->
                item.folderPath !in excludedFolders && item.folderName !in excludedFolders
            }
        }
        return when (filter) {
            LibraryFilter.ALL -> visible
            LibraryFilter.FAVORITES -> visible.filter { it.isFavorite }
            LibraryFilter.MISSING_ARTWORK -> visible.filter { it.artworkPath.isNullOrBlank() }
            LibraryFilter.MISSING_METADATA -> visible.filter {
                it.artist.equals("Artista desconhecido", true) ||
                    it.album.equals("Álbum desconhecido", true) ||
                    it.genre.isBlank()
            }
            LibraryFilter.SHORT -> visible.filter { it.durationMs in 1L..119_999L }
            LibraryFilter.LONG -> visible.filter { it.durationMs >= 300_000L }
        }
    }

    fun sortMedia(items: List<MediaItemEntity>, sortOrder: SortOrder): List<MediaItemEntity> {
        return when (sortOrder) {
            SortOrder.TITLE -> items.sortedBy { it.title.lowercase() }
            SortOrder.ARTIST -> items.sortedBy { it.artist.lowercase() }
            SortOrder.ALBUM -> items.sortedBy { it.album.lowercase() }
            SortOrder.DATE_ADDED -> items.sortedByDescending { it.addedAt }
            SortOrder.DURATION -> items.sortedByDescending { it.durationMs }
            SortOrder.PLAY_COUNT -> items.sortedByDescending { it.playCount }
            SortOrder.YEAR -> items.sortedByDescending { it.releaseYear }
            SortOrder.TRACK_NUMBER -> items.sortedWith(
                compareBy<MediaItemEntity> { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
                    .thenBy { it.title.lowercase() }
            )
        }
    }
}
