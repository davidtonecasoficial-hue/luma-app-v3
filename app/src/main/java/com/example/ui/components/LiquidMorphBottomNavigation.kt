package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SearchFilter
import com.example.data.model.SurfaceMaterial
import com.example.ui.theme.LiquidGlassCapsule
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LocalLumaGlass
import com.example.ui.viewmodel.BottomNavTab
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun LiquidMorphBottomNavigation(
    selectedTab: BottomNavTab,
    isSearchActive: Boolean,
    searchQuery: String,
    searchFilter: SearchFilter,
    isFilterMenuOpen: Boolean,
    onSelectTab: (BottomNavTab) -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleFilterMenu: () -> Unit,
    onSelectFilter: (SearchFilter) -> Unit,
    modifier: Modifier = Modifier,
    showLabels: Boolean = false
) {
    val glass = LocalLumaGlass.current
    val strings = com.example.ui.theme.LocalLumaStrings.current
    val haptics = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    val reduceMotion = glass.reduceMotion
    val morphDuration = if (reduceMotion) 110 else 250

    val morphProgress by animateFloatAsState(
        targetValue = if (isSearchActive) 1f else 0f,
        animationSpec = tween(durationMillis = morphDuration, easing = FastOutSlowInEasing),
        label = "bottomMorphProgress"
    )

    val searchAlpha = ((morphProgress - 0.12f) / 0.88f).coerceIn(0f, 1f)
    val isSearchFieldMounted = searchAlpha > 0.01f

    LaunchedEffect(isSearchActive, isSearchFieldMounted) {
        if (isSearchActive && isSearchFieldMounted) {
            kotlinx.coroutines.delay(40)
            runCatching {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        } else if (!isSearchActive) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    if (isSearchActive) {
        BackHandler {
            keyboardController?.hide()
            focusManager.clearFocus()
            onCloseSearch()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 580.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Auxiliary Filter Panel anchored above the left circular filter button
        AnimatedVisibility(
            visible = isSearchActive && isFilterMenuOpen,
            enter = if (reduceMotion) fadeIn(tween(100)) else fadeIn(tween(180)) + scaleIn(
                initialScale = 0.9f,
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            ),
            exit = if (reduceMotion) fadeOut(tween(90)) else fadeOut(tween(150)) + scaleOut(
                targetScale = 0.92f,
                animationSpec = tween(150)
            ),
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            SearchFilterPanel(
                selectedFilter = searchFilter,
                onSelectFilter = { filter ->
                    if (glass.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelectFilter(filter)
                }
            )
        }

        // Single Unified Morphing Bottom Container (no stacked/duplicate bars)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            contentAlignment = Alignment.Center
        ) {
            // STATE A: Normal Navigation Capsule + Separated Circular Search Button
            val navAlpha = (1f - morphProgress * 1.4f).coerceIn(0f, 1f)
            val navShiftPx = with(LocalDensity.current) { (if (reduceMotion) 0.dp else (-14).dp * morphProgress).toPx() }

            if (navAlpha > 0.01f) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = navAlpha
                            translationX = navShiftPx
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Main Elevated Liquid Glass Capsule containing Início, Biblioteca, Ajustes
                    LiquidGlassCapsule(
                        elevated = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                    ) {
                        SlidingPillTabRow(
                            selectedTab = selectedTab,
                            reduceMotion = reduceMotion,
                            enabled = !isSearchActive,
                            showLabels = showLabels,
                            onSelectTab = { tab ->
                                if (glass.hapticsEnabled) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onSelectTab(tab)
                            }
                        )
                    }

                    // Separated Circular Search Button aligned vertically with the capsule
                    val searchInteraction = remember { MutableInteractionSource() }
                    val isSearchPressed by searchInteraction.collectIsPressedAsState()
                    val searchBtnScale by animateFloatAsState(
                        targetValue = if (isSearchPressed && !reduceMotion) 0.88f else 1f,
                        animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.48f, stiffness = 540f),
                        label = "searchBtnScale"
                    )
                    val searchIconTilt by animateFloatAsState(
                        targetValue = if (isSearchPressed && !reduceMotion) -14f else 0f,
                        animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.50f, stiffness = 500f),
                        label = "searchIconTilt"
                    )

                    LiquidGlassCapsule(
                        modifier = Modifier
                            .size(62.dp)
                            .graphicsLayer {
                                scaleX = searchBtnScale
                                scaleY = searchBtnScale
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .clickable(
                                    enabled = !isSearchActive,
                                    interactionSource = searchInteraction,
                                    indication = androidx.compose.material3.ripple(),
                                    role = Role.Button,
                                    onClick = {
                                        if (glass.hapticsEnabled) {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                        onOpenSearch()
                                    }
                                )
                                .testTag("search_fab_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Abrir Pesquisa",
                                tint = glass.textPrimary,
                                modifier = Modifier
                                    .size(26.dp)
                                    .graphicsLayer { rotationZ = searchIconTilt }
                            )
                        }
                    }
                }
            }

            // STATE B: Morphed Search Area (Circular Auxiliary Filter Button Left + Expanding Rounded Search Field Right)
            if (isSearchFieldMounted) {
                val filterScale = if (reduceMotion) 1f else (0.82f + 0.18f * searchAlpha)
                val barExpandFraction = if (reduceMotion) 1f else (0.28f + 0.72f * morphProgress).coerceIn(0.28f, 1f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = searchAlpha },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Left Circular Auxiliary Filter Button (Functional)
                    val filterInteraction = remember { MutableInteractionSource() }
                    val isFilterPressed by filterInteraction.collectIsPressedAsState()
                    val filterPressScale by animateFloatAsState(
                        targetValue = if (isFilterPressed && !reduceMotion) 0.88f else 1f,
                        animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.48f, stiffness = 540f),
                        label = "filterPressScale"
                    )
                    val filterIconSpin by animateFloatAsState(
                        targetValue = if (isFilterMenuOpen && !reduceMotion) 90f else 0f,
                        animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.56f, stiffness = 420f),
                        label = "filterIconSpin"
                    )
                    LiquidGlassCapsule(
                        modifier = Modifier
                            .size(56.dp)
                            .graphicsLayer {
                                val s = filterScale * filterPressScale
                                scaleX = s
                                scaleY = s
                            }
                    ) {
                        val isFilterActive = searchFilter != SearchFilter.ALL
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .background(
                                    if (isFilterActive) glass.accentPrimary.copy(alpha = 0.22f)
                                    else Color.Transparent
                                )
                                .clickable(
                                    enabled = isSearchActive,
                                    interactionSource = filterInteraction,
                                    indication = androidx.compose.material3.ripple(),
                                    role = Role.Button,
                                    onClick = {
                                        if (glass.hapticsEnabled) {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        onToggleFilterMenu()
                                    }
                                )
                                .testTag("search_filter_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isFilterActive) Icons.Rounded.Tune else Icons.Rounded.FilterList,
                                contentDescription = "Filtros de pesquisa: ${searchFilter.label}",
                                tint = if (isFilterActive) glass.accentPrimary else glass.textPrimary,
                                modifier = Modifier
                                    .size(23.dp)
                                    .graphicsLayer { rotationZ = filterIconSpin }
                            )
                            if (isFilterActive) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(11.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(glass.accentPrimary)
                                )
                            }
                        }
                    }

                    // Right Expanding Rounded Search Field
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        LiquidGlassCapsule(
                            modifier = Modifier
                                .fillMaxWidth(barExpandFraction)
                                .height(56.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        onCloseSearch()
                                    },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("search_close_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Fechar Pesquisa",
                                        tint = glass.textPrimary
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = glass.accentPrimary,
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = strings.searchHint,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = glass.textSecondary.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = onQueryChange,
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            color = glass.textPrimary,
                                            fontSize = 15.sp
                                        ),
                                        cursorBrush = SolidColor(glass.accentPrimary),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(
                                            onSearch = {
                                                keyboardController?.hide()
                                            }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(focusRequester)
                                            .testTag("search_input_field")
                                    )
                                }

                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onQueryChange("") },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("search_clear_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Limpar texto da pesquisa",
                                            tint = glass.textSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SlidingPillTabRow(
    selectedTab: BottomNavTab,
    reduceMotion: Boolean,
    enabled: Boolean,
    showLabels: Boolean,
    onSelectTab: (BottomNavTab) -> Unit
) {
    val glass = LocalLumaGlass.current
    val strings = com.example.ui.theme.LocalLumaStrings.current
    val density = LocalDensity.current
    val tabs = BottomNavTab.entries
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)

    // Bouncy liquid-glass spring transition into the next slot
    val animatedSlot by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = if (reduceMotion) {
            snap()
        } else {
            spring(
                dampingRatio = 0.56f,
                stiffness = 430f
            )
        },
        label = "slidingPillSlot"
    )

    // Signature per-tab icon animations (Home bounce, Library groove tilt, Discover compass pulse, Settings gear roll)
    var homePulseCount by remember { mutableIntStateOf(0) }
    var libraryPulseCount by remember { mutableIntStateOf(0) }
    var discoverPulseCount by remember { mutableIntStateOf(0) }
    var settingsPulseCount by remember { mutableIntStateOf(0) }

    val homeBounceScale = remember { Animatable(1f) }
    val homeBounceY = remember { Animatable(0f) }
    val libraryScale = remember { Animatable(1f) }
    val libraryTilt = remember { Animatable(0f) }
    val discoverScale = remember { Animatable(1f) }
    val discoverTilt = remember { Animatable(0f) }
    val settingsRoll = remember { Animatable(0f) }
    val settingsScale = remember { Animatable(1f) }

    val hopPx = with(density) { -5.5.dp.toPx() }

    LaunchedEffect(selectedTab, homePulseCount, reduceMotion) {
        if (selectedTab == BottomNavTab.HOME && !reduceMotion) {
            launch {
                homeBounceScale.snapTo(1.22f)
                homeBounceScale.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 500f))
            }
            launch {
                homeBounceY.snapTo(hopPx)
                homeBounceY.animateTo(0f, spring(dampingRatio = 0.40f, stiffness = 460f))
            }
        }
    }

    LaunchedEffect(selectedTab, libraryPulseCount, reduceMotion) {
        if (selectedTab == BottomNavTab.LIBRARY && !reduceMotion) {
            launch {
                libraryScale.snapTo(1.22f)
                libraryScale.animateTo(1f, spring(dampingRatio = 0.44f, stiffness = 500f))
            }
            launch {
                libraryTilt.snapTo(-16f)
                libraryTilt.animateTo(0f, spring(dampingRatio = 0.36f, stiffness = 440f))
            }
        }
    }

    LaunchedEffect(selectedTab, discoverPulseCount, reduceMotion) {
        if (selectedTab == BottomNavTab.DISCOVER && !reduceMotion) {
            launch {
                discoverScale.snapTo(1.22f)
                discoverScale.animateTo(1f, spring(dampingRatio = 0.44f, stiffness = 500f))
            }
            launch {
                discoverTilt.snapTo(24f)
                discoverTilt.animateTo(0f, spring(dampingRatio = 0.44f, stiffness = 360f))
            }
        }
    }

    LaunchedEffect(selectedTab, settingsPulseCount, reduceMotion) {
        if (selectedTab == BottomNavTab.SETTINGS && !reduceMotion) {
            launch {
                settingsScale.snapTo(1.22f)
                settingsScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 500f))
            }
            launch {
                val targetAngle = settingsRoll.targetValue + 180f
                settingsRoll.animateTo(
                    targetValue = targetAngle,
                    animationSpec = spring(dampingRatio = 0.56f, stiffness = 230f)
                )
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(5.dp)
            .drawBehind {
                val slotWidth = size.width / tabs.size.toFloat()
                val travel = abs(animatedSlot - selectedIndex.toFloat()).coerceIn(0f, 1f)
                // Liquid droplet stretch during transit across slots
                val stretchPx = if (reduceMotion) 0f else sin(travel * PI).toFloat() * 13.dp.toPx()
                val squashY = if (reduceMotion) 0f else sin(travel * PI).toFloat() * 1.8.dp.toPx()

                val rawLeft = animatedSlot * slotWidth - stretchPx / 2f
                val pillWidth = slotWidth + stretchPx
                val clampedLeft = rawLeft.coerceIn(0f, (size.width - pillWidth).coerceAtLeast(0f))
                val pillHeight = (size.height - squashY * 2f).coerceAtLeast(8f)
                val pillTop = squashY
                val radius = pillHeight / 2f
                val corner = CornerRadius(radius, radius)

                if (size.minDimension <= 1f || pillWidth <= 1f || pillHeight <= 1f) return@drawBehind

                when (glass.surfaceMaterial) {
                    SurfaceMaterial.NEUMORPHIC_BW -> {
                        val activeFill = if (glass.isDark) glass.surfaceElevated else Color.White
                        drawRoundRect(
                            color = activeFill,
                            topLeft = Offset(clampedLeft, pillTop),
                            size = Size(pillWidth, pillHeight),
                            cornerRadius = corner
                        )
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    glass.reliefShadowLight.copy(alpha = if (glass.isDark) 0.14f else 0.85f),
                                    Color.Transparent,
                                    glass.reliefShadowDark.copy(alpha = if (glass.isDark) 0.24f else 0.14f)
                                ),
                                start = Offset(clampedLeft, pillTop),
                                end = Offset(clampedLeft + pillWidth, pillTop + pillHeight)
                            ),
                            topLeft = Offset(clampedLeft, pillTop),
                            size = Size(pillWidth, pillHeight),
                            cornerRadius = corner
                        )
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    glass.reliefShadowLight,
                                    glass.reliefShadowDark.copy(alpha = if (glass.isDark) 0.90f else 0.65f)
                                ),
                                start = Offset(clampedLeft, pillTop),
                                end = Offset(clampedLeft + pillWidth, pillTop + pillHeight)
                            ),
                            topLeft = Offset(clampedLeft, pillTop),
                            size = Size(pillWidth, pillHeight),
                            cornerRadius = corner,
                            style = Stroke(width = 1.35.dp.toPx())
                        )
                    }

                    SurfaceMaterial.NEUMORPHIC_SOFT -> {
                        // Crystal Soft-UI Liquid Lens pill with rose-tinted refraction & optical upper-lip gleam
                        val glowR = (pillWidth * 0.68f).coerceAtLeast(1f)
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    glass.accentPrimary.copy(alpha = if (glass.isDark) 0.24f else 0.16f),
                                    Color.Transparent
                                ),
                                center = Offset(clampedLeft + pillWidth / 2f, size.height * 0.56f),
                                radius = glowR
                            ),
                            topLeft = Offset(clampedLeft, pillTop),
                            size = Size(pillWidth, pillHeight),
                            cornerRadius = corner
                        )
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = if (glass.isDark) {
                                    listOf(
                                        glass.surfaceElevated.copy(alpha = 0.94f),
                                        glass.accentPrimary.copy(alpha = 0.18f)
                                    )
                                } else {
                                    listOf(
                                        Color.White,
                                        lerp(Color.White, glass.accentSecondary, 0.12f),
                                        Color.White.copy(alpha = 0.96f)
                                    )
                                },
                                start = Offset(clampedLeft, pillTop),
                                end = Offset(clampedLeft + pillWidth, pillTop + pillHeight)
                            ),
                            topLeft = Offset(clampedLeft, pillTop),
                            size = Size(pillWidth, pillHeight),
                            cornerRadius = corner
                        )
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                0.0f to Color.White.copy(alpha = if (glass.isDark) 0.30f else 1.0f),
                                0.38f to Color.White.copy(alpha = if (glass.isDark) 0.05f else 0.26f),
                                0.65f to Color.Transparent,
                                1.0f to glass.accentPrimary.copy(alpha = 0.10f),
                                startY = pillTop,
                                endY = pillTop + pillHeight
                            ),
                            topLeft = Offset(clampedLeft, pillTop),
                            size = Size(pillWidth, pillHeight),
                            cornerRadius = corner
                        )
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = if (glass.isDark) {
                                    listOf(
                                        Color.White.copy(alpha = 0.52f),
                                        glass.accentPrimary.copy(alpha = 0.48f)
                                    )
                                } else {
                                    listOf(
                                        Color.White,
                                        glass.accentPrimary.copy(alpha = 0.42f)
                                    )
                                },
                                startY = pillTop,
                                endY = pillTop + pillHeight
                            ),
                            topLeft = Offset(clampedLeft, pillTop),
                            size = Size(pillWidth, pillHeight),
                            cornerRadius = corner,
                            style = Stroke(width = 1.4.dp.toPx())
                        )
                    }

                    SurfaceMaterial.AMOLED_CRISP -> {
                        val amoledFill = if (glass.isDark) {
                            glass.surfaceElevated
                        } else {
                            lerp(Color.White, glass.accentPrimary, 0.10f)
                        }
                        drawRoundRect(
                            color = amoledFill,
                            topLeft = Offset(clampedLeft, pillTop),
                            size = Size(pillWidth, pillHeight),
                            cornerRadius = corner
                        )
                        drawRoundRect(
                            color = glass.accentPrimary.copy(alpha = if (glass.isDark) 0.65f else 0.55f),
                            topLeft = Offset(clampedLeft, pillTop),
                            size = Size(pillWidth, pillHeight),
                            cornerRadius = corner,
                            style = Stroke(width = 1.15.dp.toPx())
                        )
                    }

                    SurfaceMaterial.LIQUID_GLASS -> {
                        val lightPillStart = lerp(Color.White, glass.accentPrimary, 0.12f)
                        val lightPillEnd = lerp(Color.White, glass.accentSecondary, 0.16f)

                        // Soft optical glow beneath active liquid-glass droplet
                        val dropletGlowR = (pillWidth * 0.72f).coerceAtLeast(1f)
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    glass.accentPrimary.copy(alpha = if (glass.isDark) 0.28f else 0.24f),
                                    Color.Transparent
                                ),
                                center = Offset(clampedLeft + pillWidth / 2f, size.height * 0.55f),
                                radius = dropletGlowR
                            ),
                            topLeft = Offset(clampedLeft, pillTop),
                            size = Size(pillWidth, pillHeight),
                            cornerRadius = corner
                        )

                        // Real translucent crystal droplet body
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = if (glass.isDark) {
                                    listOf(
                                        Color.White.copy(alpha = 0.22f),
                                        glass.accentPrimary.copy(alpha = 0.19f),
                                        glass.accentSecondary.copy(alpha = 0.15f),
                                        Color.White.copy(alpha = 0.10f)
                                    )
                                } else {
                                    listOf(
                                        Color.White.copy(alpha = 0.98f),
                                        lightPillStart.copy(alpha = 0.94f),
                                        lightPillEnd.copy(alpha = 0.92f),
                                        Color.White.copy(alpha = 0.96f)
                                    )
                                },
                                start = Offset(clampedLeft, pillTop),
                                end = Offset(clampedLeft + pillWidth, pillTop + pillHeight)
                            ),
                            topLeft = Offset(clampedLeft, pillTop),
                            size = Size(pillWidth, pillHeight),
                            cornerRadius = corner
                        )

                        // Curved upper-lip optical caustic highlight
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                0.0f to Color.White.copy(alpha = if (glass.isDark) 0.34f else 0.95f),
                                0.34f to Color.White.copy(alpha = if (glass.isDark) 0.06f else 0.24f),
                                0.60f to Color.Transparent,
                                1.0f to glass.accentPrimary.copy(alpha = if (glass.isDark) 0.10f else 0.10f),
                                startY = pillTop,
                                endY = pillTop + pillHeight
                            ),
                            topLeft = Offset(clampedLeft, pillTop),
                            size = Size(pillWidth, pillHeight),
                            cornerRadius = corner
                        )

                        // Specular glint near upper-right edge of the sliding glass pill
                        val glintR = (pillHeight * 0.58f).coerceAtLeast(1f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (glass.isDark) 0.28f else 0.72f),
                                    Color.Transparent
                                ),
                                center = Offset(clampedLeft + pillWidth * 0.78f, pillTop + pillHeight * 0.24f),
                                radius = glintR
                            ),
                            center = Offset(clampedLeft + pillWidth * 0.78f, pillTop + pillHeight * 0.24f),
                            radius = glintR
                        )

                        // Crisp Fresnel rim around the sliding glass pill
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = if (glass.isDark) {
                                    listOf(
                                        Color.White.copy(alpha = 0.62f),
                                        Color.White.copy(alpha = 0.18f),
                                        glass.accentPrimary.copy(alpha = 0.42f)
                                    )
                                } else {
                                    listOf(
                                        Color.White,
                                        glass.accentPrimary.copy(alpha = 0.34f),
                                        glass.accentPrimary.copy(alpha = 0.48f)
                                    )
                                },
                                startY = pillTop,
                                endY = pillTop + pillHeight
                            ),
                            topLeft = Offset(clampedLeft, pillTop),
                            size = Size(pillWidth, pillHeight),
                            cornerRadius = corner,
                            style = Stroke(width = 1.2.dp.toPx())
                        )
                    }
                }
            }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEach { tab ->
                val isSelected = tab == selectedTab
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val selectionAnimMs = if (reduceMotion) 0 else 160

                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        when {
                            glass.surfaceMaterial == SurfaceMaterial.NEUMORPHIC_BW -> glass.textPrimary
                            glass.surfaceMaterial == SurfaceMaterial.NEUMORPHIC_SOFT -> glass.accentPrimary
                            glass.isDark -> glass.textPrimary
                            else -> glass.accentPrimary
                        }
                    } else {
                        glass.textSecondary
                    },
                    animationSpec = tween(selectionAnimMs),
                    label = "tabColor_${tab.name}"
                )

                val itemScale by animateFloatAsState(
                    targetValue = when {
                        isPressed && !reduceMotion -> 0.86f
                        isSelected && !reduceMotion -> 1.10f
                        else -> 1f
                    },
                    animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.50f, stiffness = 500f),
                    label = "tabScale_${tab.name}"
                )

                val contentAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else if (glass.isDark) 0.72f else 0.80f,
                    animationSpec = tween(selectionAnimMs),
                    label = "tabAlpha_${tab.name}"
                )

                val icon = when (tab) {
                    BottomNavTab.HOME -> if (isSelected) Icons.Rounded.Home else Icons.Outlined.Home
                    BottomNavTab.LIBRARY -> if (isSelected) Icons.Rounded.LibraryMusic else Icons.Outlined.LibraryMusic
                    BottomNavTab.DISCOVER -> if (isSelected) Icons.Rounded.Explore else Icons.Outlined.Explore
                    BottomNavTab.SETTINGS -> if (isSelected) Icons.Rounded.Settings else Icons.Outlined.Settings
                }

                val tabLabel = when (tab) {
                    BottomNavTab.HOME -> strings.navHome
                    BottomNavTab.LIBRARY -> strings.navLibrary
                    BottomNavTab.DISCOVER -> strings.navDiscover
                    BottomNavTab.SETTINGS -> strings.navSettings
                }

                val testTag = when (tab) {
                    BottomNavTab.HOME -> "nav_tab_home"
                    BottomNavTab.LIBRARY -> "nav_tab_library"
                    BottomNavTab.DISCOVER -> "nav_tab_discover"
                    BottomNavTab.SETTINGS -> "nav_tab_settings"
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .semantics {
                            selected = isSelected
                            contentDescription = tabLabel
                        }
                        .clickable(
                            enabled = enabled,
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Tab,
                            onClick = {
                                when (tab) {
                                    BottomNavTab.HOME -> homePulseCount++
                                    BottomNavTab.LIBRARY -> libraryPulseCount++
                                    BottomNavTab.DISCOVER -> discoverPulseCount++
                                    BottomNavTab.SETTINGS -> settingsPulseCount++
                                }
                                onSelectTab(tab)
                            }
                        )
                        .testTag(testTag),
                    contentAlignment = Alignment.Center
                ) {
                    val iconModifier = Modifier
                        .size(26.dp)
                        .graphicsLayer {
                            alpha = contentAlpha
                            when (tab) {
                                BottomNavTab.HOME -> {
                                    val totalScale = itemScale * homeBounceScale.value
                                    scaleX = totalScale
                                    scaleY = totalScale
                                    translationY = homeBounceY.value
                                }
                                BottomNavTab.LIBRARY -> {
                                    val totalScale = itemScale * libraryScale.value
                                    scaleX = totalScale
                                    scaleY = totalScale
                                    rotationZ = libraryTilt.value
                                }
                                BottomNavTab.DISCOVER -> {
                                    val totalScale = itemScale * discoverScale.value
                                    scaleX = totalScale
                                    scaleY = totalScale
                                    rotationZ = discoverTilt.value
                                }
                                BottomNavTab.SETTINGS -> {
                                    val totalScale = itemScale * settingsScale.value
                                    scaleX = totalScale
                                    scaleY = totalScale
                                    rotationZ = settingsRoll.value
                                }
                            }
                        }

                    if (showLabels) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = tabLabel,
                                tint = contentColor,
                                modifier = iconModifier.size(21.dp)
                            )
                            Text(
                                text = tabLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = contentColor,
                                maxLines = 1
                            )
                        }
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = tabLabel,
                            tint = contentColor,
                            modifier = iconModifier
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchFilterPanel(
    selectedFilter: SearchFilter,
    onSelectFilter: (SearchFilter) -> Unit
) {
    val glass = LocalLumaGlass.current
    LiquidGlassSurface(
        shape = RoundedCornerShape(22.dp),
        elevated = true,
        modifier = Modifier
            .widthIn(min = 210.dp, max = 260.dp)
            .testTag("search_filter_panel")
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "Filtrar resultados",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
            SearchFilter.entries.forEach { filter ->
                val isSelected = filter == selectedFilter
                val icon = when (filter) {
                    SearchFilter.ALL -> Icons.Rounded.Tune
                    SearchFilter.SONGS -> Icons.Rounded.MusicNote
                    SearchFilter.ALBUMS -> Icons.Rounded.Album
                    SearchFilter.ARTISTS -> Icons.Rounded.Person
                    SearchFilter.VIDEOS -> Icons.Rounded.VideoLibrary
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) glass.pillIndicatorBg else Color.Transparent
                        )
                        .clickable { onSelectFilter(filter) }
                        .minimumInteractiveComponentSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("search_filter_${filter.name.lowercase()}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) glass.accentPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selecionado",
                            tint = glass.accentPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
