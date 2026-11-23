package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.MediaItemEntity
import com.example.ui.theme.LocalLumaGlass
import com.example.ui.theme.rememberLumaBounceScale

/**
 * Custom App Identity Pill – Luma Player.
 * Minimalist Samsung One UI Now Playing-inspired horizontal capsule featuring:
 * - Official Luma Player app icon stationary on the left in crisp proportions
 * - Current playing track title immediately after with smooth horizontal marquee
 * - Liquid Glass translucent capsule adapting cleanly to Light and Dark modes
 * - Fade + scale entrance and subtle spring title transition
 */
@Composable
fun LumaIdentityPill(
    currentTrack: MediaItemEntity?,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalLumaGlass.current

    AnimatedVisibility(
        visible = currentTrack != null,
        enter = if (glass.reduceMotion) {
            fadeIn(tween(120))
        } else {
            fadeIn(tween(240, easing = FastOutSlowInEasing)) +
                scaleIn(
                    initialScale = 0.86f,
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f)
                )
        },
        exit = if (glass.reduceMotion) {
            fadeOut(tween(100))
        } else {
            fadeOut(tween(180)) + scaleOut(targetScale = 0.88f)
        },
        modifier = modifier
    ) {
        val track = currentTrack ?: return@AnimatedVisibility
        val interactionSource = remember { MutableInteractionSource() }
        val bounceScale = rememberLumaBounceScale(interactionSource, pressedScale = 0.96f)

        val pillFill by animateColorAsState(
            targetValue = if (glass.isDark) {
                glass.glassElevated.copy(alpha = 0.86f)
            } else {
                Color.White.copy(alpha = 0.88f)
            },
            animationSpec = if (glass.reduceMotion) snap() else tween(220),
            label = "identityPillFill"
        )
        val dotColor by animateColorAsState(
            targetValue = if (isPlaying) glass.accentPrimary else glass.textSecondary.copy(alpha = 0.45f),
            animationSpec = if (glass.reduceMotion) snap() else tween(220),
            label = "identityPillDot"
        )
        val dotScale by animateFloatAsState(
            targetValue = if (isPlaying) 1f else 0.72f,
            animationSpec = if (glass.reduceMotion) snap() else spring(dampingRatio = 0.7f, stiffness = 400f),
            label = "identityPillDotScale"
        )

        val bgBrush = remember(pillFill, glass.glassSurface, glass.isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    pillFill,
                    glass.glassSurface.copy(alpha = if (glass.isDark) 0.92f else 0.94f)
                )
            )
        }
        val borderBrush = remember(glass.glassBorderTop, glass.glassBorderBottom) {
            Brush.verticalGradient(
                colors = listOf(
                    glass.glassBorderTop,
                    glass.glassBorderBottom
                )
            )
        }

        Row(
            modifier = Modifier
                .widthIn(min = 120.dp, max = 264.dp)
                .height(40.dp)
                .graphicsLayer {
                    scaleX = bounceScale
                    scaleY = bounceScale
                }
                .shadow(
                    elevation = if (glass.isDark) 10.dp else 8.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = if (glass.isDark) 0.42f else 0.14f),
                    spotColor = Color.Black.copy(alpha = if (glass.isDark) 0.42f else 0.14f)
                )
                .clip(CircleShape)
                .background(brush = bgBrush)
                .border(
                    width = 1.dp,
                    brush = borderBrush,
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    onClick = onClick
                )
                .padding(start = 6.dp, end = 12.dp)
                .testTag("luma_identity_pill"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Stationary official Luma Player app icon on the left
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(
                        width = 0.7.dp,
                        color = if (glass.isDark) Color.White.copy(alpha = 0.30f) else Color(0xFFDCE3EE),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_luma_pink_monogram),
                    contentDescription = "Luma Player",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                        .testTag("luma_identity_pill_icon")
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Subtle spring transition only on track title while icon stays completely stationary
            AnimatedContent(
                targetState = track.title,
                transitionSpec = {
                    if (glass.reduceMotion) {
                        fadeIn(tween(80)) togetherWith fadeOut(tween(80))
                    } else {
                        (fadeIn(tween(200)) + slideInVertically(
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = 360f)
                        ) { it / 2 }) togetherWith
                            (fadeOut(tween(130)) + slideOutVertically { -it / 2 })
                    }
                },
                modifier = Modifier.weight(1f, fill = false),
                label = "identityPillTitle"
            ) { titleText ->
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = glass.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .then(
                            if (isPlaying && !glass.reduceMotion) {
                                Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                            } else Modifier
                        )
                        .testTag("luma_identity_pill_title")
                )
            }

            Spacer(modifier = Modifier.width(7.dp))

            Box(
                modifier = Modifier
                    .size(6.dp)
                    .graphicsLayer {
                        scaleX = dotScale
                        scaleY = dotScale
                    }
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}
