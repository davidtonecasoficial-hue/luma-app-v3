package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SurfaceMaterial
import com.example.player.PlaybackProgressUiState
import com.example.player.PlaybackUiState
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LocalLumaGlass
import com.example.ui.theme.MediaArtwork
import com.example.ui.theme.SculptedCircleButton
import com.example.ui.theme.rememberLumaBounceScale
import kotlinx.coroutines.flow.StateFlow

@Composable
fun MiniPlayerBar(
    playbackState: PlaybackUiState,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onPrevious: () -> Unit = {},
    progressFlow: StateFlow<PlaybackProgressUiState>? = null
) {
    val track = playbackState.currentTrack ?: return
    val glass = LocalLumaGlass.current
    val haptics = LocalHapticFeedback.current

    LiquidGlassSurface(
        shape = RoundedCornerShape(22.dp),
        elevated = true,
        accentGlow = glass.accentPrimary,
        onClick = onExpand,
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 580.dp)
            .testTag("mini_player_card")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MediaArtwork(
                    artworkPath = track.artworkPath,
                    contentDescription = "Capa de ${track.title}",
                    cornerRadius = 12.dp,
                    modifier = Modifier.size(46.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val prevInteraction = remember { MutableInteractionSource() }
                val prevScale = rememberLumaBounceScale(prevInteraction)
                IconButton(
                    onClick = {
                        if (glass.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPrevious()
                    },
                    interactionSource = prevInteraction,
                    modifier = Modifier
                        .size(40.dp)
                        .graphicsLayer { scaleX = prevScale; scaleY = prevScale }
                        .testTag("mini_player_previous")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Faixa anterior",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (glass.isNeumorphic) {
                    SculptedCircleButton(
                        onClick = {
                            if (glass.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTogglePlayPause()
                        },
                        emphasized = true,
                        modifier = Modifier
                            .size(42.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("mini_player_play_pause")
                    ) {
                        AnimatedContent(
                            targetState = playbackState.isPlaying,
                            transitionSpec = {
                                if (glass.reduceMotion) {
                                    fadeIn(tween(80)) togetherWith fadeOut(tween(80))
                                } else {
                                    (fadeIn(tween(150)) + scaleIn(initialScale = 0.8f)) togetherWith
                                        (fadeOut(tween(120)) + scaleOut(targetScale = 0.8f))
                                }
                            },
                            label = "miniPlayPauseMorphBw"
                        ) { playing ->
                            Icon(
                                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (playing) "Pausar" else "Reproduzir",
                                tint = if (glass.surfaceMaterial == SurfaceMaterial.NEUMORPHIC_SOFT) glass.accentPrimary else glass.textPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                } else {
                    val playInteraction = remember { MutableInteractionSource() }
                    val playScale = rememberLumaBounceScale(playInteraction)
                    val playBtnBrush = remember(glass.accentPrimary, glass.accentSecondary) {
                        Brush.linearGradient(colors = listOf(glass.accentPrimary, glass.accentSecondary))
                    }
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(40.dp)
                            .graphicsLayer { scaleX = playScale; scaleY = playScale }
                            .clip(CircleShape)
                            .background(playBtnBrush)
                            .clickable(
                                interactionSource = playInteraction,
                                indication = androidx.compose.material3.ripple(),
                                onClick = {
                                    if (glass.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onTogglePlayPause()
                                }
                            )
                            .testTag("mini_player_play_pause"),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = playbackState.isPlaying,
                            transitionSpec = {
                                if (glass.reduceMotion) {
                                    fadeIn(tween(80)) togetherWith fadeOut(tween(80))
                                } else {
                                    (fadeIn(tween(150)) + scaleIn(initialScale = 0.8f)) togetherWith
                                        (fadeOut(tween(120)) + scaleOut(targetScale = 0.8f))
                                }
                            },
                            label = "miniPlayPauseMorph"
                        ) { playing ->
                            Icon(
                                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (playing) "Pausar" else "Reproduzir",
                                tint = glass.onAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                val nextInteraction = remember { MutableInteractionSource() }
                val nextScale = rememberLumaBounceScale(nextInteraction)
                IconButton(
                    onClick = {
                        if (glass.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNext()
                    },
                    interactionSource = nextInteraction,
                    modifier = Modifier
                        .size(40.dp)
                        .graphicsLayer { scaleX = nextScale; scaleY = nextScale }
                        .testTag("mini_player_next")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Próxima música",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }

                val dismissInteraction = remember { MutableInteractionSource() }
                val dismissScale = rememberLumaBounceScale(dismissInteraction)
                IconButton(
                    onClick = onDismiss,
                    interactionSource = dismissInteraction,
                    modifier = Modifier
                        .size(36.dp)
                        .graphicsLayer { scaleX = dismissScale; scaleY = dismissScale }
                        .testTag("mini_player_dismiss")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Dispensar mini leitor",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            MiniPlayerProgressBar(
                progressFlow = progressFlow,
                fallbackPositionMs = playbackState.positionMs,
                fallbackDurationMs = playbackState.durationMs,
                accentColor = glass.accentPrimary,
                trackColor = glass.textPrimary.copy(alpha = 0.14f)
            )
        }
    }
}

@Composable
private fun MiniPlayerProgressBar(
    progressFlow: StateFlow<PlaybackProgressUiState>?,
    fallbackPositionMs: Long,
    fallbackDurationMs: Long,
    accentColor: Color,
    trackColor: Color
) {
    val progressState = progressFlow?.collectAsStateWithLifecycle()?.value
    val pos = progressState?.positionMs ?: fallbackPositionMs
    val dur = (progressState?.durationMs?.takeIf { it > 0L } ?: fallbackDurationMs).coerceAtLeast(0L)
    val progress = if (dur > 0L) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(bottom = 6.dp)
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(50)),
            color = accentColor,
            trackColor = trackColor,
            strokeCap = StrokeCap.Round
        )
    }
}
