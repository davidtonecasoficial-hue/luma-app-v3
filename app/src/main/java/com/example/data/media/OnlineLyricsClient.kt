package com.example.data.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class OnlineLyricsCandidate(
    val id: String,
    val trackName: String,
    val artistName: String,
    val albumName: String,
    val syncedLyrics: String?,
    val plainLyrics: String?,
    val source: String
) {
    val bestLyricsText: String
        get() = syncedLyrics?.takeIf { it.isNotBlank() } ?: plainLyrics.orEmpty()

    val isSynced: Boolean
        get() = !syncedLyrics.isNullOrBlank()
}

data class OnlineLyricsSearchResponse(
    val results: List<OnlineLyricsCandidate> = emptyList(),
    val errorMessage: String? = null
)

/**
 * Pure Kotlin HTTP client using Android platform HttpURLConnection + org.json
 * to look up synchronized (.lrc) and plain lyrics from public lyrics databases
 * (LRCLIB primary, Lyrics.ovh fallback) without needing API keys or heavy dependencies.
 */
object OnlineLyricsClient {

    private const val CONNECT_TIMEOUT_MS = 7000
    private const val READ_TIMEOUT_MS = 9000
    private const val USER_AGENT = "LumaPlayer/3.0 (Android Music Player)"

    suspend fun searchLyrics(
        rawTitle: String,
        rawArtist: String,
        album: String = "",
        durationMs: Long = 0L
    ): OnlineLyricsSearchResponse = withContext(Dispatchers.IO) {
        val cleanTitle = cleanQueryToken(rawTitle)
        val cleanArtist = ArtistNormalizer.normalizePrimaryArtist(rawArtist).let {
            if (it == "Artista desconhecido") "" else cleanQueryToken(it)
        }
        if (cleanTitle.isBlank() && cleanArtist.isBlank()) {
            return@withContext OnlineLyricsSearchResponse(errorMessage = "Indique o nome da música ou artista para pesquisar.")
        }

        val collected = mutableListOf<OnlineLyricsCandidate>()
        var networkError: String? = null

        // 1. Exact match via LRCLIB /api/get when artist & title are available
        if (cleanTitle.isNotBlank() && cleanArtist.isNotBlank()) {
            val encodedArtist = urlEncode(cleanArtist)
            val encodedTitle = urlEncode(cleanTitle)
            val durationParam = if (durationMs > 10_000L) "&duration=${durationMs / 1000L}" else ""
            val albumParam = if (album.isNotBlank() && !album.equals("Álbum desconhecido", true)) {
                "&album_name=${urlEncode(cleanQueryToken(album))}"
            } else ""
            val url = "https://lrclib.net/api/get?artist_name=$encodedArtist&track_name=$encodedTitle$albumParam$durationParam"
            val getResult = httpGet(url)
            getResult.onSuccess { body ->
                parseLrcLibSingleJson(body)?.let { collected.add(it) }
            }.onFailure { err ->
                if (err is java.io.IOException && err !is java.io.FileNotFoundException) {
                    networkError = err.message
                }
            }
        }

        // 2. Broader LRCLIB /api/search query
        val searchUrl = buildString {
            append("https://lrclib.net/api/search?")
            if (cleanTitle.isNotBlank() && cleanArtist.isNotBlank()) {
                append("track_name=").append(urlEncode(cleanTitle))
                append("&artist_name=").append(urlEncode(cleanArtist))
            } else {
                append("q=").append(urlEncode("$cleanArtist $cleanTitle".trim()))
            }
        }
        httpGet(searchUrl).onSuccess { body ->
            val list = parseLrcLibSearchJson(body)
            list.forEach { candidate ->
                val dup = collected.any {
                    it.trackName.equals(candidate.trackName, true) &&
                        it.artistName.equals(candidate.artistName, true) &&
                        it.isSynced == candidate.isSynced
                }
                if (!dup) collected.add(candidate)
            }
        }.onFailure { err ->
            if (networkError == null && err is java.io.IOException && err !is java.io.FileNotFoundException) {
                networkError = err.message
            }
        }

        // 3. Also run a free-text q= search on LRCLIB if nothing synced was found yet
        if (collected.none { it.isSynced } && cleanTitle.isNotBlank()) {
            val fallbackQ = urlEncode(listOf(cleanArtist, cleanTitle).filter { it.isNotBlank() }.joinToString(" "))
            httpGet("https://lrclib.net/api/search?q=$fallbackQ").onSuccess { body ->
                parseLrcLibSearchJson(body).forEach { candidate ->
                    val dup = collected.any {
                        it.trackName.equals(candidate.trackName, true) &&
                            it.artistName.equals(candidate.artistName, true) &&
                            it.isSynced == candidate.isSynced
                    }
                    if (!dup) collected.add(candidate)
                }
            }
        }

        // 4. Fallback to Lyrics.ovh if LRCLIB returned nothing
        if (collected.isEmpty() && cleanTitle.isNotBlank() && cleanArtist.isNotBlank()) {
            val ovhUrl = "https://api.lyrics.ovh/v1/${urlPathEncode(cleanArtist)}/${urlPathEncode(cleanTitle)}"
            httpGet(ovhUrl).onSuccess { body ->
                parseLyricsOvhJson(body, trackName = cleanTitle, artistName = cleanArtist)?.let {
                    collected.add(it)
                }
            }.onFailure { err ->
                if (networkError == null && err is java.io.IOException && err !is java.io.FileNotFoundException) {
                    networkError = err.message
                }
            }
        }

        val sorted = collected
            .filter { it.bestLyricsText.isNotBlank() }
            .sortedByDescending { it.isSynced }
            .take(10)

        if (sorted.isEmpty()) {
            val msg = if (networkError != null) {
                "Sem ligação ou servidor indisponível. Verifique a internet e tente novamente."
            } else {
                "Nenhuma letra encontrada para \"$cleanTitle\"."
            }
            OnlineLyricsSearchResponse(results = emptyList(), errorMessage = msg)
        } else {
            OnlineLyricsSearchResponse(results = sorted, errorMessage = null)
        }
    }

    internal fun cleanQueryToken(raw: String): String {
        return raw
            .replace(
                Regex(
                    """\s*[\(\[][^\(\)\[\]]*(?:official|music\s+video|video|audio|lyric|lyrics|visualizer|hd|hq|4k|remaster(?:ed)?|feat\.?|ft\.?)[^\(\)\[\]]*[\)\]]""",
                    RegexOption.IGNORE_CASE
                ),
                ""
            )
            .replace(Regex("""\.(mp3|m4a|flac|wav|ogg|aac|opus)$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    fun parseLrcLibSingleJson(rawJson: String): OnlineLyricsCandidate? = runCatching {
        val obj = JSONObject(rawJson)
        val synced = if (obj.isNull("syncedLyrics")) null else obj.optString("syncedLyrics", "").takeIf { it.isNotBlank() && it != "null" }
        val plain = if (obj.isNull("plainLyrics")) null else obj.optString("plainLyrics", "").takeIf { it.isNotBlank() && it != "null" }
        if (synced.isNullOrBlank() && plain.isNullOrBlank()) return@runCatching null
        OnlineLyricsCandidate(
            id = "lrclib_${obj.optLong("id", rawJson.hashCode().toLong())}",
            trackName = obj.optString("trackName", obj.optString("name", "")).ifBlank { "Desconhecida" },
            artistName = obj.optString("artistName", "").ifBlank { "Desconhecido" },
            albumName = obj.optString("albumName", ""),
            syncedLyrics = synced,
            plainLyrics = plain,
            source = "LRCLIB"
        )
    }.getOrNull()

    fun parseLrcLibSearchJson(rawJson: String): List<OnlineLyricsCandidate> = runCatching {
        val arr = JSONArray(rawJson)
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val synced = if (obj.isNull("syncedLyrics")) null else obj.optString("syncedLyrics", "").takeIf { it.isNotBlank() && it != "null" }
                val plain = if (obj.isNull("plainLyrics")) null else obj.optString("plainLyrics", "").takeIf { it.isNotBlank() && it != "null" }
                if (synced.isNullOrBlank() && plain.isNullOrBlank()) continue
                add(
                    OnlineLyricsCandidate(
                        id = "lrclib_${obj.optLong("id", i.toLong())}",
                        trackName = obj.optString("trackName", obj.optString("name", "")).ifBlank { "Desconhecida" },
                        artistName = obj.optString("artistName", "").ifBlank { "Desconhecido" },
                        albumName = obj.optString("albumName", ""),
                        syncedLyrics = synced,
                        plainLyrics = plain,
                        source = "LRCLIB"
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    fun parseLyricsOvhJson(
        rawJson: String,
        trackName: String,
        artistName: String
    ): OnlineLyricsCandidate? = runCatching {
        val obj = JSONObject(rawJson)
        val text = obj.optString("lyrics", "").trim()
        if (text.isBlank()) return@runCatching null
        OnlineLyricsCandidate(
            id = "ovh_${artistName.hashCode()}_${trackName.hashCode()}",
            trackName = trackName,
            artistName = artistName,
            albumName = "",
            syncedLyrics = null,
            plainLyrics = text,
            source = "Lyrics.ovh"
        )
    }.getOrNull()

    private fun httpGet(urlString: String): Result<String> = runCatching {
        val url = URL(urlString)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                if (code == 404) throw java.io.FileNotFoundException("HTTP 404")
                throw java.io.IOException("HTTP $code")
            }
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun urlPathEncode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
