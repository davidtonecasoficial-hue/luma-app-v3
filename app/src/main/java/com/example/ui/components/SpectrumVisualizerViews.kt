package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.VisualizerPreset
import com.example.data.model.VisualizerStyle
import com.example.player.AudioSpectrumSnapshot
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
fun SpectrumVisualizerView(
    snapshot: AudioSpectrumSnapshot,
    isPlaying: Boolean,
    style: VisualizerStyle,
    preset: VisualizerPreset,
    reactiveLighting: Boolean,
    reduceMotion: Boolean,
    accentPrimary: Color,
    accentSecondary: Color,
    modifier: Modifier = Modifier
) {
    val phase = if (isPlaying && !reduceMotion) {
        val transition = rememberInfiniteTransition(label = "spectrumPhase")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
            label = "spectrumPhaseAngle"
        ).value
    } else 0f

    val (colorA, colorB) = when (preset) {
        VisualizerPreset.NEON_PULSE -> accentPrimary to lerp(accentSecondary, Color(0xFFFF4D9D), 0.35f)
        VisualizerPreset.AURORA_SILK -> accentPrimary to accentSecondary
        VisualizerPreset.STUDIO_SPECTRUM -> lerp(accentPrimary, Color(0xFF22C55E), 0.4f) to lerp(accentSecondary, Color(0xFF38BDF8), 0.35f)
        VisualizerPreset.MINIMAL_BREATH -> accentPrimary.copy(alpha = 0.78f) to accentPrimary.copy(alpha = 0.38f)
    }

    Box(modifier = modifier.testTag("spectrum_visualizer_${style.name.lowercase()}")) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val energy = if (isPlaying) max(snapshot.peakEnergy, 0.22f) else 0.08f
            val bands = snapshot.bands
            val wave = snapshot.waveform

            if (reactiveLighting && isPlaying) {
                val glowRadius = size.maxDimension * (0.42f + 0.25f * energy)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colorA.copy(alpha = (0.14f + 0.16f * energy).coerceIn(0f, 0.35f)),
                            Color.Transparent
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = glowRadius
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = glowRadius
                )
            }

            fun bandAmp(i: Int, total: Int): Float {
                val live = if (bands.isNotEmpty()) {
                    bands[(i * bands.size / total).coerceIn(0, bands.lastIndex)]
                } else 0f
                val animFallback = if (!isPlaying) 0.12f else if (reduceMotion) 0.42f else {
                    0.22f + 0.46f * abs(sin(phase + i * 0.52f))
                }
                val blended = if (live > 0.02f) (live * 0.85f + animFallback * 0.15f) else animFallback
                return if (isPlaying) blended.coerceIn(0.14f, 1f) else 0.12f
            }

            when (style) {
                VisualizerStyle.BARS_3D -> {
                    val count = 18
                    val step = size.width / count
                    val barW = step * 0.56f
                    val depthOffset = barW * 0.24f
                    for (i in 0 until count) {
                        val amp = bandAmp(i, count)
                        val barH = (size.height * amp).coerceAtLeast(4.dp.toPx())
                        val left = i * step + (step - barW) / 2f
                        val top = (size.height - barH) / 2f
                        // 3D depth shadow extruded edge
                        drawRoundRect(
                            color = colorB.copy(alpha = 0.34f),
                            topLeft = Offset(left + depthOffset, top + depthOffset * 0.5f),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                        )
                        drawRoundRect(
                            brush = Brush.verticalGradient(listOf(colorA, colorB)),
                            topLeft = Offset(left, top),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                        )
                    }
                }

                VisualizerStyle.CIRCULAR_SPECTRUM -> {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val baseR = (size.minDimension * 0.26f).coerceAtLeast(6.dp.toPx())
                    val maxSpeak = size.minDimension * 0.24f
                    val rays = 32
                    drawCircle(
                        color = colorA.copy(alpha = 0.28f),
                        radius = baseR,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    for (i in 0 until rays) {
                        val angle = (i.toFloat() / rays) * (2f * PI.toFloat()) + phase * 0.15f
                        val amp = bandAmp(i, rays)
                        val r0 = baseR
                        val r1 = baseR + maxSpeak * amp
                        val sx = cx + cos(angle) * r0
                        val sy = cy + sin(angle) * r0
                        val ex = cx + cos(angle) * r1
                        val ey = cy + sin(angle) * r1
                        drawLine(
                            brush = Brush.linearGradient(listOf(colorA, colorB), Offset(sx, sy), Offset(ex, ey)),
                            start = Offset(sx, sy),
                            end = Offset(ex, ey),
                            strokeWidth = 2.4.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                VisualizerStyle.WAVEFORM -> {
                    val path = Path()
                    val mirrorPath = Path()
                    val points = 32
                    val midY = size.height / 2f
                    for (i in 0 until points) {
                        val x = (i.toFloat() / (points - 1)) * size.width
                        val liveSample = wave.getOrElse(i) { 0f }
                        val synthetic = if (!isPlaying) 0.05f else if (reduceMotion) 0.25f else {
                            sin(phase * 1.4f + i * 0.45f) * 0.42f * energy
                        }
                        val yAmp = (if (abs(liveSample) > 0.01f) liveSample else synthetic) * (size.height * 0.46f)
                        val y = midY + yAmp
                        val my = midY - yAmp * 0.65f
                        if (i == 0) {
                            path.moveTo(x, y)
                            mirrorPath.moveTo(x, my)
                        } else {
                            path.lineTo(x, y)
                            mirrorPath.lineTo(x, my)
                        }
                    }
                    drawPath(
                        path = mirrorPath,
                        brush = Brush.horizontalGradient(listOf(colorB.copy(alpha = 0.45f), colorA.copy(alpha = 0.45f))),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(listOf(colorA, colorB)),
                        style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                VisualizerStyle.PARTICLES -> {
                    val count = 22
                    val step = size.width / count
                    for (i in 0 until count) {
                        val amp = bandAmp(i, count)
                        val x = i * step + step * 0.5f + if (isPlaying && !reduceMotion) cos(phase + i) * 3.dp.toPx() else 0f
                        val driftY = if (isPlaying && !reduceMotion) sin(phase * 1.2f + i * 0.7f) * (size.height * 0.28f) else 0f
                        val y = (size.height * 0.5f + driftY).coerceIn(4.dp.toPx(), size.height - 4.dp.toPx())
                        val radius = (2.dp.toPx() + 4.dp.toPx() * amp)
                        val tint = lerp(colorA, colorB, i.toFloat() / count)
                        drawCircle(
                            color = tint.copy(alpha = (0.35f + 0.65f * amp).coerceIn(0f, 1f)),
                            radius = radius,
                            center = Offset(x, y)
                        )
                    }
                }

                VisualizerStyle.FREQUENCY_ANALYZER -> {
                    val count = 16
                    val step = size.width / count
                    val barW = step * 0.74f
                    val segments = 5
                    for (i in 0 until count) {
                        val amp = bandAmp(i, count)
                        val activeSegs = (amp * segments).toInt().coerceAtLeast(if (isPlaying) 1 else 0)
                        val x = i * step + (step - barW) / 2f
                        val segH = (size.height / segments) * 0.72f
                        val segStep = size.height / segments
                        for (s in 0 until segments) {
                            val y = size.height - (s + 1) * segStep + (segStep - segH) / 2f
                            val isLit = s < activeSegs
                            val frac = s.toFloat() / (segments - 1).coerceAtLeast(1)
                            val col = if (isLit) lerp(colorA, colorB, frac) else colorA.copy(alpha = 0.14f)
                            drawRoundRect(
                                color = col,
                                topLeft = Offset(x, y),
                                size = Size(barW, segH),
                                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                            )
                        }
                    }
                }
            }
        }
    }
}
