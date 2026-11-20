package com.example.ui.theme

import android.app.Activity
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.model.AppSettings
import com.example.data.model.AppThemeStyle
import com.example.data.model.ArtworkShape
import com.example.data.model.GlassQuality
import com.example.data.model.HapticIntensity
import com.example.data.model.PlaybackAnimationStyle
import com.example.data.model.SurfaceMaterial
import com.example.data.model.ThemeMode

data class LumaGlassTokens(
    val isDark: Boolean,
    val themeStyle: AppThemeStyle,
    val surfaceMaterial: SurfaceMaterial,
    val isMonochrome: Boolean,
    val glassQuality: GlassQuality,
    val reduceMotion: Boolean,
    val hapticsEnabled: Boolean,
    val hapticIntensity: HapticIntensity,
    val glassTransparencyScale: Float,
    val backgroundBlurScale: Float,
    val artworkShape: ArtworkShape,
    val playbackAnimationStyle: PlaybackAnimationStyle,
    val background: Color,
    val backgroundSecondary: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val glassSurface: Color,
    val glassElevated: Color,
    val glassBorder: Color,
    val glassBorderTop: Color,
    val glassBorderBottom: Color,
    val reliefShadowLight: Color,
    val reliefShadowDark: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val onAccent: Color,
    val iconPrimary: Color,
    val divider: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val pillIndicatorBg: Color,
    val scrim: Color
) {
    val isNeumorphic: Boolean
        get() = surfaceMaterial == SurfaceMaterial.NEUMORPHIC_BW || surfaceMaterial == SurfaceMaterial.NEUMORPHIC_SOFT
}

val LocalLumaGlass = compositionLocalOf {
    LumaGlassTokens(
        isDark = true,
        themeStyle = AppThemeStyle.LUMA_GLASS,
        surfaceMaterial = SurfaceMaterial.LIQUID_GLASS,
        isMonochrome = false,
        glassQuality = GlassQuality.ULTRA,
        reduceMotion = false,
        hapticsEnabled = true,
        hapticIntensity = HapticIntensity.SUBTLE,
        glassTransparencyScale = 1.0f,
        backgroundBlurScale = 1.0f,
        artworkShape = ArtworkShape.ROUNDED,
        playbackAnimationStyle = PlaybackAnimationStyle.SPRING_FLOAT,
        background = DeepNavyBg,
        backgroundSecondary = DeepPetrolBgSecondary,
        surface = MidnightIndigoSurface,
        surfaceElevated = DarkSurfaceVariant,
        glassSurface = Color(0x9E102638),
        glassElevated = Color(0xCC153147),
        glassBorder = Color(0x33FFFFFF),
        glassBorderTop = Color(0x47FFFFFF),
        glassBorderBottom = Color(0x18FFFFFF),
        reliefShadowLight = Color(0x28FFFFFF),
        reliefShadowDark = Color(0x66020812),
        textPrimary = LightTextPrimary,
        textSecondary = LightTextSecondary,
        textDisabled = DarkTextDisabled,
        accentPrimary = EmeraldLight,
        accentSecondary = EmeraldTeal,
        onAccent = DeepNavyBg,
        iconPrimary = EmeraldLight,
        divider = DarkDivider,
        success = SuccessGreenDark,
        warning = WarningAmberDark,
        error = ErrorRedDark,
        pillIndicatorBg = EmeraldLight.copy(alpha = 0.24f),
        scrim = Color(0x99040A14)
    )
}

private val LumaShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

private data class ThemePaletteSpec(
    val bg: Color,
    val bgSecondary: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceVariant: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val divider: Color,
    val reliefLight: Color,
    val reliefDark: Color,
    val glassSurface: Color,
    val glassElevated: Color,
    val borderTop: Color,
    val borderBottom: Color,
    val borderUnified: Color,
    val pillBg: Color
)

@Composable
fun LumaTheme(
    settings: AppSettings,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val systemAnimScale = runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }.getOrDefault(1f)
    val effectiveReduceMotion = settings.reduceMotion || systemAnimScale == 0f

    val style = settings.themeStyle
    val material = style.material
    val isMonochrome = material == SurfaceMaterial.NEUMORPHIC_BW

    val baseSpec = resolveThemeSpec(style = style, isDark = isDark, quality = settings.glassQuality)
    val customAccentColor = if (settings.customAccentHex != 0L && !isMonochrome) {
        Color(settings.customAccentHex)
    } else null
    val transparencyScale = settings.glassTransparencyScale.coerceIn(0.45f, 1.45f)

    val effectivePrimary = customAccentColor ?: baseSpec.accentPrimary
    val effectiveSecondary = customAccentColor?.let { lerp(it, if (isDark) Color.White else Color.Black, 0.22f) }
        ?: baseSpec.accentSecondary
    val spec = baseSpec.copy(
        accentPrimary = effectivePrimary,
        accentSecondary = effectiveSecondary,
        glassSurface = baseSpec.glassSurface.copy(
            alpha = (baseSpec.glassSurface.alpha * transparencyScale).coerceIn(0.25f, 0.98f)
        ),
        glassElevated = baseSpec.glassElevated.copy(
            alpha = (baseSpec.glassElevated.alpha * transparencyScale).coerceIn(0.32f, 0.99f)
        ),
        pillBg = effectivePrimary.copy(alpha = if (isDark) 0.22f else 0.14f)
    )
    val rawOnAccent = when {
        isMonochrome -> if (spec.accentPrimary.luminance() > 0.46f) NeuBwInkBlack else Color.White
        spec.accentPrimary.luminance() > 0.46f -> DeepNavyBg
        else -> Color.White
    }

    val animDuration = if (effectiveReduceMotion) 0 else 260
    val colorSpec = tween<Color>(durationMillis = animDuration, easing = FastOutSlowInEasing)

    val primary by animateColorAsState(spec.accentPrimary, colorSpec, label = "primary")
    val secondary by animateColorAsState(spec.accentSecondary, colorSpec, label = "secondary")
    val onAccent by animateColorAsState(rawOnAccent, colorSpec, label = "onAccent")
    val bg by animateColorAsState(spec.bg, colorSpec, label = "bg")
    val bgSecondary by animateColorAsState(spec.bgSecondary, colorSpec, label = "bgSecondary")
    val surface by animateColorAsState(spec.surface, colorSpec, label = "surface")
    val surfaceElevated by animateColorAsState(spec.surfaceElevated, colorSpec, label = "surfaceElevated")
    val surfaceVariant by animateColorAsState(spec.surfaceVariant, colorSpec, label = "surfaceVariant")
    val textPrimary by animateColorAsState(spec.textPrimary, colorSpec, label = "textPrimary")
    val textSecondary by animateColorAsState(spec.textSecondary, colorSpec, label = "textSecondary")
    val textDisabled by animateColorAsState(spec.textDisabled, colorSpec, label = "textDisabled")
    val dividerColor by animateColorAsState(spec.divider, colorSpec, label = "divider")
    val reliefLight by animateColorAsState(spec.reliefLight, colorSpec, label = "reliefLight")
    val reliefDark by animateColorAsState(spec.reliefDark, colorSpec, label = "reliefDark")

    val successColor by animateColorAsState(
        when {
            isMonochrome && isDark -> NeuBwInkWhite
            isMonochrome -> NeuBwInkBlack
            isDark -> SuccessGreenDark
            else -> SuccessGreenLight
        },
        colorSpec,
        label = "success"
    )
    val warningColor by animateColorAsState(
        if (isDark) WarningAmberDark else WarningAmberLight,
        colorSpec,
        label = "warning"
    )
    val errorColor by animateColorAsState(
        if (isDark) ErrorRedDark else ErrorRedLight,
        colorSpec,
        label = "error"
    )

    val glassSurface by animateColorAsState(spec.glassSurface, colorSpec, label = "glassSurface")
    val glassElevated by animateColorAsState(spec.glassElevated, colorSpec, label = "glassElevated")
    val glassBorder by animateColorAsState(spec.borderUnified, colorSpec, label = "glassBorder")
    val glassBorderTop by animateColorAsState(spec.borderTop, colorSpec, label = "glassBorderTop")
    val glassBorderBottom by animateColorAsState(spec.borderBottom, colorSpec, label = "glassBorderBottom")
    val pillBg by animateColorAsState(spec.pillBg, colorSpec, label = "pillBg")

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onAccent,
            primaryContainer = primary.copy(alpha = 0.20f),
            onPrimaryContainer = textPrimary,
            secondary = secondary,
            onSecondary = if (secondary.luminance() > 0.46f) DeepNavyBg else Color.White,
            secondaryContainer = secondary.copy(alpha = 0.20f),
            onSecondaryContainer = textPrimary,
            background = bg,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = textSecondary,
            surfaceContainerLowest = bg,
            surfaceContainerLow = bgSecondary,
            surfaceContainer = surface,
            surfaceContainerHigh = surfaceElevated,
            surfaceContainerHighest = surfaceVariant,
            error = errorColor,
            onError = DeepNavyBg,
            outline = dividerColor,
            outlineVariant = glassBorderBottom
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onAccent,
            primaryContainer = primary.copy(alpha = 0.14f),
            onPrimaryContainer = textPrimary,
            secondary = secondary,
            onSecondary = if (secondary.luminance() > 0.46f) NeuBwInkBlack else Color.White,
            secondaryContainer = secondary.copy(alpha = 0.14f),
            onSecondaryContainer = textPrimary,
            background = bg,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = textSecondary,
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = bgSecondary,
            surfaceContainer = surface,
            surfaceContainerHigh = surfaceElevated,
            surfaceContainerHighest = surfaceVariant,
            error = errorColor,
            onError = Color.White,
            outline = dividerColor,
            outlineVariant = glassBorderBottom
        )
    }

    val effectiveHaptics = settings.hapticsEnabled && settings.hapticIntensity != HapticIntensity.OFF
    val glassTokens = LumaGlassTokens(
        isDark = isDark,
        themeStyle = style,
        surfaceMaterial = material,
        isMonochrome = isMonochrome,
        glassQuality = settings.glassQuality,
        reduceMotion = effectiveReduceMotion,
        hapticsEnabled = effectiveHaptics,
        hapticIntensity = settings.hapticIntensity,
        glassTransparencyScale = transparencyScale,
        backgroundBlurScale = settings.backgroundBlurScale.coerceIn(0.4f, 1.5f),
        artworkShape = settings.artworkShape,
        playbackAnimationStyle = settings.playbackAnimationStyle,
        background = bg,
        backgroundSecondary = bgSecondary,
        surface = surface,
        surfaceElevated = surfaceElevated,
        glassSurface = glassSurface,
        glassElevated = glassElevated,
        glassBorder = glassBorder,
        glassBorderTop = glassBorderTop,
        glassBorderBottom = glassBorderBottom,
        reliefShadowLight = reliefLight,
        reliefShadowDark = reliefDark,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        textDisabled = textDisabled,
        accentPrimary = primary,
        accentSecondary = secondary,
        onAccent = onAccent,
        iconPrimary = primary,
        divider = dividerColor,
        success = successColor,
        warning = warningColor,
        error = errorColor,
        pillIndicatorBg = pillBg,
        scrim = if (isDark) Color(0xA6040A14) else textPrimary.copy(alpha = 0.34f)
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.decorView.setBackgroundColor(bg.toArgb())
                @Suppress("DEPRECATION")
                window.statusBarColor = Color.Transparent.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = Color.Transparent.toArgb()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }
        }
    }

    CompositionLocalProvider(LocalLumaGlass provides glassTokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LumaTypography,
            shapes = LumaShapes,
            content = content
        )
    }
}

private fun resolveThemeSpec(
    style: AppThemeStyle,
    isDark: Boolean,
    quality: GlassQuality
): ThemePaletteSpec {
    return when (style) {
        AppThemeStyle.ALABASTER_BW -> {
            if (isDark) {
                ThemePaletteSpec(
                    bg = NeuBwDarkBg,
                    bgSecondary = NeuBwDarkBgSecondary,
                    surface = NeuBwDarkSurface,
                    surfaceElevated = NeuBwDarkElevated,
                    surfaceVariant = NeuBwDarkElevated,
                    textPrimary = NeuBwInkWhite,
                    textSecondary = NeuBwInkSilver,
                    textDisabled = NeuBwInkDarkMuted,
                    accentPrimary = NeuBwInkWhite,
                    accentSecondary = NeuBwInkSilver,
                    divider = Color(0x22FFFFFF),
                    reliefLight = NeuBwDarkReliefHighlight,
                    reliefDark = NeuBwDarkReliefShadow,
                    glassSurface = NeuBwDarkSurface,
                    glassElevated = NeuBwDarkElevated,
                    borderTop = Color(0x40FFFFFF),
                    borderBottom = Color(0x99050608),
                    borderUnified = Color(0x26FFFFFF),
                    pillBg = Color(0x24FFFFFF)
                )
            } else {
                ThemePaletteSpec(
                    bg = NeuBwLightBg,
                    bgSecondary = NeuBwLightBgSecondary,
                    surface = NeuBwLightSurface,
                    surfaceElevated = NeuBwLightElevated,
                    surfaceVariant = Color(0xFFDCE3EE),
                    textPrimary = NeuBwInkBlack,
                    textSecondary = NeuBwInkSlate,
                    textDisabled = NeuBwInkMuted,
                    accentPrimary = NeuBwInkBlack,
                    accentSecondary = Color(0xFF39414E),
                    divider = Color(0x18111318),
                    reliefLight = NeuBwLightReliefHighlight,
                    reliefDark = NeuBwLightReliefShadow,
                    glassSurface = NeuBwLightSurface,
                    glassElevated = NeuBwLightElevated,
                    borderTop = Color(0xF5FFFFFF),
                    borderBottom = Color(0x669AA8BC),
                    borderUnified = Color(0xFFC9D3E0),
                    pillBg = Color(0x16111318)
                )
            }
        }

        AppThemeStyle.ROSE_PORCELAIN -> {
            if (isDark) {
                ThemePaletteSpec(
                    bg = SoftPinkDarkBg,
                    bgSecondary = SoftPinkDarkBgSecondary,
                    surface = SoftPinkDarkSurface,
                    surfaceElevated = SoftPinkDarkElevated,
                    surfaceVariant = SoftPinkDarkElevated,
                    textPrimary = NeuBwInkWhite,
                    textSecondary = Color(0xFFB7BED0),
                    textDisabled = Color(0xFF6D7487),
                    accentPrimary = SoftPinkSecondary,
                    accentSecondary = SoftPinkPrimary,
                    divider = Color(0x22FFFFFF),
                    reliefLight = SoftPinkDarkReliefHighlight,
                    reliefDark = SoftPinkDarkReliefShadow,
                    glassSurface = SoftPinkDarkSurface,
                    glassElevated = SoftPinkDarkElevated,
                    borderTop = Color(0x3DFFFFFF),
                    borderBottom = Color(0x9E07080B),
                    borderUnified = Color(0x28FFFFFF),
                    pillBg = SoftPinkSecondary.copy(alpha = 0.20f)
                )
            } else {
                ThemePaletteSpec(
                    bg = SoftWhiteLightBg,
                    bgSecondary = SoftWhiteLightBgSecondary,
                    surface = SoftWhiteLightSurface,
                    surfaceElevated = SoftWhiteLightElevated,
                    surfaceVariant = Color(0xFFE9EEF7),
                    textPrimary = SoftWhiteInk,
                    textSecondary = SoftWhiteInkSecondary,
                    textDisabled = SoftWhiteInkMuted,
                    accentPrimary = SoftPinkPrimary,
                    accentSecondary = SoftPinkSecondary,
                    divider = Color(0x16171923),
                    reliefLight = SoftWhiteReliefHighlight,
                    reliefDark = SoftWhiteReliefShadow,
                    glassSurface = SoftWhiteLightSurface,
                    glassElevated = SoftWhiteLightElevated,
                    borderTop = Color(0xFAFFFFFF),
                    borderBottom = Color(0x6BA4B1C6),
                    borderUnified = Color(0xFFD2DBE8),
                    pillBg = SoftPinkPrimary.copy(alpha = 0.14f)
                )
            }
        }

        AppThemeStyle.OBSIDIAN_AMOLED -> {
            if (isDark) {
                ThemePaletteSpec(
                    bg = AmoledDarkBg,
                    bgSecondary = AmoledDarkBg,
                    surface = AmoledDarkSurface,
                    surfaceElevated = AmoledDarkElevated,
                    surfaceVariant = AmoledDarkElevated,
                    textPrimary = Color(0xFFF8FAFC),
                    textSecondary = Color(0xFF94A3B8),
                    textDisabled = Color(0xFF64748B),
                    accentPrimary = AmoledDarkAccent,
                    accentSecondary = AmoledDarkAccentSecondary,
                    divider = Color(0x26FFFFFF),
                    reliefLight = Color(0x28FFFFFF),
                    reliefDark = Color(0xCC000000),
                    glassSurface = AmoledDarkSurface,
                    glassElevated = AmoledDarkElevated,
                    borderTop = Color(0x33FFFFFF),
                    borderBottom = Color(0x1AFFFFFF),
                    borderUnified = Color(0x2AFFFFFF),
                    pillBg = AmoledDarkAccent.copy(alpha = 0.20f)
                )
            } else {
                ThemePaletteSpec(
                    bg = AmoledLightBg,
                    bgSecondary = AmoledLightElevated,
                    surface = AmoledLightSurface,
                    surfaceElevated = AmoledLightSurface,
                    surfaceVariant = Color(0xFFE6ECF4),
                    textPrimary = Color(0xFF0F172A),
                    textSecondary = Color(0xFF475569),
                    textDisabled = Color(0xFF94A3B8),
                    accentPrimary = OceanCyan,
                    accentSecondary = OceanIndigoLightMode,
                    divider = Color(0x1A0F172A),
                    reliefLight = Color.White,
                    reliefDark = Color(0x1A0F172A),
                    glassSurface = AmoledLightSurface,
                    glassElevated = AmoledLightSurface,
                    borderTop = Color(0x1F0F172A),
                    borderBottom = Color(0x1F0F172A),
                    borderUnified = Color(0x1D0F172A),
                    pillBg = OceanCyan.copy(alpha = 0.14f)
                )
            }
        }

        else -> {
            val (bg, bgSec, surf, surfElev, primary, secondary) = when (style) {
                AppThemeStyle.LUMA_GLASS -> if (isDark) {
                    GlassColors(DeepNavyBg, DeepPetrolBgSecondary, MidnightIndigoSurface, DarkSurfaceVariant, EmeraldLight, EmeraldTeal)
                } else {
                    GlassColors(FrostedIceBg, FrostedIceBgSecondary, PearlWhiteSurface, PearlWhiteSurface, EmeraldMint, EmeraldTealLightMode)
                }

                AppThemeStyle.OCEAN_SAPPHIRE -> if (isDark) {
                    GlassColors(OceanDarkBg, OceanDarkBgSecondary, OceanDarkSurface, OceanDarkElevated, OceanCyanLight, OceanIndigo)
                } else {
                    GlassColors(OceanLightBg, OceanLightBgSecondary, PearlWhiteSurface, PearlWhiteSurface, OceanCyan, OceanIndigoLightMode)
                }

                AppThemeStyle.VELVET_VIOLET -> if (isDark) {
                    GlassColors(VioletDarkBg, VioletDarkBgSecondary, VioletDarkSurface, VioletDarkElevated, VioletLight, VioletMagenta)
                } else {
                    GlassColors(VioletLightBg, VioletLightBgSecondary, PearlWhiteSurface, PearlWhiteSurface, VioletPulse, VioletMagentaLightMode)
                }

                AppThemeStyle.SUNSET_AMBER -> if (isDark) {
                    GlassColors(SunsetDarkBg, SunsetDarkBgSecondary, SunsetDarkSurface, SunsetDarkElevated, SunsetAmberLight, SunsetCoral)
                } else {
                    GlassColors(SunsetLightBg, SunsetLightBgSecondary, PearlWhiteSurface, PearlWhiteSurface, SunsetAmber, SunsetCoralLightMode)
                }

                AppThemeStyle.NORDIC_FOREST -> if (isDark) {
                    GlassColors(ForestDarkBg, ForestDarkBgSecondary, ForestDarkSurface, ForestDarkElevated, ForestGreenDarkMode, ForestTealDarkMode)
                } else {
                    GlassColors(ForestLightBg, ForestLightBgSecondary, PearlWhiteSurface, PearlWhiteSurface, ForestGreenLightMode, ForestTealLightMode)
                }

                else -> GlassColors(DeepNavyBg, DeepPetrolBgSecondary, MidnightIndigoSurface, DarkSurfaceVariant, EmeraldLight, EmeraldTeal)
            }

            val lightInk = when (style) {
                AppThemeStyle.SUNSET_AMBER -> Color(0xFF231712)
                AppThemeStyle.VELVET_VIOLET -> Color(0xFF1B152B)
                AppThemeStyle.NORDIC_FOREST -> Color(0xFF10241B)
                else -> DarkTextPrimary
            }
            val lightSecondaryInk = when (style) {
                AppThemeStyle.SUNSET_AMBER -> Color(0xFF6B5347)
                AppThemeStyle.VELVET_VIOLET -> Color(0xFF5D5374)
                AppThemeStyle.NORDIC_FOREST -> Color(0xFF476356)
                else -> DarkTextSecondary
            }

            val lightFrostBase = lerp(Color.White, bgSec, 0.22f)
            val lightFrostElevated = lerp(Color.White, bgSec, 0.08f)

            val glassBase = when (quality) {
                GlassQuality.ULTRA -> if (isDark) surf.copy(alpha = 0.62f) else lightFrostBase.copy(alpha = 0.56f)
                GlassQuality.BALANCED -> if (isDark) surf.copy(alpha = 0.78f) else lightFrostBase.copy(alpha = 0.68f)
                GlassQuality.MINIMAL -> if (isDark) surf.copy(alpha = 0.92f) else Color(0xEBFFFFFF)
            }
            val glassHigh = when (quality) {
                GlassQuality.ULTRA -> if (isDark) surfElev.copy(alpha = 0.75f) else lightFrostElevated.copy(alpha = 0.66f)
                GlassQuality.BALANCED -> if (isDark) surfElev.copy(alpha = 0.86f) else lightFrostElevated.copy(alpha = 0.78f)
                GlassQuality.MINIMAL -> if (isDark) surfElev.copy(alpha = 0.96f) else Color(0xF5FFFFFF)
            }

            val lightBorderSoft = lerp(lightInk, primary, 0.25f)

            ThemePaletteSpec(
                bg = bg,
                bgSecondary = bgSec,
                surface = if (isDark) surf else lightFrostElevated,
                surfaceElevated = if (isDark) surfElev else Color.White,
                surfaceVariant = if (isDark) surfElev else lerp(bgSec, primary, 0.10f),
                textPrimary = if (isDark) LightTextPrimary else lightInk,
                textSecondary = if (isDark) LightTextSecondary else lightSecondaryInk,
                textDisabled = if (isDark) DarkTextDisabled else LightTextDisabled,
                accentPrimary = primary,
                accentSecondary = secondary,
                divider = if (isDark) DarkDivider else lightInk.copy(alpha = 0.10f),
                reliefLight = Color.White.copy(alpha = if (isDark) 0.22f else 0.95f),
                reliefDark = if (isDark) Color(0x66020812) else lightBorderSoft.copy(alpha = 0.14f),
                glassSurface = glassBase,
                glassElevated = glassHigh,
                borderTop = if (isDark) Color.White.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.95f),
                borderBottom = if (isDark) Color.White.copy(alpha = 0.08f) else lightBorderSoft.copy(alpha = 0.14f),
                borderUnified = if (isDark) Color.White.copy(alpha = 0.19f) else lightBorderSoft.copy(alpha = 0.14f),
                pillBg = primary.copy(alpha = if (isDark) 0.22f else 0.14f)
            )
        }
    }
}

data class ThemePreviewColors(
    val bg: Color,
    val bgSecondary: Color,
    val surface: Color,
    val elevated: Color,
    val text: Color,
    val subtext: Color,
    val accent: Color,
    val accentSecondary: Color,
    val reliefLight: Color,
    val reliefDark: Color,
    val border: Color
)

fun themePreviewColors(style: AppThemeStyle, isDark: Boolean): ThemePreviewColors {
    val spec = resolveThemeSpec(style = style, isDark = isDark, quality = GlassQuality.ULTRA)
    return ThemePreviewColors(
        bg = spec.bg,
        bgSecondary = spec.bgSecondary,
        surface = spec.surface,
        elevated = spec.surfaceElevated,
        text = spec.textPrimary,
        subtext = spec.textSecondary,
        accent = spec.accentPrimary,
        accentSecondary = spec.accentSecondary,
        reliefLight = spec.reliefLight,
        reliefDark = spec.reliefDark,
        border = spec.borderUnified
    )
}

private data class GlassColors(
    val bg: Color,
    val bgSec: Color,
    val surf: Color,
    val surfElev: Color,
    val primary: Color,
    val secondary: Color
)
