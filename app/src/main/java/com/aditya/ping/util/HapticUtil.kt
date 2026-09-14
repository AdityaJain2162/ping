package com.aditya.ping.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Lightweight haptic feedback helper.
 * Uses short vibrations for common interactions.
 */
object HapticUtil {

    fun toggle(context: Context) {
        perform(context, VibrationEffect.EFFECT_TICK, 20)
    }

    fun complete(context: Context) {
        // Double pulse — feels like a satisfying "click click"
        perform(context, longArrayOf(0, 40, 60, 40), -1)
    }

    fun delete(context: Context) {
        // Single heavy pulse
        perform(context, longArrayOf(0, 30), -1)
    }

    fun swipe(context: Context) {
        perform(context, VibrationEffect.EFFECT_TICK, 15)
    }

    private fun perform(context: Context, effectId: Int, fallbackDuration: Int) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(effectId))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(fallbackDuration.toLong())
            }
        } catch (e: Exception) {
            // Haptics not available — silent fail
        }
    }

    private fun perform(context: Context, pattern: LongArray, repeat: Int) {
        try {
            val vibrator = getVibrator(context) ?: return
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, repeat))
        } catch (e: Exception) {
            // Haptics not available — silent fail
        }
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
