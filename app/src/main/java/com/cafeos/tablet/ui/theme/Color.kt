package com.cafeos.tablet.ui.theme

import androidx.compose.ui.graphics.Color

val PosInk = Color(0xFF1C1917)
val PosInkSoft = Color(0xFF625D57)
val PosPaper = Color(0xFF1C1917)
val PosSurface = Color(0xFFFFFFFF)
val PosMuted = Color(0xFF918B84)
val PosBorder = Color(0xFFDDD6C8)
val PosAccent = Color(0xFF4A5D3A)
val PosAccentHover = Color(0xFF34452A)
val PosAccentSoft = Color(0xFFE7EEE2)
val PosDanger = Color(0xFFB3261E)
val PosDangerSoft = Color(0xFFF6E4E2)
val PosGold = Color(0xFFB58A4A)
val PosInfo = Color(0xFF5B728A)
val PosCoffee = Color(0xFFF5F3EE)
val PosCoffeeLight = Color(0xFFFBFAF7)
val PosCream = Color(0xFFF7F5F0)
val PosCoffeeDeep = Color(0xFF23271F)

// --- Gamified (MOBA shop) tokens — extend, do not replace, the premium palette above ---

// Neon selection / glow accent (cyan) — used for tap glows, rarity-legendary frames.
val PosNeon = Color(0xFF00E5FF)
val PosNeonGlow = Color(0x8000E5FF)

// Darker game-mode background base (deep navy/charcoal) layered behind the coffee surface.
val PosGameBackground = Color(0xFF0B0E12)

// Rarity corner badges (best-seller = gold/epic, low-stock = red pulse). Color is
// always paired with icon+text per guardrail #4 (never color alone).
val RarityCommon = Color(0xFF9CA3AF)
val RarityUncommon = Color(0xFF4ADE80)
val RarityRare = Color(0xFF3B82F6)
val RarityEpic = PosGold

// Low-stock "rare item running out" pulse color (supplementary to icon+text).
val PosLowStockPulse = PosDanger

// ─── Hub-and-Spoke Navigation palette (spec 003 — from HTML mockup) ───────────
// Espresso / cream / sienna / gold / sage — used by HubScreen and AppTopBar.
val HubEspresso = Color(0xFF1B140F)
val HubEspressoDeep = Color(0xFF241B14)
val HubEspressoDarker = Color(0xFF3A2C1F)
val HubCream = Color(0xFFF4EDE0)
val HubCreamSoft = Color(0xFFEAE1CF)
val HubInk = Color(0xFF241A12)
val HubInkMuted = Color(0xFF8A7A68)
val HubSienna = Color(0xFFB5541E)
val HubSiennaDark = Color(0xFF8F3F14)
val HubSage = Color(0xFF6B7A5E)
val HubSageDark = Color(0xFF4E5C44)
val HubGold = Color(0xFFC9A227)
val HubGoldDark = Color(0xFF9C7D1D)
