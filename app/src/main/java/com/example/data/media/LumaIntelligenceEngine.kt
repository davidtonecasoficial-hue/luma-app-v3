package com.example.data.media

import androidx.compose.runtime.Immutable
import com.example.data.model.HistoryWithMedia
import com.example.data.model.ListeningSessionEntity
import com.example.data.model.MediaItemEntity
import com.example.data.model.MediaType
import java.util.Calendar
import kotlin.math.abs

enum class LumaMoodCategory(val title: String, val subtitle: String) {
    CALM_CHILL("Chill & Atmosfera", "Acústico, jazz, ambiente e ritmo suave"),
    FOCUS_FLOW("Foco & Fluxo", "Instrumental, clássico, lo-fi e concentração"),
    HIGH_ENERGY("Energia & Movimento", "Pop, rock, afrobeats, eletrónica e treino"),
    LATE_NIGHT("Noite & Profundidade", "Kizomba, R&B, soul e madrugadas")
}

@Immutable
data class TrackMoodClassification(
    val track: MediaItemEntity,
    val mood: LumaMoodCategory,
    val confidence: Float
)

@Immutable
data class AutoCategorizedBucket(
    val title: String,
    val subtitle: String,
    val tracks: List<MediaItemEntity>
)

@Immutable
data class ListeningPatternReport(
    val peakWindowLabel: String = "Noite",
    val preferredMood: LumaMoodCategory = LumaMoodCategory.CALM_CHILL,
    val completionRatePercent: Int = 100,
    val skipRatePercent: Int = 0,
    val avgSessionMinutes: Int = 0,
    val topRouteLabel: String = "Altifalante",
    val habitSummary: String = "Ouve mais músicas locais para gerar o teu perfil de hábitos."
)

@Immutable
data class LumaIntelligenceSnapshot(
    val smartRecommendations: List<MediaItemEntity> = emptyList(),
    val personalizedMix: List<MediaItemEntity> = emptyList(),
    val patternReport: ListeningPatternReport = ListeningPatternReport(),
    val moodBuckets: Map<LumaMoodCategory, List<MediaItemEntity>> = emptyMap(),
    val autoCategories: List<AutoCategorizedBucket> = emptyList(),
    val activeMoodHint: LumaMoodCategory = LumaMoodCategory.CALM_CHILL
)

object LumaIntelligenceEngine {

    fun classifyMood(track: MediaItemEntity): TrackMoodClassification {
        val text = "${track.genre} ${track.title} ${track.album} ${track.folderName}".lowercase()
        val dur = track.durationMs

        val energyTokens = listOf("pop", "rock", "dance", "edm", "electr", "hip", "rap", "trap", "afro", "kuduro", "reggaeton", "funk", "workout", "gym")
        val focusTokens = listOf("classic", "clássic", "piano", "instrumental", "lo-fi", "lofi", "study", "focus", "ost", "soundtrack", "orchestra")
        val nightTokens = listOf("kizomba", "r&b", "rnb", "soul", "blues", "midnight", "night", "noite", "tarrax", "slow")
        val chillTokens = listOf("chill", "ambient", "acúst", "acoustic", "jazz", "bossa", "folk", "soft", "relax", "indie")

        val energyScore = energyTokens.count { text.contains(it) } * 2 + if (dur in 120_000L..225_000L) 1 else 0
        val focusScore = focusTokens.count { text.contains(it) } * 2 + if (dur in 150_000L..340_000L) 1 else 0
        val nightScore = nightTokens.count { text.contains(it) } * 2 + if (dur >= 210_000L) 1 else 0
        val chillScore = chillTokens.count { text.contains(it) } * 2 + if (dur >= 195_000L) 1 else 0

        val scores = listOf(
            LumaMoodCategory.HIGH_ENERGY to energyScore,
            LumaMoodCategory.FOCUS_FLOW to focusScore,
            LumaMoodCategory.LATE_NIGHT to nightScore,
            LumaMoodCategory.CALM_CHILL to chillScore
        )
        val best = scores.maxByOrNull { it.second } ?: (LumaMoodCategory.CALM_CHILL to 1)
        val total = scores.sumOf { it.second }.coerceAtLeast(1)
        val confidence = (best.second.toFloat() / total.toFloat()).coerceIn(0.42f, 0.98f)
        return TrackMoodClassification(track = track, mood = best.first, confidence = confidence)
    }

    fun findSimilarTracks(
        seed: MediaItemEntity,
        library: List<MediaItemEntity>,
        limit: Int = 15
    ): List<MediaItemEntity> {
        val candidates = library.filter { it.mediaType == MediaType.AUDIO && it.isAvailable && it.id != seed.id }
        if (candidates.isEmpty()) return emptyList()

        val seedArtistKey = ArtistNormalizer.artistGroupingKey(seed.artist, seed.fileName)
        val seedGenre = seed.genre.ifBlank {
            ArtistNormalizer.inferGenre(seed.title, seed.artist, seed.album, seed.folderName, seed.durationMs)
        }.lowercase()
        val seedMood = classifyMood(seed).mood

        return candidates.sortedByDescending { cand ->
            var score = 0
            val candArtistKey = ArtistNormalizer.artistGroupingKey(cand.artist, cand.fileName)
            if (candArtistKey == seedArtistKey) score += 35
            if (seed.album.isNotBlank() && !seed.album.equals("Álbum desconhecido", true) &&
                cand.album.equals(seed.album, ignoreCase = true)
            ) score += 18

            val candGenre = cand.genre.ifBlank {
                ArtistNormalizer.inferGenre(cand.title, cand.artist, cand.album, cand.folderName, cand.durationMs)
            }.lowercase()
            if (candGenre == seedGenre) score += 30
            if (classifyMood(cand).mood == seedMood) score += 18
            if (seed.releaseYear > 0 && cand.releaseYear > 0 && abs(seed.releaseYear - cand.releaseYear) <= 4) score += 8
            if (abs(seed.durationMs - cand.durationMs) <= 45_000L) score += 6
            if (cand.isFavorite) score += 5
            score
        }.take(limit)
    }

    fun buildAutoDjMix(
        seed: MediaItemEntity?,
        library: List<MediaItemEntity>,
        sessions: List<ListeningSessionEntity> = emptyList(),
        limit: Int = 20
    ): List<MediaItemEntity> {
        val audio = library.filter { it.mediaType == MediaType.AUDIO && it.isAvailable }
        if (audio.isEmpty()) return emptyList()

        val earlySkippedIds = sessions
            .filter { it.skippedEarly }
            .groupingBy { it.mediaId }
            .eachCount()

        val moodMap = audio.associate { it.id to classifyMood(it).mood }
        val remaining = audio.toMutableList()
        val mix = mutableListOf<MediaItemEntity>()

        val first = seed?.takeIf { s -> remaining.any { it.id == s.id } }
            ?: remaining.maxByOrNull { it.playCount + (if (it.isFavorite) 4 else 0) - (earlySkippedIds[it.id] ?: 0) * 3 }
            ?: remaining.first()

        remaining.removeAll { it.id == first.id }
        mix.add(first)

        var current = first
        while (remaining.isNotEmpty() && mix.size < limit) {
            val currentArtist = ArtistNormalizer.artistGroupingKey(current.artist, current.fileName)
            val currentMood = moodMap[current.id]
            val currentGenre = current.genre.lowercase()

            val next = remaining.maxByOrNull { cand ->
                var transitionScore = 0
                if (moodMap[cand.id] == currentMood) transitionScore += 28
                if (currentGenre.isNotBlank() && cand.genre.equals(currentGenre, ignoreCase = true)) transitionScore += 20
                val candArtist = ArtistNormalizer.artistGroupingKey(cand.artist, cand.fileName)
                // Slight variety penalty if repeating the exact same artist back-to-back
                if (candArtist == currentArtist) transitionScore -= 8 else transitionScore += 8
                if (abs(cand.durationMs - current.durationMs) <= 40_000L) transitionScore += 8
                if (cand.isFavorite) transitionScore += 6
                transitionScore += cand.playCount.coerceAtMost(8)
                transitionScore -= (earlySkippedIds[cand.id] ?: 0) * 12
                transitionScore
            } ?: break

            remaining.remove(next)
            mix.add(next)
            current = next
        }
        return mix
    }

    fun analyzeListeningPatterns(
        tracks: List<MediaItemEntity>,
        sessions: List<ListeningSessionEntity>,
        history: List<HistoryWithMedia>,
        nowMs: Long = System.currentTimeMillis()
    ): ListeningPatternReport {
        val audio = tracks.filter { it.mediaType == MediaType.AUDIO && it.isAvailable }
        val cal = Calendar.getInstance()
        val windowCounts = IntArray(4) // 0 Madrugada, 1 Manhã, 2 Tarde, 3 Noite
        val timestamps = if (sessions.isNotEmpty()) {
            sessions.map { it.startedAt }
        } else {
            history.map { it.history.playedAt }
        }
        timestamps.forEach { ts ->
            cal.timeInMillis = ts
            when (cal.get(Calendar.HOUR_OF_DAY)) {
                in 0..5 -> windowCounts[0]++
                in 6..11 -> windowCounts[1]++
                in 12..17 -> windowCounts[2]++
                else -> windowCounts[3]++
            }
        }
        val peakIdx = if (timestamps.isEmpty()) {
            cal.timeInMillis = nowMs
            when (cal.get(Calendar.HOUR_OF_DAY)) {
                in 0..5 -> 0
                in 6..11 -> 1
                in 12..17 -> 2
                else -> 3
            }
        } else {
            windowCounts.indices.maxByOrNull { windowCounts[it] } ?: 3
        }
        val peakWindowLabel = when (peakIdx) {
            0 -> "Madrugada"
            1 -> "Manhã"
            2 -> "Tarde"
            else -> "Noite"
        }

        val completedCount = sessions.count { it.completedTrack }
        val skipCount = sessions.count { it.skippedEarly }
        val completionPct = if (sessions.isNotEmpty()) {
            ((completedCount * 100f) / sessions.size).toInt().coerceIn(0, 100)
        } else 100
        val skipPct = if (sessions.isNotEmpty()) {
            ((skipCount * 100f) / sessions.size).toInt().coerceIn(0, 100)
        } else 0

        val avgSessionMin = if (sessions.isNotEmpty()) {
            ((sessions.sumOf { it.durationListenedMs } / sessions.size) / 60_000L).toInt().coerceAtLeast(1)
        } else if (audio.any { it.playCount > 0 }) {
            3
        } else 0

        val routeCounts = sessions.groupingBy { it.outputRoute }.eachCount()
        val topRawRoute = routeCounts.maxByOrNull { it.value }?.key ?: "SPEAKER"
        val topRouteLabel = when (topRawRoute.uppercase()) {
            "BLUETOOTH" -> "Bluetooth"
            "WIRED" -> "Auscultadores"
            "USB" -> "Áudio USB"
            "HDMI_CAST" -> "HDMI / Cast"
            else -> "Altifalante"
        }

        val weightedTracks = audio.filter { it.isFavorite || it.playCount > 0 }.ifEmpty { audio }
        val preferredMood = weightedTracks
            .groupingBy { classifyMood(it).mood }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: LumaMoodCategory.CALM_CHILL

        val summary = when {
            audio.isEmpty() -> "Importe músicas locais para ativar a análise inteligente offline."
            sessions.isNotEmpty() ->
                "Costuma ouvir mais à $peakWindowLabel via $topRouteLabel, com preferência por ${preferredMood.title} ($completionPct% conclusão)."
            else ->
                "A sua biblioteca destaca-se em ${preferredMood.title}. Comece a reproduzir para refinar os padrões horários."
        }

        return ListeningPatternReport(
            peakWindowLabel = peakWindowLabel,
            preferredMood = preferredMood,
            completionRatePercent = completionPct,
            skipRatePercent = skipPct,
            avgSessionMinutes = avgSessionMin,
            topRouteLabel = topRouteLabel,
            habitSummary = summary
        )
    }

    fun buildSnapshot(
        tracks: List<MediaItemEntity>,
        sessions: List<ListeningSessionEntity>,
        history: List<HistoryWithMedia>,
        nowMs: Long = System.currentTimeMillis()
    ): LumaIntelligenceSnapshot {
        val audio = tracks.filter { it.mediaType == MediaType.AUDIO && it.isAvailable }
        val pattern = analyzeListeningPatterns(audio, sessions, history, nowMs)

        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val timeMood = when (hour) {
            in 6..11 -> LumaMoodCategory.FOCUS_FLOW
            in 12..17 -> LumaMoodCategory.HIGH_ENERGY
            in 18..22 -> LumaMoodCategory.CALM_CHILL
            else -> LumaMoodCategory.LATE_NIGHT
        }

        val classified = audio.map { classifyMood(it) }
        val moodBuckets = LumaMoodCategory.entries.associateWith { mood ->
            val direct = classified.filter { it.mood == mood }.map { it.track }
            direct.ifEmpty { audio.take(10) }.take(20)
        }

        val smartRecommendations = SmartPlaylistGenerator.personalizedRecommendations(audio, limit = 14)
        val personalizedMix = buildAutoDjMix(
            seed = smartRecommendations.firstOrNull(),
            library = audio,
            sessions = sessions,
            limit = 16
        )

        val autoCategories = buildList {
            val gems = audio.filter { it.playCount <= 1 }.sortedByDescending { it.addedAt }.take(12)
            if (gems.isNotEmpty()) {
                add(AutoCategorizedBucket("Joias Por Descobrir", "Faixas pouco tocadas na biblioteca", gems))
            }
            val deepCuts = audio.filter { it.durationMs >= 240_000L }.take(12)
            if (deepCuts.isNotEmpty()) {
                add(AutoCategorizedBucket("Sessões Imersivas", "Composições com 4+ minutos", deepCuts))
            }
            val crispShort = audio.filter { it.durationMs in 1L..179_999L }.take(12)
            if (crispShort.isNotEmpty()) {
                add(AutoCategorizedBucket("Rápidas & Diretas", "Faixas curtas e dinâmicas", crispShort))
            }
            val withSyncedLyrics = audio.filter { !it.embeddedLyrics.isNullOrBlank() }.take(12)
            if (withSyncedLyrics.isNotEmpty()) {
                add(AutoCategorizedBucket("Prontas para Cantar", "Músicas com letra associada", withSyncedLyrics))
            }
        }

        return LumaIntelligenceSnapshot(
            smartRecommendations = smartRecommendations,
            personalizedMix = personalizedMix,
            patternReport = pattern,
            moodBuckets = moodBuckets,
            autoCategories = autoCategories,
            activeMoodHint = timeMood
        )
    }
}
