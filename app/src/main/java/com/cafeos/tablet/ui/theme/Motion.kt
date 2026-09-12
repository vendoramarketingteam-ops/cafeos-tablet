package com.cafeos.tablet.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Gamified motion primitives and skippable animation specs.
 *
 * Every spec honors ThemeMode: in FAST mode durations collapse to 0 so the
 * "game" never gets in the way of a quick ring-up (constitution IV: motion is
 * purposeful and fast). All animations are interruptible by nature of using
 * updateAnimator / animate*AsState with oneshot specs (guardrail #5).
 */

// MOBA-style "spring" curve for card tap-scale / item pop.
val ShopSpringEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

// "Coin spend" / currency count-up easing.
val CurrencyCountUpEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

/**
 * Gamified mode is ON only in GAMIFIED. Both FAST (peak-hour speed) and CLASSIC
 * (plain revert) disable non-essential motion/glow/sound (spec §4 guardrail #5,
 * FR-003/FR-004). This keeps a Fast/classic ring-up identical in tap cost to the
 * original UI.
 */
@Composable
fun isGamified(): Boolean = LocalThemeMode.current == ThemeMode.GAMIFIED

/** Flat revert: Classic mode hides the gamified skin entirely (spec FR-004).
 *  Fast Mode keeps the gamified *screens* (spec §3: "flatter, faster variant of the
 *  same screens") — only non-essential motion/glow/sound are disabled, so static
 *  gamified elements (quest cards, rarity badges, gem counter) stay visible in the
 *  default rollout mode. */
@Composable
fun isClassic(): Boolean = LocalThemeMode.current == ThemeMode.CLASSIC

/**
 * Scale a duration by the active theme mode. Non-gamified => 0 (instant),
 * preserving the same composable behavior, only faster.
 */
@Composable
fun scaledDuration(ms: Int): Int = if (!isGamified()) 0 else ms

/** Spec for the tap scale+glow on product cards / buttons. */
@Composable
fun tapScaleSpec(): AnimationSpec<Float> = tween(
    durationMillis = scaledDuration(120),
    easing = ShopSpringEasing
)

/** Spec for the coin/currency spend count-up on checkout confirm. */
@Composable
fun currencyCountUpSpec(durationMs: Int = 700): AnimationSpec<Float> = tween(
    durationMillis = scaledDuration(durationMs),
    easing = CurrencyCountUpEasing
)

/** Glow tint for selected/hovered items (cyan neon); off in FAST & CLASSIC. */
@Composable
fun glowColor(): Color = if (!isGamified()) Color.Transparent else PosNeon.copy(alpha = 0.45f)

/** Alpha for non-essential "sparkle" overlays; off in FAST & CLASSIC. */
@Composable
fun sparkleAlpha(): Float = if (!isGamified()) 0f else 1f

/** Linear progress tween used by quest/checklist fill bars. */
fun progressSpec(durationMs: Int) = tween<Float>(
    durationMillis = durationMs,
    easing = LinearEasing
)
