package com.dopamine.ratt

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * A short industrial knock when the screen arrives: one hard hit, then two
 * softer echoes. Falls back to plain timings on devices with no amplitude
 * control, and does nothing at all if the phone cannot vibrate.
 */
fun knock(context: Context) {
    val vibrator = vibrator(context) ?: return
    if (!vibrator.hasVibrator()) return

    val timings = longArrayOf(0, 42, 80, 26, 60, 90)
    val amplitudes = intArrayOf(0, 255, 0, 150, 0, 70)

    val effect = if (vibrator.hasAmplitudeControl()) {
        VibrationEffect.createWaveform(timings, amplitudes, -1)
    } else {
        VibrationEffect.createWaveform(timings, -1)
    }

    runCatching { vibrator.vibrate(effect) }
}

private fun vibrator(context: Context): Vibrator? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
}.getOrNull()
