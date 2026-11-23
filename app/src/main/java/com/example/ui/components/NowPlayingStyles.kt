package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.media.LrcParser
import com.example.data.model.MediaItemEntity
import com.example.data.model.PlaybackAnimationStyle
import com.example.data.model.PlayerScreenStyle
import com.example.data.model.SurfaceMaterial
import com.example.player.PlaybackProgressUiState
import com.example.player.PlaybackUiState
import com.example.player.RepeatModeState
import com.example.ui.theme.LiquidGlassCapsule
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LocalLumaGlass
import com.example.ui.theme.MediaArtwork
import com.example.ui.theme.SculptedCircleButton
import com.example.ui.theme.formatDuration
import com.example.ui.theme.rememberLumaBounceScale
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

private data class DeckSlotItem(
    val slotOffset: Int,
    val queueIndex: Int?,
    val track: MediaItemEntity?,
    val badge: String
)

/**
 * Spatial 3D Cover Flow Stage modeled on the user's reference:
 * - Left: Previous track cover(s) in queue
 * - Center: Highlighted Now Playing cover + title + artist glass frame
 * - Right: Next track(s) to play in queue
 * Interactive horizontal swipe + tap-to-jump on side cards.
 */
@Composable
fun SpatialGlassCoverDeck(
    playbackState: PlaybackUiState,
    isPlayingForeground: Boolean,
    gesturesEnabled: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onJumpToIndex: (Int) -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit,
    onDoubleTapCenter: () -> Unit = onTogglePlayPause,
    modifier: Modifier = Modifier
) {
    val glass = LocalLumaGlass.current
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val currentTrack = playbackState.currentTrack ?: return
    val queue = playbackState.queue
    val currentIdx = playbackState.currentIndex.coerceAtLeast(0)

    val breathScale = if (isPlayingForeground && !glass.reduceMotion &&
        (glass.playbackAnimationStyle == PlaybackAnimationStyle.SPRING_FLOAT || glass.playbackAnimationStyle == PlaybackAnimationStyle.BREATH_PULSE)
    ) {
        val inf = rememberInfiniteTransition(label = "deckBreath")
        inf.animateFloat(
            initialValue = 1f,
            targetValue = if (glass.playbackAnimationStyle == PlaybackAnimationStyle.BREATH_PULSE) 1.032f else 1.018f,
            animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "deckBreathScale"
        ).value
    } else 1f

    fun resolveRelativeIndex(offset: Int): Int? {
        if (queue.isEmpty()) return null
        val candidate = currentIdx + offset
        return when {
            candidate in queue.indices -> candidate
            playbackState.repeatMode == RepeatModeState.ALL && queue.size > 1 -> {
                ((candidate % queue.size) + queue.size) % queue.size
            }
            else -> null
        }
    }

    val slots = remember(currentTrack.id, currentIdx, queue, playbackState.repeatMode) {
        listOf(-2, -1, 0, 1, 2).map { offset ->
            if (offset == 0) {
                DeckSlotItem(0, currentIdx.takeIf { it in queue.indices }, currentTrack, "A TOCAR")
            } else {
                val idx = resolveRelativeIndex(offset)
                val item = idx?.let { queue.getOrNull(it) }
                val label = when {
                    offset < 0 && item != null -> "ANTERIOR"
                    offset > 0 && item != null -> "A SEGUIR"
                    offset < 0 -> "INÍCIO"
                    else -> "FIM DA FILA"
                }
                DeckSlotItem(offset, idx, item, label)
            }
        }
    }

    val dragProgress = remember(currentTrack.id, currentIdx) { Animatable(0f) }

    val dragModifier = if (gesturesEnabled) {
        val thresholdPx = with(density) { 120.dp.toPx() }
        Modifier.pointerInput(currentTrack.id, currentIdx, queue.size) {
            detectHorizontalDragGestures(
                onHorizontalDrag = { _, dragAmount ->
                    val nextValue = (dragProgress.value + dragAmount / thresholdPx).coerceIn(-1.15f, 1.15f)
                    scope.launch { dragProgress.snapTo(nextValue) }
                },
                onDragCancel = {
                    scope.launch {
                        dragProgress.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                    }
                },
                onDragEnd = {
                    val value = dragProgress.value
                    scope.launch {
                        when {
                            value < -0.35f -> {
                                if (glass.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNext()
                                dragProgress.snapTo(0f)
                            }
                            value > 0.35f -> {
                                if (glass.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onPrevious()
                                dragProgress.snapTo(0f)
                            }
                            else -> {
                                dragProgress.animateTo(
                                    0f,
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                                )
                            }
                        }
                    }
                }
            )
        }
    } else Modifier

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .then(dragModifier)
            .testTag("nps_spatial_cover_deck"),
        contentAlignment = Alignment.Center
    ) {
        val isWide = maxWidth >= 520.dp
        val centerCardWidth = if (isWide) 254.dp else (maxWidth * 0.52f).coerceIn(174.dp, 240.dp)
        val centerCardHeight = centerCardWidth * 1.24f
        val primaryStepPx = with(density) { (centerCardWidth * (if (isWide) 0.64f else 0.62f)).toPx() }
        val secondaryStepPx = with(density) { (centerCardWidth * (if (isWide) 1.02f else 0.90f)).toPx() }
        val cameraDist = 14f * density.density

        // Draw far slots first, adjacent slots second, center slot last so overlapping depth is crisp
        val orderedSlots = slots.sortedByDescending { abs(it.slotOffset.toFloat() + dragProgress.value) }

        orderedSlots.forEach { slot ->
            val effectivePos = slot.slotOffset.toFloat() + dragProgress.value
            val dist = abs(effectivePos)
            val isCenter = slot.slotOffset == 0
            val sideAlpha = when {
                dist < 0.45f -> 1f
                dist < 1.35f -> if (slot.track != null) 0.94f else 0.45f
                else -> if (slot.track != null) (if (isWide) 0.78f else 0.48f) else 0.16f
            }

            val scale = when {
                dist <= 1f -> 1f - 0.16f * dist
                else -> (0.84f - 0.12f * (dist - 1f)).coerceAtLeast(0.64f)
            }

            val offsetX = when {
                dist <= 1f -> effectivePos * primaryStepPx
                else -> {
                    val sign = if (effectivePos >= 0f) 1f else -1f
                    sign * (primaryStepPx + (dist - 1f) * (secondaryStepPx - primaryStepPx))
                }
            }

            val rotY = (-effectivePos * 14f).coerceIn(-24f, 24f)

            val slotTag = when (slot.slotOffset) {
                -1 -> "nps_deck_prev_card"
                0 -> "nps_deck_current_card"
                1 -> "nps_deck_next_card"
                else -> "nps_deck_card_${slot.slotOffset}"
            }

            val combinedScale = if (isCenter) scale * breathScale else scale

            DeckCoverCard(
                track = slot.track,
                badgeLabel = slot.badge,
                isCenter = isCenter,
                isPlaying = isCenter && isPlayingForeground,
                onClick = {
                    when {
                        isCenter -> onTogglePlayPause()
                        slot.queueIndex != null -> {
                            if (glass.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onJumpToIndex(slot.queueIndex)
                        }
                        slot.slotOffset < 0 -> onPrevious()
                        slot.slotOffset > 0 -> onNext()
                    }
                },
                onDoubleTap = if (isCenter && gesturesEnabled) onDoubleTapCenter else null,
                onToggleFavorite = { slot.track?.let(onToggleFavorite) },
                modifier = Modifier
                    .width(centerCardWidth)
                    .height(centerCardHeight)
                    .zIndex(10f - dist * 2f)
                    .graphicsLayer {
                        translationX = offsetX
                        scaleX = combinedScale
                        scaleY = combinedScale
                        rotationY = rotY
                        alpha = sideAlpha
                        cameraDistance = cameraDist
                    }
                    .testTag(slotTag)
            )
        }
    }
}

@Composable
private fun DeckCoverCard(
    track: MediaItemEntity?,
    badgeLabel: String,
    isCenter: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onDoubleTap: (() -> Unit)? = null,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalLumaGlass.current
    val shape = RoundedCornerShape(24.dp)
    val isNeumorphic = glass.isNeumorphic
    val cardBg = when (glass.surfaceMaterial) {
        SurfaceMaterial.NEUMORPHIC_BW, SurfaceMaterial.NEUMORPHIC_SOFT -> if (isCenter) glass.surfaceElevated else glass.surface
        SurfaceMaterial.AMOLED_CRISP -> if (isCenter) glass.surfaceElevated else glass.surface
        SurfaceMaterial.LIQUID_GLASS -> if (isCenter) {
            if (glass.isDark) glass.surfaceElevated.copy(alpha = 0.84f) else lerp(Color.White, glass.surfaceElevated, 0.42f).copy(alpha = 0.88f)
        } else {
            if (glass.isDark) glass.surface.copy(alpha = 0.70f) else lerp(Color.White, glass.surface, 0.36f).copy(alpha = 0.78f)
        }
    }
    val borderStroke = when {
        isNeumorphic -> BorderStroke(
            width = if (isCenter) 1.6.dp else 1.dp,
            brush = Brush.linearGradient(
                listOf(glass.reliefShadowLight, glass.reliefShadowDark.copy(alpha = 0.65f))
            )
        )
        isCenter -> BorderStroke(
            width = 1.6.dp,
            brush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = if (glass.isDark) 0.58f else 0.96f),
                    glass.accentPrimary.copy(alpha = if (glass.isDark) 0.58f else 0.42f)
                )
            )
        )
        else -> BorderStroke(0.9.dp, glass.glassBorder)
    }

    val tapModifier = when {
        track == null -> Modifier
        onDoubleTap != null -> Modifier.pointerInput(track.id, onDoubleTap) {
            detectTapGestures(
                onTap = { onClick() },
                onDoubleTap = { onDoubleTap() }
            )
        }
        else -> Modifier.clickable(onClick = onClick)
    }

    Surface(
        modifier = modifier
            .shadow(
                elevation = if (isCenter) 20.dp else 8.dp,
                shape = shape,
                ambientColor = if (isNeumorphic) glass.reliefShadowDark else glass.accentPrimary.copy(alpha = 0.25f),
                spotColor = if (isNeumorphic) glass.reliefShadowDark else Color.Black.copy(alpha = 0.45f)
            )
            .clip(shape)
            .then(tapModifier),
        shape = shape,
        color = cardBg,
        contentColor = glass.textPrimary,
        border = borderStroke
    ) {
        if (track == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = glass.textSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = badgeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.textSecondary.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isCenter) 10.dp else 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    MediaArtwork(
                        artworkPath = track.artworkPath,
                        contentDescription = "Capa de ${track.title}",
                        cornerRadius = 18.dp,
                        maxDecodeSize = if (isCenter) 360.dp else 220.dp,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Subtle side badge ("ANTERIOR" / "A SEGUIR") for instant queue orientation
                    if (!isCenter) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = badgeLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    } else {
                        val favPopScale = remember { Animatable(1f) }
                        LaunchedEffect(track.isFavorite, glass.reduceMotion) {
                            if (track.isFavorite && !glass.reduceMotion) {
                                favPopScale.snapTo(1.32f)
                                favPopScale.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 480f))
                            }
                        }
                        val favInteraction = remember { MutableInteractionSource() }
                        val favBounce = rememberLumaBounceScale(favInteraction)
                        IconButton(
                            onClick = onToggleFavorite,
                            interactionSource = favInteraction,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(38.dp)
                                .graphicsLayer {
                                    val s = favBounce * favPopScale.value
                                    scaleX = s
                                    scaleY = s
                                }
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.36f))
                                .testTag("player_favorite_button")
                        ) {
                            Icon(
                                imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = if (track.isFavorite) "Remover favorito" else "Adicionar favorito",
                                tint = if (track.isFavorite) {
                                    if (glass.isMonochrome) Color.White else glass.accentSecondary
                                } else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (isPlaying) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.GraphicEq,
                                        contentDescription = null,
                                        tint = if (glass.isMonochrome) Color.White else glass.accentPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "A TOCAR",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isCenter) 10.dp else 7.dp))

                Text(
                    text = track.title,
                    style = (if (isCenter) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge).copy(
                        fontWeight = if (isCenter) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = glass.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
                Text(
                    text = track.artist,
                    style = if (isCenter) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelSmall,
                    color = glass.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

/**
 * Floating translucent bottom control dock modeled after the reference image:
 * Left: Transport controls (<< || >>)
 * Center: Compact Now Playing track badge with cover + title/artist + live waveform
 * Right: Quick actions (Lyrics, Queue, Equalizer)
 */
@Composable
fun FloatingGlassPlayerDock(
    track: MediaItemEntity,
    playbackState: PlaybackUiState,
    isPlayingForeground: Boolean,
    minimalistMode: Boolean,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalLumaGlass.current
    val haptics = LocalHapticFeedback.current
    val isNeumorphic = glass.isNeumorphic

    LiquidGlassCapsule(
        elevated = true,
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 580.dp)
            .testTag("nps_floating_dock")
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp)
        ) {
            val isCompact = maxWidth < 430.dp

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Left Transport Cluster: Prev, Play/Pause, Next
                val prevInteraction = remember { MutableInteractionSource() }
                val prevScale = rememberLumaBounceScale(prevInteraction)
                IconButton(
                    onClick = {
                        if (glass.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPrevious()
                    },
                    interactionSource = prevInteraction,
                    modifier = Modifier
                        .size(42.dp)
                        .minimumInteractiveComponentSize()
                        .graphicsLayer { scaleX = prevScale; scaleY = prevScale }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Faixa anterior",
                        tint = glass.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (isNeumorphic) {
                    SculptedCircleButton(
                        onClick = {
                            if (glass.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTogglePlayPause()
                        },
                        emphasized = true,
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("player_play_pause_button")
                    ) {
                        PlayPauseAnimatedIcon(
                            isPlaying = playbackState.isPlaying,
                            tint = if (glass.surfaceMaterial == SurfaceMaterial.NEUMORPHIC_SOFT) glass.accentPrimary else glass.textPrimary,
                            reduceMotion = glass.reduceMotion
                        )
                    }
                } else {
                    val playInteraction = remember { MutableInteractionSource() }
                    val playScale = rememberLumaBounceScale(playInteraction)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .graphicsLayer { scaleX = playScale; scaleY = playScale }
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(glass.accentPrimary, glass.accentSecondary)))
                            .clickable(
                                interactionSource = playInteraction,
                                indication = androidx.compose.material3.ripple(),
                                onClick = {
                                    if (glass.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onTogglePlayPause()
                                }
                            )
                            .testTag("player_play_pause_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        PlayPauseAnimatedIcon(isPlaying = playbackState.isPlaying, tint = glass.onAccent, reduceMotion = glass.reduceMotion)
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
                        .size(42.dp)
                        .minimumInteractiveComponentSize()
                        .graphicsLayer { scaleX = nextScale; scaleY = nextScale }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Próxima faixa",
                        tint = glass.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Center Now Playing Glass Inset Pill
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (glass.isDark) Color.White.copy(alpha = 0.08f)
                            else glass.textPrimary.copy(alpha = 0.06f)
                        )
                        .border(0.7.dp, glass.glassBorder, RoundedCornerShape(16.dp))
                        .clickable(onClick = onOpenOptions)
                        .padding(horizontal = 7.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MediaArtwork(
                        artworkPath = track.artworkPath,
                        contentDescription = null,
                        cornerRadius = 8.dp,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = glass.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = glass.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!isCompact) {
                        MiniDockWaveform(
                            isPlaying = isPlayingForeground,
                            reduceMotion = glass.reduceMotion,
                            color = if (glass.isMonochrome) glass.textPrimary else glass.accentPrimary,
                            modifier = Modifier
                                .width(20.dp)
                                .height(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.MoreHoriz,
                            contentDescription = "Mais opções",
                            tint = glass.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Right Quick Actions: Lyrics, Queue, Equalizer
                if (!minimalistMode) {
                    val lyricsInteraction = remember { MutableInteractionSource() }
                    val lyricsScale = rememberLumaBounceScale(lyricsInteraction)
                    IconButton(
                        onClick = onOpenLyrics,
                        interactionSource = lyricsInteraction,
                        modifier = Modifier
                            .size(40.dp)
                            .minimumInteractiveComponentSize()
                            .graphicsLayer { scaleX = lyricsScale; scaleY = lyricsScale }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lyrics,
                            contentDescription = "Letras",
                            tint = glass.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    val queueInteraction = remember { MutableInteractionSource() }
                    val queueScale = rememberLumaBounceScale(queueInteraction)
                    IconButton(
                        onClick = onOpenQueue,
                        interactionSource = queueInteraction,
                        modifier = Modifier
                            .size(40.dp)
                            .minimumInteractiveComponentSize()
                            .graphicsLayer { scaleX = queueScale; scaleY = queueScale }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = "Fila de reprodução",
                            tint = glass.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (!isCompact) {
                        val eqInteraction = remember { MutableInteractionSource() }
                        val eqScale = rememberLumaBounceScale(eqInteraction)
                        IconButton(
                            onClick = onOpenEqualizer,
                            interactionSource = eqInteraction,
                            modifier = Modifier
                                .size(40.dp)
                                .minimumInteractiveComponentSize()
                                .graphicsLayer { scaleX = eqScale; scaleY = eqScale }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Equalizer,
                                contentDescription = "Equalizador",
                                tint = if (playbackState.equalizer.enabled) glass.accentPrimary else glass.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PosterImmersiveStage(
    track: MediaItemEntity,
    playbackState: PlaybackUiState,
    gesturesEnabled: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit,
    onDoubleTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val glass = LocalLumaGlass.current
    val nextTrack = playbackState.queue.getOrNull(playbackState.currentIndex + 1)

    val swipeModifier = if (gesturesEnabled) {
        var dragSum = 0f
        Modifier
            .pointerInput(track.id) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount -> dragSum += dragAmount },
                    onDragEnd = {
                        if (dragSum < -110f) onNext()
                        else if (dragSum > 110f) onPrevious()
                        dragSum = 0f
                    },
                    onDragCancel = { dragSum = 0f }
                )
            }
            .pointerInput(track.id, onDoubleTap) {
                detectTapGestures(onDoubleTap = { onDoubleTap() })
            }
    } else Modifier

    LiquidGlassSurface(
        shape = RoundedCornerShape(30.dp),
        elevated = true,
        accentGlow = if (glass.isMonochrome) null else glass.accentPrimary,
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 460.dp)
            .aspectRatio(0.92f)
            .then(swipeModifier)
            .testTag("nps_poster_stage")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MediaArtwork(
                artworkPath = track.artworkPath,
                contentDescription = "Capa de ${track.title}",
                cornerRadius = 30.dp,
                maxDecodeSize = 480.dp,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.14f),
                            0.55f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.80f)
                        )
                    )
            )

            IconButton(
                onClick = { onToggleFavorite(track) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.42f))
                    .testTag("player_favorite_button")
            ) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (track.isFavorite) "Remover favorito" else "Adicionar favorito",
                    tint = if (track.isFavorite && !glass.isMonochrome) glass.accentSecondary else Color.White
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${track.artist} • ${track.album}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (nextTrack != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.16f))
                            .clickable(onClick = onNext)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "A seguir: ${nextTrack.title}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LyricsStageView(
    track: MediaItemEntity,
    positionMs: Long,
    onSeekTo: (Long) -> Unit,
    onImportLyrics: () -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit,
    lyricsOffsetMs: Long = track.lyricsOffsetMs,
    lyricsFontScale: Float = 1f,
    lyricsCentered: Boolean = true,
    onOpenFullScreenLyrics: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    progressFlow: StateFlow<PlaybackProgressUiState>? = null
) {
    val glass = LocalLumaGlass.current
    val livePositionMs = progressFlow?.collectAsStateWithLifecycle()?.value?.positionMs ?: positionMs
    val parsed = remember(track.embeddedLyrics) { LrcParser.parse(track.embeddedLyrics) }
    val activeIdx = remember(parsed, livePositionMs, lyricsOffsetMs) {
        if (parsed.isTimed) LrcParser.findActiveLineIndex(parsed.lines, livePositionMs, lyricsOffsetMs) else -1
    }
    val listState = rememberLazyListState()

    LaunchedEffect(activeIdx) {
        if (parsed.isTimed && activeIdx >= 0) {
            listState.animateScrollToItem((activeIdx - 2).coerceAtLeast(0))
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 580.dp)
            .testTag("nps_lyrics_stage"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LiquidGlassSurface(shape = RoundedCornerShape(20.dp), elevated = true) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MediaArtwork(
                    artworkPath = track.artworkPath,
                    contentDescription = track.title,
                    cornerRadius = 12.dp,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = glass.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = { onToggleFavorite(track) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("player_favorite_button")
                ) {
                    Icon(
                        imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (track.isFavorite) "Remover favorito" else "Adicionar favorito",
                        tint = if (track.isFavorite) glass.accentSecondary else glass.textSecondary
                    )
                }
            }
        }

        LiquidGlassSurface(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (parsed.lines.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lyrics,
                        contentDescription = null,
                        tint = glass.accentPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Sem letra associada a esta faixa",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Importe um ficheiro .lrc ou .txt local para acompanhar em tempo real.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = glass.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(onClick = onImportLyrics) {
                        Icon(Icons.Rounded.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Carregar .lrc")
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .let { mod ->
                            if (onOpenFullScreenLyrics != null) mod.clickable(onClick = onOpenFullScreenLyrics) else mod
                        },
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(parsed.lines, key = { idx, line -> "${idx}_${line.timeMs ?: -1L}" }) { idx, line ->
                        val isCurrent = parsed.isTimed && idx == activeIdx
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isCurrent) glass.pillIndicatorBg else Color.Transparent)
                                .then(
                                    if (line.timeMs != null) Modifier.clickable { onSeekTo((line.timeMs - lyricsOffsetMs).coerceAtLeast(0L)) }
                                    else Modifier
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalAlignment = if (lyricsCentered) Alignment.CenterHorizontally else Alignment.Start
                        ) {
                            Text(
                                text = line.text,
                                style = (if (isCurrent) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge).copy(
                                    fontSize = (if (isCurrent) 17f else 15f).sp * lyricsFontScale
                                ),
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) glass.accentPrimary else glass.textPrimary,
                                textAlign = if (lyricsCentered) TextAlign.Center else TextAlign.Start
                            )
                            if (!line.translatedText.isNullOrBlank()) {
                                Text(
                                    text = line.translatedText,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp * lyricsFontScale),
                                    color = glass.textSecondary,
                                    textAlign = if (lyricsCentered) TextAlign.Center else TextAlign.Start
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive Modal Sheet letting users switch between the 4 Now Playing Screen styles live.
 */
@Composable
fun PlayerStylePickerSheetContent(
    selectedStyle: PlayerScreenStyle,
    onSelectStyle: (PlayerScreenStyle) -> Unit
) {
    val glass = LocalLumaGlass.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = glass.accentPrimary)
            Text("Estilo do Leitor", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = "Escolha a disposição visual do ecrã A Reproduzir.",
            style = MaterialTheme.typography.bodyMedium,
            color = glass.textSecondary
        )

        PlayerScreenStyle.entries.forEach { style ->
            val isSelected = style == selectedStyle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isSelected) glass.pillIndicatorBg else glass.textPrimary.copy(alpha = 0.05f))
                    .border(
                        width = if (isSelected) 1.6.dp else 0.8.dp,
                        color = if (isSelected) glass.accentPrimary else glass.glassBorder,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { onSelectStyle(style) }
                    .padding(12.dp)
                    .testTag("player_style_option_${style.name.lowercase()}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerStyleMiniGlyph(style = style, accent = glass.accentPrimary, ink = glass.textPrimary)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = style.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        color = if (isSelected) glass.accentPrimary else glass.textPrimary
                    )
                    Text(
                        text = style.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = glass.textSecondary
                    )
                }
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Selecionado",
                        tint = glass.accentPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerStyleMiniGlyph(
    style: PlayerScreenStyle,
    accent: Color,
    ink: Color
) {
    Box(
        modifier = Modifier
            .size(width = 58.dp, height = 46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ink.copy(alpha = 0.07f))
            .border(0.8.dp, ink.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            PlayerScreenStyle.GLASS_DECK -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                    Box(
                        modifier = Modifier
                            .size(width = 14.dp, height = 20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(ink.copy(alpha = 0.28f))
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 20.dp, height = 28.dp)
                            .zIndex(2f)
                            .clip(RoundedCornerShape(5.dp))
                            .background(accent)
                            .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(5.dp))
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 14.dp, height = 20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(ink.copy(alpha = 0.28f))
                    )
                }
            }
            PlayerScreenStyle.CAROUSEL_VINYL -> {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.30f))
                        .border(1.5.dp, accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(ink)
                    )
                }
            }
            PlayerScreenStyle.MINIMAL_POSTER -> {
                Box(
                    modifier = Modifier
                        .size(width = 34.dp, height = 34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.75f))
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(Color.Black.copy(alpha = 0.45f))
                    )
                }
            }
            PlayerScreenStyle.LYRICS_STAGE -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.width(30.dp).height(3.dp).clip(CircleShape).background(ink.copy(alpha = 0.35f)))
                    Box(Modifier.width(38.dp).height(4.dp).clip(CircleShape).background(accent))
                    Box(Modifier.width(28.dp).height(3.dp).clip(CircleShape).background(ink.copy(alpha = 0.35f)))
                }
            }
        }
    }
}

@Composable
private fun PlayPauseAnimatedIcon(
    isPlaying: Boolean,
    tint: Color,
    reduceMotion: Boolean
) {
    AnimatedContent(
        targetState = isPlaying,
        transitionSpec = {
            if (reduceMotion) {
                fadeIn(tween(80)) togetherWith fadeOut(tween(80))
            } else {
                (fadeIn(tween(150)) + scaleIn(initialScale = 0.8f)) togetherWith
                    (fadeOut(tween(120)) + scaleOut(targetScale = 0.8f))
            }
        },
        label = "dockPlayPauseMorph"
    ) { playing ->
        Icon(
            imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (playing) "Pausar" else "Reproduzir",
            tint = tint,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun MiniDockWaveform(
    isPlaying: Boolean,
    reduceMotion: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val phase = if (isPlaying && !reduceMotion) {
        val transition = rememberInfiniteTransition(label = "dockWave")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (Math.PI * 2).toFloat(),
            animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
            label = "dockWavePhase"
        ).value
    } else 0f

    Canvas(modifier = modifier) {
        val bars = 4
        val step = size.width / bars
        val barW = step * 0.56f
        for (i in 0 until bars) {
            val amp = if (!isPlaying) 0.25f else if (reduceMotion) 0.6f else (0.3f + 0.7f * abs(sin(phase + i * 0.9f)))
            val h = size.height * amp
            val x = i * step + (step - barW) / 2f
            val y = (size.height - h) / 2f
            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f)
            )
        }
    }
}
