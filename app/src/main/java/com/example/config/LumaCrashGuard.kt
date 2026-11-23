package com.example.config

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Process
import android.util.Log
import com.example.MainActivity
import kotlinx.coroutines.CoroutineExceptionHandler
import java.io.File
import kotlin.math.max
import kotlin.system.exitProcess

/**
 * Central crash shield, safe coroutine exception handler, and memory-bounded bitmap utility
 * for Luma Player. Prevents background uncaught exceptions and gracefully recovers if an
 * unexpected fatal runtime error happens in the UI or system layer.
 */
object LumaCrashGuard {

    private const val TAG = "LumaCrashGuard"
    private const val PREFS_NAME = "luma_crash_guard"
    private const val KEY_RECOVERED_FROM_CRASH = "recovered_from_crash"
    private const val KEY_LAST_CRASH_AT_MS = "last_crash_at_ms"
    private const val KEY_RECENT_CRASH_COUNT = "recent_crash_count"
    private const val CRASH_LOOP_WINDOW_MS = 10_000L

    @Volatile
    private var installed = false

    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Recovered background coroutine failure safely", throwable)
    }

    fun install(application: Application) {
        if (installed) return
        installed = true
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val handled = handleUncaughtException(application, thread, throwable)
            if (!handled) {
                previousHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun handleUncaughtException(
        application: Application,
        thread: Thread,
        throwable: Throwable
    ): Boolean {
        return try {
            Log.e(TAG, "Intercepted fatal exception on thread=${thread.name}", throwable)
            val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            val lastCrash = prefs.getLong(KEY_LAST_CRASH_AT_MS, 0L)
            val recentCount = prefs.getInt(KEY_RECENT_CRASH_COUNT, 0)
            val nextCount = if (now - lastCrash <= CRASH_LOOP_WINDOW_MS) recentCount + 1 else 1

            if (!shouldAttemptAutoRestart(now, lastCrash, recentCount)) {
                prefs.edit()
                    .putLong(KEY_LAST_CRASH_AT_MS, now)
                    .putInt(KEY_RECENT_CRASH_COUNT, nextCount)
                    .putBoolean(KEY_RECOVERED_FROM_CRASH, false)
                    .commit()
                return false
            }

            prefs.edit()
                .putLong(KEY_LAST_CRASH_AT_MS, now)
                .putInt(KEY_RECENT_CRASH_COUNT, nextCount)
                .putBoolean(KEY_RECOVERED_FROM_CRASH, true)
                .commit()

            val restartIntent = Intent(application, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                )
            }
            application.startActivity(restartIntent)
            Process.killProcess(Process.myPid())
            exitProcess(0)
            @Suppress("UNREACHABLE_CODE")
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Failed during graceful restart", t)
            false
        }
    }

    fun shouldAttemptAutoRestart(
        nowMs: Long,
        lastCrashMs: Long,
        recentCount: Int
    ): Boolean {
        if (lastCrashMs <= 0L) return true
        val elapsed = nowMs - lastCrashMs
        if (elapsed > CRASH_LOOP_WINDOW_MS) return true
        return recentCount < 2
    }

    fun consumeRecoveryNotice(context: Context): Boolean {
        return runCatching {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val recovered = prefs.getBoolean(KEY_RECOVERED_FROM_CRASH, false)
            if (recovered) {
                prefs.edit().putBoolean(KEY_RECOVERED_FROM_CRASH, false).apply()
            }
            recovered
        }.getOrDefault(false)
    }

    /**
     * Memory-safe bitmap decoder using inJustDecodeBounds before full pixel allocation
     * so huge local cover art files never trigger OutOfMemoryError or Binder crashes.
     */
    fun decodeSampledBitmap(
        context: Context,
        path: String?,
        maxDimensionPx: Int
    ): Bitmap? {
        if (path.isNullOrBlank() || maxDimensionPx <= 0) return null
        return runCatching {
            val isUri = path.startsWith("content://") || path.startsWith("file://")
            val uri = if (isUri) Uri.parse(path) else null
            val file = if (!isUri) File(path).takeIf { it.exists() && it.length() > 0L } else null
            if (!isUri && file == null) return@runCatching null

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            if (uri != null) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, bounds)
                }
            } else if (file != null) {
                BitmapFactory.decodeFile(file.absolutePath, bounds)
            }

            val rawW = bounds.outWidth
            val rawH = bounds.outHeight
            if (rawW <= 0 || rawH <= 0) return@runCatching null

            var sampleSize = 1
            while ((rawW / sampleSize) > maxDimensionPx * 2 || (rawH / sampleSize) > maxDimensionPx * 2) {
                sampleSize *= 2
            }
            if (max(rawW, rawH) / sampleSize > maxDimensionPx && sampleSize == 1) {
                sampleSize = 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize.coerceAtLeast(1)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            if (uri != null) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOpts)
                }
            } else if (file != null) {
                BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
            } else {
                null
            }
        }.getOrNull()
    }
}
