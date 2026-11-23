package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.OfflineBolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SoftPinkPrimary
import com.example.ui.theme.SoftPinkSecondary
import com.example.ui.theme.SoftWhiteInk
import com.example.ui.theme.SoftWhiteInkSecondary
import com.example.ui.theme.SoftWhiteLightBg
import com.example.ui.theme.SoftWhiteLightBgSecondary
import com.example.ui.theme.SoftWhiteReliefShadow
import com.example.ui.theme.rememberLumaBounceScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Sophisticated animated monogram logo containing ONLY the signature pink "L"
 * sculpted onto a classic pure white (#FFFFFF) Soft-UI medallion.
 */
@Composable
fun AnimatedLumaMonogramLogo(
    modifier: Modifier = Modifier,
    medallionSize: Dp = 128.dp,
    reduceMotion: Boolean = false
) {
    val drawProgress = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val medallionScale = remember { Animatable(if (reduceMotion) 1f else 0.78f) }

    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            drawProgress.snapTo(1f)
            medallionScale.snapTo(1f)
        } else {
            launch {
                medallionScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.56f, stiffness = 320f)
                )
            }
            delay(110L)
            drawProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 920, easing = FastOutSlowInEasing)
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "lumaLogoBreath")
    val breath by if (reduceMotion) {
        remember { androidx.compose.runtime.mutableFloatStateOf(0.5f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "logoHaloBreath"
        )
    }

    val sheenPhase by if (reduceMotion) {
        remember { androidx.compose.runtime.mutableFloatStateOf(0.4f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = -0.25f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "logoPinkSheen"
        )
    }

    val pathMeasure = remember { PathMeasure() }
    val letterPath = remember { Path() }
    val revealPath = remember { Path() }

    Box(
        modifier = modifier
            .size(medallionSize)
            .semantics { contentDescription = "Logótipo L" }
            .graphicsLayer {
                val pulse = if (reduceMotion) 1f else 1f + 0.022f * breath
                scaleX = medallionScale.value * pulse
                scaleY = medallionScale.value * pulse
            },
        contentAlignment = Alignment.Center
    ) {
        // Soft breathing pink & white halo behind the pure white Soft-UI medallion
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.minDimension <= 1f) return@Canvas
            val haloAlpha = 0.14f + 0.10f * breath
            val haloRadius = (size.minDimension * 0.62f).coerceAtLeast(1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SoftPinkPrimary.copy(alpha = haloAlpha),
                        SoftPinkSecondary.copy(alpha = haloAlpha * 0.45f),
                        Color.Transparent
                    ),
                    radius = haloRadius
                ),
                radius = haloRadius
            )
        }

        // Sculpted classic pure white (#FFFFFF) Soft-UI cushion holding ONLY the pink "L"
        Box(
            modifier = Modifier
                .fillMaxSize(0.84f)
                .shadow(
                    elevation = 14.dp,
                    shape = CircleShape,
                    ambientColor = SoftWhiteReliefShadow.copy(alpha = 0.72f),
                    spotColor = SoftPinkPrimary.copy(alpha = 0.22f)
                )
                .clip(CircleShape)
                .background(Color.White)
                .drawBehind {
                    // Upper-left crisp white highlight bevel & lower-right pale relief shadow
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White,
                                Color(0xFFF9FAFD),
                                SoftWhiteReliefShadow.copy(alpha = 0.48f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height)
                        ),
                        style = Stroke(width = 2.2.dp.toPx())
                    )
                    // Delicate pink hairline inner ring
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                SoftPinkSecondary.copy(alpha = 0.34f + 0.16f * breath),
                                Color.White.copy(alpha = 0.85f),
                                SoftPinkPrimary.copy(alpha = 0.28f + 0.14f * breath)
                            )
                        ),
                        radius = size.minDimension * 0.43f,
                        style = Stroke(width = 1.dp.toPx())
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize(0.66f)
                    .testTag("animated_luma_pink_l")
            ) {
                if (size.minDimension <= 1f) return@Canvas
                val w = size.width
                val h = size.height

                // Sculpted calligraphic editorial "L" curve (resembling the app icon monogram)
                letterPath.reset()
                // Upper curved serif entry sweeping into the inclined spine
                letterPath.moveTo(w * 0.45f, h * 0.23f)
                letterPath.cubicTo(
                    w * 0.26f, h * 0.23f,
                    w * 0.22f, h * 0.47f,
                    w * 0.44f, h * 0.48f
                )
                // Ascent to apex loop
                letterPath.cubicTo(
                    w * 0.67f, h * 0.49f,
                    w * 0.76f, h * 0.20f,
                    w * 0.64f, h * 0.16f
                )
                letterPath.cubicTo(
                    w * 0.54f, h * 0.13f,
                    w * 0.46f, h * 0.36f,
                    w * 0.42f, h * 0.56f
                )
                // Downward stroke into base calligraphic curl + sweeping foot
                letterPath.cubicTo(
                    w * 0.39f, h * 0.72f,
                    w * 0.32f, h * 0.82f,
                    w * 0.24f, h * 0.80f
                )
                letterPath.cubicTo(
                    w * 0.17f, h * 0.78f,
                    w * 0.19f, h * 0.64f,
                    w * 0.31f, h * 0.66f
                )
                letterPath.cubicTo(
                    w * 0.43f, h * 0.68f,
                    w * 0.55f, h * 0.81f,
                    w * 0.72f, h * 0.79f
                )
                letterPath.cubicTo(
                    w * 0.80f, h * 0.78f,
                    w * 0.83f, h * 0.71f,
                    w * 0.83f, h * 0.65f
                )

                pathMeasure.setPath(letterPath, false)
                val totalLength = pathMeasure.length
                val trimEnd = totalLength * drawProgress.value.coerceIn(0f, 1f)
                revealPath.reset()
                pathMeasure.getSegment(0f, trimEnd, revealPath, true)

                val mainStroke = (w * 0.088f).coerceAtLeast(3.5f)
                val sheenOffset = w * sheenPhase

                // Soft embossed pink shadow beneath the "L"
                drawPath(
                    path = revealPath,
                    color = SoftPinkPrimary.copy(alpha = 0.22f),
                    style = Stroke(
                        width = mainStroke * 1.25f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Signature luminous pink body of the "L"
                drawPath(
                    path = revealPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFF472B6),
                            SoftPinkPrimary,
                            Color(0xFFDB2777),
                            SoftPinkPrimary
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    ),
                    style = Stroke(
                        width = mainStroke,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Travelling silk specular highlight inside the pink stroke
                if (!reduceMotion && drawProgress.value > 0.75f) {
                    drawPath(
                        path = revealPath,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.68f),
                                Color.Transparent
                            ),
                            start = Offset(sheenOffset - w * 0.22f, 0f),
                            end = Offset(sheenOffset + w * 0.22f, h)
                        ),
                        style = Stroke(
                            width = mainStroke * 0.34f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}

/**
 * First-time Welcome Intro welcoming the listener with poetic copy and an
 * interactive color/theme selector — NO entrance icon logo, and automatically
 * adapts to Dark or Light mode so dark-mode users are never blinded by white.
 */
@Composable
fun LumaWelcomeIntroOverlay(
    selectedThemeStyle: com.example.data.model.AppThemeStyle,
    reduceMotion: Boolean,
    hapticsEnabled: Boolean,
    onSelectThemeStyle: (com.example.data.model.AppThemeStyle) -> Unit,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onEnter)

    val glass = com.example.ui.theme.LocalLumaGlass.current
    val haptics = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    val eyebrowAnim = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val headlineAnim = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val bodyAnim = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val paletteAnim = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val pillarsAnim = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val actionsAnim = remember { Animatable(if (reduceMotion) 1f else 0f) }

    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            eyebrowAnim.snapTo(1f)
            headlineAnim.snapTo(1f)
            bodyAnim.snapTo(1f)
            paletteAnim.snapTo(1f)
            pillarsAnim.snapTo(1f)
            actionsAnim.snapTo(1f)
        } else {
            delay(80L)
            launch { eyebrowAnim.animateTo(1f, spring(dampingRatio = 0.80f, stiffness = 360f)) }
            delay(90L)
            launch { headlineAnim.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 320f)) }
            delay(90L)
            launch { bodyAnim.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = 320f)) }
            delay(90L)
            launch { paletteAnim.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 320f)) }
            delay(90L)
            launch { pillarsAnim.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 320f)) }
            delay(90L)
            launch { actionsAnim.animateTo(1f, spring(dampingRatio = 0.74f, stiffness = 320f)) }
        }
    }

    val bgGradient = listOf(
        glass.background,
        glass.backgroundSecondary,
        glass.background
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = bgGradient))
            .drawBehind {
                if (size.minDimension <= 1f) return@drawBehind
                val rTop = (size.maxDimension * 0.50f).coerceAtLeast(1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glass.accentPrimary.copy(alpha = if (glass.isDark) 0.18f else 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.22f),
                        radius = rTop
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.22f),
                    radius = rTop
                )
                val rBottom = (size.maxDimension * 0.44f).coerceAtLeast(1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glass.accentSecondary.copy(alpha = if (glass.isDark) 0.14f else 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.82f, size.height * 0.78f),
                        radius = rBottom
                    ),
                    center = Offset(size.width * 0.82f, size.height * 0.78f),
                    radius = rBottom
                )
            }
            .testTag("welcome_intro_overlay")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sculpted Eyebrow Pill (No entrance logo icon)
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = eyebrowAnim.value.coerceIn(0f, 1f)
                            translationY = (1f - eyebrowAnim.value) * 20f
                        }
                        .clip(RoundedCornerShape(50))
                        .background(glass.glassElevated)
                        .border(1.dp, glass.accentPrimary.copy(alpha = 0.34f), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(glass.accentPrimary)
                        )
                        Text(
                            text = "PRIMEIRA EXPERIÊNCIA",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.6.sp
                            ),
                            color = glass.accentPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Magnificent Poetic Welcome Headline
                Text(
                    text = buildAnnotatedString {
                        append("Onde cada som encontra ")
                        withStyle(
                            SpanStyle(
                                color = glass.accentPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        ) {
                            append("a sua luz.")
                        }
                    },
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 31.sp,
                        lineHeight = 38.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = glass.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = headlineAnim.value.coerceIn(0f, 1f)
                            translationY = (1f - headlineAnim.value) * 28f
                        }
                        .testTag("welcome_intro_headline")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Magnificent Poetic Subtitle Copy
                Text(
                    text = "Um santuário sonoro desenhado para sentir cada nota em estado puro. Silêncio, relevo tátil e vidro líquido envolvem a tua coleção de música e vídeo — 100% offline, sereno e eternamente teu.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 24.sp
                    ),
                    color = glass.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .graphicsLayer {
                            alpha = bodyAnim.value.coerceIn(0f, 1f)
                            translationY = (1f - bodyAnim.value) * 24f
                        }
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Interactive Color & Theme Picker for First-Time Players
                com.example.ui.theme.LiquidGlassSurface(
                    elevated = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = paletteAnim.value.coerceIn(0f, 1f)
                            translationY = (1f - paletteAnim.value) * 24f
                        }
                        .testTag("welcome_color_picker_card")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Escolhe a tua cor e atmosfera",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = glass.textPrimary
                            )
                            Text(
                                text = "Adapta-se automaticamente ao modo ${if (glass.isDark) "Escuro" else "Claro"} do sistema",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary
                            )
                        }

                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(com.example.data.model.AppThemeStyle.entries.size) { index ->
                                val style = com.example.data.model.AppThemeStyle.entries[index]
                                val preview = com.example.ui.theme.themePreviewColors(style, isDark = glass.isDark)
                                val isSelected = style == selectedThemeStyle

                                Surface(
                                    onClick = {
                                        if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectThemeStyle(style)
                                    },
                                    shape = RoundedCornerShape(18.dp),
                                    color = preview.surface,
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) preview.accent else preview.border
                                    ),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("welcome_theme_option_${style.name.lowercase()}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(preview.accent, preview.accentSecondary)
                                                    )
                                                )
                                                .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                                        )
                                        Text(
                                            text = style.title,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = preview.text
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Three Sculpted Pillar Cushions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = pillarsAnim.value.coerceIn(0f, 1f)
                            translationY = (1f - pillarsAnim.value) * 22f
                        },
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WelcomeSoftPillarCard(
                        icon = Icons.Rounded.Headphones,
                        title = "Som Puro",
                        subtitle = "Áudio sem perdas",
                        modifier = Modifier.weight(1f)
                    )
                    WelcomeSoftPillarCard(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Espectro Vivo",
                        subtitle = "5 visualizadores",
                        modifier = Modifier.weight(1f)
                    )
                    WelcomeSoftPillarCard(
                        icon = Icons.Rounded.OfflineBolt,
                        title = "100% Teu",
                        subtitle = "Leitura offline",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Primary Accent Sculpted CTA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp)
                    .graphicsLayer {
                        alpha = actionsAnim.value.coerceIn(0f, 1f)
                        translationY = (1f - actionsAnim.value) * 24f
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val enterInteraction = remember { MutableInteractionSource() }
                val enterScale = rememberLumaBounceScale(enterInteraction, pressedScale = 0.95f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .height(56.dp)
                        .graphicsLayer {
                            scaleX = enterScale
                            scaleY = enterScale
                        }
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = glass.accentPrimary.copy(alpha = 0.42f),
                            spotColor = glass.accentPrimary.copy(alpha = 0.48f)
                        )
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(glass.accentPrimary, glass.accentSecondary)
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.40f),
                            shape = RoundedCornerShape(28.dp)
                        )
                        .clickable(
                            interactionSource = enterInteraction,
                            indication = androidx.compose.material3.ripple(color = glass.onAccent),
                            onClick = {
                                if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onEnter()
                            }
                        )
                        .testTag("welcome_intro_enter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = glass.onAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Entrar e Ouvir",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = glass.onAccent
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = glass.onAccent,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Returning-user smooth non-blocking "Bem-vindo de volta" transition banner
 * straight into Home (no icon logo, adapts smoothly to dark or light mode).
 */
@Composable
fun WelcomeBackBanner(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = com.example.ui.theme.LocalLumaGlass.current
    com.example.ui.theme.LiquidGlassSurface(
        elevated = true,
        shape = RoundedCornerShape(28.dp),
        accentGlow = glass.accentPrimary,
        onClick = onDismiss,
        modifier = modifier
            .widthIn(max = 380.dp)
            .testTag("welcome_back_banner")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(glass.accentPrimary)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bem-vindo de volta",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = glass.textPrimary
                )
                Text(
                    text = "A tua música está pronta para tocar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.textSecondary
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = "Ir para Início",
                tint = glass.accentPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun WelcomeSoftPillarCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val glass = com.example.ui.theme.LocalLumaGlass.current
    com.example.ui.theme.LiquidGlassSurface(
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(glass.accentPrimary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = glass.accentPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = glass.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = glass.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
