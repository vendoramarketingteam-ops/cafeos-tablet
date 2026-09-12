# Feature Specification: Gamified POS UI/UX Transformation (MOBA Shop Skin)

**Feature Branch**: `001-gamified-pos-ui`
**Created**: 2026-09-10
**Status**: Draft
**Spec Kit**: 1.0.2.dev0 (sequential feature numbering)

**Input**: User description: "GAMIFIED POS UI/UX TRANSFORMATION — AGENT BUILD BRIEF" (Pebot café POS tablet re-skin with MOBA shop aesthetics).

---

## Scope Quickfill (from brief)

- **Store/brand name**: Pebot
- **Store type**: Cafe
- **Currency & symbol**: ₱
- **Brand colors**: Current palette — `PebotTheme` (premium coffee/gold). Extend with one game accent (gold/cyan) on top of the existing dark coffee base. Do not discard existing tokens; map them to game roles.
- **Terminals**: Unlimited cashiers (multi-staff, role-gated).
- **Languages**: English.
- **Loyalty**: As per the current system (`LoyaltyVoucher`/`LoyaltySetting` entities and `loyalty` screen) — re-skinned, behavior unchanged.
- **Audience**: Both customer-facing (POS/shop/receipt) and staff-facing (daily quest targets, shift leaderboard).
- **Receipts**: BIR official-receipt fields must remain editable and toggleable on/off at any time; content never hidden/truncated.

---

## 0. Architecture & Risk Note (Audit First)

### Stack
- Native Android, **Kotlin**, **Jetpack Compose (Material 3)**. Single-activity architecture.
- Build: Gradle 8.7 via wrapper, **AGP 8.5.2**, Kotlin 1.9.24, Compose compiler 1.5.14.
- Persistence: **Room (SQLite)**, coroutine `Dispatchers.IO`, `Flow`/`StateFlow` for reactive reads.
- Navigation: `navigation-compose` `NavHost` with 17 composable routes + a login gate + an access-denied screen.
- Theming: a dedicated `ui/theme/` package already exists — `Color.kt`, `Type.kt`, `Theme.kt` (`PebotTheme`). Re-skin lives here (extend/extend, do **not** fork).

### Current UI structure
- `MainActivity` -> `PebotTheme { Surface { MainScreen() } }`.
- `MainScreen` holds the shared `CafeViewModel`, a collapsible navigation rail (OPERATIONS / MANAGEMENT / SYSTEM groups) and a `NavHost` with per-route `AccessControl.canAccessRoute` gating.
- Each screen composable receives the shared `viewModel` (e.g. `POSScreen(viewModel)`), which keeps UI/data concerns separated already.

### Existing thematic tokens (do not reinvent; repurpose)
`PosCoffee` (#F5F3EE) / `PosCoffeeDeep` (#23271F) coffee base, `PosGold` (#B58A4A) secondary accents, `PosAccent` (#4A5D3A) green primary, `PosCream`/`PosInk` neutrals, `PosDanger`. The brief's "deep navy/charcoal + neon/metallic accent + gold" maps cleanly onto these (swap/extend `PosCoffee`/`PosCoffeeDeep` for the dark base, promote `PosGold` to the currency/CTA accent; add a small `PosNeon` accent palette for MOBA glow).

### Screen / flow enumeration (all existing)
Login → Dashboard/MainScreen (rail nav) → `live_orders` | `pos` (cart sidebar → checkout → payment → receipt) | `kitchen` | `analytics` | `products` | `inventory` (+ stock history) | `expenses` | `loyalty` | `order_history` | `reviews` | `tables` | `stations` | `staff` | `settings` (export/sync/about) | `audit_logs` | `connection`.

### Business logic that MUST NOT change behavior (single source of truth = `CafeViewModel`)
Pricing/tax/discount/change: `calculateTotals()`, `calculateBirPrice()`, `getVoucherDiscountAmount()`, `setDiscountType()`, `setSelectedVoucher()`.
Cart: `addToCart()`, `removeFromCart()`, `updateCartItemQuantity()`, `clearCart()`, `openProductOptions()`/`closeProductOptions()`.
Checkout/order: `placeOrder()`, `openCheckout()`/`closeCheckout()`, `setOrderType()`, `setCustomerName()`, `setDeliveryAddress()`, `setSelectedTableId()`.
Payment: `setPaymentMethod()`, `setAmountTendered()`, `setScreenshotPath()`/`clearScreenshot()`.
Inventory/receipting: `adjustStock()`, `saveProductRecipe()`, `ReceiptPrinter`, `ReceiptNumbers`.
Auth/access: `AccessControl.canAccessRoute()`, `Staff` role normalization.
Offline/sync: `SocketSyncManager`, `LocalWebServer`, `PcBridgeClient`, `MenuImportParser` (pebot_sync SharedPreferences).

### Risks / fragility to work around carefully
1. **Receipt content**: BIR-mandated fields are rendered in `POSScreen`/`ReceiptPrinter`. The re-skin must never truncate/obscure/reformat legally-required receipt content. Verify before/after.
2. **Numeric legibility**: currency uses `NumberFormat` in many composables. The gamified theme must preserve contrast & tabular figures for price/tax/change/totals under all lighting.
3. **No DataStore**: only `SharedPreferences("pebot_sync")` exists. The "Fast Mode" toggle is net-new infra — implement as a thin, theme-level Compose state backed by `SharedPreferences` (no DB migration needed), NOT tied to business logic.
4. **Shared ViewModel coupling**: screens already receive `viewModel` directly (good), but some composables may contain inline formatting/formatting calls that look "UI" but actually compute business numbers — treat any `viewModel` call that returns a computed total/price as untouchable.
5. **Build already compiles** with warnings (unused params, deprecated APIs, label shadowing in `CafeViewModel`); do not introduce new compile errors while editing theme resources.
6. **Offline-first**: theme additions must add no network/runtime dependency; animations must be skippable/OFF-by-default-fast-mode to protect low-to-mid-range hardware at 60fps.

---

## 1. User Scenarios & Testing

### User Story 1 — Cashier rings up a sale in full gamified mode (PRIORITY: P1 — MVP)
**Plain language**: A staff member opens the app, logs in, browses the product shop grid (item-shop/champion-select style), adds items to the cart (loadout bag with stack counters), taps checkout, pays, and receives a victory-style receipt screen that contains the complete, legally-required BIR receipt content.

**Why this priority**: This is the core value slice — if the gamified skin can't ring up a correct sale, nothing else matters. Must be independently testable.

**Independent Test**: Start the app, complete one full order (product → cart → checkout → cash payment → receipt), and assert: (a) the exact same total/tax/change as the classic UI, (b) the receipt contains every BIR field, (c) stock was deducted identically.

**Acceptance Scenarios**:
1. **Given** the gamified shop grid, **When** the cashier taps a product, **Then** the product card scales+glow (0.35s animation) and the product is added to the cart; the cart badge count increments.
2. **Given** a cart with items, **When** the cashier taps "Checkout", **Then** a Confirm Purchase modal opens with the same cart total; no extra taps vs. classic.
3. **Given** the Confirm modal, **When** the cashier confirms, **Then** a currency count-up animation plays (skippable by tapping) and the Victory/Receipt screen appears.
4. **Given** the receipt screen, **When** the cashier scrolls/views full receipt, **Then** ALL original BIR receipt fields are present and identical to the classic receipt (verified via before/after diff).
5. **Given** Fast Mode is enabled, **When** the cashier performs the same flow, **Then** no non-essential animations/sound play, and the same totals/receipt result.

### User Story 2 — Loyalty currency HUD animates on earn (PRIORITY: P2)
**Plain language**: When an order is placed, the persistent top-bar "gems/gold" counter animates a count-up reflecting loyalty points earned, in both gamified and Fast modes.

**Why this priority**: Reinforces engagement without adding taps; must be independent of the sale flow.

**Independent Test**: Complete an order that earns loyalty points; assert the HUD counter ends at exactly the points the classic UI would award.

**Acceptance Scenarios**:
1. **Given** a completed order that earns N loyalty points, **When** the victory screen completes, **Then** the top-bar currency counter tweens from its prior value to prior+N (Fast Mode: instant, no tween).
2. **Given** the counter, **When** tapped, **Then** it navigates to the Loyalty screen (existing behavior, restyled).

### User Story 3 — Staff daily quests & shift leaderboard (PRIORITY: P3 — staff-facing gamification)
**Plain language**: A staff member opens the lobby/dashboard; a "Quests" section shows today's sales targets (e.g. "Sell ₱X in snacks today") as a checklist, and a "Leaderboard" shows a ranked list of cashiers by shift sales.

**Why this priority**: Optional staff motivation; must not slow ring-ups.

**Independent Test**: On the dashboard, assert the quest progress equals the same daily total the classic dashboard would show, and the leaderboard ranking matches `getDailySales()`/top-seller data.

**Acceptance Scenarios**:
1. **Given** a logged-in staff member, **When** they open the dashboard, **Then** a quest log card shows today's target progress computed from the same `getDailySales()` source.
2. **Given** multiple cashiers have sales today, **When** the leaderboard is opened, **Then** it ranks them using the same sales figures the classic analytics/report would produce.

### Edge Cases
- What happens when a promo has no real time bound → render as a static banner; **never** a fake countdown.
- Low stock → icon + text label + numeric count; color is supplementary only (never color-alone).
- Discount expired vs active → text label "Expired"/"Active" + icon, not color alone.
- Payment status pending/paid → icon + text, not color alone.
- Screen reader / TalkBack still reads product names, prices, and cart items; gamified icons must have labels.
- Low-end tablet (mid SDK-26 device): all animations skip instantly when Fast Mode on; no dropped frames on the core ring-up path.

---

## 2. Creative Direction — POS ↔ MOBA Shop Mapping

| POS concept | MOBA-shop equivalent | Design notes |
|---|---|---|
| Login screen | Player/Summoner sign-in | Gradient/particle background, brand "team crest" logo, bold condensed display font for "Pebot". |
| Dashboard/home | Lobby screen | Top HUD bar (cashier name/avatar, shift stats, live loyalty-gems counter); rotating promo banner carousel = "featured event" banner. |
| Product catalog + categories | Item Shop / Champion Select grid | Grid of product cards w/ large photo, category tabs across top, rarity corner badge (gold border = best-seller, red pulse = low stock). |
| Product detail / variant picker | Item stat card | Tap-to-expand card; price shown as currency icon + amount; add-ons as small stacked icons. |
| Cart | Loadout / Inventory bag | Slide-in side panel; items stack with quantity badges like MOBA inventory counters. |
| Discounts/vouchers/combo deals | Event banner / Limited-Time Offer | Banner styling only for real time-bound promos; no fake countdown timers. |
| Checkout / order review | Confirm Purchase modal | One large confirm button; skippable coin/currency-spend animation on confirm. |
| Payment success + receipt | Victory / Match Summary | Reward-summary card ("+X gems earned") on top; full BIR receipt content rendered in full underneath (expandable, never hidden). |
| Loyalty points / rewards | In-game currency (gems/gold) | Persistent HUD element (top bar) that animates count-up when earned. |
| Daily/weekly sales targets (staff) | Quests / Missions | Checklist quest log: "Sell ₱X in snacks today". |
| Staff leaderboard (multi-cashier) | Leaderboard / Season Rank | Ranked list by shift sales, framed like a rank ladder. |
| Low stock alert | "Rare item running out" | Icon + urgency text + numeric count, never color alone. |
| Admin/settings panel | Profile & Settings menu | Calmer/utilitarian styling than shop-facing screens; adds the Fast Mode toggle. |

---

## 3. Visual Design System

- **Color**: Dark coffee base (deep navy/charcoal — reuse/extend `PosCoffee`/`PosCoffeeDeep`) + metallic accent (**gold** = existing `PosGold`, the currency/CTA color) + optional **cyan** neon accent (`PosNeon`) for selection/glow. State color language must combine **icon + text + (supplementary) color** for stock/discount/payment status.
- **Typography**: Bold condensed/display font for headers & category titles; clean tabular-figure font for all prices, totals, quantities, change due. Money numerals must retain high legibility; no stylistic font on numeric amounts.
- **Iconography**: Flat vector game-icon style; beveled/glowing card frames on selection.
- **Motion**: Micro-interactions on tap (scale + glow), currency count-up tweens, menu-wipe/slide transitions. All at 60fps on mid-range hardware; all **interruptible** and **skippable**.
- **Sound/Haptics**: Optional "cha-ching"/confirm sound + haptic tap on add-to-cart & payment confirm. Togglable in Settings; **OFF by default when Fast Mode is on**.

---

## 4. Non-Negotiable Guardrails

1. Add **no extra required taps** to complete a sale, apply a discount, or process a refund/void.
2. Never delay, truncate, obscure, or reformat any legally required receipt/invoice content (BIR fields).
3. Never reduce numeric legibility of price, tax, change, or totals under any lighting.
4. Never rely on color alone for stock level, discount validity, or payment status — pair with icon + text.
5. Never force an unskippable animation; all must be skippable or OFF.
6. Never touch payment processing, tax computation, inventory deduction, or role/permission logic — re-skinned visually only.
7. Never break existing offline mode / sync behavior.

Plus the **Fast Mode** toggle: disables non-essential animation/sound and shows a flatter, faster variant of the same screens; protects cashier speed while the gamified version can be showcased when calm or demoing. **Fast Mode is ON by default during initial rollout** (documented opt-in semantics).

---

## 5. Technical Implementation Rules

- Implement the new look as a **theme/design-token layer** (colors, spacing, type scale, component variants) over existing components — do **not** fork business-logic components into "game" duplicates.
- Presentation components stay separate from data/state logic; `CafeViewModel` + DAO remain the single source of truth regardless of active theme.
- New icon/illustration assets = lightweight **vector (SVG)** assets, not raster/Lottie that lag older devices.
- Preserve existing localization (strings via `strings.xml`); new gamified copy gets the same treatment (English; structure ready for Filipino later).
- Ship a **settings toggle** to fully revert to the classic UI without a rebuild.
- Theme tokens must not introduce new runtime/network dependencies; all animations must be composable-level, cancellable, 60fps-capable.

---

## 6. Requirements

### Functional Requirements
- **FR-001**: System MUST apply a MOBA-shop skin to the existing POS surfaces (login, lobby/dashboard, shop grid, cart loadout, checkout, payment, receipt, loyalty HUD, inventory low-stock) without modifying computed totals, tax, change, or inventory deduction.
- **FR-002**: System MUST render the complete, unmodified BIR receipt content on the victory/receipt screen in gamified mode (expandable view, never hidden).
- **FR-003**: System MUST provide a **Fast Mode** toggle (Settings) that disables non-essential animation/sound and uses the flatter screen variant; Fast Mode on by default at rollout.
- **FR-004**: System MUST provide a **Classic Mode** toggle (Settings) that reverts the UI to the plain theme without a rebuild.
- **FR-005**: System MUST convey stock level, discount validity, and payment status via icon + text (color only as supplementary).
- **FR-006**: System MUST NOT add required taps to complete a sale, discount, or refund/void vs. the current UI.
- **FR-007**: System MUST preserve all money-related numeric legibility (contrast + tabular figures) in both modes under all lighting.
- **FR-008**: System MUST keep `AccessControl` role/permission gating and offline sync/socket behavior unchanged.
- **FR-009**: System MUST ship gamified assets as vector (SVG) and avoid Lottie/raster that lags mid-tier devices.
- **FR-010**: System MUST keep sound/haptics off by default when Fast Mode is on; fully toggleable in Settings.

### Key Entities (unchanged; re-skinned only)
- `Product`, `Category`, `Order`, `OrderItem`, `OrderOption`, `Payment`, `Ingredient`, `IngredientTransaction`, `Customer`, `LoyaltyVoucher`, `LoyaltySetting`, `Staff`, `Table`, `Station`, `Expense`, `Voucher`, `Review`, `OptionGroup`, `Option`, `SyncMeta`. (Source of truth: `Entities.kt` + `CafeDao.kt`.)

---

## 7. Build Order (Phase Sequence)
1. Architecture & Risk Note (this section). ✅
2. Design tokens/theme layer (colors, type, spacing, motion primitives) — no screens yet.
3. Home/dashboard (lobby) re-skin.
4. Catalog/shop grid re-skin.
5. Cart (loadout) + checkout (confirm purchase) re-skin.
6. Receipt (victory screen) re-skin — with full legal receipt content verified untouched.
7. Loyalty/currency HUD bar.
8. Optional: staff quests/leaderboard.
9. QA pass against the Step 4 guardrails checklist, on both Fast Mode and full gamified mode.

---

## 8. Success Criteria / Definition of Done (self-check)

- [ ] Every existing POS function produces the exact same computed result (price, tax, change, inventory count) as before the re-skin.
- [ ] Full legally-required receipt content is present, unmodified, and readable in both modes.
- [ ] No flow requires more taps than the original to complete a sale, discount, or refund.
- [ ] All money-related numbers meet contrast/legibility requirements in the new theme.
- [ ] Stock/discount states are conveyed with icon + text, not color alone.
- [ ] All animations are skippable and Fast Mode fully disables non-essential motion/sound.
- [ ] Existing localization strings are extended, not bypassed.
- [ ] Existing offline/sync behavior still works.
- [ ] A revert-to-classic-UI toggle exists and works.

| Metric | Target |
|---|---| 
| Ring-up tap parity vs. classic | same or fewer taps |
| Receipt field diff before/after | 0 (identical) |
| Fast Mode frame drops on mid SDK-26 device | 0 during ring-up path |
| Money-number contrast (WCAG) | ≥ 4.5:1 in both modes |

---

## 9. Assumptions
- Target platform is the existing native Android tablet app (Jetpack Compose), unchanged from the APK just produced (`com.cafeos.tablet`, minSdk 26, targetSdk 34).
- The existing `PebotTheme` / `Pos*` token layer is the correct extension point; no redesign of `CafeViewModel` or DAO is in scope.
- "Current system" loyalty rules are whatever `CafeViewModel`/DAO already compute; this spec does not redefine loyalty math.
- BIR receipt fields are currently rendered in `POSScreen`/`ReceiptPrinter`; the spec treats their **exact content** as the immutable baseline.
- Initial rollout is English-only localization (Filipino is future-scope; strings structured to allow it).
- Fast Mode is opt-in-by-default at rollout so peak-hour speed is protected while the gamified look can be demoed in calm periods.

---

## 10. New Assets / Open Questions for the Designer
- MOBA-style vector icons for: cart loadout, champion/item cards, quest checkmarks, rank-ladder markers, gem/currency counter. (All SVG; lightweight.)
- A "brand team crest" variant of the Pebot logo for the login screen.
- Optional: a subtle particle/gradient background asset for the lobby (vector-based to avoid raster bloat).
- Sound designer needed for an optional "cha-ching"/confirm SFX (off by default in Fast Mode).
