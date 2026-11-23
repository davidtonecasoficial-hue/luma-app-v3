package com.example.data.local

import com.example.data.model.AccentPalette
import com.example.data.model.AppLanguage
import com.example.data.model.AppSettings
import com.example.data.model.AppThemeStyle
import com.example.data.model.ArtworkShape
import com.example.data.model.CoverDoubleTapAction
import com.example.data.model.CustomCollectionWithTracks
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
 * Offline JSON backup/restore utility v2 for Luma Player settings, playlists,
 * custom collections, and per-track metadata (lyrics, offset, tags, favorites).
 */
object BackupManager {

    private const val BACKUP_FORMAT_VERSION = 2

    fun serializeBackup(
        settings: AppSettings,
        playlists: List<PlaylistWithTracks>,
        includeSettings: Boolean = true,
        includePlaylists: Boolean = true,
        collections: List<CustomCollectionWithTracks> = emptyList(),
        mediaMetadata: List<MediaItemEntity> = emptyList()
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

        if (collections.isNotEmpty()) {
            val colsArray = JSONArray()
            collections.forEach { col ->
                val colObj = JSONObject()
                colObj.put("name", col.collection.name)
                colObj.put("description", col.collection.description)
                colObj.put("accentTag", col.collection.accentTag)
                colObj.put("isPinned", col.collection.isPinned)
                val tracksArr = JSONArray()
                col.tracks.forEach { track ->
                    tracksArr.put(
                        JSONObject().apply {
                            put("uri", track.uri)
                            put("fileName", track.fileName)
                            put("title", track.title)
                        }
                    )
                }
                colObj.put("tracks", tracksArr)
                colsArray.put(colObj)
            }
            root.put("collections", colsArray)
        }

        if (mediaMetadata.isNotEmpty()) {
            val metaArray = JSONArray()
            mediaMetadata
                .filter { it.isFavorite || !it.embeddedLyrics.isNullOrBlank() || it.lyricsOffsetMs != 0L || it.genre.isNotBlank() || it.releaseYear > 0 }
                .forEach { track ->
                    metaArray.put(
                        JSONObject().apply {
                            put("uri", track.uri)
                            put("fileName", track.fileName)
                            put("sizeBytes", track.sizeBytes)
                            put("title", track.title)
                            put("artist", track.artist)
                            put("album", track.album)
                            put("genre", track.genre)
                            put("releaseYear", track.releaseYear)
                            put("trackNumber", track.trackNumber)
                            put("isFavorite", track.isFavorite)
                            put("lyricsRaw", track.embeddedLyrics ?: "")
                            put("lyricsOffsetMs", track.lyricsOffsetMs)
                        }
                    )
                }
            root.put("trackMetadata", metaArray)
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

        val collectionsArr = root.optJSONArray("collections") ?: JSONArray()
        val collections = mutableListOf<RestoredCollectionSpec>()
        for (i in 0 until collectionsArr.length()) {
            val colObj = collectionsArr.optJSONObject(i) ?: continue
            val name = colObj.optString("name").trim()
            if (name.isEmpty()) continue
            val desc = colObj.optString("description")
            val accent = colObj.optString("accentTag", "emerald")
            val tracksArr = colObj.optJSONArray("tracks") ?: JSONArray()
            val matchedIds = mutableListOf<Long>()
            for (j in 0 until tracksArr.length()) {
                val trackObj = tracksArr.optJSONObject(j) ?: continue
                val uri = trackObj.optString("uri")
                val fileName = trackObj.optString("fileName").lowercase()
                val match = uriMap[uri] ?: fileMap[fileName]
                if (match != null) matchedIds.add(match.id)
            }
            collections.add(
                RestoredCollectionSpec(
                    name = name,
                    description = desc,
                    accentTag = accent,
                    trackIds = matchedIds.distinct()
                )
            )
        }

        val metaArr = root.optJSONArray("trackMetadata") ?: JSONArray()
        val trackMetaList = mutableListOf<RestoredTrackMetadataSpec>()
        for (i in 0 until metaArr.length()) {
            val mObj = metaArr.optJSONObject(i) ?: continue
            val uri = mObj.optString("uri")
            val fileName = mObj.optString("fileName").lowercase()
            val match = uriMap[uri] ?: fileMap[fileName] ?: continue
            trackMetaList.add(
                RestoredTrackMetadataSpec(
                    mediaId = match.id,
                    title = mObj.optString("title", match.title),
                    artist = mObj.optString("artist", match.artist),
                    album = mObj.optString("album", match.album),
                    genre = mObj.optString("genre", match.genre),
                    releaseYear = mObj.optInt("releaseYear", match.releaseYear),
                    trackNumber = mObj.optInt("trackNumber", match.trackNumber),
                    isFavorite = mObj.optBoolean("isFavorite", match.isFavorite),
                    lyricsRaw = mObj.optString("lyricsRaw").takeIf { it.isNotBlank() },
                    lyricsOffsetMs = mObj.optLong("lyricsOffsetMs", 0L)
                )
            )
        }

        return ParsedBackupPayload(
            settings = restoredSettings,
            playlists = playlists,
            collections = collections,
            trackMetadata = trackMetaList
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
            put("appLanguage", s.appLanguage.name)
            put("pitchSemitones", s.pitchSemitones.toDouble())
            put("shakeToSkipEnabled", s.shakeToSkipEnabled)
            put("queueRadioEnabled", s.queueRadioEnabled)
            put("autoPauseOnDisconnect", s.autoPauseOnDisconnect)
            put("autoResumeOnBluetooth", s.autoResumeOnBluetooth)
            put("headsetButtonDoubleTapSkip", s.headsetButtonDoubleTapSkip)
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
            hasSeenWelcomeIntro = true,
            appLanguage = enumOrDefault(obj.optString("appLanguage"), AppLanguage.PT),
            pitchSemitones = obj.optDouble("pitchSemitones", 0.0).toFloat().coerceIn(-6f, 6f),
            shakeToSkipEnabled = obj.optBoolean("shakeToSkipEnabled", false),
            queueRadioEnabled = obj.optBoolean("queueRadioEnabled", false),
            autoPauseOnDisconnect = obj.optBoolean("autoPauseOnDisconnect", true),
            autoResumeOnBluetooth = obj.optBoolean("autoResumeOnBluetooth", false),
            headsetButtonDoubleTapSkip = obj.optBoolean("headsetButtonDoubleTapSkip", true)
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

data class RestoredCollectionSpec(
    val name: String,
    val description: String,
    val accentTag: String,
    val trackIds: List<Long>
)

data class RestoredTrackMetadataSpec(
    val mediaId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val releaseYear: Int,
    val trackNumber: Int,
    val isFavorite: Boolean,
    val lyricsRaw: String?,
    val lyricsOffsetMs: Long
)

data class ParsedBackupPayload(
    val settings: AppSettings?,
    val playlists: List<RestoredPlaylistSpec>,
    val collections: List<RestoredCollectionSpec> = emptyList(),
    val trackMetadata: List<RestoredTrackMetadataSpec> = emptyList()
)
