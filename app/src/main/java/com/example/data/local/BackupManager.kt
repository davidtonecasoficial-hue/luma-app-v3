package com.example.data.local

import com.example.data.model.AccentPalette
import com.example.data.model.AppSettings
import com.example.data.model.AppThemeStyle
import com.example.data.model.ArtworkShape
import com.example.data.model.CoverDoubleTapAction
import com.example.data.model.GlassQuality
import com.example.data.model.HapticIntensity
import com.example.data.model.HomeSectionType
import com.example.data.model.MediaItemEntity
import com.example.data.model.PlaybackAnimationStyle
import com.example.data.model.PlayerScreenStyle
import com.example.data.model.PlaylistWithTracks
import com.example.data.model.ReverbEffectPreset
import com.example.data.model.ThemeMode
import com.example.data.model.VisualizerPreset
import com.example.data.model.VisualizerStyle
import org.json.JSONArray
import org.json.JSONObject

data class BackupRestoreResult(
    val restoredPlaylistsCount: Int,
    val restoredTracksMatched: Int,
    val restoredSettings: AppSettings?
)

/**
 * Pure offline JSON backup/restore utility for Luma Player playlists and settings.
 */
object BackupManager {

    private const val BACKUP_FORMAT_VERSION = 1

    fun serializeBackup(
        settings: AppSettings,
        playlists: List<PlaylistWithTracks>,
        includeSettings: Boolean = true,
        includePlaylists: Boolean = true
    ): String {
        val root = JSONObject()
        root.put("app", "Luma Player")
        root.put("version", BACKUP_FORMAT_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        if (includeSettings) {
            root.put("settings", settingsToJson(settings))
        }

        if (includePlaylists) {
            val plsArray = JSONArray()
            playlists.forEach { pl ->
                val plObj = JSONObject()
                plObj.put("name", pl.playlist.name)
                plObj.put("description", pl.playlist.description)
                val urisArr = JSONArray()
                pl.tracks.forEach { track ->
                    val itemObj = JSONObject()
                    itemObj.put("uri", track.uri)
                    itemObj.put("fileName", track.fileName)
                    itemObj.put("title", track.title)
                    urisArr.put(itemObj)
                }
                plObj.put("tracks", urisArr)
                plsArray.put(plObj)
            }
            root.put("playlists", plsArray)
        }

        return root.toString(2)
    }

    fun parseBackup(
        jsonString: String,
        existingMedia: List<MediaItemEntity>
    ): ParsedBackupPayload {
        val root = JSONObject(jsonString)
        val settingsObj = root.optJSONObject("settings")
        val restoredSettings = settingsObj?.let { jsonToSettings(it) }

        val uriMap = existingMedia.associateBy { it.uri }
        val fileMap = existingMedia.associateBy { it.fileName.lowercase() }

        val playlistsArr = root.optJSONArray("playlists") ?: JSONArray()
        val playlists = mutableListOf<RestoredPlaylistSpec>()
        for (i in 0 until playlistsArr.length()) {
            val plObj = playlistsArr.optJSONObject(i) ?: continue
            val name = plObj.optString("name").trim()
            if (name.isEmpty()) continue
            val tracksArr = plObj.optJSONArray("tracks") ?: JSONArray()
            val matchedIds = mutableListOf<Long>()
            for (j in 0 until tracksArr.length()) {
                val trackObj = tracksArr.optJSONObject(j) ?: continue
                val uri = trackObj.optString("uri")
                val fileName = trackObj.optString("fileName").lowercase()
                val match = uriMap[uri] ?: fileMap[fileName]
                if (match != null) {
                    matchedIds.add(match.id)
                }
            }
            playlists.add(RestoredPlaylistSpec(name = name, trackIds = matchedIds.distinct()))
        }

        return ParsedBackupPayload(
            settings = restoredSettings,
            playlists = playlists
        )
    }

    private fun settingsToJson(s: AppSettings): JSONObject {
        return JSONObject().apply {
            put("themeMode", s.themeMode.name)
            put("themeStyle", s.themeStyle.name)
            put("playerScreenStyle", s.playerScreenStyle.name)
            put("accentPalette", s.accentPalette.name)
            put("glassQuality", s.glassQuality.name)
            put("reduceMotion", s.reduceMotion)
            put("hapticsEnabled", s.hapticsEnabled)
            put("hapticIntensity", s.hapticIntensity.name)
            put("playbackSpeed", s.playbackSpeed.toDouble())
            put("crossfadeEnabled", s.crossfadeEnabled)
            put("crossfadeDurationSec", s.crossfadeDurationSec)
            put("gaplessPlaybackEnabled", s.gaplessPlaybackEnabled)
            put("volumeNormalizationEnabled", s.volumeNormalizationEnabled)
            put("stereoBalance", s.stereoBalance.toDouble())
            put("reverbPreset", s.reverbPreset.name)
            put("virtualizerStrength", s.virtualizerStrength)
            put("dynamicCoverTheme", s.dynamicCoverTheme)
            put("visualizerEnabled", s.visualizerEnabled)
            put("visualizerStyle", s.visualizerStyle.name)
            put("visualizerPreset", s.visualizerPreset.name)
            put("reactiveLightingEnabled", s.reactiveLightingEnabled)
            put("coverGesturesEnabled", s.coverGesturesEnabled)
            put("coverDoubleTapAction", s.coverDoubleTapAction.name)
            put("minimalistPlayerMode", s.minimalistPlayerMode)
            put("albumArtAnimationEnabled", s.albumArtAnimationEnabled)
            put("playbackAnimationStyle", s.playbackAnimationStyle.name)
            put("artworkShape", s.artworkShape.name)
            put("customAccentHex", s.customAccentHex)
            put("glassTransparencyScale", s.glassTransparencyScale.toDouble())
            put("backgroundBlurScale", s.backgroundBlurScale.toDouble())
            put("navBarShowLabels", s.navBarShowLabels)
            put("homeSectionOrder", s.homeSectionOrder)
            put("hiddenHomeSections", JSONArray(s.hiddenHomeSections.toList()))
            put("excludedFolders", JSONArray(s.excludedFolders.toList()))
            put("pauseHistoryTracking", s.pauseHistoryTracking)
            put("lyricsFontScale", s.lyricsFontScale.toDouble())
            put("lyricsCentered", s.lyricsCentered)
            put("customAudioPresetsJson", s.customAudioPresetsJson)
        }
    }

    private fun jsonToSettings(obj: JSONObject): AppSettings {
        val hiddenSet = buildSet {
            val arr = obj.optJSONArray("hiddenHomeSections") ?: JSONArray()
            for (i in 0 until arr.length()) add(arr.optString(i))
        }.filter { it.isNotBlank() }.toSet()
        val excludedSet = buildSet {
            val arr = obj.optJSONArray("excludedFolders") ?: JSONArray()
            for (i in 0 until arr.length()) add(arr.optString(i))
        }.filter { it.isNotBlank() }.toSet()

        return AppSettings(
            themeMode = enumOrDefault(obj.optString("themeMode"), ThemeMode.SYSTEM),
            themeStyle = enumOrDefault(obj.optString("themeStyle"), AppThemeStyle.LUMA_GLASS),
            playerScreenStyle = enumOrDefault(obj.optString("playerScreenStyle"), PlayerScreenStyle.GLASS_DECK),
            accentPalette = enumOrDefault(obj.optString("accentPalette"), AccentPalette.EMERALD_MINT),
            glassQuality = enumOrDefault(obj.optString("glassQuality"), GlassQuality.ULTRA),
            reduceMotion = obj.optBoolean("reduceMotion", false),
            hapticsEnabled = obj.optBoolean("hapticsEnabled", true),
            hapticIntensity = enumOrDefault(obj.optString("hapticIntensity"), HapticIntensity.SUBTLE),
            playbackSpeed = obj.optDouble("playbackSpeed", 1.0).toFloat().coerceIn(0.5f, 2.0f),
            crossfadeEnabled = obj.optBoolean("crossfadeEnabled", false),
            crossfadeDurationSec = obj.optInt("crossfadeDurationSec", 3).coerceIn(1, 12),
            gaplessPlaybackEnabled = obj.optBoolean("gaplessPlaybackEnabled", true),
            volumeNormalizationEnabled = obj.optBoolean("volumeNormalizationEnabled", false),
            stereoBalance = obj.optDouble("stereoBalance", 0.0).toFloat().coerceIn(-1f, 1f),
            reverbPreset = enumOrDefault(obj.optString("reverbPreset"), ReverbEffectPreset.NONE),
            virtualizerStrength = obj.optInt("virtualizerStrength", 0).coerceIn(0, 1000),
            dynamicCoverTheme = obj.optBoolean("dynamicCoverTheme", true),
            visualizerEnabled = obj.optBoolean("visualizerEnabled", true),
            visualizerStyle = enumOrDefault(obj.optString("visualizerStyle"), VisualizerStyle.BARS_3D),
            visualizerPreset = enumOrDefault(obj.optString("visualizerPreset"), VisualizerPreset.AURORA_SILK),
            reactiveLightingEnabled = obj.optBoolean("reactiveLightingEnabled", true),
            coverGesturesEnabled = obj.optBoolean("coverGesturesEnabled", true),
            coverDoubleTapAction = enumOrDefault(obj.optString("coverDoubleTapAction"), CoverDoubleTapAction.PLAY_PAUSE),
            minimalistPlayerMode = obj.optBoolean("minimalistPlayerMode", false),
            albumArtAnimationEnabled = obj.optBoolean("albumArtAnimationEnabled", true),
            playbackAnimationStyle = enumOrDefault(obj.optString("playbackAnimationStyle"), PlaybackAnimationStyle.SPRING_FLOAT),
            artworkShape = enumOrDefault(obj.optString("artworkShape"), ArtworkShape.ROUNDED),
            customAccentHex = obj.optLong("customAccentHex", 0L),
            glassTransparencyScale = obj.optDouble("glassTransparencyScale", 1.0).toFloat().coerceIn(0.45f, 1.45f),
            backgroundBlurScale = obj.optDouble("backgroundBlurScale", 1.0).toFloat().coerceIn(0.4f, 1.5f),
            navBarShowLabels = obj.optBoolean("navBarShowLabels", false),
            homeSectionOrder = obj.optString("homeSectionOrder", HomeSectionType.DEFAULT_ORDER_STRING),
            hiddenHomeSections = hiddenSet,
            excludedFolders = excludedSet,
            pauseHistoryTracking = obj.optBoolean("pauseHistoryTracking", false),
            lyricsFontScale = obj.optDouble("lyricsFontScale", 1.0).toFloat().coerceIn(0.8f, 1.5f),
            lyricsCentered = obj.optBoolean("lyricsCentered", true),
            customAudioPresetsJson = obj.optString("customAudioPresetsJson", "[]"),
            hasSeenWelcomeIntro = true
        )
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(value: String, default: T): T {
        return runCatching { enumValueOf<T>(value) }.getOrDefault(default)
    }
}

data class RestoredPlaylistSpec(
    val name: String,
    val trackIds: List<Long>
)

data class ParsedBackupPayload(
    val settings: AppSettings?,
    val playlists: List<RestoredPlaylistSpec>
)
