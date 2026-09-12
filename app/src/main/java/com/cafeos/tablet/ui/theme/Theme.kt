package com.cafeos.tablet.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode { GAMIFIED, FAST, CLASSIC }

/**
 * Readable from any composable to adapt visuals/animations to the active UI mode.
 * - GAMIFIED: full MOBA skin + animations.
 * - FAST: flat variant, all non-essential animation duration = 0.
 * - CLASSIC: revert-to-vanilla look (existing palette, no game accents).
 */
val LocalThemeMode: ProvidableCompositionLocal<ThemeMode> = staticCompositionLocalOf { ThemeMode.GAMIFIED }

/** Multiplier applied to all non-essential motion; 0 in Fast Mode. */
val ThemeMode.animationScale: Float
    @ReadOnlyComposable
    get() = if (this == ThemeMode.FAST) 0f else 1f

private val DarkColorScheme = darkColorScheme(
    primary = PosAccent,
    onPrimary = Color.White,
    primaryContainer = PosAccentSoft,
    onPrimaryContainer = PosAccentHover,
    secondary = PosGold,
    onSecondary = PosCoffee,
    tertiary = PosCream,
    background = PosCoffee,
    onBackground = PosInk,
    surface = PosCoffeeLight,
    onSurface = PosInk,
    surfaceVariant = PosCoffeeLight,
    onSurfaceVariant = PosInkSoft,
    error = PosDanger,
    onError = Color.White,
    errorContainer = PosDangerSoft,
    onErrorContainer = PosDanger,
    outline = PosBorder,
    outlineVariant = PosMuted
)

private val LightColorScheme = lightColorScheme(
    primary = PosAccent,
    onPrimary = Color.White,
    primaryContainer = PosAccentSoft,
    onPrimaryContainer = PosAccentHover,
    secondary = PosGold,
    onSecondary = PosCoffee,
    tertiary = PosCream,
    background = PosCoffee,
    onBackground = PosInk,
    surface = PosSurface,
    onSurface = PosInk,
    surfaceVariant = PosMuted,
    onSurfaceVariant = PosInkSoft,
    error = PosDanger,
    onError = Color.White,
    errorContainer = PosDangerSoft,
    onErrorContainer = PosDanger,
    outline = PosBorder,
    outlineVariant = PosMuted
)

/**
 * CLASSIC = revert-to-vanilla look: neutral white/ink palette, no game accents.
 * This is a full visual revert (per FR-004) on top of Motion/GameFeedback already
 * disabling gamified motion+flashy feedback in CLASSIC.
 */
private val ClassicColorScheme = lightColorScheme(
    primary = PosAccent,
    onPrimary = PosCoffee,
    primaryContainer = PosAccentSoft,
    onPrimaryContainer = PosAccentHover,
    secondary = PosGold,
    onSecondary = PosCoffee,
    tertiary = PosCream,
    background = PosSurface,
    onBackground = PosInk,
    surface = PosSurface,
    onSurface = PosInk,
    surfaceVariant = PosMuted,
    onSurfaceVariant = PosInkSoft,
    error = PosDanger,
    onError = Color.White,
    errorContainer = PosDangerSoft,
    onErrorContainer = PosDanger,
    outline = PosBorder,
    outlineVariant = PosMuted
)

@Composable
fun PebotTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    themeMode: ThemeMode = ThemeMode.GAMIFIED,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        themeMode == ThemeMode.CLASSIC -> ClassicColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalThemeMode provides themeMode
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
