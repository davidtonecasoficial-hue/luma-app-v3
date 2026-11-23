package com.example.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.config.LumaCrashGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

data class ExtractedArtworkColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color = secondary
)

/**
 * Real offline bitmap color extractor using Dispatchers.IO downsampling + LruCache.
 */
object ArtworkPaletteExtractor {

    private val cache = LruCache<String, ExtractedArtworkColors>(48)

    suspend fun extract(context: Context, artworkPath: String?): ExtractedArtworkColors? {
        if (artworkPath.isNullOrBlank()) return null
        cache.get(artworkPath)?.let { return it }

        return withContext(Dispatchers.IO) {
            val bmp = LumaCrashGuard.decodeSampledBitmap(context, artworkPath, maxDimensionPx = 128)
                ?: return@withContext null
            val colors = try {
                runCatching { computeDominantColors(bmp) }.getOrNull()
            } finally {
                runCatching { if (!bmp.isRecycled) bmp.recycle() }
            }
            if (colors != null) {
                cache.put(artworkPath, colors)
            }
            colors
        }
    }

    private fun computeDominantColors(bitmap: Bitmap): ExtractedArtworkColors? {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return null

        val bucketCounts = HashMap<Int, Int>()
        val bucketRgbSum = HashMap<Int, Triple<Long, Long, Long>>()
        var fallbackR = 0L
        var fallbackG = 0L
        var fallbackB = 0L
        var fallbackCount = 0

        val stepX = max(1, w / 28)
        val stepY = max(1, h / 28)
        for (y in 0 until h step stepY) {
            for (x in 0 until w step stepX) {
                val px = bitmap.getPixel(x, y)
                val r = (px shr 16) and 0xFF
                val g = (px shr 8) and 0xFF
                val b = px and 0xFF

                fallbackR += r
                fallbackG += g
                fallbackB += b
                fallbackCount++

                val maxC = max(r, max(g, b))
                val minC = min(r, min(g, b))
                val sat = if (maxC == 0) 0f else (maxC - minC).toFloat() / maxC.toFloat()
                if (maxC < 22 || (minC > 234 && sat < 0.10f)) continue

                val weight = if (sat > 0.35f) 4 else if (sat > 0.16f) 2 else 1
                val key = ((r shr 5) shl 6) or ((g shr 5) shl 3) or (b shr 5)
                bucketCounts[key] = (bucketCounts[key] ?: 0) + weight
                val prev = bucketRgbSum[key] ?: Triple(0L, 0L, 0L)
                bucketRgbSum[key] = Triple(
                    prev.first + r * weight,
                    prev.second + g * weight,
                    prev.third + b * weight
                )
            }
        }

        if (bucketCounts.isEmpty()) {
            if (fallbackCount == 0) return null
            val avg = Color(
                red = (fallbackR.toFloat() / fallbackCount / 255f).coerceIn(0f, 1f),
                green = (fallbackG.toFloat() / fallbackCount / 255f).coerceIn(0f, 1f),
                blue = (fallbackB.toFloat() / fallbackCount / 255f).coerceIn(0f, 1f),
                alpha = 1f
            )
            return ExtractedArtworkColors(primary = avg, secondary = avg, tertiary = avg)
        }

        val sortedKeys = bucketCounts.entries.sortedByDescending { it.value }.map { it.key }
        fun avgColor(key: Int): Color? {
            val sum = bucketRgbSum[key] ?: return null
            val count = (bucketCounts[key] ?: 1).toFloat().coerceAtLeast(1f)
            return Color(
                red = (sum.first / count / 255f).coerceIn(0f, 1f),
                green = (sum.second / count / 255f).coerceIn(0f, 1f),
                blue = (sum.third / count / 255f).coerceIn(0f, 1f),
                alpha = 1f
            )
        }

        fun colorDistSq(a: Color, b: Color): Float {
            val dr = a.red - b.red
            val dg = a.green - b.green
            val db = a.blue - b.blue
            return dr * dr + dg * dg + db * db
        }

        val candidates = sortedKeys.take(16).mapNotNull { avgColor(it) }
        if (candidates.isEmpty()) return null
        val primary = candidates.first()
        val secondary = candidates.drop(1).firstOrNull { colorDistSq(it, primary) > 0.045f }
            ?: candidates.getOrElse(1) { primary }
        val tertiary = candidates.drop(1).firstOrNull {
            colorDistSq(it, primary) > 0.03f && colorDistSq(it, secondary) > 0.03f
        } ?: candidates.getOrElse(2) {
            Color(
                red = ((primary.red * 0.55f + secondary.red * 0.45f) + 0.08f).coerceIn(0f, 1f),
                green = ((primary.green * 0.55f + secondary.green * 0.45f) + 0.08f).coerceIn(0f, 1f),
                blue = ((primary.blue * 0.55f + secondary.blue * 0.45f) + 0.08f).coerceIn(0f, 1f),
                alpha = 1f
            )
        }
        return ExtractedArtworkColors(primary = primary, secondary = secondary, tertiary = tertiary)
    }
}

@Composable
fun rememberDynamicArtworkColors(
    artworkPath: String?,
    enabled: Boolean,
    fallbackPrimary: Color,
    fallbackSecondary: Color
): ExtractedArtworkColors {
    val context = LocalContext.current
    var extracted by remember(artworkPath, enabled) { mutableStateOf<ExtractedArtworkColors?>(null) }

    LaunchedEffect(artworkPath, enabled) {
        extracted = if (enabled && !artworkPath.isNullOrBlank()) {
            ArtworkPaletteExtractor.extract(context, artworkPath)
        } else {
            null
        }
    }

    return extracted ?: ExtractedArtworkColors(fallbackPrimary, fallbackSecondary)
}
