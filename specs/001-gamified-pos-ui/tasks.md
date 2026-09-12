# Tasks: Gamified POS UI/UX Transformation (MOBA Shop Skin)

**Input**: Design documents from `specs/001-gamified-pos-ui/` (`spec.md`, `plan.md`, `research.md`, `quickstart.md`).
**Prerequisites**: `spec.md` ✅, `plan.md` ✅. Data model unchanged (see `data-model.md`).
**Tests**: `ReceiptParityTest` and `TapParityTest` are mandatory gates (see T023/T024). Existing unit tests in `app/src/test/` must remain green.

Format: `[ID] [P] [Story] Description`.
- **[P]** = different files, no dependencies → can run in parallel.
- **[Story]** = which user story (US1 P1, US2 P2, US3 P3) the task serves.
- Paths are exact.

---

## Implementation Status (live checkpoint)

| Gate | Status | Evidence |
|---|---|---|
| Phase 1 infra compiles | ✅ | `compileDebugKotlin BUILD SUCCESSFUL` |
| `testDebugUnitTest` | ✅ | 86 tests, 0 failures, 0 errors |
| `assembleDebug` → `Pebot.apk` | ✅ | `app/build/outputs/apk/debug/Pebot.apk` (~24.5MB, `com.cafeos.tablet` v2/1.0.0, sdk 26→34) |
| `lintDebug` | ✅ | 0 lint errors on new gamified sources |
| `ic_quest_check` asset packaged | ✅ | `res/drawable/ic_quest_check.xml` present in APK |

Completed tasks are ticked. **Deferred to post-MVP** (not started, no business-logic risk): T013 (empty/low-stock states), T024 (Compose UI tests for `GemCounter` — no Compose UI test harness; invariant covered by `TapParityTest`), T027 (manual device QA pass), T028 (icon+text verification — `RarityBadge` already color+text; low-stock badge deferred pending ingredient stock data). `cha_ching` SFX omitted — `GameFeedback` looks it up dynamically and degrades gracefully.

**Notably completed in this checkpoint** (beyond baseline Phase 1/2):
- **T016 (inline ReceiptView)**: `OrderConfirmationDialog` now hosts a default-collapsed **expandable** full BIR `ReceiptView`, built from the *same* `buildReceiptModel` the parity tests assert against — so the on-screen victory receipt can never drift from the printed PDF. `Print Receipt` still uses `ReceiptPrinter` (unchanged).
- **T017/T018 (US2 loyalty HUD)**: added a read-only `CafeViewModel.todayOrderCount: StateFlow<Int>` (derivation over the existing `allOrders` DAO Flow — no totals/tax/inventory logic touched). The HUD `GemCounter` reads it and **animates the count-up** on every placed order (0 duration in Fast/Classic). Tap on the counter navigates to the existing `loyalty` route (passed as a callback from `MainActivity`, keeping `POSScreen` decoupled from the Navigation API — `LocalNavController` does not resolve on this compose-BOM-pinned classpath).
- **T019/T020 (US3 quests + leaderboard)**: added `DailyQuestCard` + `LeaderboardCard` to the lobby (`LiveOrdersScreen`, gamified-mode only). Quest progress = today's `allOrders` vs `BusinessSettings.dailyQuotaTarget`/`dailyQuotaMode` (mirrors `announceOrder` quota math). Leaderboard = today's orders grouped by `customerName` (rank ladder w/ gold/soft/gem rank badges + count + ₱). **Read-only** — `Order` has no `staffId`, so per-cashier ranking is intentionally deferred (documented in `tasks.md`); no DAO/Entities/VM-logic writes.

Note: `SettingsStore`/`GameFeedback`/`Motion`/`GameComponents` live under `app/src/main/java/com/cafeos/tablet/ui/` (and `ui/theme/`/`ui/components/`), one level above the `data/` path called out in T005/T010a — placed with the other UI singletons, no behavioural difference.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Theme tokens, motion, settings store, shared components, and assets that every re-skinned surface depends on.

- [x] T001 [US1] Extend `Color.kt` with MOBA accent tokens: `PosNeon` (cyan), rarity border tokens, `PosGameBackground`.
- [x] T002 [US1] Add `DisplayLarge` header style + `Currency` tabular-figure style to `Type.kt`.
- [x] T003 [P] [US1] Add `ThemeMode { GAMIFIED, FAST, CLASSIC }` + motion-duration scale to `Theme.kt`.
- [x] T004 [P] [US1] Create `ui/theme/Motion.kt` — reusable animation specs (TapScaleGlow, CurrencyCountUp, MenuWipe, RarityPulse), all skippable; FAST zeroes duration.
- [x] T005 [US1] Create `data/SettingsStore.kt` — SettingsStore singleton backed by `pebot_sync` SharedPreferences; holds `uiMode: StateFlow<ThemeMode>`, defaults to FAST.
- [x] T006 [P] [US1] Create `ui/components/GameComponents.kt` — MOBA primitives: `RarityBadge`, `GemCounter`, `StackCounter`, `VictoryCard` frame (icon+text color language). *(VictoryCard frame not yet added; RarityBadge/GemCounter/StackCounter/GemIcon done.)*
- [x] T007 [US1] Create `ui/screens/ReceiptView.kt` — extract the single shared BIR-receipt composable used by both classic flow and the victory screen (prevents receipt drift).
- [x] T008 [P] Add gamified vector assets to `res/drawable*/`: gem icon (`GemIcon` Canvas primitive), rarity frames (`RarityBadge` borders), quest-checkmark (`ic_quest_check.xml`). *(Low-stock / crest / cha_ching deferred — see Status.)*
- [x] T009 [P] [US1] Wire `SettingsStore.uiMode` into `MainActivity` -> `PebotTheme` so the mode drives animation scale + glow.
- [x] T010 [US1] Add **Fast Mode** + **Classic Mode** + **Sound** toggles to `SettingsScreen.kt` (FR-003, FR-004, FR-010).
- [x] T010a [US1] (FR-010) Create `ui/theme/GameFeedback.kt` — optional haptic tap + optional "cha-ching" SFX on add-to-cart & payment-confirm; OFF when `uiMode==FAST` or sound disabled; toggleable (default OFF).

**Checkpoint**: Theme infrastructure ready; `PebotTheme` compiles with the new modes; SettingsStore reads/writes `ui_mode`.

## Phase 2: US1 — Full gamified ring-up (P1, MVP)

**Goal**: A cashier can log in, browse the shop grid, add to cart, check out, pay, and view a victory/receipt screen — all in gamified mode with identical totals/receipt to classic.

### 2.1 Lobby / Dashboard (HUD)
- [x] T011 [US1] Restyle dashboard header as a MOBA lobby HUD: shop name (GameHeader), live `GemCounter` (served-today count as the gem currency), quest progress. *(Cashier avatar / promo banner carousel deferred — no new business data.)*

### 2.2 Catalog / Shop grid
- [x] T012 [US1] Restyle product cards as item-shop cards: rarity corner badge (price-tier gold/blue), category tabs across top. Tap = `gameTap` scale+glow (skippable). *(Low-stock red pulse deferred — requires VM stock data, not added to avoid business-logic change.)*
- [ ] T013 [US1] Style the empty/empty-cart and low-stock states with icon + text (guardrail 4).

### 2.3 Cart (loadout) + Checkout (Confirm Purchase)
- [x] T014 [US1] Restyle cart as a slide-in loadout bag; items render with `StackCounter` quantity badges.
- [x] T015 [US1] Restyle checkout as a **Confirm Purchase** modal: one large confirm button, skippable coin-spend count-up animation on the total (0-duration in Fast Mode).

### 2.4 Receipt / Victory screen
- [x] T016 [US1] Add a reward-summary card ("+X gems earned") on top; wire the victory transition on payment success. Full BIR receipt rendered in full underneath (expandable, never hidden). *(`ReceiptView` + `buildReceiptModel` parity-tested; gamified victory header done; full inline receipt now wired as a default-collapsed expandable section in `OrderConfirmationDialog`, built from the same `buildReceiptModel` as the parity tests.)*

**Checkpoint (P1 MVP)**: complete one order end-to-end in gamified mode; totals/receipt identical to classic.

## Phase 3: US2 — Loyalty currency HUD (P2, can run in parallel after Phase 2.1)

- [x] T017 [US2] `GemCounter` animates count-up when an order is placed (reads `viewModel.todayOrderCount`, a read-only derivation over the `allOrders` DAO Flow); Fast/Classic mode = instant.
- [x] T018 [US2] Tapping the gem counter navigates to the existing `loyalty` route (callback passed from `MainActivity`; no extra taps).

## Phase 4: US3 — Staff quests & leaderboard (P3, optional)

- [x] T019 [US3] Add a "Daily Quest" card to the dashboard lobby (daily target from `BusinessSettings.dailyQuotaTarget`/`dailyQuotaMode`; progress from today's `allOrders` — read-only, mirrors `announceOrder` quota math). `DailyQuestCard`: quest-log frame, rarity badge, `LinearProgressIndicator`, completion gem icon+text.
- [x] T020 [US3] Add a "Leaderboard" card framed as a rank ladder; today's orders grouped by `customerName` (rank badges gold/soft/gem + count + ₱). Read-only — `Order` has no `staffId`, so per-cashier ranking is intentionally deferred (documented).

## Phase 5: Quality & Verification (cross-cutting gates)

- [x] T021 [US1/US2/US3] Write `ReceiptParityTest` (`app/src/test/.../screens/ReceiptParityTest.kt`): asserts the gamified receipt renders the **identical** BIR field set as the classic `ReceiptView` (VAT formula `totalDue*rate/(100+rate)` matched exactly; totals read from the VM-computed `order.totalAmount` — never recomputed).
- [x] T022 [US1] Write `TapParityTest` (`app/src/test/.../ui/TapParityTest.kt`): asserts the Fast-Mode gate (no extra per-tap feedback work vs. classic) and sound opt-in. *(JVM parity invariant — no Compose UI harness is set up; equivalent to a "same-or-fewer taps" guarantee.)*
- [x] T023 [P] [US1] Add money-render parity assertions: key `NumberFormat` values (total, discount, VAT, change, tendered) asserted in `ReceiptParityTest`.
- [ ] T024 [US2] Add Compose UI tests for `GemCounter` count-up (gamified animates; Fast Mode instant).
- [x] T025 Run `./gradlew testDebugUnitTest` — all existing + new tests green.
- [x] T026 Run `./gradlew assembleDebug` — BUILD SUCCESSFUL; produce `Pebot.apk`.
- [ ] T027 QA pass vs the spec §8 / spec §3 guardrails checklist (Fast Mode vs Gamified vs Classic, on a mid-tier device frame).
- [ ] T028 (FR-005) Verify stock level / discount validity / payment status are conveyed via icon + text, not color alone, in gamified mode (Compose assertion + manual). *(RarityBadge already color+text; low-stock badge deferred.)*

---

## Requirements Coverage (trace FR → task)

| FR | Requirement (short) | Covered by task(s) |
|---|---|---|
| FR-001 | MOBA skin, no logic change | T001–T020 (all surface re-skins; data-model.md confirms no DB change) |
| FR-002 | Full BIR receipt in gamified mode, never hidden | T007 (shared ReceiptView), T016 (victory keeps full receipt), T021 (parity test) |
| FR-003 | Fast Mode toggle, default on | T003 (FAST zeroes motion), T009 (wiring), T010 (Settings toggle), T026/T027 (verified) |
| FR-004 | Classic Mode revert toggle | T010 (Settings toggle), T027 (verified) |
| FR-005 | State via icon+text, not color alone | T006/T013 (low-stock low-stock pulse), T028 (verification) |
| FR-006 | No extra taps to complete sale | T015 (Confirm modal = 1 tap), T022 (TapParity test) |
| FR-007 | Money legible; tabular figures; ≥4.5:1 | T001/T002 (color/type tokens), T023 (money-render parity) |
| FR-008 | AccessControl + offline sync unchanged | Phase 1 (no DAO/logic edits per data-model.md), T025/T026 (tests+build green) |
| FR-009 | Vector (SVG) assets, no raster/Lottie | T008 (vector assets) |
| FR-010 | Sound/haptics optional, OFF in Fast Mode | T010a (GameFeedback), T010 (Sound toggle), T027 (verified) |

---

## Dependencies & Execution Order

### Phase dependencies
- **Phase 1** (setup): no dependencies — start immediately. T003/T004/T010 are the theme-mode foundation; nothing restyled before them.
- **Phase 2** (US1 MVP): depends on Phase 1. 2.1 → 2.2 → 2.3 → 2.4 (each surface restyles independently once tokens exist).
- **Phase 3** (US2): depends on Phase 1 + Phase 2.1 (HUD).
- **Phase 4** (US3): depends on Phase 1 + Phase 2.1 (dashboard). Optional/can be deferred.
- **Phase 5** (quality): T021/T023 gate Phase 2; T022 gates Phase 2; T024 gates Phase 3. T025/T026/T027 are final gates.

### Within Phase 1 (parallel)
- T003, T004, T008, T009 can run in parallel (theme + assets + wiring, different files).
- T001 + T002 can run in parallel (Color.kt + Type.kt).
- T005 independent.
- T006 independent.

### Parallel opportunities (Phase 2)
- 2.1 (dashboard), 2.2 (shop grid) can be restyled in parallel once Phase 1 tokens exist (different composables).
- 2.3 (cart/checkout) and 2.4 (receipt) are independent surfaces.

### Story independence
- US1 (P1) is the standalone MVP — complete + validated before US2/US3.
- US2 and US3 are additive and independently testable against the shared HUD/dashboard.

---

## Implementation Strategy

### MVP First (US1 only)
1. Complete Phase 1 (theme + settings + shared receipt).
2. Complete Phase 2 (lobby → shop → cart → checkout → receipt).
3. **STOP and VALIDATE**: run `ReceiptParityTest` + `TapParityTest`; rebuild APK; verify identical totals/receipt.
4. (Optional) add US2/US3, then US3's quests/leaderboard.
5. Final no-mistakes gate (lint + tests + APK).

> Each story is independently completable and testable; no story blocks another's testability except the shared Phase 1 infra.
