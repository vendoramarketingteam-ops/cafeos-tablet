package com.cafeos.tablet.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafeos.tablet.ui.theme.CurrencyCountUpEasing
import com.cafeos.tablet.ui.theme.Dimens
import com.cafeos.tablet.ui.theme.RarityEpic
import com.cafeos.tablet.ui.theme.RarityRare
import com.cafeos.tablet.ui.theme.RarityUncommon
import com.cafeos.tablet.ui.theme.RarityCommon
import com.cafeos.tablet.ui.theme.PosCoffeeLight
import com.cafeos.tablet.ui.theme.PosGold
import com.cafeos.tablet.ui.theme.PosNeon
import com.cafeos.tablet.ui.theme.PosPaper
import com.cafeos.tablet.ui.theme.isClassic
import com.cafeos.tablet.ui.theme.isGamified
import com.cafeos.tablet.ui.theme.scaledDuration
import com.cafeos.tablet.ui.theme.glowColor
import com.cafeos.tablet.ui.theme.PosDanger
import com.cafeos.tablet.ui.theme.PosAccent
import com.cafeos.tablet.ui.theme.PosInk

/**
 * Rarity corner badge for product cards.
 * - EPIC (gold)  => best-seller
 * - RARE (blue)  => featured
 * - LOW_STOCK variant rendered via [lowStock] using red pulse (icon+text, not
 *   color alone — always paired with a numeric label elsewhere).
 * Color is supplementary; stock/status is always also conveyed by icon+text.
 */
enum class Rarity { COMMON, UNCOMMON, RARE, EPIC }

@Composable
fun RarityBadge(
    rarity: Rarity,
    modifier: Modifier = Modifier,
    label: String = ""
) {
    val tint = when (rarity) {
        Rarity.COMMON -> RarityCommon
        Rarity.UNCOMMON -> RarityUncommon
        Rarity.RARE -> RarityRare
        Rarity.EPIC -> RarityEpic
    }
    Box(
        modifier = modifier
            .background(color = tint.copy(alpha = 0.15f), shape = RoundedCornerShape(Dimens.space4))
            .border(width = 1.5.dp, color = tint, shape = RoundedCornerShape(Dimens.space4))
            .padding(horizontal = Dimens.space4, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Top-bar loyalty gem / currency counter with a skippable count-up tween.
 * In Fast Mode the number updates instantly (duration 0).
 */
@Composable
fun GemCounter(
    count: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val animated by animateIntAsState(
        targetValue = count,
        animationSpec = tween(
            durationMillis = scaledDuration(700),
            easing = CurrencyCountUpEasing
        )
    )
    val clickableModifier = if (onClick != null) {
        modifier
            .background(color = PosPaper.copy(alpha = 0.10f), shape = CircleShape)
            .border(1.dp, PosNeon.copy(alpha = 0.5f), CircleShape)
            .padding(horizontal = Dimens.space8, vertical = Dimens.space4)
            .clickable(onClick = onClick)
    } else {
        modifier
            .background(color = PosPaper.copy(alpha = 0.10f), shape = CircleShape)
            .border(1.dp, PosNeon.copy(alpha = 0.5f), CircleShape)
            .padding(horizontal = Dimens.space8, vertical = Dimens.space4)
    }
    Box(
        modifier = clickableModifier,
        contentAlignment = Alignment.Center
    ) {
        GemIcon(Modifier.size(Dimens.space16))
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(Dimens.space4))
        Text(
            text = animated.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = PosGold,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * MOBA inventory-style stack counter: a small pill badge overlaid on an item
 * corner (e.g. cart quantity, product stock-on-hand).
 */
@Composable
fun StackCounter(
    quantity: Int,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = PosGold
) {
    if (quantity <= 0) return
    val text = if (quantity > 99) "99+" else quantity.toString()
    Box(
        modifier = modifier
            .background(color = color, shape = CircleShape)
            .padding(horizontal = Dimens.space4, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.Bold,
            color = PosPaper
        )
    }
}

/** Lightweight vector gem icon drawn with Canvas (no asset dependency). */
@Composable
fun GemIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size
        val cut = Path().apply {
            moveTo(s.width / 2f, 0f)
            lineTo(s.width, s.height / 2f)
            lineTo(s.width / 2f, s.height)
            lineTo(0f, s.height / 2f)
            close()
        }
        drawPath(path = cut, color = PosNeon, style = Stroke(width = 2.dp.toPx()))
        // inner glow quad
        drawPath(
            path = cut,
            color = PosGold.copy(alpha = 0.85f)
        )
    }
}

/** Tint color for a given rarity tier (item-slot border badge color). */
fun rarityColor(rarity: Rarity): Color = when (rarity) {
    Rarity.COMMON -> RarityCommon
    Rarity.UNCOMMON -> RarityUncommon
    Rarity.RARE -> RarityRare
    Rarity.EPIC -> RarityEpic
}

/**
 * Price-tier rarity used by the item-shop grids (Mobile Legends item-value tiers).
 * Mirrors POSScreen's price bands: ≥₱150 EPIC, ≥₱80 RARE, ≥₱40 UNCOMMON, else COMMON.
 */
fun rarityByPrice(price: Double): Rarity = when {
    price >= 150.0 -> Rarity.EPIC
    price >= 80.0 -> Rarity.RARE
    price >= 40.0 -> Rarity.UNCOMMON
    else -> Rarity.COMMON
}

/**
 * Loyalty-points tier: ≥500 EPIC, ≥200 RARE, ≥50 UNCOMMON, else COMMON.
 * (A "legendary customer" glows the same way a high-value order glows.) */
fun rarityByPoints(points: Int): Rarity = when {
    points >= 500 -> Rarity.EPIC
    points >= 200 -> Rarity.RARE
    points >= 50 -> Rarity.UNCOMMON
    else -> Rarity.COMMON
}

/**
 * MOBA item-slot card: a rarity-framed container. The border is the rarity tint
 * (gold/blue/green/gray — like ML item-tier frames), full-strength in GAMIFIED
 * and flat-tinted in FAST (spec §3 "flatter variant"); the elevation lift gives
 * the neon "rare item" glow without an extra drawable, plain in CLASSIC (FR-004).
 * No corner badge/gem is overlaid so it never collides with a card's own leading
 * /trailing content — the frame itself *is* the rarity signal.
 *
 * @param selected When true, renders a RarityPulse neon glow ring (Zone D selection
 *   state). Only visible in GAMIFIED mode; in FAST/CLASSIC the glow is suppressed.
 * @param progress Optional 0–1 stock/quantity progress bar drawn as a thin bar
 *   at the bottom edge of the card frame. Color transitions green→amber→red.
 *   Omitted (null) means no progress bar is drawn.
 */
@Composable
fun GameCard(
    modifier: Modifier = Modifier,
    rarity: Rarity = Rarity.COMMON,
    selected: Boolean = false,
    progress: Float? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val tint = rarityColor(rarity)
    val gamified = isGamified()
    val classic = isClassic()
    val glow = if (selected && gamified) glowColor() else Color.Transparent
    val borderWidth = if (gamified) 2.dp else 1.5.dp
    val borderColor = if (gamified) {
        if (selected) {
            // Blend rarity tint with the neon selection glow
            lerp(tint, glow, 0.5f)
        } else tint
    } else tint.copy(alpha = 0.6f)
    Card(
        modifier = modifier
            .then(
                if (classic) Modifier else Modifier.border(
                    width = borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(Dimens.radiusLarge)
                )
            )
            .then(
                if (glow != Color.Transparent) {
                    val animatedGlow by animateFloatAsState(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = scaledDuration(800))
                    )
                    Modifier.border(
                        width = (Dimens.space4 * animatedGlow),
                        color = glow,
                        shape = RoundedCornerShape(Dimens.radiusXLarge)
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(Dimens.radiusLarge),
        colors = CardDefaults.cardColors(containerColor = PosCoffeeLight),
        elevation = CardDefaults.cardElevation(defaultElevation = if (gamified) Dimens.space4 else 2.dp)
    ) {
        Box {
            content()
            // Optional stock/quantity progress bar at the bottom edge
            progress?.let { p ->
                val safeP = p.coerceIn(0f, 1f)
                val progressColor = when {
                    safeP <= 0.25f -> PosDanger
                    safeP <= 0.5f -> PosGold
                    else -> PosAccent
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.space4)
                        .align(Alignment.BottomStart)
                        .background(progressColor.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width((safeP * 100).coerceAtLeast(4f).dp)
                            .background(progressColor)
                    )
                }
            }
        }
    }
}

// ── Phase 4: Motion primitives for the Item Shop skin ─────────────────────

/**
 * RarityPulse — a selection glow ring that pulses with the item's rarity tint
 * (spec CHK005, Zone D). Replaces the generic neon [glowColor] with a rarity-
 * colored pulse so EPIC items glow gold, RARE items glow blue, etc.
 *
 * In FAST/CLASSIC mode the pulse is fully suppressed (same as [glowColor]),
 * so the card falls back to its static rarity border only.
 *
 * Usage: apply as a modifier on top of GameCard, e.g.:
 *   Modifier.rarityPulse(rarity = item.rarity, selected = selected)
 */
@Composable
fun Modifier.rarityPulse(
    rarity: Rarity,
    selected: Boolean,
    borderWidth: Dp = Dimens.space4
): Modifier {
    // FAST & CLASSIC: no pulse — return unchanged (GameCard already applies
    // a flat rarity border in those modes).
    if (!selected || !isGamified()) return this

    val tint = rarityColor(rarity)
    val transition = rememberInfiniteTransition(label = "rarityPulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = scaledDuration(1600),
                easing = CurrencyCountUpEasing
            ),
            repeatMode = RepeatMode.Reverse
        )
    )
    return this.border(
        width = borderWidth,
        color = tint.copy(alpha = pulseAlpha),
        shape = RoundedCornerShape(Dimens.radiusXLarge)
    )
}

/**
 * CurrencyCountUp — animated peso-amount text with a skippable count-up tween
 * (spec CHK041, Zone H). In FAST mode [scaledDuration] returns 0, so the number
 * snaps to its final value instantly.
 *
 * Pass the amount in cents (Long) for integer-precision money math, matching
 * the rest of the café app (constitution §II: no float money errors).
 */
@Composable
fun CurrencyCountUp(
    amountCents: Long,
    modifier: Modifier = Modifier,
    currencyLabel: String = "₱"
) {
    val pesoTarget = amountCents / 100.0
    val animated by animateFloatAsState(
        targetValue = pesoTarget.toFloat(),
        animationSpec = tween(
            durationMillis = scaledDuration(700),
            easing = CurrencyCountUpEasing
        )
    )
    val text = "$currencyLabel${String.format("%.2f", animated)}"
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.headlineMedium,
        color = if (isGamified()) PosGold else PosInk,
        fontWeight = FontWeight.Black
    )
}
