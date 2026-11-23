package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CollectionTrackCrossRef
import com.example.data.model.CustomCollectionEntity
import com.example.data.model.ListeningSessionEntity
import com.example.data.model.MediaItemEntity
import com.example.data.model.MediaType
import com.example.data.model.PlaybackHistoryEntity
import com.example.data.model.PlaylistEntity
import com.example.data.model.PlaylistTrackCrossRef
import com.example.data.model.QueueItemEntity

class LumaTypeConverters {
    @TypeConverter
    fun fromMediaType(value: MediaType): String = value.name

    @TypeConverter
    fun toMediaType(value: String): MediaType = runCatching { MediaType.valueOf(value) }.getOrDefault(MediaType.AUDIO)
}

@Database(
    entities = [
        MediaItemEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        PlaybackHistoryEntity::class,
        QueueItemEntity::class,
        CustomCollectionEntity::class,
        CollectionTrackCrossRef::class,
        ListeningSessionEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(LumaTypeConverters::class)
abstract class LumaDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao
    abstract fun queueDao(): QueueDao
    abstract fun customCollectionDao(): CustomCollectionDao
    abstract fun listeningSessionDao(): ListeningSessionDao

    companion object {
        @Volatile
        private var INSTANCE: LumaDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_items ADD COLUMN releaseYear INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE media_items ADD COLUMN trackNumber INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE media_items ADD COLUMN lyricsOffsetMs INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS custom_collections (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        accentTag TEXT NOT NULL DEFAULT 'emerald',
                        iconKey TEXT NOT NULL DEFAULT 'sparkles',
                        isPinned INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS collection_tracks (
                        collectionId INTEGER NOT NULL,
                        mediaId INTEGER NOT NULL,
                        position INTEGER NOT NULL DEFAULT 0,
                        addedAt INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(collectionId, mediaId)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_collection_tracks_mediaId ON collection_tracks (mediaId)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS listening_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        mediaId INTEGER NOT NULL,
                        startedAt INTEGER NOT NULL DEFAULT 0,
                        durationListenedMs INTEGER NOT NULL DEFAULT 0,
                        completedTrack INTEGER NOT NULL DEFAULT 0,
                        skippedEarly INTEGER NOT NULL DEFAULT 0,
                        outputRoute TEXT NOT NULL DEFAULT 'SPEAKER'
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_listening_sessions_mediaId ON listening_sessions (mediaId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_listening_sessions_startedAt ON listening_sessions (startedAt)"
                )
            }
        }

        fun getInstance(context: Context): LumaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LumaDatabase::class.java,
                    "luma_player.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
