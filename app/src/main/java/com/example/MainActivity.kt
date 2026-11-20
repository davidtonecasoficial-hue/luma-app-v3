package com.example

import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ui.LumaAppRoot

data class PipControllerState(
    val isSupported: Boolean = false,
    val isInPipMode: Boolean = false,
    val enterPip: (aspectWidth: Int, aspectHeight: Int) -> Unit = { _, _ -> }
)

val LocalPipController = compositionLocalOf { PipControllerState() }

class MainActivity : ComponentActivity() {

    private var inPipModeState by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val pipSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

        setContent {
            CompositionLocalProvider(
                LocalPipController provides PipControllerState(
                    isSupported = pipSupported,
                    isInPipMode = inPipModeState,
                    enterPip = { width, height -> enterVideoPip(width, height) }
                )
            ) {
                LumaAppRoot()
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPipModeState = isInPictureInPictureMode
    }

    private fun enterVideoPip(aspectWidth: Int, aspectHeight: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return

        val safeW = aspectWidth.takeIf { it > 0 } ?: 16
        val safeH = aspectHeight.takeIf { it > 0 } ?: 9
        val ratio = (safeW.toFloat() / safeH.toFloat()).coerceIn(0.45f, 2.35f)
        val rational = Rational((ratio * 100).toInt(), 100)

        runCatching {
            val builder = PictureInPictureParams.Builder().setAspectRatio(rational)
            enterPictureInPictureMode(builder.build())
        }
    }
}
