package com.example.data.media

import java.util.Locale

data class LyricLine(
    val timeMs: Long?,
    val text: String,
    val translatedText: String? = null
)

data class ParsedLyrics(
    val isTimed: Boolean,
    val lines: List<LyricLine>,
    val hasTranslations: Boolean = false
)

object LrcParser {
    private val TIMESTAMP_REGEX = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val META_TAG_REGEX = Regex("""^\[(ar|ti|al|au|by|offset|length|re|ve):.*]$""", RegexOption.IGNORE_CASE)

    fun parse(raw: String?): ParsedLyrics {
        if (raw.isNullOrBlank()) {
            return ParsedLyrics(isTimed = false, lines = emptyList(), hasTranslations = false)
        }

        val timedLines = mutableListOf<LyricLine>()
        val plainLines = mutableListOf<LyricLine>()

        raw.lineSequence().forEach { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) return@forEach
            if (META_TAG_REGEX.matches(trimmed)) return@forEach

            val matches = TIMESTAMP_REGEX.findAll(trimmed).toList()
            if (matches.isNotEmpty()) {
                val lyricRaw = TIMESTAMP_REGEX.replace(trimmed, "").trim()
                if (lyricRaw.isNotEmpty()) {
                    val (primary, translation) = splitBilingualText(lyricRaw)
                    matches.forEach { match ->
                        val min = match.groupValues[1].toLongOrNull() ?: 0L
                        val sec = match.groupValues[2].toLongOrNull() ?: 0L
                        val fracRaw = match.groupValues.getOrNull(3).orEmpty()
                        val millis = when (fracRaw.length) {
                            1 -> (fracRaw.toLongOrNull() ?: 0L) * 100L
                            2 -> (fracRaw.toLongOrNull() ?: 0L) * 10L
                            3 -> fracRaw.toLongOrNull() ?: 0L
                            else -> 0L
                        }
                        val timeMs = min * 60_000L + sec * 1_000L + millis
                        timedLines.add(LyricLine(timeMs = timeMs, text = primary, translatedText = translation))
                    }
                }
            } else {
                val (primary, translation) = splitBilingualText(trimmed)
                plainLines.add(LyricLine(timeMs = null, text = primary, translatedText = translation))
            }
        }

        // Collapse adjacent identical-timestamp lines into a single bilingual line if detected
        val finalLines = if (timedLines.isNotEmpty()) {
            val sorted = timedLines.sortedBy { it.timeMs ?: 0L }
            val merged = mutableListOf<LyricLine>()
            for (line in sorted) {
                val last = merged.lastOrNull()
                if (last != null && last.timeMs == line.timeMs && last.translatedText == null && line.translatedText == null) {
                    merged[merged.lastIndex] = last.copy(translatedText = line.text)
                } else {
                    merged.add(line)
                }
            }
            merged
        } else {
            plainLines
        }

        val hasTranslations = finalLines.any { !it.translatedText.isNullOrBlank() }
        return ParsedLyrics(
            isTimed = timedLines.isNotEmpty(),
            lines = finalLines,
            hasTranslations = hasTranslations
        )
    }

    private fun splitBilingualText(raw: String): Pair<String, String?> {
        val sep = when {
            " | " in raw -> " | "
            " // " in raw -> " // "
            else -> null
        } ?: return raw to null
        val parts = raw.split(sep, limit = 2)
        return parts[0].trim() to parts.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun findActiveLineIndex(
        lines: List<LyricLine>,
        positionMs: Long,
        offsetMs: Long = 0L
    ): Int {
        if (lines.isEmpty()) return -1
        val adjustedPos = positionMs + offsetMs
        var active = -1
        for (i in lines.indices) {
            val t = lines[i].timeMs ?: continue
            if (t <= adjustedPos + 150L) {
                active = i
            } else {
                break
            }
        }
        return active
    }

    fun formatTimestamp(timeMs: Long): String {
        val clamped = timeMs.coerceAtLeast(0L)
        val totalSec = clamped / 1000L
        val min = totalSec / 60L
        val sec = totalSec % 60L
        val centis = (clamped % 1000L) / 10L
        return String.format(Locale.US, "[%02d:%02d.%02d]", min, sec, centis)
    }

    fun serializeLrc(lines: List<LyricLine>): String {
        return lines.joinToString("\n") { line ->
            val content = if (!line.translatedText.isNullOrBlank()) {
                "${line.text} | ${line.translatedText}"
            } else {
                line.text
            }
            val stamp = line.timeMs?.let { formatTimestamp(it) }
            if (stamp != null) "$stamp$content" else content
        }
    }
}
