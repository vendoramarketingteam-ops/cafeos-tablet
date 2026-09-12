# Audit — Confirmed Defects vs. Real Composables

## Defect 1: Tab labels wrap into vertical letter-stacks

**Root cause:** `Tab` composables using `Text(title)` with no `maxLines = 1` or `TextOverflow.Ellipsis`, inside a fixed-width `TabRow` that divides its width evenly across all tabs.

**Confirmed locations:**

| File | Function | Line | Tab count | Status |
|------|----------|------|-----------|--------|
| `InventoryScreen.kt` | `InventoryScreen()` | 62 | 7 tabs ("Products", "Ingredients", "Suppliers", "Capacity", "History", "Purchases", "Alerts") | `TabRow` → **needs ScrollableTabRow** |
| `LiveOrdersScreen.kt` | `LiveOrdersScreen()` | 47 | 3 tabs ("Kanban", "Tables", "Stations") | `TabRow` → **needs ScrollableTabRow** |
| `LoyaltyScreen.kt` | (root function) | 51 | (tabs list) | `TabRow` → **needs ScrollableTabRow** |
| `ProductScreen.kt` | `ProductScreen()` | 167 | 2 tabs ("Products", "Options") | `TabRow` → **needs ScrollableTabRow** |
| `StaffManagementScreen.kt` | (root function) | 112 | (tabs list) | `ScrollableTabRow` → **needs maxLines/ellipsis** |
| `ProductScreen.kt` | `ProductScreen()` | 207 | (category list, dynamic) | `ScrollableTabRow` → **needs maxLines/ellipsis** |
| `SettingsScreen.kt` | (root function) | 76 | (tabs list) | `ScrollableTabRow` → **needs maxLines/ellipsis** |
| `KitchenScreen.kt` | (root function) | 70 | (station names, dynamic) | `ScrollableTabRow` → **needs maxLines/ellipsis** |
| `POSScreen.kt` | `PosMenuContent()` | 316 | (category list, dynamic) | `ScrollableTabRow` → **needs maxLines/ellipsis** |

**Fix:** Replace `TabRow` with `ScrollableTabRow` where not already scrollable; add `maxLines = 1, overflow = TextOverflow.Ellipsis` to every `Text` inside `Tab`/`TabRow` across all touched files.

## Defect 2: Kanban column headers wrap

**Root cause:** Same — `Text` in `KanbanBoard()` column headers with no `maxLines`/ellipsis.

**Confirmed location:**
- `LiveOrdersScreen.kt` line 111-116 — `Text(text = columnTitles[status] ...)` in `KanbanBoard()`, no `maxLines`/`overflow`.

**Fix:** Add `maxLines = 1, overflow = TextOverflow.Ellipsis` to the header `Text`. Also widen columns (addressed in Phase 4 Kanban redesign).

## Defect 6: Pay button / order total clips at bottom; no edge-to-edge

**Root cause:** No `enableEdgeToEdge()`, no `WindowInsets` handling anywhere in the app.

**Confirmed locations:**

| File | Function | Line | Issue |
|------|----------|------|-------|
| `MainActivity.kt` | `MainActivity.onCreate()` | 52-67 | No `enableEdgeToEdge()` or `WindowCompat.setDecorFitsSystemWindows()` |
| `MainActivity.kt` | `AppScreenFrame()` | 379-394 | Plain `Column` (AppTopBar + Box); no `navigationBarsPadding()`, no `Scaffold` |
| `POSScreen.kt` | `PosCartContent()`, Checkout Button | 459-467 | `Button(Modifier.fillMaxWidth())` — no `navigationBarsPadding()` |

**Fix:** Add `enableEdgeToEdge()` in `MainActivity`; add `navigationBarsPadding()` to `AppScreenFrame` content `Box` and to the Checkout `Button` in `PosCartContent`.

## Defect 7: Bottom bar (NOT APPLICABLE)

No persistent bottom bar exists in the current codebase. Navigation is hub-and-spoke with `AppTopBar` (top-left "Home" button) + `AppScreenFrame`. This defect is N/A — no action needed.

## Defect 8: No shared design-token system

Colors exist in `Color.kt` (semantically named), Typography in `Type.kt`, motion in `Motion.kt`. But there is **no `Dimens`/`Spacing` object** — dp values are hardcoded per-Composable (e.g., `12.dp`, `14.dp`, `16.dp`, `18.dp` scattered throughout). A `Dimens` object needs to be created and flagged files migrated (Phase 3).

---

## Phase 1 Verification (on-device — Samsung Galaxy A16 SM-A165F)

| Check | Result |
|-------|--------|
| Inventory sub-nav tabs (7 tabs) | ✅ All single-line (`maxLines=1`), no wrapping. ScrollableTabRow allows horizontal scroll to see all 7. |
| Live Orders tabs (Kanban/Tables/Stations) | ✅ All single-line, height ≈49dp. |
| Kanban column headers (PENDING/PREPARING/COMPLETED/CANCELLED) | ✅ All single-line, height ≈57dp. |
| POS Checkout button position | ✅ At y=2143-2269px, above nav bar (2340px). Not clipped. |
| Edge-to-edge | ✅ AppTopBar title at y=43px (status bar drawn behind content). |
| `compileDebugKotlin` | ✅ BUILD SUCCESSFUL, 0 errors |
| `lintDebug` | ✅ 0 errors, only pre-existing warnings |
| `testDebugUnitTest` | ✅ 87 tests pass, 0 failures |
| Debug auto-login code | ✅ Fully reverted |

