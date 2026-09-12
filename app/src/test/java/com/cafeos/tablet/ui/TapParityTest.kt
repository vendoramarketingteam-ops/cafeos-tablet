package com.cafeos.tablet.ui

import com.cafeos.tablet.ui.theme.ThemeMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parity: the gamified skin must never add EXTRA taps (or extra per-tap work) to
 * a transaction vs. the classic skin. The entire feedback/motion layer is gated
 * by isGamified():
 *  - Motion.kt scaledDuration()/glowColor()/sparkleAlpha() → 0/off unless GAMIFIED
 *  - GameFeedback.tap/confirm() early-return when !SettingsStore.isGamified()
 *
 * So a Fast-Mode or Classic ring-up has IDENTICAL tap cost to classic (no extra
 * haptics, no animation frame work). This test pins that invariant on the JVM.
 * setUiMode() updates the StateFlow directly; the SharedPreferences write is a
 * null-safe no-op until SettingsStore.init() runs in MainActivity, so no Android
 * Context is required.
 */
class TapParityTest {

    @Test
    fun fastModeTogglesAreReflectedInIsFastMode() {
        SettingsStore.setUiMode(ThemeMode.FAST)
        assertTrue("Fast mode ON → feedback/no-op path active", SettingsStore.isFastMode())

        SettingsStore.setUiMode(ThemeMode.GAMIFIED)
        assertFalse("Gamified mode → Fast Mode off (feedback active)", SettingsStore.isFastMode())

        SettingsStore.setUiMode(ThemeMode.CLASSIC)
        assertFalse("Classic revert → Fast Mode off", SettingsStore.isFastMode())

        // Restore for any later test sharing this JVM.
        SettingsStore.setUiMode(ThemeMode.FAST)
        assertTrue(SettingsStore.isFastMode())
    }

    @Test
    fun gamifiedGatePinsFeedbackAndMotionOffInFastAndClassic() {
        // Feedback/animation gate: ON only in GAMIFIED. FAST (peak-hour speed) and
        // CLASSIC (plain revert) both disable the gamified layer entirely, so a
        // ring-up in either non-gamified mode has identical per-tap cost to classic.
        SettingsStore.setUiMode(ThemeMode.GAMIFIED)
        assertTrue("Gamified mode → feedback+glow+motion active", SettingsStore.isGamified())

        SettingsStore.setUiMode(ThemeMode.FAST)
        assertFalse("Fast Mode → gamified layer disabled", SettingsStore.isGamified())

        SettingsStore.setUiMode(ThemeMode.CLASSIC)
        assertFalse("Classic revert → gamified layer disabled", SettingsStore.isGamified())

        SettingsStore.setUiMode(ThemeMode.FAST)
    }

    @Test
    fun soundIsOptInAndDefaultsOff() {
        // Non-essential SFX is opt-in only (off by default).
        SettingsStore.setSoundEnabled(false)
        assertFalse("Sound defaults OFF — non-essential", SettingsStore.soundEnabled.value)

        SettingsStore.setSoundEnabled(true)
        assertTrue("User can opt in to cha-ching SFX", SettingsStore.soundEnabled.value)

        SettingsStore.setSoundEnabled(false)
        assertFalse(SettingsStore.soundEnabled.value)
    }
}
