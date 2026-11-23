package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.example.config.LumaCrashGuard
import com.example.data.model.MediaItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Generates high-resolution Liquid Glass shareable story cards (1080x1350 PNG)
 * combining cover art, harmonized gradient palette, optional lyric quote, and Luma branding.
 */
object StoryCardExporter {

    suspend fun loadTrackArtworkBitmap(context: Context, artworkPath: String?): Bitmap? = withContext(Dispatchers.IO) {
        LumaCrashGuard.decodeSampledBitmap(context, artworkPath, maxDimensionPx = 1080)
    }

    fun renderStoryCardBitmap(
        track: MediaItemEntity,
        artworkBitmap: Bitmap?,
        quoteLine: String?,
        accentPrimary: Color,
        accentSecondary: Color,
        isDark: Boolean
    ): Bitmap {
        val width = 1080
        val height = 1350
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val bgTop = if (isDark) android.graphics.Color.rgb(16, 20, 34) else android.graphics.Color.rgb(244, 247, 252)
        val bgBottom = if (isDark) android.graphics.Color.rgb(10, 12, 22) else android.graphics.Color.rgb(230, 236, 246)
        val accentA = accentPrimary.copy(alpha = if (isDark) 0.42f else 0.24f).toArgb()
        val accentB = accentSecondary.copy(alpha = if (isDark) 0.32f else 0.18f).toArgb()

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                width.toFloat(),
                height.toFloat(),
                intArrayOf(bgTop, accentA, bgBottom),
                floatArrayOf(0f, 0.48f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Subtle luminous orb highlight behind artwork
        val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentB }
        canvas.drawCircle(width * 0.78f, height * 0.24f, 260f, orbPaint)

        // Glass card container
        val cardRect = RectF(76f, 88f, width - 76f, height - 108f)
        val cardFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) android.graphics.Color.argb(185, 24, 30, 48) else android.graphics.Color.argb(226, 255, 255, 255)
        }
        canvas.drawRoundRect(cardRect, 56f, 56f, cardFill)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = if (isDark) android.graphics.Color.argb(72, 255, 255, 255) else android.graphics.Color.argb(180, 255, 255, 255)
        }
        canvas.drawRoundRect(cardRect, 56f, 56f, borderPaint)

        // Cover art frame
        val artLeft = 148f
        val artTop = 160f
        val artSize = width - 296f
        val artRect = RectF(artLeft, artTop, artLeft + artSize, artTop + artSize)
        if (artworkBitmap != null && !artworkBitmap.isRecycled) {
            val saveCount = canvas.save()
            val clipPath = android.graphics.Path().apply {
                addRoundRect(artRect, 40f, 40f, android.graphics.Path.Direction.CW)
            }
            canvas.clipPath(clipPath)
            canvas.drawBitmap(artworkBitmap, null, artRect, Paint(Paint.FILTER_BITMAP_FLAG))
            canvas.restoreToCount(saveCount)
        } else {
            val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    artLeft,
                    artTop,
                    artLeft + artSize,
                    artTop + artSize,
                    accentPrimary.toArgb(),
                    accentSecondary.toArgb(),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(artRect, 40f, 40f, placeholderPaint)
        }

        val textPrimary = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.rgb(20, 24, 36)
        val textSecondary = if (isDark) android.graphics.Color.argb(190, 224, 232, 245) else android.graphics.Color.rgb(90, 102, 124)

        // Title
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimary
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val titleLayout = StaticLayout.Builder.obtain(
            track.title,
            0,
            track.title.length,
            titlePaint,
            (width - 296).coerceAtLeast(200)
        ).setMaxLines(2).build()
        canvas.save()
        canvas.translate(148f, artTop + artSize + 44f)
        titleLayout.draw(canvas)
        canvas.restore()

        val afterTitleY = artTop + artSize + 44f + titleLayout.height + 12f
        val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSecondary
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(track.artist, 148f, afterTitleY + 26f, artistPaint)

        // Optional selected lyric quote
        val trimmedQuote = quoteLine?.trim().orEmpty()
        if (trimmedQuote.isNotBlank()) {
            val quoteTop = afterTitleY + 64f
            val quotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentPrimary.toArgb()
                textSize = 36f
                typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            }
            val quoteText = "\"$trimmedQuote\""
            val quoteLayout = StaticLayout.Builder.obtain(
                quoteText,
                0,
                quoteText.length,
                quotePaint,
                (width - 296).coerceAtLeast(200)
            ).setAlignment(Layout.Alignment.ALIGN_NORMAL).setMaxLines(3).build()
            canvas.save()
            canvas.translate(148f, quoteTop)
            quoteLayout.draw(canvas)
            canvas.restore()
        }

        // Subtle Luma Player signature pill at bottom
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentPrimary.toArgb()
        }
        canvas.drawCircle(164f, height - 168f, 10f, badgePaint)
        val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSecondary
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("LUMA PLAYER", 190f, height - 158f, brandPaint)

        return bmp
    }

    suspend fun saveAndShareCard(
        context: Context,
        bitmap: Bitmap,
        trackTitle: String
    ): Result<Uri> = runCatching {
        val file = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "shared").apply { mkdirs() }
            val outFile = File(dir, "luma_story_card.png")
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 96, fos)
            }
            outFile
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "$trackTitle • Luma Player")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(shareIntent, "Partilhar Cartão Luma").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        uri
    }
}
