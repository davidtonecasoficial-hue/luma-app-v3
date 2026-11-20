package com.example.ui.theme

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.GlassQuality
import com.example.data.model.SurfaceMaterial
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    dynamicAccentTint: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalLumaGlass.current
    val animMs = if (glass.reduceMotion) 0 else 300
    val primaryGlow = if (glass.isMonochrome) glass.accentPrimary else (dynamicAccentTint ?: glass.accentPrimary)

    when (glass.surfaceMaterial) {
        SurfaceMaterial.NEUMORPHIC_BW, SurfaceMaterial.NEUMORPHIC_SOFT -> {
            // Pure matte soft-alabaster/porcelain backdrop with subtle rose-pearl ambient bloom for Soft UI Claro
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                glass.background,
                                glass.backgroundSecondary,
                                glass.background
                            )
                        )
                    )
                    .drawBehind {
                        if (glass.surfaceMaterial == SurfaceMaterial.NEUMORPHIC_SOFT) {
                            val r = size.maxDimension * 0.56f
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        primaryGlow.copy(alpha = if (glass.isDark) 0.11f else 0.065f),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width * 0.82f, size.height * 0.14f),
                                    radius = r
                                ),
                                center = Offset(size.width * 0.82f, size.height * 0.14f),
                                radius = r
                            )
                        }
                    },
                content = content
            )
        }

        SurfaceMaterial.AMOLED_CRISP -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(glass.background),
                content = content
            )
        }

        SurfaceMaterial.LIQUID_GLASS -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                glass.background,
                                glass.backgroundSecondary,
                                glass.background
                            )
                        )
                    )
            ) {
                Crossfade(
                    targetState = glass.isDark,
                    animationSpec = tween(animMs),
                    label = "bgThemeCrossfade"
                ) { dark ->
                    Image(
                        painter = painterResource(
                            id = if (dark) R.drawable.img_liquid_bg_dark else R.drawable.img_liquid_bg_light
                        ),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        colorFilter = if (!dark && !glass.isMonochrome) {
                            ColorFilter.tint(primaryGlow.copy(alpha = 0.20f), BlendMode.SrcAtop)
                        } else null,
                        alpha = when (glass.glassQuality) {
                            GlassQuality.ULTRA -> if (dark) 0.42f else 0.30f
                            GlassQuality.BALANCED -> if (dark) 0.30f else 0.20f
                            GlassQuality.MINIMAL -> if (dark) 0.10f else 0.08f
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val blurScale = glass.backgroundBlurScale
                            val topOrbRadius = size.maxDimension * (0.65f * blurScale.coerceAtLeast(0.6f))
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        primaryGlow.copy(alpha = (0.24f * blurScale).coerceIn(0.05f, 0.42f)),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width * 0.18f, size.height * 0.14f),
                                    radius = topOrbRadius
                                ),
                                center = Offset(size.width * 0.18f, size.height * 0.14f),
                                radius = topOrbRadius
                            )

                            val bottomOrbRadius = size.maxDimension * (0.62f * blurScale.coerceAtLeast(0.6f))
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        glass.accentSecondary.copy(alpha = (0.20f * blurScale).coerceIn(0.05f, 0.38f)),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width * 0.85f, size.height * 0.82f),
                                    radius = bottomOrbRadius
                                ),
                                center = Offset(size.width * 0.85f, size.height * 0.82f),
                                radius = bottomOrbRadius
                            )
                        }
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    glass.background.copy(alpha = if (glass.isDark) 0.22f else 0.22f),
                                    glass.backgroundSecondary.copy(alpha = if (glass.isDark) 0.48f else 0.38f),
                                    glass.background.copy(alpha = if (glass.isDark) 0.86f else 0.64f)
                                )
                            )
                        )
                )

                content()
            }
        }
    }
}

@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    elevated: Boolean = false,
    accentGlow: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalLumaGlass.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (onClick != null && isPressed && !glass.reduceMotion) 0.968f else 1f,
        animationSpec = if (glass.reduceMotion) snap() else spring(dampingRatio = 0.54f, stiffness = 560f),
        label = "surfacePressScale"
    )
    val scaleMod = if (onClick != null && !glass.reduceMotion) {
        Modifier.graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
        }
    } else Modifier

    when (glass.surfaceMaterial) {
        SurfaceMaterial.NEUMORPHIC_BW, SurfaceMaterial.NEUMORPHIC_SOFT -> {
            val fill = if (elevated) glass.surfaceElevated else glass.surface
            val isSoftRose = glass.surfaceMaterial == SurfaceMaterial.NEUMORPHIC_SOFT
            val endBevel = if (isSoftRose && elevated && accentGlow != null && !glass.isDark) {
                lerp(glass.reliefShadowDark.copy(alpha = 0.52f), accentGlow.copy(alpha = 0.28f), 0.24f)
            } else {
                glass.reliefShadowDark.copy(alpha = if (glass.isDark) 0.85f else 0.55f)
            }
            val bevelBrush = Brush.linearGradient(
                colors = listOf(
                    glass.reliefShadowLight,
                    fill,
                    endBevel
                ),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
            val shadowDp = if (glass.isDark) {
                if (elevated) 8.dp else 5.dp
            } else {
                if (elevated) 5.dp else 2.5.dp
            }
            Surface(
                modifier = modifier
                    .then(scaleMod)
                    .shadow(
                        elevation = shadowDp,
                        shape = shape,
                        ambientColor = glass.reliefShadowDark.copy(alpha = if (glass.isDark) 0.55f else 0.34f),
                        spotColor = glass.reliefShadowDark.copy(alpha = if (glass.isDark) 0.70f else 0.42f)
                    )
                    .clip(shape)
                    .then(
                        if (onClick != null) {
                            Modifier.clickable(
                                interactionSource = interactionSource,
                                indication = androidx.compose.material3.ripple(),
                                onClick = onClick
                            )
                        } else Modifier
                    ),
                shape = shape,
                color = fill,
                contentColor = glass.textPrimary,
                border = BorderStroke(1.2.dp, bevelBrush)
            ) {
                Box(
                    modifier = Modifier.background(
                        Brush.linearGradient(
                            colors = listOf(
                                glass.reliefShadowLight.copy(alpha = if (glass.isDark) 0.08f else 0.70f),
                                fill,
                                glass.reliefShadowDark.copy(alpha = if (glass.isDark) 0.12f else 0.045f)
                            )
                        )
                    ),
                    content = content
                )
            }
        }

        SurfaceMaterial.AMOLED_CRISP -> {
            val fill = if (elevated) glass.surfaceElevated else glass.surface
            val borderColor = if (elevated && accentGlow != null) {
                accentGlow.copy(alpha = if (glass.isDark) 0.55f else 0.35f)
            } else {
                glass.glassBorder
            }
            Surface(
                modifier = modifier
                    .then(scaleMod)
                    .shadow(
                        elevation = if (glass.isDark) 0.dp else if (elevated) 3.dp else 1.dp,
                        shape = shape,
                        ambientColor = glass.textPrimary.copy(alpha = 0.08f),
                        spotColor = glass.textPrimary.copy(alpha = 0.10f)
                    )
                    .clip(shape)
                    .then(
                        if (onClick != null) {
                            Modifier.clickable(
                                interactionSource = interactionSource,
                                indication = androidx.compose.material3.ripple(),
                                onClick = onClick
                            )
                        } else Modifier
                    ),
                shape = shape,
                color = fill,
                contentColor = glass.textPrimary,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Box(content = content)
            }
        }

        SurfaceMaterial.LIQUID_GLASS -> {
            val baseFill = if (elevated) glass.glassElevated else glass.glassSurface
            val sheenBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (glass.isDark) 0.11f else 0.30f),
                    Color.Transparent,
                    (accentGlow ?: glass.accentPrimary).copy(alpha = if (glass.isDark) 0.08f else 0.07f)
                )
            )
            val borderBrush = Brush.verticalGradient(
                colors = if (glass.isDark) {
                    listOf(glass.glassBorderTop, glass.glassBorderBottom)
                } else {
                    listOf(
                        Color.White.copy(alpha = 0.94f),
                        lerp(glass.glassBorderBottom, (accentGlow ?: glass.accentPrimary).copy(alpha = 0.26f), 0.38f)
                    )
                }
            )

            val shadowDp: Dp = if (glass.isDark) {
                when (glass.glassQuality) {
                    GlassQuality.MINIMAL -> 1.dp
                    GlassQuality.BALANCED -> if (elevated) 8.dp else 4.dp
                    GlassQuality.ULTRA -> if (elevated) 14.dp else 7.dp
                }
            } else {
                when (glass.glassQuality) {
                    GlassQuality.MINIMAL -> 1.dp
                    GlassQuality.BALANCED -> if (elevated) 5.dp else 2.5.dp
                    GlassQuality.ULTRA -> if (elevated) 8.dp else 4.dp
                }
            }

            val shadowColor = if (glass.isDark) {
                (accentGlow ?: glass.accentPrimary).copy(alpha = 0.20f)
            } else {
                lerp(glass.textPrimary, accentGlow ?: glass.accentPrimary, 0.38f).copy(alpha = 0.14f)
            }

            Surface(
                modifier = modifier
                    .then(scaleMod)
                    .shadow(
                        elevation = shadowDp,
                        shape = shape,
                        ambientColor = shadowColor,
                        spotColor = shadowColor
                    )
                    .clip(shape)
                    .then(
                        if (onClick != null) {
                            Modifier.clickable(
                                interactionSource = interactionSource,
                                indication = androidx.compose.material3.ripple(),
                                onClick = onClick
                            )
                        } else Modifier
                    ),
                shape = shape,
                color = baseFill,
                contentColor = glass.textPrimary,
                border = BorderStroke(1.dp, borderBrush)
            ) {
                Box(
                    modifier = Modifier.background(sheenBrush),
                    content = content
                )
            }
        }
    }
}

@Composable
fun LiquidGlassCapsule(
    modifier: Modifier = Modifier,
    elevated: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalLumaGlass.current
    val shape = RoundedCornerShape(50)

    when (glass.surfaceMaterial) {
        SurfaceMaterial.NEUMORPHIC_BW -> {
            val fill = when {
                elevated -> glass.surfaceElevated
                glass.isDark -> glass.backgroundSecondary
                else -> Color(0xFFE2E8F2)
            }
            val shadowDp = if (glass.isDark) {
                if (elevated) 14.dp else 4.dp
            } else {
                if (elevated) 10.dp else 2.5.dp
            }
            Box(
                modifier = modifier
                    .shadow(
                        elevation = shadowDp,
                        shape = shape,
                        ambientColor = glass.reliefShadowDark.copy(alpha = if (glass.isDark) 0.60f else 0.38f),
                        spotColor = glass.reliefShadowDark.copy(alpha = if (glass.isDark) 0.78f else 0.48f)
                    )
                    .clip(shape)
                    .background(fill)
                    .drawBehind {
                        val r = size.height / 2f
                        val corner = CornerRadius(r, r)
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    glass.reliefShadowLight.copy(alpha = if (glass.isDark) 0.16f else 0.62f),
                                    Color.Transparent,
                                    glass.reliefShadowDark.copy(alpha = if (glass.isDark) 0.24f else 0.14f)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(size.width, size.height)
                            ),
                            cornerRadius = corner
                        )
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    glass.reliefShadowLight,
                                    glass.reliefShadowDark.copy(alpha = if (glass.isDark) 0.90f else 0.68f)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(0f, size.height)
                            ),
                            cornerRadius = corner,
                            style = Stroke(width = 1.3.dp.toPx())
                        )
                    },
                content = content
            )
        }

        SurfaceMaterial.NEUMORPHIC_SOFT -> {
            val opticalFill = when {
                glass.isDark -> (if (elevated) glass.surfaceElevated else glass.backgroundSecondary)
                    .copy(alpha = if (elevated) 0.84f else 0.90f)
                elevated -> Color.White.copy(alpha = 0.84f)
                else -> Color(0xFFF3F6FC).copy(alpha = 0.92f)
            }
            val shadowDp = if (glass.isDark) {
                if (elevated) 22.dp else 8.dp
            } else {
                if (elevated) 16.dp else 5.dp
            }
            val softShadowColor = if (glass.isDark) {
                lerp(Color(0xFF060812), glass.accentPrimary, 0.22f).copy(alpha = 0.66f)
            } else {
                lerp(glass.reliefShadowDark, glass.accentPrimary, 0.28f).copy(alpha = 0.34f)
            }

            Box(
                modifier = modifier
                    .shadow(
                        elevation = shadowDp,
                        shape = shape,
                        ambientColor = softShadowColor,
                        spotColor = softShadowColor
                    )
                    .clip(shape)
                    .background(opticalFill)
                    .drawBehind {
                        val r = size.height / 2f
                        val corner = CornerRadius(r, r)

                        // Translucent crystal body wash with subtle rose refraction
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = if (glass.isDark) {
                                    listOf(
                                        Color.White.copy(alpha = 0.16f),
                                        glass.accentPrimary.copy(alpha = 0.10f),
                                        glass.accentSecondary.copy(alpha = 0.08f),
                                        Color.White.copy(alpha = 0.08f)
                                    )
                                } else {
                                    listOf(
                                        Color.White.copy(alpha = 0.92f),
                                        glass.accentPrimary.copy(alpha = 0.06f),
                                        glass.accentSecondary.copy(alpha = 0.08f),
                                        Color.White.copy(alpha = 0.78f)
                                    )
                                },
                                start = Offset(0f, 0f),
                                end = Offset(size.width, size.height)
                            ),
                            cornerRadius = corner
                        )

                        // Curved upper-lip optical meniscus highlight
                        if (glass.glassQuality != GlassQuality.MINIMAL) {
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    0.0f to Color.White.copy(alpha = if (glass.isDark) 0.30f else 0.98f),
                                    0.34f to Color.White.copy(alpha = if (glass.isDark) 0.07f else 0.28f),
                                    0.58f to Color.Transparent,
                                    1.0f to glass.accentPrimary.copy(alpha = if (glass.isDark) 0.10f else 0.09f)
                                ),
                                cornerRadius = corner
                            )

                            // Specular glass orb glint near upper-right rim
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = if (glass.isDark) 0.22f else 0.76f),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width * 0.82f, size.height * 0.22f),
                                    radius = size.height * 0.92f
                                ),
                                center = Offset(size.width * 0.82f, size.height * 0.22f),
                                radius = size.height * 0.92f
                            )
                        }

                        // Beveled Fresnel crystal rim
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = if (glass.isDark) {
                                    listOf(
                                        Color.White.copy(alpha = 0.56f),
                                        glass.accentPrimary.copy(alpha = 0.26f),
                                        glass.reliefShadowDark.copy(alpha = 0.75f)
                                    )
                                } else {
                                    listOf(
                                        Color.White,
                                        glass.accentSecondary.copy(alpha = 0.24f),
                                        lerp(glass.reliefShadowDark, glass.accentPrimary, 0.24f).copy(alpha = 0.45f)
                                    )
                                },
                                start = Offset(0f, 0f),
                                end = Offset(0f, size.height)
                            ),
                            cornerRadius = corner,
                            style = Stroke(width = 1.4.dp.toPx())
                        )
                    }
                    .border(
                        width = 0.6.dp,
                        color = if (glass.isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.85f),
                        shape = shape
                    ),
                content = content
            )
        }

        SurfaceMaterial.AMOLED_CRISP -> {
            val fill = when {
                elevated -> glass.surfaceElevated
                glass.isDark -> glass.surface
                else -> Color(0xFFEDF2F7)
            }
            Box(
                modifier = modifier
                    .shadow(
                        elevation = if (glass.isDark) 0.dp else if (elevated) 8.dp else 2.dp,
                        shape = shape,
                        ambientColor = glass.textPrimary.copy(alpha = 0.12f),
                        spotColor = glass.textPrimary.copy(alpha = 0.16f)
                    )
                    .clip(shape)
                    .background(fill)
                    .border(
                        width = 1.15.dp,
                        color = glass.glassBorder,
                        shape = shape
                    ),
                content = content
            )
        }

        SurfaceMaterial.LIQUID_GLASS -> {
            val lightFrostCapsule = lerp(
                Color.White,
                glass.backgroundSecondary,
                if (elevated) 0.12f else 0.28f
            )
            val opticalBase = when {
                glass.glassQuality == GlassQuality.MINIMAL ->
                    if (elevated) glass.glassElevated else glass.glassSurface
                glass.isDark ->
                    if (elevated) glass.surfaceElevated.copy(alpha = 0.36f) else glass.surface.copy(alpha = 0.30f)
                else ->
                    lightFrostCapsule.copy(alpha = if (elevated) 0.52f else 0.64f)
            }

            val shadowColor = if (glass.isDark) {
                lerp(Color(0xFF01040B), glass.accentPrimary, 0.16f).copy(alpha = 0.68f)
            } else {
                lerp(glass.textPrimary, glass.accentPrimary, 0.42f).copy(alpha = 0.22f)
            }

            val shadowDp = if (glass.isDark) {
                when (glass.glassQuality) {
                    GlassQuality.MINIMAL -> if (elevated) 6.dp else 3.dp
                    GlassQuality.BALANCED -> if (elevated) 18.dp else 10.dp
                    GlassQuality.ULTRA -> if (elevated) 26.dp else 14.dp
                }
            } else {
                when (glass.glassQuality) {
                    GlassQuality.MINIMAL -> if (elevated) 3.dp else 1.5.dp
                    GlassQuality.BALANCED -> if (elevated) 10.dp else 4.dp
                    GlassQuality.ULTRA -> if (elevated) 16.dp else 6.dp
                }
            }

            Box(
                modifier = modifier
                    .shadow(
                        elevation = shadowDp,
                        shape = shape,
                        ambientColor = shadowColor,
                        spotColor = shadowColor
                    )
                    .clip(shape)
                    .background(opticalBase)
                    .drawBehind {
                        val r = size.height / 2f
                        val corner = CornerRadius(r, r)

                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = if (glass.isDark) {
                                    listOf(
                                        Color.White.copy(alpha = 0.18f),
                                        Color.White.copy(alpha = 0.04f),
                                        glass.accentSecondary.copy(alpha = 0.15f),
                                        Color.White.copy(alpha = 0.12f)
                                    )
                                } else {
                                    listOf(
                                        Color.White.copy(alpha = 0.28f),
                                        glass.accentPrimary.copy(alpha = 0.08f),
                                        glass.accentSecondary.copy(alpha = 0.08f),
                                        Color.White.copy(alpha = 0.16f)
                                    )
                                },
                                start = Offset(0f, 0f),
                                end = Offset(size.width, size.height)
                            ),
                            cornerRadius = corner
                        )

                        if (glass.glassQuality != GlassQuality.MINIMAL) {
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    0.0f to Color.White.copy(alpha = if (glass.isDark) 0.30f else 0.76f),
                                    0.28f to Color.White.copy(alpha = if (glass.isDark) 0.07f else 0.14f),
                                    0.54f to Color.Transparent,
                                    1.0f to glass.accentPrimary.copy(alpha = if (glass.isDark) 0.09f else 0.10f)
                                ),
                                cornerRadius = corner
                            )

                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = if (glass.isDark) 0.24f else 0.45f),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width * 0.82f, size.height * 0.22f),
                                    radius = size.height * 0.92f
                                ),
                                center = Offset(size.width * 0.82f, size.height * 0.22f),
                                radius = size.height * 0.92f
                            )
                        }

                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = if (glass.isDark) {
                                    listOf(
                                        Color.White.copy(alpha = 0.62f),
                                        Color.White.copy(alpha = 0.16f),
                                        glass.accentSecondary.copy(alpha = 0.34f),
                                        Color.White.copy(alpha = 0.30f)
                                    )
                                } else {
                                    listOf(
                                        Color.White,
                                        glass.accentPrimary.copy(alpha = 0.24f),
                                        glass.textPrimary.copy(alpha = 0.14f),
                                        Color.White.copy(alpha = 0.92f)
                                    )
                                },
                                start = Offset(0f, 0f),
                                end = Offset(0f, size.height)
                            ),
                            cornerRadius = corner,
                            style = Stroke(width = 1.35.dp.toPx())
                        )
                    }
                    .border(
                        width = 0.6.dp,
                        color = if (glass.isDark) Color.White.copy(alpha = 0.12f) else glass.textPrimary.copy(alpha = 0.08f),
                        shape = shape
                    ),
                content = content
            )
        }
    }
}

/**
 * Circular soft-sculpted medallion button used for the B&W Neumorphic aesthetic
 * and cleanly adapted to AMOLED and Liquid Glass styles.
 */
@Composable
fun SculptedCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalLumaGlass.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && !glass.reduceMotion) 0.89f else 1f,
        animationSpec = if (glass.reduceMotion) snap() else spring(dampingRatio = 0.48f, stiffness = 540f),
        label = "sculptedBtnScale"
    )
    val fill = when (glass.surfaceMaterial) {
        SurfaceMaterial.NEUMORPHIC_BW, SurfaceMaterial.NEUMORPHIC_SOFT -> if (emphasized) glass.surfaceElevated else glass.surface
        SurfaceMaterial.AMOLED_CRISP -> if (emphasized) glass.accentPrimary else glass.surfaceElevated
        SurfaceMaterial.LIQUID_GLASS -> if (emphasized) glass.accentPrimary else glass.glassElevated
    }
    val isSoftRose = glass.surfaceMaterial == SurfaceMaterial.NEUMORPHIC_SOFT

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .shadow(
                elevation = if (glass.isNeumorphic) {
                    if (emphasized) 10.dp else 6.dp
                } else if (emphasized) 10.dp else 4.dp,
                shape = CircleShape,
                ambientColor = if (isSoftRose && emphasized) glass.accentPrimary.copy(alpha = 0.35f) else glass.reliefShadowDark.copy(alpha = 0.65f),
                spotColor = if (isSoftRose && emphasized) glass.accentPrimary.copy(alpha = 0.40f) else glass.reliefShadowDark.copy(alpha = 0.80f)
            )
            .clip(CircleShape)
            .background(fill)
            .drawBehind {
                if (glass.isNeumorphic) {
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                glass.reliefShadowLight,
                                glass.reliefShadowDark.copy(alpha = if (glass.isDark) 0.90f else 0.70f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height)
                        ),
                        style = Stroke(width = if (emphasized) 2.dp.toPx() else 1.4.dp.toPx())
                    )
                    if (emphasized) {
                        // Subtle inner embossed ring (rose-pink tinted for Soft UI Claro, monochrome for B&W)
                        drawCircle(
                            brush = Brush.linearGradient(
                                colors = if (isSoftRose) {
                                    listOf(
                                        glass.accentPrimary.copy(alpha = 0.65f),
                                        glass.accentSecondary.copy(alpha = 0.45f)
                                    )
                                } else {
                                    listOf(
                                        glass.reliefShadowDark.copy(alpha = 0.25f),
                                        glass.reliefShadowLight.copy(alpha = 0.70f)
                                    )
                                },
                                start = Offset(0f, 0f),
                                end = Offset(size.width, size.height)
                            ),
                            radius = size.minDimension * 0.41f,
                            style = Stroke(width = if (isSoftRose) 1.35.dp.toPx() else 1.dp.toPx())
                        )
                    }
                } else {
                    drawCircle(
                        color = glass.glassBorder,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/**
 * Staggered spring entrance wrapper for sections across Home, Library, and Settings.
 * Instant and deterministic when reduceMotion is active.
 */
@Composable
fun LumaSectionEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val glass = LocalLumaGlass.current
    if (glass.reduceMotion) {
        Box(modifier = modifier) { content() }
        return
    }
    val progress = remember { Animatable(0f) }
    val density = LocalDensity.current
    val shiftPx = with(density) { 14.dp.toPx() }

    LaunchedEffect(Unit) {
        val delayMs = (index.coerceIn(0, 6) * 32L)
        if (delayMs > 0L) kotlinx.coroutines.delay(delayMs)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f)
        )
    }

    Box(
        modifier = modifier.graphicsLayer {
            val v = progress.value.coerceIn(0f, 1.1f)
            alpha = v.coerceIn(0f, 1f)
            translationY = (1f - v) * shiftPx
            val s = 0.985f + 0.015f * v
            scaleX = s
            scaleY = s
        }
    ) {
        content()
    }
}

/**
 * Sophisticated fluid ON/OFF switcher with animated "ON"/"OFF" labels inside the track,
 * elastic pill stretch during transit, spring overshoot bounce, and animated thumb icon.
 */
@Composable
fun LumaAnimatedSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    val glass = LocalLumaGlass.current
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val thumbProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = if (glass.reduceMotion) {
            snap()
        } else {
            spring(dampingRatio = 0.54f, stiffness = 420f)
        },
        label = "switchThumbProgress"
    )
    val clampedProgress = thumbProgress.coerceIn(0f, 1f)
    val transitStretch = if (glass.reduceMotion) 0f else sin(clampedProgress * PI).toFloat()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && !glass.reduceMotion) 0.94f else 1f,
        animationSpec = if (glass.reduceMotion) snap() else spring(dampingRatio = 0.55f, stiffness = 550f),
        label = "switchPressScale"
    )

    val offTrackColor = when (glass.surfaceMaterial) {
        SurfaceMaterial.NEUMORPHIC_BW -> if (glass.isDark) Color(0xFF14161B) else Color(0xFFD9E1ED)
        SurfaceMaterial.NEUMORPHIC_SOFT -> if (glass.isDark) Color(0xFF15171F) else Color(0xFFE1E6F2)
        SurfaceMaterial.AMOLED_CRISP -> if (glass.isDark) Color(0xFF121620) else Color(0xFFE5ECF4)
        SurfaceMaterial.LIQUID_GLASS -> if (glass.isDark) glass.surfaceElevated.copy(alpha = 0.78f) else lerp(glass.backgroundSecondary, glass.textPrimary, 0.09f)
    }
    val trackColor by animateColorAsState(
        targetValue = if (checked) glass.accentPrimary else offTrackColor,
        animationSpec = if (glass.reduceMotion) snap() else tween(220, easing = FastOutSlowInEasing),
        label = "switchTrackColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) {
            if (glass.isMonochrome) glass.accentPrimary else Color.White.copy(alpha = if (glass.isDark) 0.42f else 0.65f)
        } else {
            glass.glassBorder
        },
        animationSpec = if (glass.reduceMotion) snap() else tween(200),
        label = "switchBorderColor"
    )

    val thumbFillColor = when {
        glass.isMonochrome && glass.isDark && checked -> NeuBwInkBlack
        glass.isMonochrome && glass.isDark -> Color(0xFFE5E9F0)
        else -> Color.White
    }
    val thumbIconColor = when {
        glass.isMonochrome && glass.isDark && checked -> Color.White
        else -> glass.accentPrimary
    }

    val trackWidth = 66.dp
    val trackHeight = 34.dp
    val baseThumbSize = 26.dp
    val thumbWidth = baseThumbSize + (9.dp * transitStretch)
    val thumbPad = 4.dp
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .toggleable(
                value = checked,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = { next ->
                    if (glass.hapticsEnabled) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onCheckedChange(next)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .size(width = trackWidth, height = trackHeight)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .clip(RoundedCornerShape(50))
                .background(trackColor)
                .drawBehind {
                    val r = size.height / 2f
                    val corner = CornerRadius(r, r)
                    if (checked && !glass.isMonochrome && glass.surfaceMaterial == SurfaceMaterial.LIQUID_GLASS) {
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    glass.accentPrimary,
                                    glass.accentSecondary
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(size.width, size.height)
                            ),
                            cornerRadius = corner,
                            alpha = clampedProgress
                        )
                    }
                    // Specular top caustic sheen inside the switch track
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            0.0f to Color.White.copy(alpha = if (checked) 0.28f else if (glass.isDark) 0.14f else 0.55f),
                            0.45f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = if (glass.isDark) 0.20f else 0.06f)
                        ),
                        cornerRadius = corner
                    )
                }
                .border(1.1.dp, borderColor, RoundedCornerShape(50))
        ) {
            // "ON" label revealed on the left side when checked
            val onAlpha = ((thumbProgress - 0.12f) / 0.72f).coerceIn(0f, 1f)
            if (onAlpha > 0.01f) {
                Text(
                    text = "ON",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp
                    ),
                    color = glass.onAccent,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 10.dp)
                        .graphicsLayer {
                            alpha = onAlpha
                            translationX = (-6f + 6f * onAlpha)
                            val s = 0.85f + 0.15f * onAlpha
                            scaleX = s
                            scaleY = s
                        }
                )
            }

            // "OFF" label revealed on the right side when unchecked
            val offAlpha = ((0.88f - thumbProgress) / 0.72f).coerceIn(0f, 1f)
            if (offAlpha > 0.01f) {
                Text(
                    text = "OFF",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = glass.textSecondary,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 9.dp)
                        .graphicsLayer {
                            alpha = offAlpha
                            translationX = (6f - 6f * offAlpha)
                            val s = 0.85f + 0.15f * offAlpha
                            scaleX = s
                            scaleY = s
                        }
                )
            }

            // Elastic bouncing thumb orb with animated check/off micro-mark
            val travelPx = with(density) { (maxWidth - thumbPad * 2 - thumbWidth).toPx().coerceAtLeast(0f) }
            val padPx = with(density) { thumbPad.toPx() }
            val offsetX = padPx + (travelPx * thumbProgress)

            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset(offsetX.roundToInt(), 0) }
                    .size(width = thumbWidth, height = baseThumbSize)
                    .shadow(
                        elevation = if (glass.isDark) 4.dp else 3.dp,
                        shape = RoundedCornerShape(50),
                        ambientColor = Color.Black.copy(alpha = 0.25f),
                        spotColor = Color.Black.copy(alpha = 0.30f)
                    )
                    .clip(RoundedCornerShape(50))
                    .background(thumbFillColor)
                    .border(
                        width = 0.8.dp,
                        color = if (glass.isMonochrome && glass.isDark && checked) {
                            Color.White.copy(alpha = 0.22f)
                        } else {
                            Color.White.copy(alpha = 0.9f)
                        },
                        shape = RoundedCornerShape(50)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (clampedProgress >= 0.5f) {
                    val checkScale = ((clampedProgress - 0.5f) * 2f).coerceIn(0f, 1f)
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = thumbIconColor,
                        modifier = Modifier
                            .size(14.dp)
                            .graphicsLayer {
                                scaleX = 0.5f + 0.5f * checkScale
                                scaleY = 0.5f + 0.5f * checkScale
                                rotationZ = (1f - checkScale) * -40f
                                alpha = checkScale
                            }
                    )
                } else {
                    val offDotAlpha = (1f - clampedProgress * 2f).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .alpha(offDotAlpha)
                            .border(1.6.dp, glass.textSecondary.copy(alpha = 0.65f), CircleShape)
                    )
                }
            }
        }
    }
}

/**
 * Helper composable returning a spring-bouncing scale when pressed.
 */
@Composable
fun rememberLumaBounceScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.88f
): Float {
    val glass = LocalLumaGlass.current
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !glass.reduceMotion) pressedScale else 1f,
        animationSpec = if (glass.reduceMotion) snap() else spring(dampingRatio = 0.48f, stiffness = 560f),
        label = "lumaBounceScale"
    )
    return scale
}

@Composable
fun resolveArtworkShape(
    artworkShape: com.example.data.model.ArtworkShape = LocalLumaGlass.current.artworkShape,
    defaultCornerRadius: Dp = 14.dp
): Shape {
    return when (artworkShape) {
        com.example.data.model.ArtworkShape.ROUNDED -> RoundedCornerShape(defaultCornerRadius)
        com.example.data.model.ArtworkShape.SQUIRCLE -> RoundedCornerShape((defaultCornerRadius * 1.65f).coerceAtLeast(18.dp))
        com.example.data.model.ArtworkShape.CIRCLE -> CircleShape
        com.example.data.model.ArtworkShape.SHARP -> RoundedCornerShape(4.dp)
    }
}

@Composable
fun MediaArtwork(
    artworkPath: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    shapeOverride: Shape? = null
) {
    val context = LocalContext.current
    val shape = shapeOverride ?: resolveArtworkShape(defaultCornerRadius = cornerRadius)
    val imageModel: Any? = when {
        artworkPath.isNullOrBlank() -> null
        artworkPath.startsWith("content://") || artworkPath.startsWith("file://") -> artworkPath
        else -> File(artworkPath).takeIf { it.exists() && it.length() > 0L }
    }

    if (imageModel != null) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageModel)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            error = painterResource(id = R.drawable.img_default_album_art),
            placeholder = painterResource(id = R.drawable.img_default_album_art),
            modifier = modifier.clip(shape)
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.img_default_album_art),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(shape)
        )
    }
}

@Composable
fun LumaDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = LocalLumaGlass.current
    val menuBg = when (glass.surfaceMaterial) {
        SurfaceMaterial.NEUMORPHIC_BW, SurfaceMaterial.NEUMORPHIC_SOFT -> glass.surfaceElevated
        SurfaceMaterial.AMOLED_CRISP -> glass.surfaceElevated
        SurfaceMaterial.LIQUID_GLASS -> if (glass.isDark) {
            glass.surfaceElevated
        } else {
            lerp(Color.White, glass.backgroundSecondary, 0.18f)
        }
    }
    val borderStroke = BorderStroke(
        width = 1.dp,
        color = when {
            glass.isNeumorphic && !glass.isDark ->
                glass.reliefShadowDark.copy(alpha = 0.55f)
            glass.isDark -> glass.glassBorderTop
            else -> lerp(glass.textPrimary, glass.accentPrimary, 0.30f).copy(alpha = 0.16f)
        }
    )
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        containerColor = menuBg,
        tonalElevation = 0.dp,
        shadowElevation = if (glass.isDark) 10.dp else 5.dp,
        border = borderStroke,
        content = content
    )
}

@Composable
fun lumaMenuItemColors(): MenuItemColors {
    val glass = LocalLumaGlass.current
    return MenuDefaults.itemColors(
        textColor = glass.textPrimary,
        leadingIconColor = if (glass.isMonochrome) glass.textPrimary else glass.accentPrimary,
        trailingIconColor = glass.accentPrimary
    )
}

fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "—"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.getDefault(), "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
        else -> String.format(Locale.getDefault(), "%.0f KB", kb)
    }
}
