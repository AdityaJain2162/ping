package com.aditya.ping.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.aditya.ping.domain.ThemeMode
import com.kyant.m3color.hct.Hct
import com.kyant.m3color.scheme.SchemeMonochrome
import com.kyant.m3color.scheme.SchemeNeutral
import com.kyant.m3color.scheme.SchemeTonalSpot

private fun m3Scheme(seedColor: Color, isDark: Boolean, contrastLevel: Double = 0.0) =
    Hct.fromInt(seedColor.toArgb()).let { hct ->
        when {
            hct.chroma < 4.0 -> SchemeMonochrome(hct, isDark, contrastLevel)
            hct.chroma < 12.0 -> SchemeNeutral(hct, isDark, contrastLevel)
            else -> SchemeTonalSpot(hct, isDark, contrastLevel)
        }
    }

private fun Int.toComposeColor(): Color = Color(this.toLong() and 0xFFFFFFFFL)

fun hctLightScheme(seed: Color): ColorScheme {
    val s = m3Scheme(seed, isDark = false)
    return lightColorScheme(
        primary = s.primary.toComposeColor(),
        onPrimary = s.onPrimary.toComposeColor(),
        primaryContainer = s.primaryContainer.toComposeColor(),
        onPrimaryContainer = s.onPrimaryContainer.toComposeColor(),
        secondary = s.secondary.toComposeColor(),
        onSecondary = s.onSecondary.toComposeColor(),
        secondaryContainer = s.secondaryContainer.toComposeColor(),
        onSecondaryContainer = s.onSecondaryContainer.toComposeColor(),
        tertiary = s.tertiary.toComposeColor(),
        onTertiary = s.onTertiary.toComposeColor(),
        tertiaryContainer = s.tertiaryContainer.toComposeColor(),
        onTertiaryContainer = s.onTertiaryContainer.toComposeColor(),
        error = s.error.toComposeColor(),
        onError = s.onError.toComposeColor(),
        errorContainer = s.errorContainer.toComposeColor(),
        onErrorContainer = s.onErrorContainer.toComposeColor(),
        background = s.background.toComposeColor(),
        onBackground = s.onBackground.toComposeColor(),
        surface = s.surface.toComposeColor(),
        onSurface = s.onSurface.toComposeColor(),
        surfaceVariant = s.surfaceVariant.toComposeColor(),
        onSurfaceVariant = s.onSurfaceVariant.toComposeColor(),
        outline = s.outline.toComposeColor(),
        outlineVariant = s.outlineVariant.toComposeColor(),
        surfaceContainer = s.surfaceContainer.toComposeColor(),
        surfaceContainerLow = s.surfaceContainerLow.toComposeColor(),
        surfaceContainerLowest = s.surfaceContainerLowest.toComposeColor(),
        surfaceContainerHigh = s.surfaceContainerHigh.toComposeColor(),
        surfaceContainerHighest = s.surfaceContainerHighest.toComposeColor(),
        surfaceBright = s.surfaceBright.toComposeColor(),
        surfaceDim = s.surfaceDim.toComposeColor(),
        inverseSurface = s.inverseSurface.toComposeColor(),
        inverseOnSurface = s.inverseOnSurface.toComposeColor(),
        inversePrimary = s.inversePrimary.toComposeColor(),
        scrim = s.scrim.toComposeColor(),
    )
}

fun hctDarkScheme(seed: Color): ColorScheme {
    val s = m3Scheme(seed, isDark = true)
    return darkColorScheme(
        primary = s.primary.toComposeColor(),
        onPrimary = s.onPrimary.toComposeColor(),
        primaryContainer = s.primaryContainer.toComposeColor(),
        onPrimaryContainer = s.onPrimaryContainer.toComposeColor(),
        secondary = s.secondary.toComposeColor(),
        onSecondary = s.onSecondary.toComposeColor(),
        secondaryContainer = s.secondaryContainer.toComposeColor(),
        onSecondaryContainer = s.onSecondaryContainer.toComposeColor(),
        tertiary = s.tertiary.toComposeColor(),
        onTertiary = s.onTertiary.toComposeColor(),
        tertiaryContainer = s.tertiaryContainer.toComposeColor(),
        onTertiaryContainer = s.onTertiaryContainer.toComposeColor(),
        error = s.error.toComposeColor(),
        onError = s.onError.toComposeColor(),
        errorContainer = s.errorContainer.toComposeColor(),
        onErrorContainer = s.onErrorContainer.toComposeColor(),
        background = s.background.toComposeColor(),
        onBackground = s.onBackground.toComposeColor(),
        surface = s.surface.toComposeColor(),
        onSurface = s.onSurface.toComposeColor(),
        surfaceVariant = s.surfaceVariant.toComposeColor(),
        onSurfaceVariant = s.onSurfaceVariant.toComposeColor(),
        outline = s.outline.toComposeColor(),
        outlineVariant = s.outlineVariant.toComposeColor(),
        surfaceContainer = s.surfaceContainer.toComposeColor(),
        surfaceContainerLow = s.surfaceContainerLow.toComposeColor(),
        surfaceContainerLowest = s.surfaceContainerLowest.toComposeColor(),
        surfaceContainerHigh = s.surfaceContainerHigh.toComposeColor(),
        surfaceContainerHighest = s.surfaceContainerHighest.toComposeColor(),
        surfaceBright = s.surfaceBright.toComposeColor(),
        surfaceDim = s.surfaceDim.toComposeColor(),
        inverseSurface = s.inverseSurface.toComposeColor(),
        inverseOnSurface = s.inverseOnSurface.toComposeColor(),
        inversePrimary = s.inversePrimary.toComposeColor(),
        scrim = s.scrim.toComposeColor(),
    )
}

val LocalAccentPreset = compositionLocalOf { AccentPresets[0] }
val LocalAnimationsEnabled = compositionLocalOf { true }

@Composable
fun PingTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentName: String = "Coral",
    dynamicColor: Boolean = false,
    animationsEnabled: Boolean = true,
    hapticController: HapticController = HapticController(
        enabled = true,
        intensity = HapticIntensity.MEDIUM,
        feedback = NoOpHapticFeedback,
    ),
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }

    val context = LocalContext.current
    val preset = accentPresetByName(accentName)
    val supportsDynamic = dynamicColor && themeMode != ThemeMode.AMOLED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val targetScheme = when {
        supportsDynamic && isDark -> dynamicDarkColorScheme(context)
        supportsDynamic && !isDark -> dynamicLightColorScheme(context)
        themeMode == ThemeMode.AMOLED -> hctDarkScheme(preset.primary).copy(
            background = Color(0xFF000000),
            surface = Color(0xFF000000),
            surfaceVariant = Color(0xFF0A0A0A),
            surfaceContainer = Color(0xFF111111),
            surfaceContainerLow = Color(0xFF0D0D0D),
            surfaceContainerLowest = Color(0xFF000000),
            surfaceContainerHigh = Color(0xFF161616),
            surfaceContainerHighest = Color(0xFF1A1A1A),
            surfaceBright = Color(0xFF222222),
            surfaceDim = Color(0xFF000000),
        )
        isDark -> hctDarkScheme(preset.primary)
        else -> hctLightScheme(preset.primary)
    }

    val animatedScheme = if (animationsEnabled) animateColorScheme(targetScheme) else targetScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    CompositionLocalProvider(
        LocalAccentPreset provides preset,
        LocalAnimationsEnabled provides animationsEnabled,
        LocalHaptics provides hapticController,
    ) {
        MaterialTheme(
            colorScheme = animatedScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme {
    val spec = spring<Color>(stiffness = Spring.StiffnessLow)
    return ColorScheme(
        primary = animateColorAsState(target.primary, spec, label = "primary").value,
        onPrimary = animateColorAsState(target.onPrimary, spec, label = "onPrimary").value,
        primaryContainer = animateColorAsState(target.primaryContainer, spec, label = "pc").value,
        onPrimaryContainer = animateColorAsState(target.onPrimaryContainer, spec, label = "opc").value,
        inversePrimary = animateColorAsState(target.inversePrimary, spec, label = "ip").value,
        secondary = animateColorAsState(target.secondary, spec, label = "sec").value,
        onSecondary = animateColorAsState(target.onSecondary, spec, label = "os").value,
        secondaryContainer = animateColorAsState(target.secondaryContainer, spec, label = "sc").value,
        onSecondaryContainer = animateColorAsState(target.onSecondaryContainer, spec, label = "osc").value,
        tertiary = animateColorAsState(target.tertiary, spec, label = "ter").value,
        onTertiary = animateColorAsState(target.onTertiary, spec, label = "ot").value,
        tertiaryContainer = animateColorAsState(target.tertiaryContainer, spec, label = "tc").value,
        onTertiaryContainer = animateColorAsState(target.onTertiaryContainer, spec, label = "otc").value,
        background = animateColorAsState(target.background, spec, label = "bg").value,
        onBackground = animateColorAsState(target.onBackground, spec, label = "obg").value,
        surface = animateColorAsState(target.surface, spec, label = "surf").value,
        onSurface = animateColorAsState(target.onSurface, spec, label = "osurf").value,
        surfaceVariant = animateColorAsState(target.surfaceVariant, spec, label = "sv").value,
        onSurfaceVariant = animateColorAsState(target.onSurfaceVariant, spec, label = "osv").value,
        surfaceTint = animateColorAsState(target.surfaceTint, spec, label = "st").value,
        inverseSurface = animateColorAsState(target.inverseSurface, spec, label = "is").value,
        inverseOnSurface = animateColorAsState(target.inverseOnSurface, spec, label = "ios").value,
        error = animateColorAsState(target.error, spec, label = "err").value,
        onError = animateColorAsState(target.onError, spec, label = "oerr").value,
        errorContainer = animateColorAsState(target.errorContainer, spec, label = "ec").value,
        onErrorContainer = animateColorAsState(target.onErrorContainer, spec, label = "oec").value,
        outline = animateColorAsState(target.outline, spec, label = "ol").value,
        outlineVariant = animateColorAsState(target.outlineVariant, spec, label = "olv").value,
        scrim = animateColorAsState(target.scrim, spec, label = "scrim").value,
        surfaceBright = animateColorAsState(target.surfaceBright, spec, label = "sb").value,
        surfaceDim = animateColorAsState(target.surfaceDim, spec, label = "sd").value,
        surfaceContainer = animateColorAsState(target.surfaceContainer, spec, label = "scont").value,
        surfaceContainerLow = animateColorAsState(target.surfaceContainerLow, spec, label = "scl").value,
        surfaceContainerLowest = animateColorAsState(target.surfaceContainerLowest, spec, label = "sclst").value,
        surfaceContainerHigh = animateColorAsState(target.surfaceContainerHigh, spec, label = "sch").value,
        surfaceContainerHighest = animateColorAsState(target.surfaceContainerHighest, spec, label = "schst").value,
    )
}
