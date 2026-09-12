package com.cafeos.tablet.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

/**
 * Optional MOBA "feel" feedback: a light haptic tick on tap and an optional
 * "cha-ching" SFX on add-to-cart / payment-confirm.
 *
 * - OFF by default in Fast Mode (constitution IV: motion is purposeful & fast).
 * - Sound is optional and only plays when the `cha_ching` raw asset exists AND
 *   the user enabled it in Settings (default off). We look the asset up
 *   dynamically so the build compiles whether or not the asset is shipped.
 * - Haptics are short one-shots (<15ms) and never delay a transaction.
 */
object GameFeedback {
    private var soundPool: SoundPool? = null
    private var confirmSoundId: Int = 0

    private fun ensureSoundPool(context: Context) {
        if (soundPool == null) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val pool = SoundPool.Builder().setMaxStreams(1).setAudioAttributes(attrs).build()
            // Graceful: returns 0 if the asset is absent.
            val resId = context.resources.getIdentifier("cha_ching", "raw", context.packageName)
            if (resId != 0) confirmSoundId = pool.load(context, resId, 1)
            soundPool = pool
        }
    }

    private fun doHaptic(context: Context) {
        try {
            val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE)
            else @Suppress("DEPRECATION") VibrationEffect.createOneShot(12, -1)
            vib.vibrate(effect)
        } catch (_: Exception) { /* haptics never block a transaction */ }
    }

    /** Light tap tick (add-to-cart). No-op outside gamified mode (FAST/CLASSIC). */
    fun tap(context: Context) {
        if (!SettingsStore.isGamified()) return
        doHaptic(context)
    }

    /** Confirm tick + optional cha-ching (checkout confirm). No-op outside gamified mode. */
    fun confirm(context: Context, soundEnabled: Boolean = false) {
        if (!SettingsStore.isGamified()) return
        doHaptic(context)
        if (soundEnabled) {
            try {
                ensureSoundPool(context)
                if (confirmSoundId != 0) {
                    soundPool?.play(confirmSoundId, 1f, 1f, 1, 0, 1f)
                }
            } catch (_: Exception) { /* sound is purely decorative */ }
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        confirmSoundId = 0
    }
}

/** Convenience modifier: tap feedback + callback, honoring Fast Mode. */
@Composable
fun Modifier.gameTap(onClick: () -> Unit): Modifier {
    val context = LocalContext.current
    return this.clickable {
        val soundEnabled = SettingsStore.soundEnabled.value
        GameFeedback.confirm(context, soundEnabled)
        onClick()
    }
}
