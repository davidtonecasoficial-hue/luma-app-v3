package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.CollectionTrackCrossRef
import com.example.data.model.CustomCollectionEntity
import com.example.data.model.CustomCollectionWithTracks
import com.example.data.model.HistoryWithMedia
import com.example.data.model.ListeningSessionEntity
import com.example.data.model.MediaItemEntity
import com.example.data.model.MediaType
import com.example.data.model.PlaybackHistoryEntity
import com.example.data.model.PlaylistEntity
import com.example.data.model.PlaylistTrackCrossRef
import com.example.data.model.PlaylistWithTracks
import com.example.data.model.QueueItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items ORDER BY addedAt DESC")
    fun observeAllMedia(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE mediaType = :type ORDER BY title COLLATE NOCASE ASC")
    fun observeByType(type: MediaType): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isFavorite = 1 ORDER BY lastPlayedAt DESC, title COLLATE NOCASE ASC")
    fun observeFavorites(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE lastPositionMs > 5000 AND lastPlayedAt > 0 ORDER BY lastPlayedAt DESC LIMIT 12")
    fun observeContinueListening(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE playCount > 0 ORDER BY playCount DESC, lastPlayedAt DESC LIMIT 20")
    fun observeMostPlayed(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getById(id: Long): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE uri = :uri LIMIT 1")
    suspend fun getByUri(uri: String): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE fileName = :fileName AND sizeBytes = :sizeBytes LIMIT 1")
    suspend fun findDuplicate(fileName: String, sizeBytes: Long): MediaItemEntity?

    @Query("SELECT * FROM media_items")
    suspend fun getAllOnce(): List<MediaItemEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: MediaItemEntity): Long

    @Update
    suspend fun update(item: MediaItemEntity)

    @Query("UPDATE media_items SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE media_items SET lastPositionMs = :positionMs, lastPlayedAt = :playedAt WHERE id = :id")
    suspend fun updatePosition(id: Long, positionMs: Long, playedAt: Long)

    @Query("UPDATE media_items SET playCount = playCount + 1, lastPlayedAt = :playedAt WHERE id = :id")
    suspend fun incrementPlayCount(id: Long, playedAt: Long)

    @Query("UPDATE media_items SET isAvailable = :available WHERE id = :id")
    suspend fun setAvailable(id: Long, available: Boolean)

    @Query("UPDATE media_items SET playCount = 0, lastPlayedAt = 0, lastPositionMs = 0")
    suspend fun resetStats()

    @Query("UPDATE media_items SET title = :title, artist = :artist, album = :album, genre = :genre, releaseYear = :releaseYear, trackNumber = :trackNumber WHERE id = :id")
    suspend fun updateMetadata(
        id: Long,
        title: String,
        artist: String,
        album: String,
        genre: String,
        releaseYear: Int,
        trackNumber: Int
    )

    @Query("UPDATE media_items SET artworkPath = :artworkPath WHERE id = :id")
    suspend fun updateArtwork(id: Long, artworkPath: String?)

    @Query("UPDATE media_items SET embeddedLyrics = :lyrics, lyricsOffsetMs = :offsetMs WHERE id = :id")
    suspend fun updateLyricsAndOffset(id: Long, lyrics: String?, offsetMs: Long)

    @Query("UPDATE media_items SET embeddedLyrics = :lyrics WHERE id = :id")
    suspend fun updateLyrics(id: Long, lyrics: String?)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface PlaylistDao {
    @Transaction
    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun observePlaylistsWithTracks(): Flow<List<PlaylistWithTracks>>

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun observePlaylistWithTracks(playlistId: Long): Flow<PlaylistWithTracks?>

    @Query("SELECT * FROM playlist_tracks ORDER BY position ASC")
    fun observeAllTrackRefs(): Flow<List<PlaylistTrackCrossRef>>

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC")
    fun observeTrackRefs(playlistId: Long): Flow<List<PlaylistTrackCrossRef>>

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getTrackRefs(playlistId: Long): List<PlaylistTrackCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name, updatedAt = :updatedAt WHERE id = :id")
    suspend fun renamePlaylist(id: Long, name: String, updatedAt: Long)

    @Query("UPDATE playlists SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touchPlaylist(id: Long, updatedAt: Long)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackRef(ref: PlaylistTrackCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackRefs(refs: List<PlaylistTrackCrossRef>)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND mediaId = :mediaId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, mediaId: Long)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearPlaylistTracks(playlistId: Long)
}

@Dao
interface HistoryDao {
    @Transaction
    @Query("SELECT * FROM playback_history ORDER BY playedAt DESC LIMIT 100")
    fun observeHistory(): Flow<List<HistoryWithMedia>>

    @Transaction
    @Query("SELECT * FROM playback_history ORDER BY playedAt DESC LIMIT 2000")
    fun observeInsightsHistory(): Flow<List<HistoryWithMedia>>

    @Insert
    suspend fun insert(entry: PlaybackHistoryEntity)

    @Query("DELETE FROM playback_history WHERE id = :historyId")
    suspend fun deleteEntry(historyId: Long)

    @Query("DELETE FROM playback_history")
    suspend fun clearAll()
}

@Dao
interface QueueDao {
    @Query("SELECT * FROM queue_items ORDER BY queuePosition ASC")
    suspend fun getQueueOnce(): List<QueueItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<QueueItemEntity>)

    @Query("DELETE FROM queue_items")
    suspend fun clear()

    @Transaction
    suspend fun replaceQueue(mediaIds: List<Long>) {
        clear()
        insertAll(mediaIds.mapIndexed { index, id -> QueueItemEntity(queuePosition = index, mediaId = id) })
    }
}

@Dao
interface CustomCollectionDao {
    @Transaction
    @Query("SELECT * FROM custom_collections ORDER BY isPinned DESC, updatedAt DESC")
    fun observeCollectionsWithTracks(): Flow<List<CustomCollectionWithTracks>>

    @Query("SELECT * FROM collection_tracks ORDER BY position ASC")
    fun observeAllTrackRefs(): Flow<List<CollectionTrackCrossRef>>

    @Query("SELECT * FROM collection_tracks WHERE collectionId = :collectionId ORDER BY position ASC")
    suspend fun getTrackRefs(collectionId: Long): List<CollectionTrackCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CustomCollectionEntity): Long

    @Query("UPDATE custom_collections SET name = :name, description = :description, accentTag = :accentTag, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateCollectionMeta(
        id: Long,
        name: String,
        description: String,
        accentTag: String,
        updatedAt: Long
    )

    @Query("UPDATE custom_collections SET isPinned = :pinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean, updatedAt: Long)

    @Query("UPDATE custom_collections SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touchCollection(id: Long, updatedAt: Long)

    @Query("DELETE FROM custom_collections WHERE id = :id")
    suspend fun deleteCollection(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackRef(ref: CollectionTrackCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackRefs(refs: List<CollectionTrackCrossRef>)

    @Query("DELETE FROM collection_tracks WHERE collectionId = :collectionId AND mediaId = :mediaId")
    suspend fun removeTrackFromCollection(collectionId: Long, mediaId: Long)
}

@Dao
interface ListeningSessionDao {
    @Query("SELECT * FROM listening_sessions ORDER BY startedAt DESC LIMIT 3000")
    fun observeRecentSessions(): Flow<List<ListeningSessionEntity>>

    @Query("SELECT * FROM listening_sessions ORDER BY startedAt DESC LIMIT 3000")
    suspend fun getRecentSessionsOnce(): List<ListeningSessionEntity>

    @Insert
    suspend fun insert(session: ListeningSessionEntity): Long

    @Query("DELETE FROM listening_sessions")
    suspend fun clearAll()
}
