package com.example.data.media

import com.example.data.model.MediaItemEntity
import com.example.data.model.MediaType
import java.text.Normalizer

data class ParsedFilenameTags(
    val trackNumber: Int = 0,
    val artist: String = "",
    val title: String = "",
    val releaseYear: Int = 0
)

/**
 * Scans, cleans, and unifies singer/artist names and infers missing metadata
 * so songs from the same singer (e.g., "Ed Sheeran", "Ed Sheeran feat. Khalid",
 * "ed sheeran", "Ed  Sheeran (Live)") automatically group together cleanly.
 */
object ArtistNormalizer {

    private val UNKNOWN_MARKERS = setOf(
        "", "<unknown>", "unknown", "unknown artist", "artista desconhecido",
        "various artists", "vários artistas", "none", "null", "n/a"
    )

    private val FEATURE_REGEX = Regex(
        """(?i)\s*(?:\(|\[)?\b(?:feat\.?|ft\.?|featuring|with|com|prod\.?\s*by|x)\b.*"""
    )
    private val PAREN_CREDITS_REGEX = Regex(
        """(?i)\s*[\(\[](?:feat\.?|ft\.?|prod\.?|remix|live|ao vivo|official|audio|hd|hq).*?[\)\]]"""
    )
    private val MULTI_ARTIST_SPLIT_REGEX = Regex("""\s*(?:,|;|/|&amp;|&|\band\b|\be\b)\s*""", RegexOption.IGNORE_CASE)
    private val WHITESPACE_REGEX = Regex("""\s+""")
    private val DIACRITICS_REGEX = Regex("""\p{InCombiningDiacriticalMarks}+""")
    private val NON_ALNUM_REGEX = Regex("""[^a-z0-9\s]""")

    private val FILE_TRACK_ARTIST_TITLE_REGEX = Regex("""^(\d{1,3})\s*[-._)]\s*(.+?)\s+-\s+(.+)$""")
    private val FILE_ARTIST_TITLE_REGEX = Regex("""^(.+?)\s+-\s+(.+)$""")
    private val FILE_YEAR_SUFFIX_REGEX = Regex("""[\(\[]?(19\d{2}|20\d{2})[\)\]]?$""")

    fun cleanDisplayTag(raw: String?): String {
        val trimmed = raw?.replace(WHITESPACE_REGEX, " ")?.trim().orEmpty()
        return if (trimmed.lowercase() in UNKNOWN_MARKERS) "" else trimmed
    }

    /**
     * Extracts the primary singer/artist name for grouping while respecting duo names
     * if they contain no featured-artist delimiter or when comma/slash separated.
     */
    fun normalizePrimaryArtist(rawArtist: String?, fallbackFileName: String = ""): String {
        var cleaned = cleanDisplayTag(rawArtist)
        if (cleaned.isEmpty() && fallbackFileName.isNotBlank()) {
            cleaned = parseFilenameMetadata(fallbackFileName).artist
        }
        if (cleaned.isEmpty()) return "Artista desconhecido"

        cleaned = cleaned.replace(PAREN_CREDITS_REGEX, " ").trim()
        cleaned = cleaned.replace(FEATURE_REGEX, "").trim()

        // Split multi-artist lists (",", ";", "/", " & ") keeping the primary singer.
        // Preserve canonical ampersand bands only if short and without commas (e.g., "Simon & Garfunkel" stays whole if <= 2 words around &? Wait: better split on , ; / and feature tags; split & only when both sides look like separate credited acts or >1 delimiter).
        val primaryPart = cleaned
            .split(Regex("""\s*(?:,|;|/)\s*"""))
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            ?: cleaned

        val finalPart = if (primaryPart.contains(" & ", ignoreCase = true) || primaryPart.contains(" x ", ignoreCase = true)) {
            val parts = primaryPart.split(Regex("""\s+(?:&|x)\s+""", RegexOption.IGNORE_CASE))
            if (parts.size >= 2 && parts[0].trim().length >= 3 && parts[1].trim().length >= 3) {
                // Unify collaborations like "Ed Sheeran & Justin Bieber" under the lead singer
                parts[0].trim()
            } else {
                primaryPart
            }
        } else {
            primaryPart
        }

        val normalizedSpaces = finalPart.replace(WHITESPACE_REGEX, " ").trim()
        if (normalizedSpaces.lowercase() in UNKNOWN_MARKERS) return "Artista desconhecido"
        return toTitleCaseIfNeeded(normalizedSpaces)
    }

    /**
     * Canonical grouping key: lowercase, accents removed, punctuation stripped, whitespace collapsed.
     */
    fun artistGroupingKey(rawArtist: String?, fallbackFileName: String = ""): String {
        val primary = normalizePrimaryArtist(rawArtist, fallbackFileName)
        if (primary == "Artista desconhecido") return "unknown_artist"
        val decomposed = Normalizer.normalize(primary.lowercase(), Normalizer.Form.NFD)
        val noDiacritics = DIACRITICS_REGEX.replace(decomposed, "")
        val alnum = NON_ALNUM_REGEX.replace(noDiacritics, " ")
        return WHITESPACE_REGEX.replace(alnum, " ").trim().ifBlank { "unknown_artist" }
    }

    /**
     * Picks the best human-readable artist label among tracks that share the same grouping key.
     */
    fun pickBestDisplayName(tracks: List<MediaItemEntity>): String {
        if (tracks.isEmpty()) return "Artista desconhecido"
        val candidates = tracks.map { normalizePrimaryArtist(it.albumArtist.ifBlank { it.artist }, it.fileName) }
            .filter { it != "Artista desconhecido" }
        if (candidates.isEmpty()) return "Artista desconhecido"
        return candidates
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.length })
            .first()
            .key
    }

    fun parseFilenameMetadata(fileName: String): ParsedFilenameTags {
        var base = fileName.substringBeforeLast('.', fileName)
            .replace('_', ' ')
            .replace(WHITESPACE_REGEX, " ")
            .trim()

        var year = 0
        val yearMatch = FILE_YEAR_SUFFIX_REGEX.find(base)
        if (yearMatch != null) {
            year = yearMatch.groupValues[1].toIntOrNull() ?: 0
            base = base.removeRange(yearMatch.range).trim().trimEnd('-', ' ', '(')
        }

        val trackArtistTitle = FILE_TRACK_ARTIST_TITLE_REGEX.find(base)
        if (trackArtistTitle != null) {
            val trackNo = trackArtistTitle.groupValues[1].toIntOrNull() ?: 0
            val artist = trackArtistTitle.groupValues[2].trim()
            val title = trackArtistTitle.groupValues[3].trim()
            return ParsedFilenameTags(
                trackNumber = trackNo,
                artist = cleanDisplayTag(artist),
                title = title,
                releaseYear = year
            )
        }

        val artistTitle = FILE_ARTIST_TITLE_REGEX.find(base)
        if (artistTitle != null) {
            val left = artistTitle.groupValues[1].trim()
            val right = artistTitle.groupValues[2].trim()
            val leftAsNum = left.toIntOrNull()
            return if (leftAsNum != null && leftAsNum in 1..199) {
                ParsedFilenameTags(trackNumber = leftAsNum, title = right, releaseYear = year)
            } else {
                ParsedFilenameTags(artist = cleanDisplayTag(left), title = right, releaseYear = year)
            }
        }

        return ParsedFilenameTags(title = base, releaseYear = year)
    }

    fun inferGenre(
        title: String,
        artist: String,
        album: String,
        folderName: String,
        durationMs: Long = 0L
    ): String {
        val haystack = "$title $artist $album $folderName".lowercase()
        return when {
            haystack.containsAny("kizomba", "semba", "zouk", "tarraxinha") -> "Kizomba"
            haystack.containsAny("afrobeat", "afropop", "amapiano", "kuduro") -> "Afrobeats"
            haystack.containsAny("lo-fi", "lofi", "chillhop", "beats") -> "Lo-Fi"
            haystack.containsAny("jazz", "sax", "blues", "bossa") -> "Jazz & Blues"
            haystack.containsAny("rock", "metal", "punk", "grunge", "indie") -> "Rock"
            haystack.containsAny("hip hop", "hip-hop", "rap", "trap", "drill", "freestyle") -> "Hip-Hop"
            haystack.containsAny("edm", "house", "techno", "trance", "dubstep", "remix", "electro") -> "Eletrónica"
            haystack.containsAny("classic", "symphony", "piano", "violin", "orchestra", "sonata") -> "Clássica"
            haystack.containsAny("gospel", "worship", "louvor") -> "Gospel"
            haystack.containsAny("samba", "pagode", "sertanejo", "mpb", "fado", "latin", "reggaeton") -> "Latina & Lusófona"
            haystack.containsAny("acoustic", "acústico", "unplugged", "ambient") -> "Acústico & Ambient"
            durationMs in 1..220_000L -> "Pop"
            else -> "Geral"
        }
    }

    fun enrichEntityMetadata(item: MediaItemEntity): MediaItemEntity {
        if (item.mediaType != MediaType.AUDIO) return item
        val parsed = parseFilenameMetadata(item.fileName)
        val cleanTitle = cleanDisplayTag(item.title).ifBlank { parsed.title.ifBlank { item.title } }
        val rawOrParsedArtist = cleanDisplayTag(item.artist).ifBlank { parsed.artist }
        val normalizedArtist = if (rawOrParsedArtist.isNotBlank()) {
            normalizePrimaryArtist(rawOrParsedArtist, item.fileName)
        } else {
            "Artista desconhecido"
        }
        val cleanAlbum = cleanDisplayTag(item.album).ifBlank {
            item.folderName.takeIf {
                it.isNotBlank() && !it.equals("Music", true) && !it.equals("Download", true) && !it.equals("Importados", true)
            } ?: "Álbum desconhecido"
        }
        val cleanGenre = cleanDisplayTag(item.genre).ifBlank {
            inferGenre(cleanTitle, normalizedArtist, cleanAlbum, item.folderName, item.durationMs)
        }
        val year = if (item.releaseYear > 0) item.releaseYear else parsed.releaseYear
        val trackNo = if (item.trackNumber > 0) item.trackNumber else parsed.trackNumber
        return item.copy(
            title = cleanTitle,
            artist = normalizedArtist,
            album = cleanAlbum,
            genre = cleanGenre,
            releaseYear = year,
            trackNumber = trackNo
        )
    }

    private fun String.containsAny(vararg needles: String): Boolean = needles.any { this.contains(it) }

    private fun toTitleCaseIfNeeded(text: String): String {
        val hasMixedCase = text.any { it.isUpperCase() } && text.any { it.isLowerCase() }
        if (hasMixedCase) return text
        val isAllUpper = text.any { it.isLetter() } && text == text.uppercase()
        if (isAllUpper && text.length <= 5) return text
        return text.split(' ').joinToString(" ") { word ->
            if (word.isEmpty()) word
            else word.lowercase().replaceFirstChar { ch -> ch.titlecase() }
        }
    }
}
