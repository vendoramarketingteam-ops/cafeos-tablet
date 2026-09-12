# Pebot POS → "MLBB Item Shop" Checkout Screen
## Master Implementation Prompt (paste this whole file to your coding AI)

---

## 0. HOW TO USE THIS FILE

This is a single, self-contained engineering prompt. Paste the **entire file** into your
coding assistant (Claude Code, Cursor, etc.) as the task description. It is written in
second person ("you") addressed to that assistant, and it assumes it has read/write
access to the `cafeos-tablet` (Pebot) repository.

Do not summarize or skip sections before starting — Section 5 (file plan) and Section 8
(component specs) are the load-bearing parts. Sections 1–4 exist so the assistant doesn't
propose changes that violate your locked toolchain or re-litigate decisions you've already made.

---

## 1. ONE-PARAGRAPH TASK SUMMARY

Build a new checkout/ordering screen for the Pebot Android POS, styled and *structured*
like the Mobile Legends: Bang Bang in-game Item Shop (reference screenshot: category rail
on the left, a scrollable item grid in the center with price + rarity-tinted cards, a
"Build Path" panel on the right showing a recommended combo tree with a one-tap
"Prioritize/Add All" action, a detail dock at the bottom describing the selected
item's effect and total cost, and a large primary "Purchase" button). This is **not**
a cosmetic reskin of the existing POSScreen — it is a new information architecture that
reuses your existing gamified components (`GameCard`, `Rarity`, `GemCounter`,
`PremiumHeader`, `PremiumPanel`, `GameFeedback`, `Motion.kt`) as the visual layer, wired
to new layout containers and new state/data plumbing described below. It must remain a
pure UI-layer feature that degrades cleanly to `FAST` and `CLASSIC` `ThemeMode`.

---

## 2. LOCKED CONSTRAINTS — DO NOT DEVIATE

Paste-verify these before writing any code. If a dependency bump or navigation library
seems "easier," it is out of scope — work within these versions:

```
Kotlin            1.9.24
AGP               8.5.2
Compose BOM       2023.10.01 (Compose libs 1.5.14)
KSP               1.9.24-1.0.20
Gradle wrapper    8.7
JDK               17 (Zulu 17.54.21)
minSdk / targetSdk 26 / 34
Package           com.cafeos.tablet
versionCode/Name  2 / 1.0.0 (bump versionCode when this feature ships)
Persistence       Room (SQLite) — CafeDao / CafeDatabase / Entities.kt
Navigation        NO LocalNavController (BOM resolution gap). All navigation is via
                  explicit callback lambdas passed down from MainActivity, e.g.
                  onNavigateToLoyalty: () -> Unit. Follow this exact pattern for any
                  new screen-to-screen transition this feature needs.
Theme system      SettingsStore singleton (pebot_sync SharedPreferences) exposes
                  ThemeMode { GAMIFIED, FAST, CLASSIC }. PebotTheme reads this and
                  drives animation scale + glow. Every new component must respect it.
```

Existing gamified layer you must **reuse, not reinvent**:

```
ui/components/GameComponents.kt
    - enum Rarity { COMMON, UNCOMMON, RARE, EPIC }
    - GameCard          (rarity-framed item-slot Card)
    - GemCounter        (currency/points counter widget)
    - StackCounter      (quantity badge)
    - GemIcon
    - RarityBadge       (border-color + tier styling)

ui/components/PremiumComponents.kt
    - PremiumHeader     (gem + gold title + neon divider)
    - PremiumPanel      (rarity/neon-bordered container, gem-in-title)

ui/theme/GameFeedback.kt
    - skippable haptics
    - optional "cha_ching" SFX, looked up by resource name, no-ops if resource absent

ui/theme/Motion.kt
    - ShopSpringEasing, CurrencyCountUp, RarityPulse, etc.
    - FAST ThemeMode zeroes all durations

External libs already in the project (do not add alternatives):
    - ZXing (MultiFormatWriter, BarcodeFormat) — used in ConnectionScreen for QR pairing
    - Compose foundation gestures (pointerInput, detectHorizontalDragGestures) — used in
      KanbanOrderCard swipe-to-progress on LiveOrders
```

**Hard "don't touch" list:**
- Do not modify `CafeDao` / `CafeDatabase` / `Entities.kt` beyond additive,
  backward-compatible changes with a proper Room `Migration`. Never mutate or drop
  existing columns/tables.
- Do not remove or weaken the `CLASSIC` / `FAST` rendering paths — this feature is an
  additional skin/screen, not a replacement for the plain POS flow.
- Do not touch `ConnectionScreen`'s QR pairing logic or `LiveOrders`' swipe gesture code
  except to import shared design tokens if genuinely needed.
- Do not introduce `LocalNavController`, a new nav library (Navigation-Compose, Voyager,
  etc.), or any Compose/Kotlin/AGP version bump.

---

## 3. REFERENCE UX TEARDOWN — WHAT THE SCREENSHOT ACTUALLY SHOWS

The screenshot is the MLBB in-match Item Shop. Break it into 9 zones. For each zone,
build the POS equivalent described on the right. This mapping is the actual spec —
don't reinterpret it loosely.

| Zone | What's in the game screenshot | POS equivalent to build |
|---|---|---|
| **A — Mode tabs** | "All Equipment" / "Simple Mode" toggle at top of the item panel | A toggle between **"All Products"** (full catalog grid) and **"Quick Picks"** (top sellers / favorites, fewer items, bigger touch targets — good for a busy till) |
| **B — Category rail** | Left vertical list: Recommend, Attack, Magic, Defense, Movement, Jungle, Roam | Left vertical list of **product categories** (e.g. Coffee, Pastries, Meals, Cold Drinks, Add-ons, Combos) pulled from the existing product category field |
| **C — Item grid** | 2-column scrollable grid of item icons, each with name, one-line effect blurb, and gold price bottom-left, tinted by rarity | 2-column scrollable grid of **product cards** (icon/photo, name, one-line description e.g. "Best Seller" / "Low stock", price), rarity-tinted by price tier using your existing `Rarity` enum |
| **D — Selection state** | Selected item gets a gold glow ring, a green horizontal progress bar under the icon, and a small "1" stack badge | Selected product gets a `RarityPulse` glow ring, an (optional) progress bar reused for **stock level**, and a **quantity stepper badge** using `StackCounter` |
| **E — "Builds" panel header** | Right panel titled "Builds", with a "Build Path" label, a compass/arrow icon, and a blue **"Prioritize"** pill button | A right panel titled **"Suggested Combo"**, with an **"Add All"** pill button that adds every recommended add-on to the cart in one tap |
| **F — Build path tree** | A branching diagram: one root item icon splits into 2 mid-tier items, each splitting into 2 basic items, each showing its own price | A **combo/upsell tree**: the selected base product at the top, branching into recommended add-ons/modifiers (e.g. "Oat Milk +₱30", "Extra Shot +₱40"), each node tappable to toggle inclusion, each showing its own delta price |
| **G — Item detail card** | Selected item's full name, stat lines ("+160 Physical Attack", "+5% Movement Speed"), and a "Unique Passive" description block | Selected product's **detail dock**: name, modifier/variant lines ("+ Oat Milk", "+ Extra Shot"), and a "Perk" block reusing the same visual language (e.g. loyalty point multiplier text, "Buy 5 get 1 free — 3 more to go") |
| **H — Purchase bar** | Gold-cost icon + total amount, big blue **"Purchase"** button, and quick-slot icons (Regen, Execute, Retreat, AOE) along the very bottom | **Checkout bar**: total price + big primary **"Purchase / Pay"** button, plus quick-action icon row (Hold Order, Split Bill, Apply Discount, Void Item) |
| **I — Top HUD** | Gold count, K/D counters, match timer along the very top of the screen | Existing **PremiumHeader** — till balance / loyalty gems, order count for the shift, session timer — this already exists, just needs to sit above the new shop layout |

---

## 4. TARGET SCREEN & COMPOSABLE TREE

New screen: `ItemShopPOSScreen` (does **not** replace `POSScreen` — add it as an
alternate ordering mode reachable via a toggle/callback from `POSScreen`, e.g. a
"Shop View" button in the existing header, wired the same way `onNavigateToLoyalty`
already works).

```
ItemShopPOSScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCheckoutConfirm: () -> Unit,
    viewModel: ItemShopViewModel = viewModel()
)
 └── PremiumHeader                      // reuse as-is (Zone I)
 └── Row (fills remaining height)
      ├── CategoryRail                  // Zone B
      ├── Column (weight 1f)
      │    ├── ShopModeTabRow           // Zone A ("All Products" / "Quick Picks")
      │    └── ShopItemGrid             // Zone C, contains N × ShopItemCard (Zone D)
      └── PremiumPanel (Builds column)  // Zone E wrapper
           ├── BuildPathHeader          // "Suggested Combo" + Add All button
           ├── BuildPathTree            // Zone F
           └── ItemDetailDock           // Zone G
 └── PurchaseBar                        // Zone H, pinned to bottom, full width
```

---

## 5. FILE PLAN

### 5.1 New files to create

```
ui/screens/shop/ItemShopPOSScreen.kt
    - Top-level composable described in Section 4. Owns layout only; all state comes
      from ItemShopViewModel via collectAsState().

ui/screens/shop/ItemShopViewModel.kt
    - Holds: selectedCategoryId, shopMode (ALL/QUICK_PICKS), gridItems (List<ShopItemUi>),
      selectedItem (ShopItemUi?), buildPathNodes (List<BuildPathNodeUi>), cartTotal.
    - Exposes: onCategorySelected(id), onModeToggled(mode), onItemTapped(item),
      onBuildNodeToggled(nodeId), onAddAllTapped(), onPurchaseTapped().
    - Talks to Room via existing CafeDao (read-only queries) + a new lightweight
      repository (see 5.3) for combo/upsell lookups.

ui/components/shop/CategoryRail.kt
    - CategoryRail(categories: List<CategoryUi>, selectedId: String, onSelect: (String) -> Unit)

ui/components/shop/ShopModeTabRow.kt
    - ShopModeTabRow(mode: ShopMode, onModeChange: (ShopMode) -> Unit)

ui/components/shop/ShopItemCard.kt
    - Wraps the existing GameCard, adds: quantity stepper (StackCounter), stock progress
      bar, "selected" glow state driven by Motion.kt's RarityPulse.

ui/components/shop/BuildPathTree.kt
    - BuildPathTree(root: BuildPathNodeUi, onNodeToggled: (String) -> Unit)
    - Renders the branching diagram from Zone F. See Section 8.4 for exact geometry.

ui/components/shop/ItemDetailDock.kt
    - ItemDetailDock(item: ShopItemUi?, activeModifiers: List<ModifierUi>)

ui/components/shop/PurchaseBar.kt
    - PurchaseBar(total: Long, currencyLabel: String, onPurchase: () -> Unit,
      quickActions: List<QuickActionUi>)

data/shop/ComboRepository.kt
    - Thin repository layer over a new DAO (5.3). Keeps ItemShopViewModel free of
      direct Room annotations, consistent with how CafeDao is already abstracted.

model/shop/ShopUiModels.kt
    - Plain data classes: ShopItemUi, CategoryUi, BuildPathNodeUi, ModifierUi,
      QuickActionUi, enum ShopMode { ALL, QUICK_PICKS }.
    - These are UI-layer models mapped FROM Room entities in the repository/viewmodel —
      never pass Room entities directly into Composables.
```

### 5.2 Existing files to edit

```
ui/screens/POSScreen.kt
    - Add a single toggle/button in the existing header row: "Shop View" ⇄ "List View".
      Wire it exactly like existing nav callbacks — no new nav mechanism.

MainActivity.kt
    - Add the callback wiring for ItemShopPOSScreen the same way onNavigateToLoyalty
      is already wired (state-hoisted screen switch, not a nav graph).

ui/components/GameComponents.kt
    - If GameCard doesn't currently accept a "selected" boolean + a slot for a bottom
      progress bar, extend its parameter list with sensible defaults (selected: Boolean
      = false, progress: Float? = null) rather than forking a new card component.

ui/theme/GameFeedback.kt
    - Add a call site (not new logic) for the existing haptic/SFX hook to fire on
      onBuildNodeToggled and onPurchaseTapped.
```

### 5.3 Data layer additions (Room) — additive only

Two small, additive pieces are needed. Do not touch existing tables.

```kotlin
// New entity — do not add columns to existing Product entity, keep this separate
// so it can be safely nullable/absent for stores that never configure combos.
@Entity(tableName = "combo_links")
data class ComboLinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val baseProductId: Long,       // FK to existing products table
    val addonProductId: Long,      // FK to existing products table
    val deltaPriceCents: Long,     // price added when this addon is included
    val sortOrder: Int = 0
)

@Dao
interface ComboDao {
    @Query("SELECT * FROM combo_links WHERE baseProductId = :productId ORDER BY sortOrder")
    suspend fun getComboLinksFor(productId: Long): List<ComboLinkEntity>
}
```

Add a `Migration` from the current DB version to version+1 that creates this table
only — no ALTER TABLE on existing schema. Register it in `CafeDatabase`'s
`Room.databaseBuilder(...).addMigrations(...)` chain.

**Rarity tier source of truth:** rarity is *derived*, not stored. Compute it in the
repository from `priceCents` using configurable thresholds (see Section 9) so it stays
consistent with however `GameCard`/`RarityBadge` already computes tiers elsewhere in
the app (check `ProductScreen`/`POSScreen` for the existing threshold logic and reuse
that exact function — do not write a second, possibly-inconsistent version).

---

## 6. STATE / VIEWMODEL FLOW

```
User taps category  ──▶ onCategorySelected(id)
                         └─ filters gridItems by category, clears selectedItem

User taps grid card  ──▶ onItemTapped(item)
                         ├─ sets selectedItem
                         ├─ triggers ComboRepository.getComboLinksFor(item.id)
                         ├─ maps result → buildPathNodes (root = item, children = addons)
                         └─ recomputes cartTotal from currently-toggled nodes

User taps a build-path node ──▶ onBuildNodeToggled(nodeId)
                         ├─ flips that node's `included` boolean
                         ├─ recomputes cartTotal
                         └─ fires GameFeedback haptic (light tap, not the cha-ching)

User taps "Add All" ──▶ onAddAllTapped()
                         └─ sets included = true on every buildPathNodes entry

User taps "Purchase" ──▶ onPurchaseTapped()
                         ├─ writes an Order + OrderItems via existing CafeDao
                         │   (base item + every `included` addon as separate line items)
                         ├─ fires GameFeedback "cha_ching" SFX + haptic
                         ├─ triggers CurrencyCountUp animation on the total display
                         └─ calls onNavigateToCheckoutConfirm() to show the existing
                            gamified OrderConfirmationDialog "victory" screen
```

---

## 7. INTERACTION DETAIL PER ZONE

**Zone A (mode tabs):** Two-segment control, not a full TabRow — visually a pill
switch matching the screenshot's compact "All Equipment / Simple Mode" toggle.
`QUICK_PICKS` mode should just filter `gridItems` to a `isFavorite`/`isTopSeller`
flag already available on your product model (check `Entities.kt` for an existing
flag before adding a new column).

**Zone B (category rail):** Vertical list, selected row gets a **left accent bar +
background tint**, matching the screenshot's blue-highlighted "Attack" row. Keep row
height generous (≥64dp) — this is a tablet POS, fat-finger safe touch targets matter
more than screen density.

**Zone C/D (grid + selection):** 2-column `LazyVerticalGrid`. Each `ShopItemCard`:
icon/photo top, name below, one-line subtitle, price bottom-left. On selection: glow
ring via `RarityPulse`, and if you added a stock-progress affordance, a thin bar
under the icon (green→amber→red as stock depletes, purely cosmetic reuse of the
game's cooldown-bar visual, not a new stock system — read from existing stock field
if one exists, otherwise omit the bar rather than inventing fake data).

**Zone E/F (build path):** This is the part most likely to get flattened into "just
a list" if under-specified, so be explicit: it is a **tree**, not a list. Root node
at the top-center of the panel, connector lines fanning down-left and down-right to
child nodes, exactly mirroring the screenshot's 1→2→4 branching shape (your case will
usually be 1 base item → 2–4 addon children, no need to force a second tier unless a
product genuinely has nested combos). Each node is tappable and shows: icon, price
delta, and a checked/unchecked visual state (checked = currently included in the
cart). "Add All" toggles every node to checked in one tap.

**Zone G (detail dock):** Directly below the tree, not a separate screen/dialog.
Shows the selected base item's name, then each *currently checked* modifier as a
"+ Modifier Name (+₱X)" line, then a "Perk" line if applicable (loyalty progress,
a "3 more for a free item" nudge, etc. — pull from existing Loyalty logic already
present in `OrderHistory`/`Loyalty` screens rather than inventing new loyalty rules).

**Zone H (purchase bar):** Full-width, pinned to the bottom of the screen (not just
the right column) — in the screenshot it spans the whole width, and functionally it
should show the *combined* total including the base item, not just the addon panel's
subtotal. Big primary button, label "Purchase" or "Pay ₱{total}" — pick one and be
consistent with wording used elsewhere in the app (check `OrderConfirmationDialog`
for existing copy conventions). Quick-action icon row beside/below it maps to
whatever quick actions already exist on `POSScreen` (hold order, discount, void) —
don't invent new ones not already supported by the order/cart logic.

---

## 8. COMPONENT SIGNATURES (Kotlin, Compose 1.5.14-compatible)

```kotlin
@Composable
fun CategoryRail(
    categories: List<CategoryUi>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
)

@Composable
fun ShopModeTabRow(
    mode: ShopMode,
    onModeChange: (ShopMode) -> Unit,
    modifier: Modifier = Modifier
)

@Composable
fun ShopItemCard(
    item: ShopItemUi,
    selected: Boolean,
    quantity: Int,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
)
// Internally: GameCard(rarity = item.rarity, ...) { ... } — do not fork GameCard's
// border/glow logic; extend its parameters instead (see Section 5.2).

@Composable
fun BuildPathTree(
    root: BuildPathNodeUi,
    onNodeToggled: (String) -> Unit,
    modifier: Modifier = Modifier
)

data class BuildPathNodeUi(
    val id: String,
    val name: String,
    val iconRes: Int?,
    val deltaPriceCents: Long,
    val included: Boolean,
    val children: List<BuildPathNodeUi> = emptyList()
)

@Composable
fun ItemDetailDock(
    baseItem: ShopItemUi?,
    activeModifiers: List<ModifierUi>,
    perkText: String? = null,
    modifier: Modifier = Modifier
)

@Composable
fun PurchaseBar(
    totalCents: Long,
    currencyLabel: String = "₱",
    onPurchase: () -> Unit,
    quickActions: List<QuickActionUi> = emptyList(),
    modifier: Modifier = Modifier
)
```

Every one of these takes a plain `Modifier` last-or-near-last, follows existing
naming (`*Ui` suffix for view models seen elsewhere in the codebase — confirm and
match whatever convention `ProductScreen`/`AnalyticsScreen` already use before
introducing a new naming style).

---

## 9. RARITY / PRICE-TIER RULES

Reuse the **existing** rarity-from-price function if one already exists in
`ProductScreen`/`POSScreen` (search for it before writing a new one — the prompt
you originally gave your AI likely already has this logic once, and duplicating it
is exactly the kind of drift that makes a "gamified skin" feel inconsistent across
screens). If none exists yet, centralize one in `GameComponents.kt`:

```kotlin
fun rarityForPrice(priceCents: Long, thresholds: RarityThresholds): Rarity = when {
    priceCents < thresholds.uncommon -> Rarity.COMMON
    priceCents < thresholds.rare     -> Rarity.UNCOMMON
    priceCents < thresholds.epic     -> Rarity.RARE
    else                              -> Rarity.EPIC
}
```

Make `thresholds` configurable via `SettingsStore` (not hardcoded), since price
bands that make sense for a coffee shop won't make sense for a store selling ₱2,000
combo meals.

---

## 10. THEME-MODE FALLBACK BEHAVIOR

- **GAMIFIED:** Full spec above — glow, pulse, cha-ching SFX, animated currency count-up.
- **FAST:** Same layout/structure, but `Motion.kt` already zeroes durations — verify
  that applies automatically to any new `Motion.kt` calls you add here rather than
  writing new duration constants that bypass that zeroing.
- **CLASSIC:** The tree/rarity/glow visuals should collapse to a plain, flat list —
  do not literally hide the feature; render the same data (category rail, item grid,
  combo suggestions, purchase bar) with neutral Material3 styling and no game framing.
  This mirrors how the rest of the app already treats `CLASSIC` mode — check how
  `ProductScreen` or `OrderHistory` currently branches on `ThemeMode` and match that
  exact pattern rather than adding a new one.

---

## 11. NAVIGATION WIRING (must match existing pattern exactly)

```kotlin
// In MainActivity.kt, following the same shape as the existing Loyalty wiring:
var showItemShop by remember { mutableStateOf(false) }

if (showItemShop) {
    ItemShopPOSScreen(
        onNavigateBack = { showItemShop = false },
        onNavigateToCheckoutConfirm = { /* existing OrderConfirmationDialog trigger */ }
    )
} else {
    POSScreen(
        onNavigateToLoyalty = { /* existing */ },
        onNavigateToShop = { showItemShop = true }, // new callback added to POSScreen
        // ...other existing params unchanged
    )
}
```

Do not introduce a `sealed class Screen` nav graph or any navigation library — this
must be a state-hoisted boolean/enum switch exactly like the existing pattern, so it
composes with whatever screen-switch mechanism already exists for Loyalty/Analytics.

---

## 12. ACCEPTANCE CRITERIA

Before calling this feature done, verify:

- [ ] Category rail filters the grid instantly with no flash of unfiltered content.
- [ ] Selecting an item populates both the Build Path tree *and* the Detail Dock in
      the same frame (no visible pop-in delay between the two).
- [ ] Toggling a build-path node updates the Purchase Bar total immediately.
- [ ] "Add All" checks every node and updates the total in one recomposition.
- [ ] Purchase writes exactly one Order row + one OrderItem row per included
      item/addon via the existing CafeDao — verify in Room's DB inspector, not just
      visually.
- [ ] Cha-ching SFX/haptic fires once per purchase, not once per recomposition.
- [ ] Switching `ThemeMode` to `CLASSIC` renders the same data with no game
      chrome and no crashes; switching to `FAST` keeps the same layout with
      instant (zero-duration) transitions.
- [ ] No changes to `ConnectionScreen` QR pairing or `LiveOrders` swipe gestures.
- [ ] `versionCode` bumped in `build.gradle`.
- [ ] New Room migration tested against an existing populated database (not just a
      fresh install) — combo_links table must appear without touching existing data.

---

## 13. PHASED DELIVERY (do this in order, don't jump ahead)

1. **Scaffold only** — build all new Composables from Section 5.1 with static/sample
   data (no Room, no ViewModel wiring). Get layout, spacing, and the build-path tree
   geometry visually matching the reference screenshot first.
2. **Wire real data** — connect `ItemShopViewModel` to `CafeDao` + new `ComboRepository`,
   replace sample data, verify category filtering and item selection against real
   products.
3. **Wire interactions** — build-path toggling, "Add All", Purchase → Order write,
   navigation to the existing confirmation dialog.
4. **Motion + feedback + theme fallback** — layer in `RarityPulse`, `CurrencyCountUp`,
   `GameFeedback` haptics/SFX, and verify `CLASSIC`/`FAST` fallback rendering.

Ship/review after each phase rather than all at once — this feature touches enough
new files that a single giant diff will be hard to review against the reference
screenshot.
