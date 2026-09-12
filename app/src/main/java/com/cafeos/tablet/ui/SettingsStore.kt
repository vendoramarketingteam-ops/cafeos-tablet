package com.cafeos.tablet.ui

import android.content.Context
import android.content.SharedPreferences
import com.cafeos.tablet.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Presentation preferences for the gamified skin.
 *
 * Backed by the existing `pebot_sync` SharedPreferences (same prefs file the
 * sync layer already uses) so we add no new storage dependency. These are UI
 * prefs only — they never touch orders, pricing, inventory, or receipts
 * (CaféOS Constitution II / V).
 *
 * `init()` MUST be called once (from MainActivity.onCreate, before setContent)
 * so the preference store is seeded before any composable reads it.
 */
object SettingsStore {
    private const val PREFS = "pebot_sync"
    private const val KEY_UI_MODE = "ui_mode"
    private const val KEY_SOUND = "ui_sound"

    private var prefs: SharedPreferences? = null

    private val _uiMode = MutableStateFlow(ThemeMode.FAST)
    val uiMode: StateFlow<ThemeMode> = _uiMode.asStateFlow()

    private val _soundEnabled = MutableStateFlow(false)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE).also { p ->
            _uiMode.value = parseMode(p.getString(KEY_UI_MODE, ThemeMode.FAST.name))
            _soundEnabled.value = p.getBoolean(KEY_SOUND, false)
        }
    }

    fun setUiMode(mode: ThemeMode) {
        prefs?.edit()?.putString(KEY_UI_MODE, mode.name)?.apply()
        _uiMode.value = mode
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_SOUND, enabled)?.apply()
        _soundEnabled.value = enabled
    }

    fun isFastMode(): Boolean = _uiMode.value == ThemeMode.FAST

    /** Feedback/animation gate: ON only in GAMIFIED mode (OFF in FAST & CLASSIC). */
    fun isGamified(): Boolean = _uiMode.value == ThemeMode.GAMIFIED

    private fun parseMode(raw: String?): ThemeMode = try {
        ThemeMode.valueOf(raw ?: ThemeMode.FAST.name)
    } catch (_: Exception) {
        ThemeMode.FAST
    }
}
