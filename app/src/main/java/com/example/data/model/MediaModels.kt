package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class MediaType {
    AUDIO,
    VIDEO
}

enum class SortOrder(val label: String) {
    TITLE("Nome"),
    ARTIST("Artista"),
    ALBUM("Álbum"),
    DATE_ADDED("Data de adição"),
    DURATION("Duração"),
    PLAY_COUNT("Mais ouvidas"),
    YEAR("Ano"),
    TRACK_NUMBER("Nº da faixa")
}

enum class LibraryFilter(val label: String) {
    ALL("Tudo"),
    FAVORITES("Favoritos"),
    MISSING_ARTWORK("Sem capa"),
    MISSING_METADATA("Sem metadados"),
    SHORT("Curtas (<2m)"),
    LONG("Longas (>5m)")
}

enum class ViewMode {
    LIST,
    GRID
}

enum class SearchFilter(val label: String) {
    ALL("Tudo"),
    SONGS("Músicas"),
    ALBUMS("Álbuns"),
    ARTISTS("Artistas"),
    VIDEOS("Vídeos")
}

enum class LibraryCategory(val label: String) {
    SONGS("Músicas"),
    ALBUMS("Álbuns"),
    ARTISTS("Artistas"),
    GENRES("Géneros"),
    FOLDERS("Pastas"),
    PLAYLISTS("Playlists"),
    FAVORITES("Favoritos"),
    VIDEOS("Vídeos")
}

enum class MoodType(val label: String, val subtitle: String) {
    CHILL("Chill", "Faixas suaves e relaxantes"),
    FOCUS("Focus", "Concentração contínua"),
    WORKOUT("Workout", "Ritmo e energia"),
    ROAD_TRIP("Road Trip", "Companheiras de viagem")
}

@Entity(
    tableName = "media_items",
    indices = [
        Index(value = ["uri"], unique = true),
        Index(value = ["mediaType"]),
        Index(value = ["title"]),
        Index(value = ["artist"]),
        Index(value = ["album"])
    ]
)
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val mediaType: MediaType,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String = "",
    val genre: String = "",
    val releaseYear: Int = 0,
    val trackNumber: Int = 0,
    val durationMs: Long = 0L,
    val fileName: String,
    val folderName: String,
    val folderPath: String,
    val mimeType: String = "",
    val sizeBytes: Long = 0L,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val artworkPath: String? = null,
    val embeddedLyrics: String? = null,
    val lyricsOffsetMs: Long = 0L,
    val addedAt: Long = System.currentTimeMillis(),
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L,
    val lastPositionMs: Long = 0L,
    val isFavorite: Boolean = false,
    val isAvailable: Boolean = true
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "mediaId"],
    indices = [Index(value = ["mediaId"])]
)
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val mediaId: Long,
    val position: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

data class PlaylistWithTracks(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistTrackCrossRef::class,
            parentColumn = "playlistId",
            entityColumn = "mediaId"
        )
    )
    val tracks: List<MediaItemEntity>
)

@Entity(
    tableName = "playback_history",
    indices = [Index(value = ["mediaId"]), Index(value = ["playedAt"])]
)
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaId: Long,
    val playedAt: Long = System.currentTimeMillis()
)

data class HistoryWithMedia(
    @Embedded val history: PlaybackHistoryEntity,
    @Relation(
        parentColumn = "mediaId",
        entityColumn = "id"
    )
    val media: MediaItemEntity?
)

@Entity(tableName = "queue_items")
data class QueueItemEntity(
    @PrimaryKey val queuePosition: Int,
    val mediaId: Long
)

data class AlbumSummary(
    val name: String,
    val artist: String,
    val artworkPath: String?,
    val trackCount: Int,
    val totalDurationMs: Long,
    val tracks: List<MediaItemEntity>
)

data class ArtistSummary(
    val name: String,
    val trackCount: Int,
    val albumCount: Int,
    val artworkPath: String?,
    val tracks: List<MediaItemEntity>
)

data class FolderSummary(
    val name: String,
    val path: String,
    val itemCount: Int,
    val tracks: List<MediaItemEntity>
)

data class GenreSummary(
    val name: String,
    val trackCount: Int,
    val artworkPath: String?,
    val tracks: List<MediaItemEntity>
)

data class DuplicateGroup(
    val titleLabel: String,
    val reasonLabel: String,
    val items: List<MediaItemEntity>
)

enum class ThemeMode(val label: String) {
    SYSTEM("Sistema"),
    LIGHT("Claro"),
    DARK("Escuro")
}

enum class SurfaceMaterial(val label: String) {
    LIQUID_GLASS("Cristal"),
    NEUMORPHIC_BW("Neumórfico P&B"),
    NEUMORPHIC_SOFT("Soft UI Claro"),
    AMOLED_CRISP("AMOLED Puro")
}

enum class AppThemeStyle(
    val title: String,
    val subtitle: String,
    val material: SurfaceMaterial
) {
    LUMA_GLASS("Cristal", "Vidro Translúcido", SurfaceMaterial.LIQUID_GLASS),
    OCEAN_SAPPHIRE("Ocean Safira", "Translúcido", SurfaceMaterial.LIQUID_GLASS),
    VELVET_VIOLET("Velvet Violeta", "Translúcido", SurfaceMaterial.LIQUID_GLASS),
    SUNSET_AMBER("Sunset Âmbar", "Translúcido", SurfaceMaterial.LIQUID_GLASS),
    NORDIC_FOREST("Nordic Floresta", "Translúcido", SurfaceMaterial.LIQUID_GLASS),
    ALABASTER_BW("Alabaster P&B", "Neumórfico P&B", SurfaceMaterial.NEUMORPHIC_BW),
    ROSE_PORCELAIN("Branco Soft", "Soft UI Claro", SurfaceMaterial.NEUMORPHIC_SOFT),
    OBSIDIAN_AMOLED("Obsidian", "AMOLED Puro", SurfaceMaterial.AMOLED_CRISP)
}

enum class AccentPalette(val label: String) {
    EMERALD_MINT("Verde Menta"),
    OCEAN_CYAN("Azul Oceano"),
    VIOLET_PULSE("Violeta Cristal"),
    SUNSET_AMBER("Âmbar Dourado")
}

enum class GlassQuality(val label: String) {
    ULTRA("Alto (Blur + Reflexos)"),
    BALANCED("Equilibrado"),
    MINIMAL("Leve (Alto Contraste)")
}

enum class PlayerScreenStyle(
    val title: String,
    val subtitle: String
) {
    GLASS_DECK("Deck Espacial", "Capa anterior, atual e próxima em 3D"),
    CAROUSEL_VINYL("Vinil Clássico", "Capa circular giratória e visualizador"),
    MINIMAL_POSTER("Poster Imersivo", "Arte ampliada com HUD translúcido"),
    LYRICS_STAGE("Palco de Letras", "Letra sincronizada em destaque")
}

enum class VisualizerStyle(val label: String) {
    BARS_3D("Barras 3D"),
    CIRCULAR_SPECTRUM("Espectro Circular"),
    WAVEFORM("Onda Sonora"),
    PARTICLES("Partículas"),
    FREQUENCY_ANALYZER("Analisador")
}

enum class VisualizerPreset(val label: String) {
    NEON_PULSE("Pulso Neon"),
    AURORA_SILK("Aurora Suave"),
    STUDIO_SPECTRUM("Estúdio Pro"),
    MINIMAL_BREATH("Respiração Mínima")
}

enum class ArtworkShape(val label: String) {
    ROUNDED("Arredondado"),
    SQUIRCLE("Squircle"),
    CIRCLE("Circular"),
    SHARP("Reto Clássico")
}

enum class PlaybackAnimationStyle(val label: String) {
    SPRING_FLOAT("Flutuação 3D"),
    VINYL_SPIN("Rotação Vinil"),
    BREATH_PULSE("Pulso Orgânico"),
    CALM("Calmo")
}

enum class HapticIntensity(val label: String) {
    OFF("Desativado"),
    SUBTLE("Suave"),
    CRISP("Nítido")
}

enum class CoverDoubleTapAction(val label: String) {
    PLAY_PAUSE("Reproduzir / Pausar"),
    FAVORITE("Marcar Favorito"),
    SEEK_FORWARD("Avançar 10s")
}

enum class ReverbEffectPreset(val label: String) {
    NONE("Desligado"),
    SMALL_ROOM("Sala Pequena"),
    MEDIUM_HALL("Auditório"),
    LARGE_HALL("Catedral"),
    PLATE("Estúdio Plate")
}

enum class HomeSectionType(val key: String, val label: String) {
    FOR_YOU("FOR_YOU", "Para Ti & Insights"),
    CONTINUE_LISTENING("CONTINUE_LISTENING", "Continuar a ouvir"),
    RECENTLY_ADDED("RECENTLY_ADDED", "Adicionadas recentemente"),
    SMART_MIXES("SMART_MIXES", "Playlists Inteligentes"),
    MOODS("MOODS", "Coleções por Humor"),
    MOST_PLAYED("MOST_PLAYED", "Mais ouvidas"),
    RECENT_ARTISTS("RECENT_ARTISTS", "Artistas recentes"),
    VIDEOS("VIDEOS", "Vídeos locais"),
    QUICK_PLAYLISTS("QUICK_PLAYLISTS", "Playlists rápidas");

    companion object {
        val DEFAULT_ORDER_STRING: String = entries.joinToString(",") { it.key }

        fun parseOrder(raw: String): List<HomeSectionType> {
            val mapped = raw.split(',')
                .mapNotNull { token -> entries.firstOrNull { it.key == token.trim() } }
                .distinct()
            val missing = entries.filter { it !in mapped }
            return mapped + missing
        }
    }
}

data class CustomAudioPreset(
    val name: String,
    val bandLevelsMb: List<Int>,
    val bassStrength: Int,
    val virtualizerStrength: Int,
    val reverbPreset: ReverbEffectPreset,
    val loudnessGainMb: Int,
    val stereoBalance: Float
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeStyle: AppThemeStyle = AppThemeStyle.LUMA_GLASS,
    val playerScreenStyle: PlayerScreenStyle = PlayerScreenStyle.GLASS_DECK,
    val accentPalette: AccentPalette = AccentPalette.EMERALD_MINT,
    val glassQuality: GlassQuality = GlassQuality.ULTRA,
    val reduceMotion: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val hapticIntensity: HapticIntensity = HapticIntensity.SUBTLE,
    val playbackSpeed: Float = 1.0f,
    val crossfadeEnabled: Boolean = false,
    val crossfadeDurationSec: Int = 3,
    val gaplessPlaybackEnabled: Boolean = true,
    val volumeNormalizationEnabled: Boolean = false,
    val stereoBalance: Float = 0f,
    val reverbPreset: ReverbEffectPreset = ReverbEffectPreset.NONE,
    val virtualizerStrength: Int = 0,
    val dynamicCoverTheme: Boolean = true,
    val visualizerEnabled: Boolean = true,
    val visualizerStyle: VisualizerStyle = VisualizerStyle.BARS_3D,
    val visualizerPreset: VisualizerPreset = VisualizerPreset.AURORA_SILK,
    val reactiveLightingEnabled: Boolean = true,
    val coverGesturesEnabled: Boolean = true,
    val coverDoubleTapAction: CoverDoubleTapAction = CoverDoubleTapAction.PLAY_PAUSE,
    val minimalistPlayerMode: Boolean = false,
    val albumArtAnimationEnabled: Boolean = true,
    val playbackAnimationStyle: PlaybackAnimationStyle = PlaybackAnimationStyle.SPRING_FLOAT,
    val artworkShape: ArtworkShape = ArtworkShape.ROUNDED,
    val customAccentHex: Long = 0L,
    val glassTransparencyScale: Float = 1.0f,
    val backgroundBlurScale: Float = 1.0f,
    val navBarShowLabels: Boolean = false,
    val homeSectionOrder: String = HomeSectionType.DEFAULT_ORDER_STRING,
    val hiddenHomeSections: Set<String> = emptySet(),
    val excludedFolders: Set<String> = emptySet(),
    val pauseHistoryTracking: Boolean = false,
    val lyricsFontScale: Float = 1.0f,
    val lyricsCentered: Boolean = true,
    val customAudioPresetsJson: String = "[]",
    val hasSeenWelcomeIntro: Boolean = false,
    val lastMediaId: Long = -1L,
    val lastPositionMs: Long = 0L,
    val totalListeningTimeMs: Long = 0L
)
