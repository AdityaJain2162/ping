package com.aditya.ping.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * App-wide haptic helper. Respects both [enabled] and [intensity].
 *
 * Usage:
 *   val haptics = LocalHaptics.current
 *   haptics.tap()        // light tap — buttons, chips, swatches
 *   haptics.confirm()    // medium — toggle switches, selection confirm
 *   haptics.heavy()      // strong — destructive actions, marking attendance
 *   haptics.reject()     // double-pulse — invalid action
 */
class HapticController(
    private val enabled: Boolean,
    private val intensity: HapticIntensity,
    private val feedback: HapticFeedback,
) {
    fun tap() = perform(HapticFeedbackType.TextHandleMove, intensity.tapMultiplier)
    fun confirm() = perform(HapticFeedbackType.LongPress, intensity.confirmMultiplier)
    fun heavy() = perform(HapticFeedbackType.LongPress, intensity.heavyMultiplier)
    fun reject() = perform(HapticFeedbackType.LongPress, intensity.rejectMultiplier)

    private fun perform(type: HapticFeedbackType, multiplier: Float) {
        if (!enabled || multiplier <= 0f) return
        // HapticFeedback API doesn't expose amplitude directly, but performing
        // the feedback type n times simulates intensity (clamped to 1..3).
        val times = multiplier.toInt().coerceIn(1, 3)
        repeat(times) { feedback.performHapticFeedback(type) }
    }
}

enum class HapticIntensity(val tapMultiplier: Float, val confirmMultiplier: Float, val heavyMultiplier: Float, val rejectMultiplier: Float) {
    LOW(tapMultiplier = 1f, confirmMultiplier = 1f, heavyMultiplier = 1f, rejectMultiplier = 1f),
    MEDIUM(tapMultiplier = 1f, confirmMultiplier = 2f, heavyMultiplier = 2f, rejectMultiplier = 2f),
    HIGH(tapMultiplier = 2f, confirmMultiplier = 3f, heavyMultiplier = 3f, rejectMultiplier = 3f),
    OFF(tapMultiplier = 0f, confirmMultiplier = 0f, heavyMultiplier = 0f, rejectMultiplier = 0f);

    companion object {
        fun fromName(name: String?): HapticIntensity =
            entries.firstOrNull { it.name == name } ?: MEDIUM
    }
}

val LocalHaptics = compositionLocalOf {
    HapticController(
        enabled = true,
        intensity = HapticIntensity.MEDIUM,
        feedback = NoOpHapticFeedback,
    )
}

internal object NoOpHapticFeedback : HapticFeedback {
    override fun performHapticFeedback(type: HapticFeedbackType) { }
}

@Composable
fun rememberHapticController(
    enabled: Boolean,
    intensityName: String,
): HapticController {
    val feedback = LocalHapticFeedback.current
    return HapticController(
        enabled = enabled,
        intensity = HapticIntensity.fromName(intensityName),
        feedback = feedback,
    )
}
