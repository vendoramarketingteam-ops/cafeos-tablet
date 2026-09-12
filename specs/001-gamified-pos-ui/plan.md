# Implementation Plan: Gamified POS UI/UX Transformation (MOBA Shop Skin)

**Branch**: `001-gamified-pos-ui` | **Date**: 2026-09-10 | **Spec**: `specs/001-gamified-pos-ui/spec.md`

**Input**: Feature specification from `/specs/001-gamified-pos-ui/spec.md` (Section 10 documents open design items.)

---

## Summary

Re-skin the existing CaféOS tablet POS (native Jetpack Compose, single-activity MVVM) with a MOBA item-shop / victory-screen aesthetic, **adding zero behavioral changes** to pricing, tax, discounts, inventory deduction, payment, receipts, access control, or offline sync. The gamified layer is implemented as an extension of the existing `PebotTheme` design-token system plus restyled composables, gated by a **Fast Mode** + **Classic Mode** settings toggle (revert-to-classic without rebuild). All money/receipt rendering stays numerically identical; legality-first verification (receipt field diff + tap-parity) gates the definition of done.

---

## Phase 0 — Research (audit)

Already executed (see spec.md §0 Architecture & Risk Note). Ground truth verified by a clean `./gradlew assembleDebug` (BUILD SUCCESSFUL):

- **Runtime**: Kotlin 1.9.24 / JVM 17 (Zulu 17.0.13), AGP 8.5.2, Compose compiler 1.5.14, minSdk 26 / targetSdk 34.
- **UI**: single-activity Compose, `PebotTheme` with `Pos*` tokens already in place; screens receive the shared `CafeViewModel`.
- **State**: `StateFlow`/`Flow` from ViewModel; `AccessControl` singleton gates nav routes by ADMIN/STAFF/KITCHEN.
- **Persistence**: Room DAO is the sole DB owner; `pebot_sync` SharedPreferences for sync settings. No DataStore.
- **Tests**: unit tests under `app/src/test/` (JUnit 4); `CafeViewModelTest`, `AccessControlTest`, `ExportDataRoundTripTest` present. No `androidTest/` yet.

### Research artifacts
- `research.md` — full audit transcript (extracted from spec §0).
- `quickstart.md` — build & preview instructions (see §3).
- `data-model.md` — **unchanged** (re-skin is presentation-only); entities live in `Entities.kt` + `CafeDao.kt`, referenced verbatim.

---

## Technical Context

**Language/Version**: Kotlin 1.9.24 (JVM 17) — what the build actually compiles with. See Constitution Check for the Kotlin-2.0/JVM-21 gap.
**Primary Dependencies**: Jetpack Compose 1.5.14 (Material 3), Room 2.6.1 + KSP, Navigation Compose 2.7.7, kotlin serialization, okio, socket.io-client, nanohttpd (local web server for bridge).
**Storage**: Room/SQLite (local-first, offline). No schema changes for this feature.
**Testing**: JUnit 4 unit tests (`testDebugUnitTest`). Plan adds Compose UI tests + receipt-parity test; instrumentation harness can be added but is not required to ship the toggle/theme.
**Target Platform**: Android tablet, minSdk 26, targetSdk 34.
**Project Type**: native mobile app (Jetpack Compose).
**Performance Goals**: theme animations 60fps on mid-tier; Fast Mode = 0 non-essential motion; all animations < 300ms and skippable.
**Constraints**: offline-capable (no network/assets off-device except vector drawables); receipts never truncated; money legible (≥4.5:1 contrast, tabular figures).
**Scale/Scope**: 27 screen composables + 17 nav routes + Login/AccessDenied; re-skin touches ~10 core surfaces + theme + settings toggle + assets + tests.

## Constitution Check

*CaféOS Tablet Constitution (`clauses/constitution.md`) — gate before Phase 0; re-check after Phase 1 design.*

- **I. Offline-first, offline-always**: ✅ PASS — re-skin adds no network/sync behavior. All new assets are local vector drawables; animations are local. Fast Mode reduces CPU/GPU load (protects battery offline).
- **II. Data integrity & money handling**: ⚠️ CONDITIONAL — totals computed by the single `CafeViewModel.calculateTotals()`; re-skin reads computed values from `StateFlow` and **must not** recompute or reformat numeric values. Receipt (POSScreen/ReceiptPrinter) content rendered in full, unmodified. Mitigation: a `ReceiptParityTest` diffs the gamified receipt's field set against the classic one before/after.
- **III. Thin data layer, clear boundaries**: ✅ PASS — changes confined to `ui/theme/` + `ui/` (presentation); no DAO/SQL modifications, no ViewModel business-logic changes.
- **IV. Premium minimal design**: ✅ PASS (enhanced) — Material 3 base retained; gold/cyan accents; touch targets ≥ 48dp; motion < 300ms. Game "spectacle" is layered on top, never between finger and value.
- **V. Tests are the deliverable**: ✅ COMMIT — existing unit tests must stay green; add Compose UI tap-parity test + receipt-parity test; any bug fix adds a regression test first.

### Locked Technical Decisions — discrepancy noted
The Constitution locks **Kotlin 2.0 / JVM 21 / JUnit 5**, but the verified build uses **Kotlin 1.9.24 / JVM 17 / JUnit 4**. The gamified re-skin uses **no** Kotlin-2.0-only features and needs **no** toolchain change, so this is tracked as a pre-existing gap (future upgrade task) rather than a plan violation. New tests will match the existing JUnit-4 harness to keep `testDebugUnitTest` green.

## Project Structure (docs)

```text
specs/001-gamified-pos-ui/
├── spec.md            # ✅ created (specify)
├── research.md        # Phase 0 audit (below)
├── data-model.md      # pointer to existing Entities.kt/CafeDao.kt (unchanged)
├── plan.md            # this file
├── quickstart.md      # Phase 1 build/preview notes (below)
└── tasks.md           # Phase 2 output (speckit-tasks — NOT created here)
```

## Project Structure (source — affected areas)

Changes are intentionally scoped to **presentation + theme + settings + tests**. Business/data layers are NOT modified.

```text
app/src/main/java/com/cafeos/tablet/ui/
├── theme/
│   ├── Color.kt        # MODIFY — add PosNeon (cyan), rarity/glow tokens
│   ├── Type.kt         # MODIFY — add Display/condensed header style
│   ├── Theme.kt        # MODIFY — add ThemeMode (GAMIFIED/FAST/CLASSIC) + motion scale
│   └── Motion.kt        # NEW   — reusable animation specs (scale+glow, count-up, wipes)
├── components/
│   ├── PremiumComponents.kt  # MODIFY/extend — rarity badges, stack counters, gem counter
│   └── GameComponents.kt      # NEW      — MOBA iconography primitives (vector icons)
├── screens/
│   ├── POSScreen.kt            # MODIFY — item-shop grid + loadout cart + confirm
│   ├── OrderHistoryScreen.kt   # MODIFY — status color-coding (icon+text)
│   ├── InventoryScreen.kt      # MODIFY — low-stock "rare running out" (icon+num)
│   ├── AnalyticsScreen.kt      # MODIFY — quest/mission framing
│   ├── SettingsScreen.kt       # MODIFY — add Fast Mode + Classic Mode toggles
│   ├── FloorPlanScreen.kt      # MAYBE  — table grid as arena (if low-risk)
│   └── ReceiptView.kt          # NEW      — reusable full BIR receipt (shared widget)
├── CafeViewModel.kt    # NO CHANGE (logic) — reads remain via StateFlow
├── MainActivity.kt     # MODIFY — feed ThemeMode into PebotTheme
└── theme/ (see above)
data/
└── SettingsStore.kt    # NEW — SharedPreferences-backed UI-mode settings
app/src/main/res/
├── drawable*/         # NEW — gamified vector icons (gem, rarity frames, quest marks, crest)
└── values/strings.xml # MODIFY — add gamified labels (English; i18n-safe)
app/src/test/...       # ADD — ReceiptParityTest, TapParityTest + keep existing green
```

## Complexity Tracking

No justified constitution violations. The re-skin respects every boundary (offline-first, single source of truth for totals, DAO ownership, ≥48dp touch targets, tests-first). The Kotlin-2.0/JVM-21 vs 1.9.24/17 gap is pre-existing and out of scope (does not block any re-skin feature).

## Phase 1 — Design (design tokens, component variants, motion primitives)

### 1.1 Theme tokens (Color.kt / Type.kt / Theme.kt)
- **Colors**: keep `Pos*` palette; promote `PosGold` (#B58A4A) to currency/CTA accent; add `PosNeon` (cyan #00F3FF) as the selection/glow accent; add rarity border tokens (`RarityCommon`→`RarityEpic`, gold frame = best-seller, red pulse = low stock); add `PosGameBackground` dark base. State color language is always icon+text+color (guardrail 4).
- **Typography**: add `DisplayLarge` (bold, larger, semi-condensed system sans) for headers/category titles; keep `Currency` style (tabular figures, high contrast) for all money.
- **Theme modes**: introduce `enum class ThemeMode { GAMIFIED, FAST, CLASSIC }`. `PebotTheme` exposes duration-scale + glow flag; FAST zeroes non-essential animation duration and disables glow/sound; CLASSIC flattens the aesthetic back to the current palette.

### 1.2 Motion primitives (Motion.kt)
- `TapScaleGlow` (0.35s cubic-bezier, skippable), `CurrencyCountUp` (tween, skippable), `MenuWipe`/`SlideInLoadout` (navigation/cart), `RarityPulse` (low-stock only). All default ON; FAST sets a global `fastMotion` scale of 0.

### 1.3 Settings toggle (SettingsStore.kt + SettingsScreen)
- SharedPreferences-backed (`pebot_sync` shared prefs, consistent with existing pattern). `uiMode` defaults to FAST (on-by-default rollout); `gamifiedUi` default true; `classicFallback` default false. Exposed as a `StateFlow` read by `MainScreen` → `PebotTheme`.

### 1.4 Shared receipt widget (ReceiptView.kt)
- Extract the BIR receipt rendering into a single reusable composable used by both the classic flow and the "Victory screen" so there is literally one receipt implementation (no drift). This is the anchor for the receipt-parity test.

## Phase 2 — Surface re-skins (build order from spec §5)
2.1 Home/dashboard lobby (shift stats HUD, loyalty-gem counter, promo banner carousel).
2.2 Catalog/shop grid (item cards + rarity badges + category tabs).
2.3 Cart (loadout side panel with stack-style quantity badges) + Checkout confirm modal (skippable coin-spend animation).
2.4 Receipt/victory screen (reward summary card + full BIR receipt beneath, expandable).
2.5 Loyalty currency HUD bar (top bar count-up).
2.6 Inventory low-stock "rare running out" (icon + number).
2.7 Optional: staff quests/leaderboard on dashboard.
2.8 Settings toggles wired end-to-end.

## Verification before implement
For each phase: rebuild APK (`./gradlew assembleDebug`) and assert BUILD SUCCESSFUL; run `testDebugUnitTest`; verify receipt field count + money totals unchanged vs. baseline output. Definition of done gates in spec §8.
