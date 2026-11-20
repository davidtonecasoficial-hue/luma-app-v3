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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min

data class ExtractedArtworkColors(
    val primary: Color,
    val secondary: Color
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
            val bmp = decodeSampledBitmap(context, artworkPath) ?: return@withContext null
            val colors = computeDominantColors(bmp)
            bmp.recycle()
            if (colors != null) {
                cache.put(artworkPath, colors)
            }
            colors
        }
    }

    private fun decodeSampledBitmap(context: Context, path: String): Bitmap? {
        return runCatching {
            val opts = BitmapFactory.Options().apply {
                inSampleSize = 8
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            if (path.startsWith("content://") || path.startsWith("file://")) {
                context.contentResolver.openInputStream(Uri.parse(path))?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, opts)
                }
            } else {
                val f = File(path)
                if (f.exists() && f.length() > 0L) BitmapFactory.decodeFile(f.absolutePath, opts) else null
            }
        }.getOrNull()
    }

    private fun computeDominantColors(bitmap: Bitmap): ExtractedArtworkColors? {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return null

        val bucketCounts = HashMap<Int, Int>()
        val bucketRgbSum = HashMap<Int, Triple<Long, Long, Long>>()

        val stepX = max(1, w / 24)
        val stepY = max(1, h / 24)
        for (y in 0 until h step stepY) {
            for (x in 0 until w step stepX) {
                val px = bitmap.getPixel(x, y)
                val r = (px shr 16) and 0xFF
                val g = (px shr 8) and 0xFF
                val b = px and 0xFF

                val maxC = max(r, max(g, b))
                val minC = min(r, min(g, b))
                // Skip near-black, near-white, and flat gray pixels when colorful ones exist
                val sat = if (maxC == 0) 0f else (maxC - minC).toFloat() / maxC.toFloat()
                if (maxC < 28 || (minC > 228 && sat < 0.12f)) continue

                val weight = if (sat > 0.25f) 3 else 1
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

        if (bucketCounts.isEmpty()) return null

        val sortedKeys = bucketCounts.entries.sortedByDescending { it.value }.map { it.key }
        fun avgColor(key: Int): Color {
            val sum = bucketRgbSum[key]!!
            val count = bucketCounts[key]!!.toFloat().coerceAtLeast(1f)
            return Color(
                red = (sum.first / count / 255f).coerceIn(0f, 1f),
                green = (sum.second / count / 255f).coerceIn(0f, 1f),
                blue = (sum.third / count / 255f).coerceIn(0f, 1f),
                alpha = 1f
            )
        }

        val primary = avgColor(sortedKeys.first())
        val secondary = if (sortedKeys.size > 1) avgColor(sortedKeys[1]) else primary
        return ExtractedArtworkColors(primary = primary, secondary = secondary)
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
