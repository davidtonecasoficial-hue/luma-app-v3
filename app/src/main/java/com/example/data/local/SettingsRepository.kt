package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AccentPalette
import com.example.data.model.AppSettings
import com.example.data.model.AppThemeStyle
import com.example.data.model.ArtworkShape
import com.example.data.model.CoverDoubleTapAction
import com.example.data.model.CustomAudioPreset
import com.example.data.model.GlassQuality
import com.example.data.model.HapticIntensity
import com.example.data.model.HomeSectionType
import com.example.data.model.PlaybackAnimationStyle
import com.example.data.model.PlayerScreenStyle
import com.example.data.model.ReverbEffectPreset
import com.example.data.model.ThemeMode
import com.example.data.model.VisualizerPreset
import com.example.data.model.VisualizerStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "luma_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val THEME_STYLE = stringPreferencesKey("theme_style")
        val PLAYER_SCREEN_STYLE = stringPreferencesKey("player_screen_style")
        val ACCENT_PALETTE = stringPreferencesKey("accent_palette")
        val GLASS_QUALITY = stringPreferencesKey("glass_quality")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val HAPTIC_INTENSITY = stringPreferencesKey("haptic_intensity")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val CROSSFADE_ENABLED = booleanPreferencesKey("crossfade_enabled")
        val CROSSFADE_SEC = intPreferencesKey("crossfade_sec")
        val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        val VOLUME_NORMALIZATION = booleanPreferencesKey("volume_normalization")
        val STEREO_BALANCE = floatPreferencesKey("stereo_balance")
        val REVERB_PRESET = stringPreferencesKey("reverb_preset")
        val VIRTUALIZER_STRENGTH = intPreferencesKey("virtualizer_strength")
        val DYNAMIC_COVER = booleanPreferencesKey("dynamic_cover")
        val VISUALIZER_ENABLED = booleanPreferencesKey("visualizer_enabled")
        val VISUALIZER_STYLE = stringPreferencesKey("visualizer_style")
        val VISUALIZER_PRESET = stringPreferencesKey("visualizer_preset")
        val REACTIVE_LIGHTING = booleanPreferencesKey("reactive_lighting")
        val COVER_GESTURES = booleanPreferencesKey("cover_gestures")
        val COVER_DOUBLE_TAP = stringPreferencesKey("cover_double_tap")
        val MINIMALIST_PLAYER = booleanPreferencesKey("minimalist_player")
        val ALBUM_ART_ANIM = booleanPreferencesKey("album_art_anim")
        val PLAYBACK_ANIM_STYLE = stringPreferencesKey("playback_anim_style")
        val ARTWORK_SHAPE = stringPreferencesKey("artwork_shape")
        val CUSTOM_ACCENT_HEX = longPreferencesKey("custom_accent_hex")
        val GLASS_TRANSPARENCY = floatPreferencesKey("glass_transparency")
        val BACKGROUND_BLUR = floatPreferencesKey("background_blur")
        val NAV_BAR_LABELS = booleanPreferencesKey("nav_bar_labels")
        val HOME_SECTION_ORDER = stringPreferencesKey("home_section_order")
        val HIDDEN_HOME_SECTIONS = stringSetPreferencesKey("hidden_home_sections")
        val EXCLUDED_FOLDERS = stringSetPreferencesKey("excluded_folders")
        val PAUSE_HISTORY = booleanPreferencesKey("pause_history")
        val LYRICS_FONT_SCALE = floatPreferencesKey("lyrics_font_scale")
        val LYRICS_CENTERED = booleanPreferencesKey("lyrics_centered")
        val CUSTOM_AUDIO_PRESETS = stringPreferencesKey("custom_audio_presets")
        val WELCOME_INTRO_SEEN = booleanPreferencesKey("welcome_intro_seen")
        val LAST_MEDIA_ID = longPreferencesKey("last_media_id")
        val LAST_POSITION_MS = longPreferencesKey("last_position_ms")
        val TOTAL_LISTENING_MS = longPreferencesKey("total_listening_ms")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        val storedAccent = prefs[Keys.ACCENT_PALETTE]?.let { runCatching { AccentPalette.valueOf(it) }.getOrNull() }
        val legacyAccent = storedAccent ?: AccentPalette.EMERALD_MINT
        val resolvedStyle = prefs[Keys.THEME_STYLE]?.let { runCatching { AppThemeStyle.valueOf(it) }.getOrNull() }
            ?: when (storedAccent) {
                AccentPalette.EMERALD_MINT -> AppThemeStyle.LUMA_GLASS
                AccentPalette.OCEAN_CYAN -> AppThemeStyle.OCEAN_SAPPHIRE
                AccentPalette.VIOLET_PULSE -> AppThemeStyle.VELVET_VIOLET
                AccentPalette.SUNSET_AMBER -> AppThemeStyle.SUNSET_AMBER
                null -> AppThemeStyle.LUMA_GLASS
            }
        AppSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            themeStyle = resolvedStyle,
            playerScreenStyle = prefs[Keys.PLAYER_SCREEN_STYLE]?.let {
                runCatching { PlayerScreenStyle.valueOf(it) }.getOrNull()
            } ?: PlayerScreenStyle.GLASS_DECK,
            accentPalette = legacyAccent,
            glassQuality = prefs[Keys.GLASS_QUALITY]?.let { runCatching { GlassQuality.valueOf(it) }.getOrNull() }
                ?: GlassQuality.ULTRA,
            reduceMotion = prefs[Keys.REDUCE_MOTION] ?: false,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            hapticIntensity = prefs[Keys.HAPTIC_INTENSITY]?.let { runCatching { HapticIntensity.valueOf(it) }.getOrNull() }
                ?: HapticIntensity.SUBTLE,
            playbackSpeed = prefs[Keys.PLAYBACK_SPEED] ?: 1.0f,
            crossfadeEnabled = prefs[Keys.CROSSFADE_ENABLED] ?: false,
            crossfadeDurationSec = prefs[Keys.CROSSFADE_SEC] ?: 3,
            gaplessPlaybackEnabled = prefs[Keys.GAPLESS_PLAYBACK] ?: true,
            volumeNormalizationEnabled = prefs[Keys.VOLUME_NORMALIZATION] ?: false,
            stereoBalance = (prefs[Keys.STEREO_BALANCE] ?: 0f).coerceIn(-1f, 1f),
            reverbPreset = prefs[Keys.REVERB_PRESET]?.let { runCatching { ReverbEffectPreset.valueOf(it) }.getOrNull() }
                ?: ReverbEffectPreset.NONE,
            virtualizerStrength = (prefs[Keys.VIRTUALIZER_STRENGTH] ?: 0).coerceIn(0, 1000),
            dynamicCoverTheme = prefs[Keys.DYNAMIC_COVER] ?: true,
            visualizerEnabled = prefs[Keys.VISUALIZER_ENABLED] ?: true,
            visualizerStyle = prefs[Keys.VISUALIZER_STYLE]?.let { runCatching { VisualizerStyle.valueOf(it) }.getOrNull() }
                ?: VisualizerStyle.BARS_3D,
            visualizerPreset = prefs[Keys.VISUALIZER_PRESET]?.let { runCatching { VisualizerPreset.valueOf(it) }.getOrNull() }
                ?: VisualizerPreset.AURORA_SILK,
            reactiveLightingEnabled = prefs[Keys.REACTIVE_LIGHTING] ?: true,
            coverGesturesEnabled = prefs[Keys.COVER_GESTURES] ?: true,
            coverDoubleTapAction = prefs[Keys.COVER_DOUBLE_TAP]?.let { runCatching { CoverDoubleTapAction.valueOf(it) }.getOrNull() }
                ?: CoverDoubleTapAction.PLAY_PAUSE,
            minimalistPlayerMode = prefs[Keys.MINIMALIST_PLAYER] ?: false,
            albumArtAnimationEnabled = prefs[Keys.ALBUM_ART_ANIM] ?: true,
            playbackAnimationStyle = prefs[Keys.PLAYBACK_ANIM_STYLE]?.let { runCatching { PlaybackAnimationStyle.valueOf(it) }.getOrNull() }
                ?: PlaybackAnimationStyle.SPRING_FLOAT,
            artworkShape = prefs[Keys.ARTWORK_SHAPE]?.let { runCatching { ArtworkShape.valueOf(it) }.getOrNull() }
                ?: ArtworkShape.ROUNDED,
            customAccentHex = prefs[Keys.CUSTOM_ACCENT_HEX] ?: 0L,
            glassTransparencyScale = (prefs[Keys.GLASS_TRANSPARENCY] ?: 1.0f).coerceIn(0.45f, 1.45f),
            backgroundBlurScale = (prefs[Keys.BACKGROUND_BLUR] ?: 1.0f).coerceIn(0.4f, 1.5f),
            navBarShowLabels = prefs[Keys.NAV_BAR_LABELS] ?: false,
            homeSectionOrder = prefs[Keys.HOME_SECTION_ORDER] ?: HomeSectionType.DEFAULT_ORDER_STRING,
            hiddenHomeSections = prefs[Keys.HIDDEN_HOME_SECTIONS] ?: emptySet(),
            excludedFolders = prefs[Keys.EXCLUDED_FOLDERS] ?: emptySet(),
            pauseHistoryTracking = prefs[Keys.PAUSE_HISTORY] ?: false,
            lyricsFontScale = (prefs[Keys.LYRICS_FONT_SCALE] ?: 1.0f).coerceIn(0.8f, 1.5f),
            lyricsCentered = prefs[Keys.LYRICS_CENTERED] ?: true,
            customAudioPresetsJson = prefs[Keys.CUSTOM_AUDIO_PRESETS] ?: "[]",
            hasSeenWelcomeIntro = prefs[Keys.WELCOME_INTRO_SEEN] ?: false,
            lastMediaId = prefs[Keys.LAST_MEDIA_ID] ?: -1L,
            lastPositionMs = prefs[Keys.LAST_POSITION_MS] ?: 0L,
            totalListeningTimeMs = prefs[Keys.TOTAL_LISTENING_MS] ?: 0L
        )
    }

    suspend fun getCurrentSettings(): AppSettings = settingsFlow.first()

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setThemeStyle(style: AppThemeStyle) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.THEME_STYLE] = style.name
            val mappedAccent = when (style) {
                AppThemeStyle.OCEAN_SAPPHIRE -> AccentPalette.OCEAN_CYAN
                AppThemeStyle.VELVET_VIOLET -> AccentPalette.VIOLET_PULSE
                AppThemeStyle.SUNSET_AMBER -> AccentPalette.SUNSET_AMBER
                else -> AccentPalette.EMERALD_MINT
            }
            prefs[Keys.ACCENT_PALETTE] = mappedAccent.name
        }
    }

    suspend fun setPlayerScreenStyle(style: PlayerScreenStyle) {
        context.settingsDataStore.edit { it[Keys.PLAYER_SCREEN_STYLE] = style.name }
    }

    suspend fun setAccentPalette(palette: AccentPalette) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.ACCENT_PALETTE] = palette.name
            if (prefs[Keys.THEME_STYLE] == null) {
                prefs[Keys.THEME_STYLE] = when (palette) {
                    AccentPalette.EMERALD_MINT -> AppThemeStyle.LUMA_GLASS.name
                    AccentPalette.OCEAN_CYAN -> AppThemeStyle.OCEAN_SAPPHIRE.name
                    AccentPalette.VIOLET_PULSE -> AppThemeStyle.VELVET_VIOLET.name
                    AccentPalette.SUNSET_AMBER -> AppThemeStyle.SUNSET_AMBER.name
                }
            }
        }
    }

    suspend fun setGlassQuality(quality: GlassQuality) {
        context.settingsDataStore.edit { it[Keys.GLASS_QUALITY] = quality.name }
    }

    suspend fun setReduceMotion(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.REDUCE_MOTION] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit {
            it[Keys.HAPTICS_ENABLED] = enabled
            if (!enabled) {
                it[Keys.HAPTIC_INTENSITY] = HapticIntensity.OFF.name
            } else if (it[Keys.HAPTIC_INTENSITY] == HapticIntensity.OFF.name) {
                it[Keys.HAPTIC_INTENSITY] = HapticIntensity.SUBTLE.name
            }
        }
    }

    suspend fun setHapticIntensity(intensity: HapticIntensity) {
        context.settingsDataStore.edit {
            it[Keys.HAPTIC_INTENSITY] = intensity.name
            it[Keys.HAPTICS_ENABLED] = intensity != HapticIntensity.OFF
        }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        context.settingsDataStore.edit { it[Keys.PLAYBACK_SPEED] = speed }
    }

    suspend fun setCrossfadeEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.CROSSFADE_ENABLED] = enabled }
    }

    suspend fun setGaplessPlaybackEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.GAPLESS_PLAYBACK] = enabled }
    }

    suspend fun setVolumeNormalizationEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.VOLUME_NORMALIZATION] = enabled }
    }

    suspend fun setStereoBalance(balance: Float) {
        context.settingsDataStore.edit { it[Keys.STEREO_BALANCE] = balance.coerceIn(-1f, 1f) }
    }

    suspend fun setReverbPreset(preset: ReverbEffectPreset) {
        context.settingsDataStore.edit { it[Keys.REVERB_PRESET] = preset.name }
    }

    suspend fun setVirtualizerStrength(strength: Int) {
        context.settingsDataStore.edit { it[Keys.VIRTUALIZER_STRENGTH] = strength.coerceIn(0, 1000) }
    }

    suspend fun setDynamicCoverTheme(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.DYNAMIC_COVER] = enabled }
    }

    suspend fun setVisualizerEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.VISUALIZER_ENABLED] = enabled }
    }

    suspend fun setVisualizerStyle(style: VisualizerStyle) {
        context.settingsDataStore.edit { it[Keys.VISUALIZER_STYLE] = style.name }
    }

    suspend fun setVisualizerPreset(preset: VisualizerPreset) {
        context.settingsDataStore.edit { it[Keys.VISUALIZER_PRESET] = preset.name }
    }

    suspend fun setReactiveLightingEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.REACTIVE_LIGHTING] = enabled }
    }

    suspend fun setCoverGesturesEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.COVER_GESTURES] = enabled }
    }

    suspend fun setCoverDoubleTapAction(action: CoverDoubleTapAction) {
        context.settingsDataStore.edit { it[Keys.COVER_DOUBLE_TAP] = action.name }
    }

    suspend fun setMinimalistPlayerMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.MINIMALIST_PLAYER] = enabled }
    }

    suspend fun setAlbumArtAnimationEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.ALBUM_ART_ANIM] = enabled }
    }

    suspend fun setPlaybackAnimationStyle(style: PlaybackAnimationStyle) {
        context.settingsDataStore.edit { it[Keys.PLAYBACK_ANIM_STYLE] = style.name }
    }

    suspend fun setArtworkShape(shape: ArtworkShape) {
        context.settingsDataStore.edit { it[Keys.ARTWORK_SHAPE] = shape.name }
    }

    suspend fun setCustomAccentHex(argb: Long) {
        context.settingsDataStore.edit { it[Keys.CUSTOM_ACCENT_HEX] = argb }
    }

    suspend fun setGlassTransparencyScale(scale: Float) {
        context.settingsDataStore.edit { it[Keys.GLASS_TRANSPARENCY] = scale.coerceIn(0.45f, 1.45f) }
    }

    suspend fun setBackgroundBlurScale(scale: Float) {
        context.settingsDataStore.edit { it[Keys.BACKGROUND_BLUR] = scale.coerceIn(0.4f, 1.5f) }
    }

    suspend fun setNavBarShowLabels(show: Boolean) {
        context.settingsDataStore.edit { it[Keys.NAV_BAR_LABELS] = show }
    }

    suspend fun setHomeSectionOrder(order: String, hidden: Set<String>) {
        context.settingsDataStore.edit {
            it[Keys.HOME_SECTION_ORDER] = order
            it[Keys.HIDDEN_HOME_SECTIONS] = hidden
        }
    }

    suspend fun setExcludedFolders(folders: Set<String>) {
        context.settingsDataStore.edit { it[Keys.EXCLUDED_FOLDERS] = folders }
    }

    suspend fun setPauseHistoryTracking(paused: Boolean) {
        context.settingsDataStore.edit { it[Keys.PAUSE_HISTORY] = paused }
    }

    suspend fun setLyricsAppearance(fontScale: Float, centered: Boolean) {
        context.settingsDataStore.edit {
            it[Keys.LYRICS_FONT_SCALE] = fontScale.coerceIn(0.8f, 1.5f)
            it[Keys.LYRICS_CENTERED] = centered
        }
    }

    suspend fun saveCustomAudioPreset(preset: CustomAudioPreset) {
        context.settingsDataStore.edit { prefs ->
            val current = decodeCustomPresets(prefs[Keys.CUSTOM_AUDIO_PRESETS] ?: "[]")
                .filterNot { it.name.equals(preset.name, ignoreCase = true) }
                .toMutableList()
            current.add(0, preset)
            prefs[Keys.CUSTOM_AUDIO_PRESETS] = encodeCustomPresets(current.take(12))
        }
    }

    suspend fun deleteCustomAudioPreset(name: String) {
        context.settingsDataStore.edit { prefs ->
            val updated = decodeCustomPresets(prefs[Keys.CUSTOM_AUDIO_PRESETS] ?: "[]")
                .filterNot { it.name.equals(name, ignoreCase = true) }
            prefs[Keys.CUSTOM_AUDIO_PRESETS] = encodeCustomPresets(updated)
        }
    }

    suspend fun applyRestoredSettings(s: AppSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = s.themeMode.name
            prefs[Keys.THEME_STYLE] = s.themeStyle.name
            prefs[Keys.PLAYER_SCREEN_STYLE] = s.playerScreenStyle.name
            prefs[Keys.ACCENT_PALETTE] = s.accentPalette.name
            prefs[Keys.GLASS_QUALITY] = s.glassQuality.name
            prefs[Keys.REDUCE_MOTION] = s.reduceMotion
            prefs[Keys.HAPTICS_ENABLED] = s.hapticsEnabled
            prefs[Keys.HAPTIC_INTENSITY] = s.hapticIntensity.name
            prefs[Keys.PLAYBACK_SPEED] = s.playbackSpeed
            prefs[Keys.CROSSFADE_ENABLED] = s.crossfadeEnabled
            prefs[Keys.CROSSFADE_SEC] = s.crossfadeDurationSec
            prefs[Keys.GAPLESS_PLAYBACK] = s.gaplessPlaybackEnabled
            prefs[Keys.VOLUME_NORMALIZATION] = s.volumeNormalizationEnabled
            prefs[Keys.STEREO_BALANCE] = s.stereoBalance
            prefs[Keys.REVERB_PRESET] = s.reverbPreset.name
            prefs[Keys.VIRTUALIZER_STRENGTH] = s.virtualizerStrength
            prefs[Keys.DYNAMIC_COVER] = s.dynamicCoverTheme
            prefs[Keys.VISUALIZER_ENABLED] = s.visualizerEnabled
            prefs[Keys.VISUALIZER_STYLE] = s.visualizerStyle.name
            prefs[Keys.VISUALIZER_PRESET] = s.visualizerPreset.name
            prefs[Keys.REACTIVE_LIGHTING] = s.reactiveLightingEnabled
            prefs[Keys.COVER_GESTURES] = s.coverGesturesEnabled
            prefs[Keys.COVER_DOUBLE_TAP] = s.coverDoubleTapAction.name
            prefs[Keys.MINIMALIST_PLAYER] = s.minimalistPlayerMode
            prefs[Keys.ALBUM_ART_ANIM] = s.albumArtAnimationEnabled
            prefs[Keys.PLAYBACK_ANIM_STYLE] = s.playbackAnimationStyle.name
            prefs[Keys.ARTWORK_SHAPE] = s.artworkShape.name
            prefs[Keys.CUSTOM_ACCENT_HEX] = s.customAccentHex
            prefs[Keys.GLASS_TRANSPARENCY] = s.glassTransparencyScale
            prefs[Keys.BACKGROUND_BLUR] = s.backgroundBlurScale
            prefs[Keys.NAV_BAR_LABELS] = s.navBarShowLabels
            prefs[Keys.HOME_SECTION_ORDER] = s.homeSectionOrder
            prefs[Keys.HIDDEN_HOME_SECTIONS] = s.hiddenHomeSections
            prefs[Keys.EXCLUDED_FOLDERS] = s.excludedFolders
            prefs[Keys.PAUSE_HISTORY] = s.pauseHistoryTracking
            prefs[Keys.LYRICS_FONT_SCALE] = s.lyricsFontScale
            prefs[Keys.LYRICS_CENTERED] = s.lyricsCentered
            prefs[Keys.CUSTOM_AUDIO_PRESETS] = s.customAudioPresetsJson
        }
    }

    suspend fun setWelcomeIntroSeen(seen: Boolean) {
        context.settingsDataStore.edit { it[Keys.WELCOME_INTRO_SEEN] = seen }
    }

    suspend fun saveLastPlaybackSession(mediaId: Long, positionMs: Long) {
        context.settingsDataStore.edit {
            it[Keys.LAST_MEDIA_ID] = mediaId
            it[Keys.LAST_POSITION_MS] = positionMs
        }
    }

    suspend fun addListeningTime(deltaMs: Long) {
        if (deltaMs <= 0L) return
        context.settingsDataStore.edit { prefs ->
            val current = prefs[Keys.TOTAL_LISTENING_MS] ?: 0L
            prefs[Keys.TOTAL_LISTENING_MS] = current + deltaMs
        }
    }

    suspend fun resetListeningStats() {
        context.settingsDataStore.edit { it[Keys.TOTAL_LISTENING_MS] = 0L }
    }

    companion object {
        fun decodeCustomPresets(json: String): List<CustomAudioPreset> = runCatching {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val bandsArr = obj.optJSONArray("bands") ?: JSONArray()
                    val bands = buildList {
                        for (b in 0 until bandsArr.length()) add(bandsArr.optInt(b, 0))
                    }
                    add(
                        CustomAudioPreset(
                            name = obj.optString("name", "Personalizado"),
                            bandLevelsMb = bands,
                            bassStrength = obj.optInt("bass", 0),
                            virtualizerStrength = obj.optInt("virt", 0),
                            reverbPreset = runCatching {
                                ReverbEffectPreset.valueOf(obj.optString("reverb", "NONE"))
                            }.getOrDefault(ReverbEffectPreset.NONE),
                            loudnessGainMb = obj.optInt("loud", 0),
                            stereoBalance = obj.optDouble("balance", 0.0).toFloat()
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())

        fun encodeCustomPresets(presets: List<CustomAudioPreset>): String {
            val arr = JSONArray()
            presets.forEach { preset ->
                val obj = JSONObject().apply {
                    put("name", preset.name)
                    put("bands", JSONArray(preset.bandLevelsMb))
                    put("bass", preset.bassStrength)
                    put("virt", preset.virtualizerStrength)
                    put("reverb", preset.reverbPreset.name)
                    put("loud", preset.loudnessGainMb)
                    put("balance", preset.stereoBalance.toDouble())
                }
                arr.put(obj)
            }
            return arr.toString()
        }
    }
}
