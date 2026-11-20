package com.example.config

object AppConfig {
    const val APP_NAME = "Luma Player"
    const val APP_SHORT_NAME = "Luma"
    const val APP_VERSION = "3.0.0"
    const val NOTIFICATION_CHANNEL_ID = "luma_playback_channel"
    const val NOTIFICATION_ID = 1001

    val AUDIO_EXTENSIONS = setOf(
        "mp3", "m4a", "aac", "wav", "flac", "ogg", "oga", "opus", "aiff", "aif", "alac", "wma", "amr", "3gp"
    )

    val VIDEO_EXTENSIONS = setOf(
        "mp4", "m4v", "mkv", "webm", "mov", "avi", "3gp", "ts", "flv"
    )

    val SAF_MEDIA_MIME_TYPES = arrayOf("audio/*", "video/*")
    val SAF_SUBTITLE_MIME_TYPES = arrayOf("application/x-subrip", "text/vtt", "text/plain", "*/*")
    val SAF_LYRICS_MIME_TYPES = arrayOf("text/*", "application/octet-stream", "*/*")
}
