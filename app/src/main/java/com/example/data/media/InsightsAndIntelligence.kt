package com.example.data.media

import androidx.compose.runtime.Immutable
import com.example.data.model.HistoryWithMedia
import com.example.data.model.ListeningSessionEntity
import com.example.data.model.MediaItemEntity
import com.example.data.model.MediaType
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

@Immutable
data class DayListeningStat(
    val dayLabel: String,
    val plays: Int,
    val estimatedMinutes: Int
)

@Immutable
data class WeekListeningStat(
    val weekLabel: String,
    val plays: Int,
    val estimatedMinutes: Int
)

@Immutable
data class RankedStat(
    val label: String,
    val subtitle: String,
    val plays: Int,
    val estimatedMinutes: Int,
    val artworkPath: String? = null
)

@Immutable
data class ListeningComparisonStats(
    val thisMonthPlays: Int = 0,
    val lastMonthPlays: Int = 0,
    val thisMonthMinutes: Int = 0,
    val lastMonthMinutes: Int = 0,
    val monthDeltaPercent: Int = 0,
    val thisWeekPlays: Int = 0,
    val lastWeekPlays: Int = 0,
    val thisWeekMinutes: Int = 0,
    val lastWeekMinutes: Int = 0,
    val weekDeltaPercent: Int = 0,
    val completionRatePercent: Int = 100,
    val earlySkipRatePercent: Int = 0,
    val topOutputRoute: String = "Altifalante"
)

@Immutable
data class LumaInsightsReport(
    val totalListeningTimeMs: Long,
    val totalPlays: Int,
    val uniqueTracksPlayed: Int,
    val uniqueArtistsHeard: Int,
    val topSongs: List<MediaItemEntity>,
    val topArtists: List<RankedStat>,
    val topAlbums: List<RankedStat>,
    val topGenres: List<RankedStat>,
    val weeklyStats: List<DayListeningStat>,
    val monthlyStats: List<WeekListeningStat>,
    val annualTotalPlays: Int,
    val annualMinutes: Int,
    val wrappedHeadline: String,
    val wrappedMoodTag: String,
    val peakListeningPeriod: String,
    val comparison: ListeningComparisonStats = ListeningComparisonStats(),
    val monthlyWrappedHeadline: String = "",
    val monthlyMinutes: Int = 0,
    val monthlyTotalPlays: Int = 0
)

enum class SmartPlaylistRule(val title: String, val subtitle: String) {
    MOST_PLAYED("Mais Tocadas", "As tuas faixas com maior rotação"),
    RECENTLY_ADDED("Recém-Chegadas", "Adicionadas recentemente à biblioteca"),
    HIDDEN_GEMS("Joias Escondidas", "Boas faixas que ainda ouviste pouco"),
    FAVORITES_MIX("Mix de Favoritos", "Seleção imediata das tuas preferidas"),
    ENERGETIC("Ritmo & Energia", "Faixas dinâmicas para movimento"),
    NIGHT_CHILL("Noite Tranquila", "Faixas suaves para relaxar")
}

object InsightsCalculator {

    fun compute(
        allMedia: List<MediaItemEntity>,
        history: List<HistoryWithMedia>,
        totalListeningTimeMs: Long,
        nowMs: Long = System.currentTimeMillis(),
        sessions: List<ListeningSessionEntity> = emptyList()
    ): LumaInsightsReport {
        val audio = allMedia.filter { it.mediaType == MediaType.AUDIO && it.isAvailable }
        val totalPlays = audio.sumOf { it.playCount }.coerceAtLeast(history.size)
        val uniqueTracksPlayed = audio.count { it.playCount > 0 }

        val topSongs = audio
            .filter { it.playCount > 0 }
            .sortedWith(compareByDescending<MediaItemEntity> { it.playCount }.thenByDescending { it.lastPlayedAt })
            .take(10)

        val topArtists = audio
            .groupBy { ArtistNormalizer.artistGroupingKey(it.artist, it.fileName) }
            .values
            .map { group ->
                val display = ArtistNormalizer.pickBestDisplayName(group)
                val plays = group.sumOf { it.playCount }
                val estMin = (group.sumOf { (it.playCount.coerceAtLeast(0) * it.durationMs.coerceAtLeast(180_000L)) } / 60_000L).toInt()
                RankedStat(
                    label = display,
                    subtitle = "${group.size} faixas",
                    plays = plays,
                    estimatedMinutes = estMin,
                    artworkPath = group.firstOrNull { it.artworkPath != null }?.artworkPath
                )
            }
            .filter { it.plays > 0 }
            .sortedByDescending { it.plays }
            .take(8)

        val topAlbums = audio
            .groupBy { it.album.ifBlank { "Álbum desconhecido" } }
            .map { (album, group) ->
                val plays = group.sumOf { it.playCount }
                val estMin = (group.sumOf { (it.playCount.coerceAtLeast(0) * it.durationMs.coerceAtLeast(180_000L)) } / 60_000L).toInt()
                RankedStat(
                    label = album,
                    subtitle = ArtistNormalizer.pickBestDisplayName(group),
                    plays = plays,
                    estimatedMinutes = estMin,
                    artworkPath = group.firstOrNull { it.artworkPath != null }?.artworkPath
                )
            }
            .filter { it.plays > 0 }
            .sortedByDescending { it.plays }
            .take(8)

        val topGenres = audio
            .groupBy {
                it.genre.ifBlank {
                    ArtistNormalizer.inferGenre(it.title, it.artist, it.album, it.folderName, it.durationMs)
                }
            }
            .map { (genre, group) ->
                val plays = group.sumOf { it.playCount }.coerceAtLeast(if (group.any { it.isFavorite }) 1 else 0)
                val estMin = (group.sumOf { it.playCount * it.durationMs } / 60_000L).toInt()
                RankedStat(
                    label = genre,
                    subtitle = "${group.size} músicas",
                    plays = plays,
                    estimatedMinutes = estMin,
                    artworkPath = group.firstOrNull { it.artworkPath != null }?.artworkPath
                )
            }
            .sortedByDescending { it.plays }
            .take(6)

        val cal = Calendar.getInstance()
        val dayLabels = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")
        val weeklyBuckets = MutableList(7) { 0 to 0L } // plays to ms

        val dayMillis = 86_400_000L
        val sevenDaysAgo = nowMs - 6 * dayMillis
        val thirtyDaysAgo = nowMs - 30 * dayMillis
        val yearAgo = nowMs - 365 * dayMillis

        val monthWeeks = MutableList(4) { 0 to 0L }
        var annualPlays = 0
        var annualMs = 0L
        val periodCounts = IntArray(4) // 0=Madrugada, 1=Manhã, 2=Tarde, 3=Noite

        for (entry in history) {
            val playedAt = entry.history.playedAt
            val dur = entry.media?.durationMs?.takeIf { it > 0L } ?: 190_000L
            if (playedAt >= yearAgo) {
                annualPlays++
                annualMs += dur
            }
            if (playedAt >= sevenDaysAgo) {
                val daysBack = ((nowMs - playedAt).coerceAtLeast(0L) / dayMillis).toInt().coerceIn(0, 6)
                val idx = 6 - daysBack
                val prev = weeklyBuckets[idx]
                weeklyBuckets[idx] = (prev.first + 1) to (prev.second + dur)
            }
            if (playedAt >= thirtyDaysAgo) {
                val weekBack = ((nowMs - playedAt).coerceAtLeast(0L) / (7 * dayMillis)).toInt().coerceIn(0, 3)
                val idx = 3 - weekBack
                val prev = monthWeeks[idx]
                monthWeeks[idx] = (prev.first + 1) to (prev.second + dur)
            }
            cal.timeInMillis = playedAt
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            when (hour) {
                in 0..5 -> periodCounts[0]++
                in 6..11 -> periodCounts[1]++
                in 12..17 -> periodCounts[2]++
                else -> periodCounts[3]++
            }
        }

        cal.timeInMillis = nowMs
        val todayDow = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0..6
        val weeklyStats = (0..6).map { idx ->
            val dow = (todayDow - (6 - idx) + 7) % 7
            val (plays, ms) = weeklyBuckets[idx]
            DayListeningStat(
                dayLabel = dayLabels[dow],
                plays = plays,
                estimatedMinutes = (ms / 60_000L).toInt()
            )
        }

        val monthlyStats = (0..3).map { idx ->
            val (plays, ms) = monthWeeks[idx]
            WeekListeningStat(
                weekLabel = "Sem ${idx + 1}",
                plays = plays,
                estimatedMinutes = (ms / 60_000L).toInt()
            )
        }

        val peakIdx = periodCounts.indices.maxByOrNull { periodCounts[it] } ?: 3
        val peakPeriod = when (peakIdx) {
            0 -> "Madrugada"
            1 -> "Manhã"
            2 -> "Tarde"
            else -> "Noite"
        }

        val topArtistName = topArtists.firstOrNull()?.label
        val topGenreName = topGenres.firstOrNull()?.label ?: "Eclético"
        val wrappedHeadline = when {
            topArtistName != null && totalPlays > 0 ->
                "$topArtistName dominou as tuas sessões com $totalPlays reproduções."
            audio.isNotEmpty() ->
                "Tens ${audio.size} músicas prontas para construir o teu Luma Wrapped."
            else ->
                "Importa as tuas músicas para desbloquear estatísticas personalizadas."
        }

        val sixtyDaysAgo = nowMs - 60 * dayMillis
        val fourteenDaysAgo = nowMs - 14 * dayMillis
        var thisMonthPlays = 0
        var lastMonthPlays = 0
        var thisMonthMs = 0L
        var lastMonthMs = 0L
        var thisWeekPlays = 0
        var lastWeekPlays = 0
        var thisWeekMs = 0L
        var lastWeekMs = 0L

        for (entry in history) {
            val playedAt = entry.history.playedAt
            val dur = entry.media?.durationMs?.takeIf { it > 0L } ?: 190_000L
            if (playedAt >= thirtyDaysAgo) {
                thisMonthPlays++
                thisMonthMs += dur
            } else if (playedAt >= sixtyDaysAgo) {
                lastMonthPlays++
                lastMonthMs += dur
            }
            if (playedAt >= sevenDaysAgo) {
                thisWeekPlays++
                thisWeekMs += dur
            } else if (playedAt >= fourteenDaysAgo) {
                lastWeekPlays++
                lastWeekMs += dur
            }
        }

        val completedCount = sessions.count { it.completedTrack }
        val earlySkippedCount = sessions.count { it.skippedEarly }
        val completionRate = if (sessions.isNotEmpty()) {
            ((completedCount * 100f) / sessions.size.toFloat()).roundToInt().coerceIn(0, 100)
        } else 100
        val earlySkipRate = if (sessions.isNotEmpty()) {
            ((earlySkippedCount * 100f) / sessions.size.toFloat()).roundToInt().coerceIn(0, 100)
        } else 0
        val topRoute = sessions
            .groupBy { it.outputRoute.ifBlank { "Speaker" } }
            .maxByOrNull { it.value.size }
            ?.key
            ?.let { token ->
                when (token.lowercase(Locale.ROOT)) {
                    "bluetooth" -> "Bluetooth"
                    "wired" -> "Auscultadores"
                    "usb" -> "USB Áudio"
                    "cast" -> "HDMI / Cast"
                    else -> "Altifalante"
                }
            } ?: "Altifalante"

        val monthDeltaPercent = computeDeltaPercent(thisMonthPlays, lastMonthPlays)
        val weekDeltaPercent = computeDeltaPercent(thisWeekPlays, lastWeekPlays)
        val monthlyMinutes = (thisMonthMs / 60_000L).toInt()
        val monthlyWrappedHeadline = if (thisMonthPlays > 0) {
            "Este mês ouviste $thisMonthPlays músicas (${monthlyMinutes} min), com pico em $peakPeriod."
        } else {
            wrappedHeadline
        }

        val totalMinutes = ((totalListeningTimeMs.coerceAtLeast(annualMs)) / 60_000L).toInt()
        return LumaInsightsReport(
            totalListeningTimeMs = totalListeningTimeMs.coerceAtLeast(annualMs),
            totalPlays = totalPlays,
            uniqueTracksPlayed = uniqueTracksPlayed,
            uniqueArtistsHeard = topArtists.size,
            topSongs = topSongs,
            topArtists = topArtists,
            topAlbums = topAlbums,
            topGenres = topGenres,
            weeklyStats = weeklyStats,
            monthlyStats = monthlyStats,
            annualTotalPlays = annualPlays.coerceAtLeast(totalPlays),
            annualMinutes = totalMinutes,
            wrappedHeadline = wrappedHeadline,
            wrappedMoodTag = topGenreName.uppercase(Locale.getDefault()),
            peakListeningPeriod = peakPeriod,
            comparison = ListeningComparisonStats(
                thisMonthPlays = thisMonthPlays,
                lastMonthPlays = lastMonthPlays,
                thisMonthMinutes = monthlyMinutes,
                lastMonthMinutes = (lastMonthMs / 60_000L).toInt(),
                monthDeltaPercent = monthDeltaPercent,
                thisWeekPlays = thisWeekPlays,
                lastWeekPlays = lastWeekPlays,
                thisWeekMinutes = (thisWeekMs / 60_000L).toInt(),
                lastWeekMinutes = (lastWeekMs / 60_000L).toInt(),
                weekDeltaPercent = weekDeltaPercent,
                completionRatePercent = completionRate,
                earlySkipRatePercent = earlySkipRate,
                topOutputRoute = topRoute
            ),
            monthlyWrappedHeadline = monthlyWrappedHeadline,
            monthlyMinutes = monthlyMinutes,
            monthlyTotalPlays = thisMonthPlays
        )
    }

    private fun computeDeltaPercent(current: Int, previous: Int): Int {
        return when {
            previous <= 0 && current > 0 -> 100
            previous <= 0 -> 0
            else -> (((current - previous).toFloat() / previous.toFloat()) * 100f).roundToInt()
        }
    }
}

object SmartPlaylistGenerator {

    fun generate(rule: SmartPlaylistRule, allMedia: List<MediaItemEntity>, limit: Int = 25): List<MediaItemEntity> {
        val audio = allMedia.filter { it.mediaType == MediaType.AUDIO && it.isAvailable }
        if (audio.isEmpty()) return emptyList()

        return when (rule) {
            SmartPlaylistRule.MOST_PLAYED ->
                audio.sortedWith(compareByDescending<MediaItemEntity> { it.playCount }.thenByDescending { it.isFavorite })
                    .take(limit)

            SmartPlaylistRule.RECENTLY_ADDED ->
                audio.sortedByDescending { it.addedAt }.take(limit)

            SmartPlaylistRule.HIDDEN_GEMS ->
                audio.filter { it.playCount <= 2 }
                    .sortedWith(compareBy<MediaItemEntity> { it.playCount }.thenByDescending { it.addedAt })
                    .ifEmpty { audio.sortedBy { it.playCount } }
                    .take(limit)

            SmartPlaylistRule.FAVORITES_MIX ->
                audio.filter { it.isFavorite }
                    .ifEmpty { audio.sortedByDescending { it.playCount } }
                    .take(limit)

            SmartPlaylistRule.ENERGETIC ->
                audio.filter {
                    val g = it.genre.lowercase()
                    g.contains("pop") || g.contains("rock") || g.contains("electr") ||
                        g.contains("hip") || g.contains("afro") || it.durationMs in 110_000L..235_000L
                }.ifEmpty { audio }.take(limit)

            SmartPlaylistRule.NIGHT_CHILL ->
                audio.filter {
                    val g = it.genre.lowercase()
                    g.contains("chill") || g.contains("jazz") || g.contains("acúst") ||
                        g.contains("acoustic") || g.contains("lo-fi") || g.contains("kizomba") ||
                        it.durationMs >= 205_000L
                }.ifEmpty { audio }.take(limit)
        }
    }

    fun personalizedRecommendations(allMedia: List<MediaItemEntity>, limit: Int = 12): List<MediaItemEntity> {
        val audio = allMedia.filter { it.mediaType == MediaType.AUDIO && it.isAvailable }
        if (audio.isEmpty()) return emptyList()

        val favArtistKeys = audio.filter { it.isFavorite || it.playCount >= 2 }
            .map { ArtistNormalizer.artistGroupingKey(it.artist, it.fileName) }
            .toSet()
        val favGenres = audio.filter { it.isFavorite || it.playCount >= 2 }
            .mapNotNull { it.genre.takeIf { g -> g.isNotBlank() }?.lowercase() }
            .toSet()

        return audio.sortedByDescending { track ->
            var score = 0
            val artistKey = ArtistNormalizer.artistGroupingKey(track.artist, track.fileName)
            if (artistKey in favArtistKeys) score += 25
            if (track.genre.lowercase() in favGenres) score += 20
            if (track.playCount == 0) score += 15 // Encourage discovery
            if (track.isFavorite) score += 10
            if (track.artworkPath != null) score += 5
            score
        }.take(limit)
    }

    /**
     * Picks compatible upcoming tracks for continuous Queue Radio playback without repeating
     * tracks currently in the queue.
     */
    fun pickRadioContinuation(
        seed: MediaItemEntity?,
        currentQueue: List<MediaItemEntity>,
        library: List<MediaItemEntity>,
        limit: Int = 5
    ): List<MediaItemEntity> {
        val audio = library.filter { it.mediaType == MediaType.AUDIO && it.isAvailable }
        if (audio.isEmpty()) return emptyList()

        val queuedIds = currentQueue.map { it.id }.toSet()
        val candidates = audio.filterNot { it.id in queuedIds }.ifEmpty {
            audio.filter { it.id != seed?.id }
        }
        if (candidates.isEmpty()) return emptyList()

        val seedArtistKey = seed?.let { ArtistNormalizer.artistGroupingKey(it.artist, it.fileName) }
        val seedGenre = seed?.genre?.trim()?.lowercase().orEmpty()
        val seedAlbum = seed?.album?.trim()?.lowercase().orEmpty()

        return candidates.sortedByDescending { candidate ->
            var score = 0
            val candArtistKey = ArtistNormalizer.artistGroupingKey(candidate.artist, candidate.fileName)
            if (seedArtistKey != null && candArtistKey == seedArtistKey) score += 28
            if (seedGenre.isNotBlank() && candidate.genre.trim().lowercase() == seedGenre) score += 24
            if (seedAlbum.isNotBlank() && seedAlbum != "álbum desconhecido" &&
                candidate.album.trim().lowercase() == seedAlbum
            ) score += 14
            if (candidate.isFavorite) score += 8
            if (candidate.playCount <= 2) score += 6
            score + (candidate.id % 5).toInt()
        }.take(limit)
    }
}
