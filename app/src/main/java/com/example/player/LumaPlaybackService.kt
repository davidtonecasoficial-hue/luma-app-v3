package com.example.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.LumaApplication
import com.example.MainActivity
import com.example.R
import com.example.config.AppConfig
import com.example.config.LumaCrashGuard
import com.example.data.local.LumaDatabase
import com.example.data.local.SettingsRepository
import com.example.data.media.ArtistNormalizer
import com.example.data.media.LumaIntelligenceEngine
import com.example.data.model.MediaItemEntity
import com.example.data.model.MediaType
import com.example.widget.LumaMusicWidgetProvider
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class LumaPlaybackService : MediaLibraryService() {

    private var mediaLibrarySession: MediaLibrarySession? = null
    private lateinit var player: ExoPlayer
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + LumaCrashGuard.coroutineExceptionHandler)

    @Volatile
    private var isServiceDestroyed = false

    private fun appDatabase(): LumaDatabase =
        (application as? LumaApplication)?.database ?: LumaDatabase.getInstance(applicationContext)

    private fun appSettingsRepository(): SettingsRepository =
        (application as? LumaApplication)?.settingsRepository ?: SettingsRepository(applicationContext)

    companion object {
        @Volatile
        var currentAudioSessionId: Int = 0
            private set

        private const val ACTION_TOGGLE_FAVORITE = "com.example.luma.TOGGLE_FAVORITE"
        const val ACTION_SET_GAPLESS = "com.example.luma.SET_GAPLESS"
        const val EXTRA_GAPLESS_ENABLED = "gapless_enabled"
        private const val ROOT_ID = "luma_root"
        private const val NODE_SONGS = "luma_songs"
        private const val NODE_FAVORITES = "luma_favorites"
        private const val NODE_ARTISTS = "luma_artists"
        private const val NODE_ALBUMS = "luma_albums"
        private const val NODE_PLAYLISTS = "luma_playlists"
        private const val NODE_COLLECTIONS = "luma_collections"
        private const val NODE_AUTO_DJ = "luma_autodj"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannelIfNeeded()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(
                        arrayOf(
                            StereoBalanceAudioProcessor(),
                            SpectrumAudioProcessor()
                        )
                    )
                    .build()
            }
        }

        player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        currentAudioSessionId = player.audioSessionId
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                currentAudioSessionId = audioSessionId
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) {
                    AudioPipelineBus.resetSpectrum()
                }
                syncNotificationAndWidget()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                syncNotificationAndWidget()
            }
        })

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(AppConfig.NOTIFICATION_CHANNEL_ID)
                .setNotificationId(AppConfig.NOTIFICATION_ID)
                .build()
        )

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, LibrarySessionCallback())
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }

    @OptIn(UnstableApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        runCatching {
            if (!::player.isInitialized || isServiceDestroyed) return@runCatching
            when (intent?.action) {
                LumaMusicWidgetProvider.ACTION_WIDGET_PLAY_PAUSE -> {
                    if (player.isPlaying) {
                        player.pause()
                    } else {
                        if (player.mediaItemCount > 0) {
                            if (player.playbackState == Player.STATE_IDLE) player.prepare()
                            player.play()
                        } else {
                            resumeFromLastQueue()
                        }
                    }
                }
                LumaMusicWidgetProvider.ACTION_WIDGET_NEXT -> {
                    if (player.hasNextMediaItem()) player.seekToNextMediaItem()
                }
                LumaMusicWidgetProvider.ACTION_WIDGET_PREVIOUS -> {
                    if (player.currentPosition > 3000L) player.seekTo(0L)
                    else if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun resumeFromLastQueue() {
        serviceScope.launch {
            runCatching {
                val db = appDatabase()
                val settings = appSettingsRepository().settingsFlow.first()
                val queueRefs = db.queueDao().getQueueOnce()
                val items = queueRefs.mapNotNull { db.mediaDao().getById(it.mediaId) }
                    .ifEmpty { db.mediaDao().getAllOnce().filter { it.mediaType == MediaType.AUDIO && it.isAvailable }.take(25) }
                if (items.isEmpty()) return@launch
                val startIdx = items.indexOfFirst { it.id == settings.lastMediaId }.coerceAtLeast(0)
                val mediaItems = items.map { entityToMediaItem(it) }
                launch(Dispatchers.Main) {
                    if (isServiceDestroyed || !::player.isInitialized) return@launch
                    runCatching {
                        player.setMediaItems(mediaItems, startIdx, settings.lastPositionMs.coerceAtLeast(0L))
                        player.prepare()
                        player.play()
                    }
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun syncNotificationAndWidget() {
        if (isServiceDestroyed || !::player.isInitialized) return
        val current = runCatching { player.currentMediaItem }.getOrNull()
        val meta = current?.mediaMetadata
        val title = meta?.title?.toString().orEmpty().ifBlank { "Luma Player" }
        val artist = meta?.artist?.toString().orEmpty().ifBlank { "Toque para abrir a sua música" }
        val isPlaying = runCatching { player.isPlaying }.getOrDefault(false)
        val mediaIdLong = current?.mediaId?.toLongOrNull() ?: -1L

        serviceScope.launch {
            runCatching {
                val entity = if (mediaIdLong > 0L) {
                    appDatabase().mediaDao().getById(mediaIdLong)
                } else null

                LumaMusicWidgetProvider.saveAndUpdateWidgetState(
                    context = applicationContext,
                    title = entity?.title ?: title,
                    artist = entity?.artist ?: artist,
                    isPlaying = isPlaying,
                    artworkPath = entity?.artworkPath
                )

                val isFav = entity?.isFavorite == true
                val favButton = CommandButton.Builder()
                    .setDisplayName(if (isFav) "Remover favorito" else "Favorito")
                    .setIconResId(if (isFav) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off)
                    .setSessionCommand(SessionCommand(ACTION_TOGGLE_FAVORITE, Bundle.EMPTY))
                    .build()
                launch(Dispatchers.Main) {
                    if (!isServiceDestroyed) {
                        runCatching {
                            mediaLibrarySession?.setCustomLayout(ImmutableList.of(favButton))
                        }
                    }
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val currentPlayer = mediaLibrarySession?.player
        if (currentPlayer == null || !currentPlayer.playWhenReady || currentPlayer.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        isServiceDestroyed = true
        AudioPipelineBus.resetSpectrum()
        mediaLibrarySession?.run {
            runCatching { player.release() }
            runCatching { release() }
            mediaLibrarySession = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(AppConfig.NOTIFICATION_CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    AppConfig.NOTIFICATION_CHANNEL_ID,
                    getString(R.string.notification_channel_playback),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.notification_channel_playback_desc)
                    setShowBadge(false)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun entityToMediaItem(entity: MediaItemEntity): MediaItem {
        val artUri = entity.artworkPath?.takeIf { it.isNotBlank() }?.let { path ->
            runCatching {
                if (path.startsWith("content://") || path.startsWith("file://")) Uri.parse(path)
                else Uri.fromFile(File(path))
            }.getOrNull()
        }
        val mediaUri = runCatching { Uri.parse(entity.uri) }.getOrNull() ?: Uri.EMPTY
        return MediaItem.Builder()
            .setMediaId(entity.id.toString())
            .setUri(mediaUri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(entity.title)
                    .setArtist(entity.artist)
                    .setAlbumTitle(entity.album)
                    .setArtworkUri(artUri)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .build()
    }

    private fun folderMediaItem(id: String, title: String, subtitle: String = ""): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .build()
            )
            .build()
    }

    @OptIn(UnstableApi::class)
    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                .add(SessionCommand(ACTION_TOGGLE_FAVORITE, Bundle.EMPTY))
                .add(SessionCommand(ACTION_SET_GAPLESS, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                ACTION_TOGGLE_FAVORITE -> {
                    val id = if (!isServiceDestroyed && ::player.isInitialized) {
                        player.currentMediaItem?.mediaId?.toLongOrNull() ?: -1L
                    } else -1L
                    if (id > 0L) {
                        serviceScope.launch {
                            runCatching {
                                val dao = appDatabase().mediaDao()
                                val item = dao.getById(id)
                                if (item != null) {
                                    dao.setFavorite(id, !item.isFavorite)
                                    syncNotificationAndWidget()
                                }
                            }
                        }
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                ACTION_SET_GAPLESS -> {
                    if (!isServiceDestroyed && ::player.isInitialized) {
                        val enabled = args.getBoolean(EXTRA_GAPLESS_ENABLED, true)
                        player.pauseAtEndOfMediaItems = !enabled
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val root = folderMediaItem(ROOT_ID, getString(R.string.app_name))
            return Futures.immediateFuture(LibraryResult.ofItem(root, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
            serviceScope.launch {
                try {
                    val db = appDatabase()
                    val audio = db.mediaDao().getAllOnce().filter { it.mediaType == MediaType.AUDIO && it.isAvailable }
                    val items: List<MediaItem> = when {
                        parentId == ROOT_ID -> listOf(
                            folderMediaItem(NODE_SONGS, "Músicas", "${audio.size} faixas"),
                            folderMediaItem(NODE_AUTO_DJ, "Auto DJ Luma", "Sequência inteligente"),
                            folderMediaItem(NODE_FAVORITES, "Favoritos"),
                            folderMediaItem(NODE_COLLECTIONS, "Coleções"),
                            folderMediaItem(NODE_ARTISTS, "Artistas"),
                            folderMediaItem(NODE_ALBUMS, "Álbuns"),
                            folderMediaItem(NODE_PLAYLISTS, "Playlists")
                        )
                        parentId == NODE_SONGS -> audio.sortedBy { it.title.lowercase() }.map { entityToMediaItem(it) }
                        parentId == NODE_AUTO_DJ -> LumaIntelligenceEngine.buildAutoDjMix(null, audio, limit = 25).map { entityToMediaItem(it) }
                        parentId == NODE_FAVORITES -> audio.filter { it.isFavorite }.map { entityToMediaItem(it) }
                        parentId == NODE_COLLECTIONS -> {
                            val cols = db.customCollectionDao().observeCollectionsWithTracks().first()
                            cols.map { col ->
                                folderMediaItem("collection:${col.collection.id}", col.collection.name, "${col.tracks.size} músicas")
                            }
                        }
                        parentId == NODE_ARTISTS -> audio
                            .groupBy { ArtistNormalizer.artistGroupingKey(it.artist, it.fileName) }
                            .entries
                            .map { (key, list) ->
                                folderMediaItem(
                                    "artist:$key",
                                    ArtistNormalizer.pickBestDisplayName(list),
                                    "${list.size} músicas"
                                )
                            }
                            .sortedBy { it.mediaMetadata.title.toString().lowercase() }
                        parentId == NODE_ALBUMS -> audio
                            .groupBy { it.album.ifBlank { "Álbum desconhecido" } }
                            .entries
                            .map { (album, list) ->
                                folderMediaItem("album:$album", album, "${list.size} faixas")
                            }
                            .sortedBy { it.mediaMetadata.title.toString().lowercase() }
                        parentId == NODE_PLAYLISTS -> {
                            val pls = db.playlistDao().observePlaylistsWithTracks().first()
                            pls.map { pl ->
                                folderMediaItem("playlist:${pl.playlist.id}", pl.playlist.name, "${pl.tracks.size} músicas")
                            }
                        }
                        parentId.startsWith("collection:") -> {
                            val colId = parentId.removePrefix("collection:").toLongOrNull() ?: -1L
                            val col = db.customCollectionDao().observeCollectionsWithTracks().first()
                                .firstOrNull { it.collection.id == colId }
                            col?.tracks?.map { entityToMediaItem(it) }.orEmpty()
                        }
                        parentId.startsWith("artist:") -> {
                            val key = parentId.removePrefix("artist:")
                            audio.filter { ArtistNormalizer.artistGroupingKey(it.artist, it.fileName) == key }
                                .map { entityToMediaItem(it) }
                        }
                        parentId.startsWith("album:") -> {
                            val album = parentId.removePrefix("album:")
                            audio.filter { it.album.ifBlank { "Álbum desconhecido" } == album }
                                .map { entityToMediaItem(it) }
                        }
                        parentId.startsWith("playlist:") -> {
                            val plId = parentId.removePrefix("playlist:").toLongOrNull() ?: -1L
                            val pl = db.playlistDao().observePlaylistsWithTracks().first()
                                .firstOrNull { it.playlist.id == plId }
                            pl?.tracks?.map { entityToMediaItem(it) }.orEmpty()
                        }
                        else -> emptyList()
                    }
                    val safePage = page.coerceAtLeast(0)
                    val safePageSize = pageSize.coerceIn(1, 500)
                    val from = (safePage.toLong() * safePageSize).coerceAtMost(items.size.toLong()).toInt()
                    val to = ((safePage.toLong() + 1L) * safePageSize).coerceAtMost(items.size.toLong()).toInt()
                    future.set(LibraryResult.ofItemList(ImmutableList.copyOf(items.subList(from, to)), params))
                } catch (_: Throwable) {
                    future.set(LibraryResult.ofItemList(ImmutableList.of(), params))
                }
            }
            return future
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val future = SettableFuture.create<LibraryResult<MediaItem>>()
            serviceScope.launch {
                try {
                    val idLong = mediaId.toLongOrNull()
                    val item = if (idLong != null) appDatabase().mediaDao().getById(idLong) else null
                    if (item != null) {
                        future.set(LibraryResult.ofItem(entityToMediaItem(item), null))
                    } else {
                        future.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    }
                } catch (_: Throwable) {
                    future.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                }
            }
            return future
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            serviceScope.launch {
                runCatching {
                    val q = query.trim().lowercase()
                    val audio = appDatabase().mediaDao().getAllOnce()
                        .filter { it.mediaType == MediaType.AUDIO && it.isAvailable }
                    val count = audio.count {
                        it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) || it.album.lowercase().contains(q)
                    }
                    if (!isServiceDestroyed) {
                        session.notifySearchResultChanged(browser, query, count, params)
                    }
                }
            }
            return Futures.immediateFuture(LibraryResult.ofVoid(params))
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
            serviceScope.launch {
                try {
                    val q = query.trim().lowercase()
                    val audio = appDatabase().mediaDao().getAllOnce()
                        .filter { it.mediaType == MediaType.AUDIO && it.isAvailable }
                    val matches = audio.filter {
                        it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) || it.album.lowercase().contains(q)
                    }.map { entityToMediaItem(it) }
                    val safePage = page.coerceAtLeast(0)
                    val safePageSize = pageSize.coerceIn(1, 500)
                    val from = (safePage.toLong() * safePageSize).coerceAtMost(matches.size.toLong()).toInt()
                    val to = ((safePage.toLong() + 1L) * safePageSize).coerceAtMost(matches.size.toLong()).toInt()
                    future.set(LibraryResult.ofItemList(ImmutableList.copyOf(matches.subList(from, to)), params))
                } catch (_: Throwable) {
                    future.set(LibraryResult.ofItemList(ImmutableList.of(), params))
                }
            }
            return future
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
            serviceScope.launch {
                try {
                    val db = appDatabase()
                    val settings = appSettingsRepository().settingsFlow.first()
                    val queueRefs = db.queueDao().getQueueOnce()
                    val items = queueRefs.mapNotNull { ref -> db.mediaDao().getById(ref.mediaId) }
                    if (items.isEmpty()) {
                        future.set(MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0L))
                        return@launch
                    }
                    val startIndex = items.indexOfFirst { it.id == settings.lastMediaId }.coerceAtLeast(0)
                    val mediaItems = items.map { entityToMediaItem(it) }
                    future.set(
                        MediaSession.MediaItemsWithStartPosition(
                            mediaItems,
                            startIndex,
                            settings.lastPositionMs.coerceAtLeast(0L)
                        )
                    )
                } catch (_: Throwable) {
                    future.set(MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0L))
                }
            }
            return future
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val future = SettableFuture.create<MutableList<MediaItem>>()
            serviceScope.launch {
                try {
                    val db = appDatabase()
                    val updated = mediaItems.map { item ->
                        if (item.localConfiguration != null) {
                            item
                        } else {
                            val idLong = item.mediaId.toLongOrNull()
                            val fromDb = if (idLong != null) db.mediaDao().getById(idLong) else null
                            when {
                                fromDb != null -> entityToMediaItem(fromDb)
                                item.requestMetadata.mediaUri != null -> item.buildUpon().setUri(item.requestMetadata.mediaUri).build()
                                else -> item
                            }
                        }
                    }.toMutableList()
                    future.set(updated)
                } catch (_: Throwable) {
                    future.set(mediaItems)
                }
            }
            return future
        }
    }
}

