package com.example

import android.app.Application
import com.example.data.local.LumaDatabase
import com.example.data.local.SettingsRepository
import com.example.data.media.MediaRepository
import com.example.data.media.MediaScanner
import com.example.player.PlaybackController

class LumaApplication : Application() {

    val database: LumaDatabase by lazy { LumaDatabase.getInstance(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val mediaScanner: MediaScanner by lazy { MediaScanner(this) }

    val mediaRepository: MediaRepository by lazy {
        MediaRepository(
            mediaDao = database.mediaDao(),
            playlistDao = database.playlistDao(),
            historyDao = database.historyDao(),
            queueDao = database.queueDao(),
            scanner = mediaScanner
        )
    }

    val playbackController: PlaybackController by lazy {
        PlaybackController(
            context = this,
            repository = mediaRepository,
            settingsRepository = settingsRepository
        )
    }
}
