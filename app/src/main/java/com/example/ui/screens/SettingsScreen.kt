package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.RestorePage
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.config.AppConfig
import com.example.data.media.MediaPermissionStatus
import com.example.data.model.AccentPalette
import com.example.data.model.AppLanguage
import com.example.data.model.AppSettings
import com.example.data.model.AppThemeStyle
import com.example.data.model.ArtworkShape
import com.example.data.model.CoverDoubleTapAction
import com.example.data.model.FolderSummary
import com.example.data.model.GlassQuality
import com.example.data.model.HapticIntensity
import com.example.data.model.MediaItemEntity
import com.example.data.model.PlaybackAnimationStyle
import com.example.data.model.PlayerScreenStyle
import com.example.data.model.SurfaceMaterial
import com.example.data.model.ThemeMode
import com.example.ui.components.ConfirmationDialog
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LocalLumaGlass
import com.example.ui.theme.LumaAnimatedSwitch
import com.example.ui.theme.LumaSectionEntrance
import com.example.ui.theme.formatDuration
import com.example.ui.theme.formatFileSize
import com.example.ui.theme.rememberLumaBounceScale
import com.example.ui.theme.themePreviewColors
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    mostPlayed: List<MediaItemEntity>,
    totalIndexedCount: Int,
    permissionStatus: MediaPermissionStatus = MediaPermissionStatus(),
    bottomInsetPadding: Dp,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetAccentPalette: (AccentPalette) -> Unit = {},
    onSetThemeStyle: (AppThemeStyle) -> Unit = {},
    onSetPlayerScreenStyle: (PlayerScreenStyle) -> Unit = {},
    onSetGlassQuality: (GlassQuality) -> Unit,
    onSetReduceMotion: (Boolean) -> Unit,
    onSetHaptics: (Boolean) -> Unit,
    onSetCrossfade: (Boolean) -> Unit,
    onSetDynamicCover: (Boolean) -> Unit,
    onSetVisualizer: (Boolean) -> Unit,
    onSetCoverGestures: (Boolean) -> Unit,
    onSetMinimalistMode: (Boolean) -> Unit,
    onSetAlbumArtAnimation: (Boolean) -> Unit,
    onRequestAutoScan: () -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
    onImportFiles: () -> Unit,
    onImportFolder: () -> Unit,
    onRefreshLibrary: () -> Unit,
    onOpenSupportedFormats: () -> Unit,
    onOpenAbout: () -> Unit,
    onClearHistory: () -> Unit,
    onResetStats: () -> Unit,
    cacheSizeBytes: Long = 0L,
    duplicateCount: Int = 0,
    onClearCache: () -> Unit = {},
    onOpenDuplicatesInspector: () -> Unit = {},
    folders: List<FolderSummary> = emptyList(),
    onOpenInsights: () -> Unit = {},
    onSetCustomAccentHex: (Long) -> Unit = {},
    onSetGlassTransparencyScale: (Float) -> Unit = {},
    onSetBackgroundBlurScale: (Float) -> Unit = {},
    onSetArtworkShape: (ArtworkShape) -> Unit = {},
    onSetPlaybackAnimationStyle: (PlaybackAnimationStyle) -> Unit = {},
    onSetNavBarShowLabels: (Boolean) -> Unit = {},
    onSetCoverDoubleTapAction: (CoverDoubleTapAction) -> Unit = {},
    onSetHapticIntensity: (HapticIntensity) -> Unit = {},
    onSetGaplessPlayback: (Boolean) -> Unit = {},
    onSetHomeSectionLayout: (String, Set<String>) -> Unit = { _, _ -> },
    onToggleExcludedFolder: (String) -> Unit = {},
    onSetPauseHistory: (Boolean) -> Unit = {},
    onReorganizeLibrary: () -> Unit = {},
    onExportBackup: () -> Unit = {},
    onImportBackup: () -> Unit = {},
    onSetAppLanguage: (AppLanguage) -> Unit = {},
    onSetShakeToSkip: (Boolean) -> Unit = {},
    onSetQueueRadio: (Boolean) -> Unit = {}
) {
    val glass = LocalLumaGlass.current
    val strings = com.example.ui.theme.LocalLumaStrings.current
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showResetStatsDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showHomeLayoutDialog by remember { mutableStateOf(false) }
    var showExcludedFoldersDialog by remember { mutableStateOf(false) }
    var showAccentDialog by remember { mutableStateOf(false) }

    if (showClearHistoryDialog) {
        ConfirmationDialog(
            title = "Limpar histórico",
            message = "Pretende apagar todo o histórico de reprodução local?",
            confirmLabel = "Apagar",
            onConfirm = {
                showClearHistoryDialog = false
                onClearHistory()
            },
            onDismiss = { showClearHistoryDialog = false }
        )
    }

    if (showResetStatsDialog) {
        ConfirmationDialog(
            title = "Repor estatísticas",
            message = "Isto irá zerar os contadores de reprodução e o tempo total de audição local.",
            confirmLabel = "Repor",
            onConfirm = {
                showResetStatsDialog = false
                onResetStats()
            },
            onDismiss = { showResetStatsDialog = false }
        )
    }

    if (showClearCacheDialog) {
        ConfirmationDialog(
            title = "Limpar cache da aplicação",
            message = "Apaga apenas miniaturas e ficheiros temporários (${formatFileSize(cacheSizeBytes)}). As suas músicas, vídeos, playlists e histórico permanecem intactos.",
            confirmLabel = "Limpar cache",
            onConfirm = {
                showClearCacheDialog = false
                onClearCache()
            },
            onDismiss = { showClearCacheDialog = false }
        )
    }

    if (showHomeLayoutDialog) {
        HomeLayoutDialog(
            homeSectionOrder = settings.homeSectionOrder,
            hiddenHomeSections = settings.hiddenHomeSections,
            onSave = onSetHomeSectionLayout,
            onDismiss = { showHomeLayoutDialog = false }
        )
    }

    if (showExcludedFoldersDialog) {
        ExcludedFoldersDialog(
            folders = folders,
            excludedFolders = settings.excludedFolders,
            onToggleFolder = onToggleExcludedFolder,
            onDismiss = { showExcludedFoldersDialog = false }
        )
    }

    if (showAccentDialog) {
        CustomAccentColorDialog(
            currentHex = settings.customAccentHex,
            onSelectHex = onSetCustomAccentHex,
            onDismiss = { showAccentDialog = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = 12.dp,
            bottom = bottomInsetPadding + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = strings.settingsTitle,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // 0. Language Selector
        item {
            Column {
                SettingsGroupTitle(strings.settingsLanguageSection)
                LiquidGlassSurface(shape = RoundedCornerShape(22.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.settingsLanguageTitle, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = settings.appLanguage.nativeLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = glass.accentPrimary
                            )
                        }
                        Text(
                            text = strings.settingsLanguageSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppLanguage.entries.forEach { lang ->
                                ChoiceChipPill(
                                    label = lang.nativeLabel,
                                    selected = settings.appLanguage == lang,
                                    onClick = { onSetAppLanguage(lang) },
                                    testTag = "language_${lang.name.lowercase()}"
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1. Personalization, Themes & Glass Physics
        item {
            LumaSectionEntrance(index = 0) {
                Column {
                    SettingsGroupTitle(strings.settingsAppearanceSection)
                    LiquidGlassSurface(shape = RoundedCornerShape(22.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(strings.settingsAppearanceMode, style = MaterialTheme.typography.titleSmall)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ThemeMode.entries.forEach { mode ->
                                    val selected = settings.themeMode == mode
                                    val modeInteraction = remember { MutableInteractionSource() }
                                    val modeBounce = rememberLumaBounceScale(modeInteraction, pressedScale = 0.92f)
                                    val modeBg by animateColorAsState(
                                        targetValue = if (selected) glass.pillIndicatorBg else glass.textPrimary.copy(alpha = 0.06f),
                                        animationSpec = if (glass.reduceMotion) snap() else tween(180),
                                        label = "modeBg_${mode.name}"
                                    )
                                    val modeIcon = when (mode) {
                                        ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                                        ThemeMode.LIGHT -> Icons.Rounded.LightMode
                                        ThemeMode.DARK -> Icons.Rounded.DarkMode
                                    }
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .graphicsLayer {
                                                scaleX = modeBounce
                                                scaleY = modeBounce
                                            }
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(modeBg)
                                            .border(
                                                width = if (selected) 1.5.dp else 0.8.dp,
                                                color = if (selected) glass.accentPrimary else glass.glassBorder,
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .clickable(
                                                interactionSource = modeInteraction,
                                                indication = androidx.compose.material3.ripple(),
                                                onClick = { onSetThemeMode(mode) }
                                            )
                                            .minimumInteractiveComponentSize()
                                            .padding(vertical = 10.dp, horizontal = 6.dp)
                                            .testTag("theme_${mode.name.lowercase()}"),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = modeIcon,
                                            contentDescription = null,
                                            tint = if (selected) glass.accentPrimary else glass.textSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = mode.label,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (selected) glass.accentPrimary else glass.textPrimary
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${strings.settingsThemesLabel} (${AppThemeStyle.entries.size})", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = settings.themeStyle.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = glass.accentPrimary
                                )
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("theme_carousel")
                            ) {
                                items(AppThemeStyle.entries, key = { it.name }) { style ->
                                    val isSelected = settings.themeStyle == style
                                    val previewDark = when {
                                        isSelected -> glass.isDark
                                        style == AppThemeStyle.ROSE_PORCELAIN -> false
                                        style == AppThemeStyle.OBSIDIAN_AMOLED -> true
                                        else -> glass.isDark
                                    }
                                    ThemePreviewCard(
                                        style = style,
                                        isDarkPreview = previewDark,
                                        selected = isSelected,
                                        onClick = {
                                            onSetThemeStyle(style)
                                            when (style) {
                                                AppThemeStyle.ROSE_PORCELAIN -> if (glass.isDark) onSetThemeMode(ThemeMode.LIGHT)
                                                AppThemeStyle.OBSIDIAN_AMOLED -> if (!glass.isDark) onSetThemeMode(ThemeMode.DARK)
                                                else -> Unit
                                            }
                                        }
                                    )
                                }
                            }

                            SettingsActionRow(
                                icon = Icons.Rounded.ColorLens,
                                title = "Cor de destaque personalizada",
                                subtitle = if (settings.customAccentHex == 0L) "Padrão do tema selecionado" else "Cor personalizada ativa",
                                onClick = { showAccentDialog = true },
                                testTag = "settings_custom_accent_row"
                            )

                            Text("Transparência e Efeitos", style = MaterialTheme.typography.titleSmall)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                GlassQuality.entries.forEach { quality ->
                                    val selected = settings.glassQuality == quality
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (selected) glass.pillIndicatorBg else Color.Transparent)
                                            .clickable { onSetGlassQuality(quality) }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = quality.label,
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (selected) {
                                            Icon(Icons.Rounded.Check, contentDescription = null, tint = glass.accentPrimary)
                                        }
                                    }
                                }
                            }

                            // Adjustable Glass Transparency & Background Blur
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Opacidade do vidro", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${(settings.glassTransparencyScale * 100).roundToInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = glass.accentPrimary
                                    )
                                }
                                Slider(
                                    value = settings.glassTransparencyScale,
                                    onValueChange = onSetGlassTransparencyScale,
                                    valueRange = 0.45f..1.45f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = glass.accentPrimary,
                                        activeTrackColor = glass.accentPrimary
                                    )
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Desfoque de fundo", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${(settings.backgroundBlurScale * 100).roundToInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = glass.accentPrimary
                                    )
                                }
                                Slider(
                                    value = settings.backgroundBlurScale,
                                    onValueChange = onSetBackgroundBlurScale,
                                    valueRange = 0.40f..1.50f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = glass.accentPrimary,
                                        activeTrackColor = glass.accentPrimary
                                    )
                                )
                            }

                            // Artwork Shape
                            Text("Forma da capa do álbum", style = MaterialTheme.typography.titleSmall)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ArtworkShape.entries.forEach { shape ->
                                    val selected = settings.artworkShape == shape
                                    ChoiceChipPill(
                                        label = shape.label,
                                        selected = selected,
                                        onClick = { onSetArtworkShape(shape) },
                                        testTag = "artwork_shape_${shape.name.lowercase()}"
                                    )
                                }
                            }

                            // Haptic Intensity
                            Text("Intensidade tátil", style = MaterialTheme.typography.titleSmall)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HapticIntensity.entries.forEach { intensity ->
                                    ChoiceChipPill(
                                        label = intensity.label,
                                        selected = settings.hapticIntensity == intensity,
                                        onClick = {
                                            onSetHapticIntensity(intensity)
                                            onSetHaptics(intensity != HapticIntensity.OFF)
                                        },
                                        testTag = "haptic_${intensity.name.lowercase()}"
                                    )
                                }
                            }

                            SettingsActionRow(
                                icon = Icons.Rounded.DashboardCustomize,
                                title = "Personalizar Ecrã Inicial",
                                subtitle = "Reordenar ou ocultar secções da página inicial",
                                onClick = { showHomeLayoutDialog = true },
                                testTag = "settings_home_layout_row"
                            )

                            SettingsToggleRow(
                                title = "Etiquetas na barra inferior",
                                subtitle = "Mostra o nome dos separadores abaixo dos ícones",
                                checked = settings.navBarShowLabels,
                                onCheckedChange = onSetNavBarShowLabels,
                                testTag = "toggle_nav_labels"
                            )
                            SettingsToggleRow(
                                title = "Reduzir movimento",
                                subtitle = "Usa transições simples e poupa bateria",
                                checked = settings.reduceMotion,
                                onCheckedChange = onSetReduceMotion,
                                testTag = "toggle_reduce_motion"
                            )
                        }
                    }
                }
            }
        }

        // 2. Playback & Now Playing Experience
        item {
            LumaSectionEntrance(index = 1) {
                Column {
                    SettingsGroupTitle(strings.settingsPlaybackSection)
                    LiquidGlassSurface(shape = RoundedCornerShape(22.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Estilo do ecrã A Reproduzir", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = settings.playerScreenStyle.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = glass.accentPrimary
                                )
                            }
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.testTag("settings_player_style_carousel")
                            ) {
                                items(PlayerScreenStyle.entries, key = { it.name }) { style ->
                                    val selected = settings.playerScreenStyle == style
                                    val cardInteraction = remember { MutableInteractionSource() }
                                    val pressScale = rememberLumaBounceScale(cardInteraction, pressedScale = 0.94f)
                                    val selectedScale by animateFloatAsState(
                                        targetValue = if (selected && !glass.reduceMotion) 1.02f else 1f,
                                        animationSpec = if (glass.reduceMotion) snap() else spring(dampingRatio = 0.56f, stiffness = 480f),
                                        label = "styleCardSelected_${style.name}"
                                    )
                                    Column(
                                        modifier = Modifier
                                            .width(142.dp)
                                            .graphicsLayer {
                                                val s = pressScale * selectedScale
                                                scaleX = s
                                                scaleY = s
                                            }
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (selected) glass.pillIndicatorBg else glass.textPrimary.copy(alpha = 0.04f))
                                            .border(
                                                width = if (selected) 1.6.dp else 0.9.dp,
                                                color = if (selected) glass.accentPrimary else glass.glassBorder,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clickable(
                                                interactionSource = cardInteraction,
                                                indication = androidx.compose.material3.ripple(),
                                                onClick = { onSetPlayerScreenStyle(style) }
                                            )
                                            .padding(10.dp)
                                            .testTag("settings_player_style_${style.name.lowercase()}"),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = style.title,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                                            ),
                                            color = if (selected) glass.accentPrimary else glass.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = style.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = glass.textSecondary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Text("Animação de reprodução", style = MaterialTheme.typography.titleSmall)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PlaybackAnimationStyle.entries.forEach { anim ->
                                    ChoiceChipPill(
                                        label = anim.label,
                                        selected = settings.playbackAnimationStyle == anim,
                                        onClick = { onSetPlaybackAnimationStyle(anim) }
                                    )
                                }
                            }

                            Text("Duplo toque na capa", style = MaterialTheme.typography.titleSmall)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CoverDoubleTapAction.entries.forEach { action ->
                                    ChoiceChipPill(
                                        label = action.label,
                                        selected = settings.coverDoubleTapAction == action,
                                        onClick = { onSetCoverDoubleTapAction(action) }
                                    )
                                }
                            }

                            SettingsToggleRow(
                                title = "Capa dinâmica no fundo",
                                subtitle = "Extrai as cores reais da capa para o leitor",
                                checked = settings.dynamicCoverTheme,
                                onCheckedChange = onSetDynamicCover
                            )
                            SettingsToggleRow(
                                title = "Visualizador de espectro",
                                subtitle = "Ativa análise PCM em tempo real no leitor",
                                checked = settings.visualizerEnabled,
                                onCheckedChange = onSetVisualizer
                            )
                            SettingsToggleRow(
                                title = "Animação na capa do álbum",
                                subtitle = "Movimento suave enquanto toca",
                                checked = settings.albumArtAnimationEnabled,
                                onCheckedChange = onSetAlbumArtAnimation
                            )
                            SettingsToggleRow(
                                title = "Gestos na capa",
                                subtitle = "Deslize horizontalmente para mudar de faixa",
                                checked = settings.coverGesturesEnabled,
                                onCheckedChange = onSetCoverGestures
                            )
                            SettingsToggleRow(
                                title = "Modo minimalista",
                                subtitle = "Oculta botões secundários no leitor",
                                checked = settings.minimalistPlayerMode,
                                onCheckedChange = onSetMinimalistMode
                            )
                            SettingsToggleRow(
                                title = "Reprodução contínua (Gapless)",
                                subtitle = "Transição sem pausas entre faixas seguidas",
                                checked = settings.gaplessPlaybackEnabled,
                                onCheckedChange = onSetGaplessPlayback,
                                testTag = "toggle_gapless"
                            )
                            SettingsToggleRow(
                                title = "Crossfade suave entre faixas",
                                subtitle = "Transição gradual de volume ao iniciar",
                                checked = settings.crossfadeEnabled,
                                onCheckedChange = onSetCrossfade
                            )
                            SettingsToggleRow(
                                title = strings.shakeSkipLabel,
                                subtitle = "Agite o dispositivo para saltar para a próxima faixa",
                                checked = settings.shakeToSkipEnabled,
                                onCheckedChange = onSetShakeToSkip,
                                testTag = "toggle_shake_to_skip"
                            )
                            SettingsToggleRow(
                                title = strings.queueRadioLabel,
                                subtitle = "Adiciona faixas semelhantes automaticamente no fim da fila",
                                checked = settings.queueRadioEnabled,
                                onCheckedChange = onSetQueueRadio,
                                testTag = "toggle_queue_radio"
                            )
                        }
                    }
                }
            }
        }

        // 3. Library, Organization & Insights
        item {
            LumaSectionEntrance(index = 2) {
                Column {
                    SettingsGroupTitle(strings.settingsLibrarySection)
                    LiquidGlassSurface(shape = RoundedCornerShape(22.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val permSubtitle = when {
                                permissionStatus.hasAllMediaPermissions ->
                                    "Ativa (Áudio e Vídeo) • Toque para varrer agora"
                                permissionStatus.hasAudioPermission ->
                                    "Áudio ativo, Vídeo pendente • Toque para conceder/varrer"
                                permissionStatus.hasVideoPermission ->
                                    "Vídeo ativo, Áudio pendente • Toque para conceder/varrer"
                                else ->
                                    "Permissão necessária para ler música e vídeo (READ_MEDIA_AUDIO / VIDEO)"
                            }
                            SettingsActionRow(
                                icon = Icons.Rounded.Sync,
                                title = "Varredura automática (MediaStore)",
                                subtitle = permSubtitle,
                                onClick = onRequestAutoScan,
                                testTag = "settings_auto_scan_row"
                            )
                            if (!permissionStatus.hasAllMediaPermissions) {
                                SettingsActionRow(
                                    icon = Icons.Rounded.Security,
                                    title = "Permissões do sistema",
                                    subtitle = "Abrir definições da aplicação no Android",
                                    onClick = onOpenAppSettings
                                )
                            }
                            SettingsActionRow(
                                icon = Icons.Rounded.Add,
                                title = "Importar ficheiros",
                                subtitle = "$totalIndexedCount item(ns) indexado(s) localmente",
                                onClick = onImportFiles
                            )
                            SettingsActionRow(
                                icon = Icons.Rounded.FolderOpen,
                                title = "Importar pasta",
                                subtitle = "Selecionar pasta via seletor do sistema",
                                onClick = onImportFolder
                            )
                            SettingsActionRow(
                                icon = Icons.Rounded.AutoFixHigh,
                                title = "Unificar artistas e organizar tags",
                                subtitle = "Agrupa variações do mesmo cantor e preenche géneros",
                                onClick = onReorganizeLibrary,
                                testTag = "settings_reorganize_row"
                            )
                            SettingsActionRow(
                                icon = Icons.Rounded.FolderOff,
                                title = "Pastas excluídas",
                                subtitle = if (settings.excludedFolders.isEmpty()) {
                                    "Nenhuma pasta ocultada"
                                } else {
                                    "${settings.excludedFolders.size} pasta(s) excluída(s)"
                                },
                                onClick = { showExcludedFoldersDialog = true },
                                testTag = "settings_excluded_folders_row"
                            )
                            SettingsActionRow(
                                icon = Icons.Rounded.Refresh,
                                title = "Verificar integridade dos ficheiros",
                                subtitle = "Detetar ficheiros movidos ou removidos",
                                onClick = onRefreshLibrary
                            )
                            SettingsActionRow(
                                icon = Icons.Rounded.ContentCopy,
                                title = "Inspecionar possíveis duplicados",
                                subtitle = if (duplicateCount > 0) "$duplicateCount grupo(s) encontrado(s) para revisão" else "Nenhum duplicado detetado atualmente",
                                onClick = onOpenDuplicatesInspector,
                                testTag = "settings_duplicates_row"
                            )
                            SettingsActionRow(
                                icon = Icons.Rounded.DeleteSweep,
                                title = "Limpar histórico de reprodução",
                                subtitle = "Remove o registo de faixas e vídeos recentes",
                                onClick = { showClearHistoryDialog = true }
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                    .clickable(onClick = onOpenInsights)
                                    .padding(12.dp)
                                    .testTag("settings_insights_card")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.BarChart,
                                        contentDescription = null,
                                        tint = glass.accentPrimary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Luma Insights • ${formatDuration(settings.totalListeningTimeMs)}",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        val topTrack = mostPlayed.firstOrNull()
                                        Text(
                                            text = if (topTrack != null) {
                                                "Mais ouvida: ${topTrack.title} (${topTrack.playCount}x) • Toque para abrir"
                                            } else {
                                                "Sem reproduções registadas ainda • Ver Wrapped"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            SettingsActionRow(
                                icon = Icons.Rounded.Restore,
                                title = "Repor estatísticas locais",
                                subtitle = "Zerar contadores de audição",
                                onClick = { showResetStatsDialog = true }
                            )
                        }
                    }
                }
            }
        }

        // 4. Backup & Privacy
        item {
            LumaSectionEntrance(index = 3) {
                Column {
                    SettingsGroupTitle(strings.settingsBackupSection)
                    LiquidGlassSurface(shape = RoundedCornerShape(22.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SettingsToggleRow(
                                title = "Pausar registo de histórico",
                                subtitle = "Não contabiliza novas faixas no histórico e insights",
                                checked = settings.pauseHistoryTracking,
                                onCheckedChange = onSetPauseHistory,
                                testTag = "toggle_pause_history"
                            )
                            SettingsActionRow(
                                icon = Icons.Rounded.Backup,
                                title = "Exportar backup local (JSON)",
                                subtitle = "Guarda playlists e definições num ficheiro do dispositivo",
                                onClick = onExportBackup,
                                testTag = "settings_export_backup_row"
                            )
                            SettingsActionRow(
                                icon = Icons.Rounded.RestorePage,
                                title = "Restaurar backup local (JSON)",
                                subtitle = "Recupera playlists e preferências exportadas",
                                onClick = onImportBackup,
                                testTag = "settings_import_backup_row"
                            )
                        }
                    }
                }
            }
        }

        // 5. Diagnostics & Sobre
        item {
            LumaSectionEntrance(index = 4) {
                Column {
                    SettingsGroupTitle(strings.settingsAboutSection)
                    LiquidGlassSurface(shape = RoundedCornerShape(22.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SettingsActionRow(
                                icon = Icons.Rounded.AudioFile,
                                title = "Formatos suportados pelo dispositivo",
                                subtitle = "Inspecionar descodificadores reais de áudio e vídeo",
                                onClick = onOpenSupportedFormats
                            )
                            SettingsActionRow(
                                icon = Icons.Rounded.CleaningServices,
                                title = "Limpar cache da aplicação",
                                subtitle = "Espaço ocupado em cache: ${formatFileSize(cacheSizeBytes)}",
                                onClick = { showClearCacheDialog = true },
                                testTag = "settings_clear_cache_row"
                            )
                            SettingsActionRow(
                                icon = Icons.Rounded.Info,
                                title = strings.aboutTitle,
                                subtitle = "Programador, ${strings.aboutVersionPrefix.lowercase()} ${AppConfig.APP_VERSION}, privacidade e licenças",
                                onClick = onOpenAbout,
                                testTag = "settings_about_row"
                            )
                        }
                    }
                }
            }
        }

        // Footer: App Version & Developer Credit
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .testTag("settings_version_footer"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${AppConfig.APP_NAME} • ${strings.aboutVersionPrefix} ${AppConfig.APP_VERSION}",
                    style = MaterialTheme.typography.labelLarge,
                    color = glass.accentPrimary
                )
                Text(
                    text = strings.aboutDeveloperCredit,
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.textSecondary
                )
            }
        }
    }
}

@Composable
private fun ChoiceChipPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String? = null
) {
    val glass = LocalLumaGlass.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) glass.pillIndicatorBg else glass.textPrimary.copy(alpha = 0.05f))
            .border(
                width = if (selected) 1.3.dp else 0.7.dp,
                color = if (selected) glass.accentPrimary else glass.glassBorder,
                shape = RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .let { if (testTag != null) it.testTag(testTag) else it }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (selected) glass.accentPrimary else glass.textSecondary
        )
    }
}

@Composable
private fun SettingsGroupTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp)
            .let { if (testTag != null) it.testTag(testTag) else it },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        LumaAnimatedSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String? = null
) {
    val glass = LocalLumaGlass.current
    val interactionSource = remember { MutableInteractionSource() }
    val bounceScale = rememberLumaBounceScale(interactionSource, pressedScale = 0.96f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = bounceScale
                scaleY = bounceScale
            }
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
            .minimumInteractiveComponentSize()
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .let { if (testTag != null) it.testTag(testTag) else it },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = glass.accentPrimary)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ThemePreviewCard(
    style: AppThemeStyle,
    isDarkPreview: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    val glass = LocalLumaGlass.current
    val preview = themePreviewColors(style = style, isDark = isDarkPreview)
    val cardShape = RoundedCornerShape(18.dp)
    val cardInteraction = remember { MutableInteractionSource() }
    val bounceScale = rememberLumaBounceScale(cardInteraction, pressedScale = 0.94f)
    val activeScale by animateFloatAsState(
        targetValue = if (selected && !glass.reduceMotion) 1.03f else 1f,
        animationSpec = if (glass.reduceMotion) snap() else spring(dampingRatio = 0.56f, stiffness = 460f),
        label = "themePreviewScale_${style.name}"
    )

    Column(
        modifier = Modifier
            .width(136.dp)
            .graphicsLayer {
                val totalScale = bounceScale * activeScale
                scaleX = totalScale
                scaleY = totalScale
            }
            .clip(cardShape)
            .background(
                if (selected) glass.pillIndicatorBg
                else glass.textPrimary.copy(alpha = 0.04f)
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) glass.accentPrimary else glass.glassBorder,
                shape = cardShape
            )
            .clickable(
                interactionSource = cardInteraction,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
            .padding(8.dp)
            .testTag("theme_card_${style.name.lowercase()}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (style.material == SurfaceMaterial.LIQUID_GLASS) {
                        Brush.linearGradient(listOf(preview.bg, preview.bgSecondary, preview.elevated))
                    } else {
                        Brush.verticalGradient(listOf(preview.bg, preview.bgSecondary))
                    }
                )
                .border(
                    width = 0.8.dp,
                    color = preview.border,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(8.dp)
        ) {
            when (style.material) {
                SurfaceMaterial.NEUMORPHIC_BW, SurfaceMaterial.NEUMORPHIC_SOFT -> {
                    val isSoftRose = style.material == SurfaceMaterial.NEUMORPHIC_SOFT
                    val bwShadowColor = if (isDarkPreview) preview.reliefDark else preview.reliefDark.copy(alpha = 0.42f)
                    val accentDotColor = if (isSoftRose) preview.accent else preview.text
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .shadow(2.dp, CircleShape, ambientColor = bwShadowColor, spotColor = bwShadowColor)
                                .clip(CircleShape)
                                .background(preview.elevated)
                                .border(1.2.dp, preview.reliefLight, CircleShape)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSoftRose) preview.accent.copy(alpha = 0.14f)
                                    else preview.text.copy(alpha = 0.14f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(accentDotColor)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(preview.text.copy(alpha = 0.18f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.55f)
                                    .height(3.dp)
                                    .clip(CircleShape)
                                    .background(accentDotColor)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .shadow(1.5.dp, CircleShape, ambientColor = bwShadowColor, spotColor = bwShadowColor)
                                    .clip(CircleShape)
                                    .background(preview.surface)
                            )
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .shadow(2.dp, CircleShape, ambientColor = bwShadowColor, spotColor = bwShadowColor)
                                    .clip(CircleShape)
                                    .background(preview.elevated)
                                    .border(
                                        1.dp,
                                        if (isSoftRose) preview.accent.copy(alpha = 0.45f) else preview.reliefLight,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Pause,
                                    contentDescription = null,
                                    tint = accentDotColor,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .shadow(1.5.dp, CircleShape, ambientColor = bwShadowColor, spotColor = bwShadowColor)
                                    .clip(CircleShape)
                                    .background(preview.surface)
                            )
                        }
                    }
                }

                SurfaceMaterial.AMOLED_CRISP -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(preview.surface)
                                .border(0.8.dp, preview.accent.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(preview.accent)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .width(48.dp)
                                            .height(4.dp)
                                            .clip(CircleShape)
                                            .background(preview.text)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(30.dp)
                                            .height(3.dp)
                                            .clip(CircleShape)
                                            .background(preview.subtext)
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(CircleShape)
                                .background(preview.elevated)
                                .border(0.7.dp, preview.border, CircleShape)
                        )
                    }
                }

                SurfaceMaterial.LIQUID_GLASS -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(preview.surface.copy(alpha = if (isDarkPreview) 0.72f else 0.85f))
                                .border(0.8.dp, preview.border, RoundedCornerShape(10.dp))
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(preview.accent, preview.accentSecondary)))
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .width(50.dp)
                                            .height(4.dp)
                                            .clip(CircleShape)
                                            .background(preview.text)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(32.dp)
                                            .height(3.dp)
                                            .clip(CircleShape)
                                            .background(preview.subtext)
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDarkPreview) preview.elevated.copy(alpha = 0.78f)
                                    else lerp(Color.White, preview.bgSecondary, 0.36f)
                                )
                                .border(0.7.dp, preview.border, CircleShape),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .width(38.dp)
                                    .height(10.dp)
                                    .clip(CircleShape)
                                    .background(preview.accent.copy(alpha = if (isDarkPreview) 0.35f else 0.42f))
                            )
                        }
                    }
                }
            }

            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Tema ativo",
                    tint = glass.accentPrimary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = style.title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                color = if (selected) glass.accentPrimary else glass.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = style.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = glass.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
