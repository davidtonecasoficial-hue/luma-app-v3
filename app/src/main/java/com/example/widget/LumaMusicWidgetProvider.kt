package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.config.LumaCrashGuard
import com.example.player.LumaPlaybackService

class LumaMusicWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateSingleWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_WIDGET_PREVIOUS,
            ACTION_WIDGET_PLAY_PAUSE,
            ACTION_WIDGET_NEXT -> {
                val serviceIntent = Intent(context, LumaPlaybackService::class.java).apply {
                    action = intent.action
                }
                runCatching {
                    context.startService(serviceIntent)
                }
            }
            ACTION_WIDGET_REFRESH -> {
                refreshAllWidgets(context)
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_PREVIOUS = "com.example.luma.WIDGET_PREVIOUS"
        const val ACTION_WIDGET_PLAY_PAUSE = "com.example.luma.WIDGET_PLAY_PAUSE"
        const val ACTION_WIDGET_NEXT = "com.example.luma.WIDGET_NEXT"
        const val ACTION_WIDGET_REFRESH = "com.example.luma.WIDGET_REFRESH"

        private const val PREFS_NAME = "luma_widget_state"
        private const val KEY_TITLE = "title"
        private const val KEY_ARTIST = "artist"
        private const val KEY_PLAYING = "is_playing"
        private const val KEY_ARTWORK = "artwork_path"

        fun saveAndUpdateWidgetState(
            context: Context,
            title: String,
            artist: String,
            isPlaying: Boolean,
            artworkPath: String?
        ) {
            runCatching {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                    .putString(KEY_TITLE, title)
                    .putString(KEY_ARTIST, artist)
                    .putBoolean(KEY_PLAYING, isPlaying)
                    .putString(KEY_ARTWORK, artworkPath)
                    .apply()
                refreshAllWidgets(context)
            }
        }

        fun refreshAllWidgets(context: Context) {
            runCatching {
                val manager = AppWidgetManager.getInstance(context) ?: return
                val component = ComponentName(context, LumaMusicWidgetProvider::class.java)
                val ids = manager.getAppWidgetIds(component)
                if (ids.isNotEmpty()) {
                    ids.forEach { id -> updateSingleWidget(context, manager, id) }
                }
            }
        }

        private fun updateSingleWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val title = prefs.getString(KEY_TITLE, null)?.takeIf { it.isNotBlank() }
                ?: "Luma Player"
            val artist = prefs.getString(KEY_ARTIST, null)?.takeIf { it.isNotBlank() }
                ?: "Toque para abrir a sua música"
            val isPlaying = prefs.getBoolean(KEY_PLAYING, false)
            val artworkPath = prefs.getString(KEY_ARTWORK, null)

            val views = RemoteViews(context.packageName, R.layout.luma_music_widget)
            views.setTextViewText(R.id.widget_track_title, title)
            views.setTextViewText(R.id.widget_track_artist, artist)
            views.setImageViewResource(
                R.id.widget_btn_play_pause,
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )

            var artworkLoaded = false
            if (!artworkPath.isNullOrBlank()) {
                val bmp = LumaCrashGuard.decodeSampledBitmap(context, artworkPath, maxDimensionPx = 256)
                if (bmp != null && !bmp.isRecycled) {
                    runCatching {
                        views.setImageViewBitmap(R.id.widget_artwork, bmp)
                        artworkLoaded = true
                    }
                }
            }
            if (!artworkLoaded) {
                views.setImageViewResource(R.id.widget_artwork, R.mipmap.ic_launcher_round)
            }

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPending = PendingIntent.getActivity(
                context,
                100,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openPending)
            views.setOnClickPendingIntent(R.id.widget_btn_prev, broadcastPending(context, ACTION_WIDGET_PREVIOUS, 101))
            views.setOnClickPendingIntent(R.id.widget_btn_play_pause, broadcastPending(context, ACTION_WIDGET_PLAY_PAUSE, 102))
            views.setOnClickPendingIntent(R.id.widget_btn_next, broadcastPending(context, ACTION_WIDGET_NEXT, 103))

            runCatching { appWidgetManager.updateAppWidget(appWidgetId, views) }
        }

        private fun broadcastPending(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, LumaMusicWidgetProvider::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
