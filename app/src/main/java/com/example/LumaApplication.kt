package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.config.LumaCrashGuard
import com.example.data.local.LumaDatabase
import com.example.data.local.SettingsRepository
import com.example.data.media.MediaRepository
import com.example.data.media.MediaScanner
import com.example.player.AmbientSoundEngine
import com.example.player.LumaConnectManager
import com.example.player.PlaybackController

class LumaApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        LumaCrashGuard.install(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("luma_coil_cache"))
                    .maxSizeBytes(48L * 1024L * 1024L)
                    .build()
            }
            .allowRgb565(true)
            .crossfade(120)
            .respectCacheHeaders(false)
            .build()
    }

    val database: LumaDatabase by lazy { LumaDatabase.getInstance(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val mediaScanner: MediaScanner by lazy { MediaScanner(this) }

    val mediaRepository: MediaRepository by lazy {
        MediaRepository(
            mediaDao = database.mediaDao(),
            playlistDao = database.playlistDao(),
            historyDao = database.historyDao(),
            queueDao = database.queueDao(),
            scanner = mediaScanner,
            customCollectionDao = database.customCollectionDao(),
            listeningSessionDao = database.listeningSessionDao()
        )
    }

    val playbackController: PlaybackController by lazy {
        PlaybackController(
            context = this,
            repository = mediaRepository,
            settingsRepository = settingsRepository
        )
    }

    val connectManager: LumaConnectManager by lazy {
        LumaConnectManager(this, playbackController).also { manager ->
            playbackController.setOutputRouteProvider { manager.currentRouteToken() }
        }
    }

    val ambientSoundEngine: AmbientSoundEngine by lazy {
        AmbientSoundEngine()
    }
}
