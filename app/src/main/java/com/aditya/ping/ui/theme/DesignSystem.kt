package com.aditya.ping.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Accent color presets ──────────────────────────────────────────
// Each preset defines a primary + secondary + tertiary triplet
// so the whole palette shifts when the user picks an accent.

data class AccentPreset(
    val name: String,
    val primary: Color,
    val primaryContainer: Color,
    val secondary: Color,
    val secondaryContainer: Color,
    val tertiary: Color,
    val tertiaryContainer: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
)

val AccentPresets = listOf(
    AccentPreset(
        name = "Coral",
        primary = Color(0xFFD8483E),
        primaryContainer = Color(0xFFFFDAD4),
        secondary = Color(0xFF006A6E),
        secondaryContainer = Color(0xFF6FF6FA),
        tertiary = Color(0xFFB45200),
        tertiaryContainer = Color(0xFFFFDDB3),
        gradientStart = Color(0xFFD8483E),
        gradientEnd = Color(0xFFB3261E),
    ),
    AccentPreset(
        name = "Teal",
        primary = Color(0xFF006A6A),
        primaryContainer = Color(0xFF6FF7F6),
        secondary = Color(0xFF4A6363),
        secondaryContainer = Color(0xFFCCE8E7),
        tertiary = Color(0xFF4B6074),
        tertiaryContainer = Color(0xFFDDE5F4),
        gradientStart = Color(0xFF006A6A),
        gradientEnd = Color(0xFF004949),
    ),
    AccentPreset(
        name = "Ocean",
        primary = Color(0xFF0061A4),
        primaryContainer = Color(0xFFD1E4FF),
        secondary = Color(0xFF535F70),
        secondaryContainer = Color(0xFFD7E3F7),
        tertiary = Color(0xFF6B5778),
        tertiaryContainer = Color(0xFFF2DAFF),
        gradientStart = Color(0xFF0061A4),
        gradientEnd = Color(0xFF003E6B),
    ),
    AccentPreset(
        name = "Sunset",
        primary = Color(0xFFB3261E),
        primaryContainer = Color(0xFFFFDAD6),
        secondary = Color(0xFF775656),
        secondaryContainer = Color(0xFFFFDAD6),
        tertiary = Color(0xFF705975),
        tertiaryContainer = Color(0xFFFAD8F8),
        gradientStart = Color(0xFFE6654F),
        gradientEnd = Color(0xFFB3261E),
    ),
    AccentPreset(
        name = "Purple",
        primary = Color(0xFF6750A4),
        primaryContainer = Color(0xFFEADDFF),
        secondary = Color(0xFF625B71),
        secondaryContainer = Color(0xFFE8DEF8),
        tertiary = Color(0xFF7D5260),
        tertiaryContainer = Color(0xFFFFD8E4),
        gradientStart = Color(0xFF6750A4),
        gradientEnd = Color(0xFF4527B0),
    ),
    AccentPreset(
        name = "Forest",
        primary = Color(0xFF386A20),
        primaryContainer = Color(0xFFB8F397),
        secondary = Color(0xFF55624C),
        secondaryContainer = Color(0xFFD7E3CB),
        tertiary = Color(0xFF386666),
        tertiaryContainer = Color(0xFFBBECEB),
        gradientStart = Color(0xFF386A20),
        gradientEnd = Color(0xFF1B5E20),
    ),
)

fun accentPresetByName(name: String): AccentPreset =
    AccentPresets.find { it.name.equals(name, ignoreCase = true) } ?: AccentPresets[0]

// ── Gradients ─────────────────────────────────────────────────────

fun primaryGradient(preset: AccentPreset): Brush =
    Brush.linearGradient(listOf(preset.gradientStart, preset.gradientEnd))

fun softGradient(preset: AccentPreset): Brush =
    Brush.verticalGradient(
        listOf(preset.primaryContainer.copy(alpha = 0.6f), preset.primaryContainer.copy(alpha = 0.15f))
    )

// ── Custom shapes ─────────────────────────────────────────────────

val CardShape = RoundedCornerShape(20.dp)
val PillShape = RoundedCornerShape(28.dp)
val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
val ChipShape = RoundedCornerShape(12.dp)

// ── Animation specs ───────────────────────────────────────────────

val SpringBouncy = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium,
)

val SpringSmooth = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

val TweenSmooth = tween<Float>(
    durationMillis = 300,
    easing = FastOutSlowInEasing,
)

val TweenFadeIn = tween<Float>(
    durationMillis = 250,
    easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f),
)

val TweenSlideIn = tween<androidx.compose.ui.unit.IntOffset>(
    durationMillis = 300,
    easing = FastOutSlowInEasing,
)
