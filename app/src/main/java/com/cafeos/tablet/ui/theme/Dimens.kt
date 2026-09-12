package com.cafeos.tablet.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design-token system for cafeos-tablet (spec 004).
 *
 * All spacing, corner radii, touch-target minimums, and grid constants are
 * defined here as a single source of truth. Flagged Composables must pull
 * values from [Dimens] instead of hardcoding one-off dp literals — no more
 * `.padding(13.dp)`-style magic numbers.
 *
 * Spacing follows an 8-point grid (4, 8, 12, 16, 20, 24, 32, 40).
 * Non-grid values (6, 10, 14) are mapped to the nearest grid step.
 */
object Dimens {

    // ─── Spacing scale (8-point grid) ────────────────────────────────────

    val space4: Dp = 4.dp
    val space8: Dp = 8.dp
    val space12: Dp = 12.dp
    val space16: Dp = 16.dp
    val space20: Dp = 20.dp
    val space24: Dp = 24.dp
    val space32: Dp = 32.dp
    val space40: Dp = 40.dp

    // ─── Corner radii ───────────────────────────────────────────────────

    val radiusSmall: Dp = 8.dp
    val radiusMedium: Dp = 12.dp
    val radiusLarge: Dp = 16.dp
    val radiusXLarge: Dp = 20.dp
    val radiusPill: Dp = 999.dp

    // ─── Touch targets (Android minimum = 48dp) ──────────────────────────

    val touchMin: Dp = 48.dp
    val touchComfortable: Dp = 56.dp

    // ─── Grid & layout ───────────────────────────────────────────────────

    val gridGapSmall: Dp = 8.dp
    val gridGapMedium: Dp = 12.dp
    val gridGapLarge: Dp = 16.dp
    val gridMinCell: Dp = 92.dp
    val gridMaxCell: Dp = 160.dp

    // ─── Breakpoints ─────────────────────────────────────────────────────

    /** Portrait phone breakpoint (matches POSScreen / InventoryScreen). */
    val tabletBreakpoint: Dp = 600.dp

    // ─── Icon sizes ──────────────────────────────────────────────────────

    val iconSmall: Dp = 16.dp
    val iconMedium: Dp = 24.dp
    val iconLarge: Dp = 32.dp
    val iconXL: Dp = 48.dp

    // ─── Component heights ───────────────────────────────────────────────

    val topBarHeight: Dp = 56.dp
    val buttonHeight: Dp = 48.dp
    val buttonHeightComfortable: Dp = 56.dp
    val textFieldHeight: Dp = 56.dp

    // ─── Card / content padding ─────────────────────────────────────────

    val cardPaddingSmall: Dp = 12.dp
    val cardPaddingMedium: Dp = 16.dp
    val cardPaddingLarge: Dp = 24.dp

    // ─── Progress indicators ─────────────────────────────────────────────

    val progressHeight: Dp = 6.dp

    // ─── Border width ────────────────────────────────────────────────────

    val borderWidth: Dp = 1.dp
    val borderWidthEmphasis: Dp = 2.dp

    // ─── Elevation ───────────────────────────────────────────────────────

    val elevationLow: Dp = 2.dp
    val elevationMedium: Dp = 6.dp
}
