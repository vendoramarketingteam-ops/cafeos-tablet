# Implementation Plan: Hub-and-Spoke Mobile Navigation

**Branch**: `003-hub-spoke-nav` | **Date**: 2026-09-11 | **Spec**: [`specs/003-sample html/pebot-navigation-mockup.html`](pebot-navigation-mockup.html) · [`research.md`](research.md)

**Input**: HTML mockup `pebot-navigation-mockup.html` + improved spec (hub-and-spoke, single-level, no cross-module navigation).

## Summary

Replace the current persistent side `NavigationRail` (15+ items, 250dp wide) in `MainActivity.kt` with a **hub-and-spoke** navigation model:

- **Hub** = `HubScreen` — a Home screen with a 4-column grid of 8–9 app tiles (New Order, Kitchen, Inventory, Tables, HR, Reports, Settings, Admin, Live Orders)
- **Spokes** = existing app screen composables, each rendered full-screen with a **persistent Home button** in the fixed top-left position
- **No cross-module links**: to navigate from one module to another, the user taps Home → returns to the grid → taps a different tile
- **Transitions**: 220ms slide/fade on tile tap; reverse on Home press
- **Access control**: tiles are shown/hidden based on `AccessControl.canAccessRoute()` (same logic as the current sidebar)

This is an **additive refactor** of `MainScreen()` — the existing screen composables (`POSScreen`, `KitchenScreen`, etc.) are unchanged; only the navigation scaffold around them is replaced.

## Technical Context

**Language/Version**: Kotlin 1.9.24 (note: Constitution §V says Kotlin 2.0; existing code is 1.9.24 — staying consistent with the current codebase)

**Primary Dependencies**: Jetpack Compose Material 3 (BOM 2023.10.01 / Compose 1.5.14), Jetpack Navigation Compose, Room 1.6.x (for data, unchanged)

**Storage**: Room / SQLite (unchanged — navigation is purely UI layer)

**Testing**: JUnit 5, Compose UI Test (`createComposeRule`), existing `./gradlew testDebugUnitTest` + `connectedAndroidTest`

**Target Platform**: Android tablet, minSdk 26, targetSdk 34

**Project Type**: Mobile app (Android native, Kotlin + Compose)

**Performance Goals**: Navigation transitions < 300ms (220ms target); no frame drops on tile tap

**Constraints**:
- Offline-first (Constitution §I) — navigation is fully client-side, no network dependency
- Touch targets ≥ 48dp (Constitution §IV) — tile icons are 56×56px
- Motion < 300ms (Constitution §IV) — 220ms transition
- Single-activity NavHost (locked decision) — hub is the start destination
- Thin data layer (Constitution §III) — no DAO calls in navigation composables

**Scale/Scope**: 1 new top-level composable (`HubScreen`), 1 new top bar component (`AppTopBar`), ~15 existing routes updated to include the Home button

## Constitution Check

| Principle | Status | Notes |
|---|---|---|
| §I Offline-first | ✅ Pass | Navigation is pure UI, no network |
| §II Data integrity | ✅ Pass | No financial data touched by nav change |
| §III Thin data layer | ✅ Pass | HubScreen reads from ViewModels, not DAOs |
| §IV Premium minimal design | ✅ Pass | 56×56px tiles, 220ms motion, moss/cream palette |
| §IV Touch targets ≥ 48dp | ✅ Pass | 56×56px icon + full-width label area |
| §IV Motion < 300ms | ✅ Pass | 220ms transition |
| §V Tests required | ✅ Planned | Compose UI test for tile → app transition |
| Kotlin 2.0 (locked) | ⚠️ Deviation | Existing code is 1.9.24; plan follows existing version. Flag for future upgrade. |

## Project Structure

```text
app/src/main/java/com/cafeos/tablet/
├── MainActivity.kt                         # MainScreen — REPLACE NavigationRail with HubScreen
├── ui/
│   ├── screens/
│   │   ├── HubScreen.kt                    # NEW — home grid of app tiles
│   │   ├── POSScreen.kt                    # EXISTING — add Home button top bar
│   │   ├── KitchenScreen.kt                # EXISTING — add Home button top bar
│   │   ├── InventoryScreen.kt              # EXISTING — add Home button top bar
│   │   ├── ... (all existing screens)      # EXISTING — add Home button top bar
│   └── components/
│       └── AppTopBar.kt                    # NEW — reusable top bar with Home button
└── theme/
    ├── Color.kt                            # EXTEND — add espresso/cream/sienna/gold/sage from mockup
    └── Theme.kt                            # EXISTING — integrate new colors
```

### Structure Decision
Keep all navigation changes in `MainActivity.kt` + 2 new files (`HubScreen.kt`, `AppTopBar.kt`) + minor modifications to existing screens (add a 30-line top bar each). No new packages, no new modules. This matches the existing single-project structure and minimizes diff surface.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected |
|---|---|---|
| Adding `HubScreen.kt` as a new file | The mockup defines a completely new screen type (tile grid) that doesn't exist in the current architecture | Could retrofit the tile grid into one of the existing screen composables (e.g., LiveOrdersScreen), but that conflates concerns and makes the Home screen hard to test independently |
| `AppTopBar.kt` component | 9 screens all need the same persistent Home button — DRY requirement | Could inline the top bar into each screen, but that's 9× duplicated 12-line blocks; rejected for maintainability |

## Phases

### Phase 0: Research & Planning
- [x] T001 Read HTML mockup `pebot-navigation-mockup.html` — visual design, layout, colors
- [x] T002 Read `MainActivity.kt` — current nav architecture, NavHost routes, access control
- [x] T003 Map 8 HTML tiles → existing routes; identify gaps (`admin` tile, `live_orders` tile)
- [x] T004 Analyze transition requirements (220ms slide/fade) vs Compose capabilities
- [x] T005 Write `research.md` · `plan.md` · `data-model.md` · `quickstart.md`

### Phase 1: Colors & Foundation
**Purpose**: Add the espresso/cream/sienna/gold/sage palette from the HTML mockup to the Compose theme. Create the `AppTopBar` reusable component.

- [ ] T011 Extend `Color.kt` with HTML palette tokens
  - `HomeEspresso = Color(0xFF1B140F)`, `HomeEspressoDeep = Color(0xFF241B14)`, `HomeEspressoDarker = Color(0xFF3A2C1F)`
  - `HomeCream = Color(0xFFF4EDE0)`, `HomeCreamSoft = Color(0xFFEAE1CF)`
  - `HomeSienna = Color(0xFFB5541E)`, `HomeSiennaDark = Color(0xFF8F3F14)`
  - `HomeSage = Color(0xFF6B7A5E)`, `HomeSageDark = Color(0xFF4E5C44)`
  - `HomeGold = Color(0xFFC9A227)`, `HomeGoldDark = Color(0xFF9C7D1D)`
  - `HomeInk = Color(0xFF241A12)`, `HomeInkMuted = Color(0xFF8A7A68)`
- [ ] T012 Create `AppTopBar.kt` — reusable top bar with Home button (espresso rounded-20, cream text, 56dp height)
- [ ] T013 Define `HomeTile` data class: `label: String, icon: ImageVector, route: String, tint: Color` (tile-specific accent color)

### Phase 2: HubScreen (Home)
**Purpose**: Create the hub-and-spoke Home screen with the tile grid.

- [ ] T021 Create `HubScreen.kt` — `HomeScreen(navController, currentStaff, items)` composable
- [ ] T022 Implement greeting strip — eyebrow ("GOOD MORNING"), name, sub-line
- [ ] T023 Implement `LazyVerticalGrid` (4 columns) of `HomeTile` buttons with 56×56px colored icons
- [ ] T024 Wire tile `onClick` → `navController.navigate(route)` with slide/fade enter transition
- [ ] T025 Add status bar (time placeholder for tablet; or actual system time)
- [ ] T026 Filter tiles by `AccessControl.canAccessRoute()` + show access-denied fallback
- [ ] T027 Add `@Preview` (Gamified + Classic themes)

### Phase 3: App Screens (Spokes) — Top Bar
**Purpose**: Add the persistent `AppTopBar` with Home button to every existing screen.

- [ ] T031 Add `AppTopBar(navController, title)` to `POSScreen` (title: "New Order")
- [ ] T032 Add `AppTopBar` to `KitchenScreen` (title: "Kitchen")
- [ ] T033 Add `AppTopBar` to `InventoryScreen` (title: "Inventory")
- [ ] T034 Add `AppTopBar` to `TablesManagementScreen` (title: "Tables")
- [ ] T035 Add `AppTopBar` to `StaffManagementScreen` (title: "HR")
- [ ] T036 Add `AppTopBar` to `AnalyticsScreen` (title: "Reports")
- [ ] T037 Add `AppTopBar` to `SettingsScreen` (title: "Settings")
- [ ] T038 Create `AdminScreen` (title: "Admin") — stub or redirect to Settings
- [ ] T039 Add `AppTopBar` to `LiveOrdersScreen` (title: "Live Orders") — if separate tile

**Note**: Each screen's Home button calls `navController.popBackStack("home", inclusive = false)` — returns to the HubScreen. No cross-module links in any app screen.

### Phase 4: MainActivity Refactor
**Purpose**: Replace the side `NavigationRail` + expanded `NavHost` with the hub-and-spoke model.

- [ ] T041 Change `NavHost` `startDestination` from current default → `"home"`
- [ ] T042 Add `composable("home") { HubScreen(...) }` route
- [ ] T043 Remove `NavigationRail` column entirely (150+ lines of `NavigationRailItem`s)
- [ ] T044 Add `enterTransition`/`exitTransition` to each route composable for slide/fade (220ms)
- [ ] T045 Add `BackHandler` — back from any app screen returns to `"home"` (not exit app)
- [ ] T046 Update `onNavigateToItemShop` / `onNavigateToLoyalty` to use `navController.navigate` (already works, just remove nav-rail context)
- [ ] T047 Remove `navCollapsed` state and `NavigationRail` toggle button — no longer needed

### Phase 5: Transitions & Motion
**Purpose**: Implement the 220ms slide/fade transitions matching the HTML mockup.

- [ ] T051 Add `slideIn` + `fadeIn` (220ms ease) as `enterTransition` for app routes
- [ ] T052 Add `slideOut` + `fadeOut` (220ms ease) as `exitTransition` for app routes
- [ ] T053 Add reverse transition for Home button press (app → home)
- [ ] T054 Verify `GameFeedback.tap()` fires on tile tap (gamification)

### Phase 6: Quality Gate
**Purpose**: Compile, lint, tests, and acceptance validation.

- [ ] T061 `compileDebugKotlin` — 0 errors
- [ ] T062 `lintDebug` — 0 errors
- [ ] T063 `testDebugUnitTest` — all existing tests pass
- [ ] T064 `assembleDebug` — APK builds
- [ ] T065 Compose UI test: tile tap navigates to correct app
- [ ] T066 Compose UI test: Home button returns to hub
- [ ] T067 Compose UI test: back button returns to hub (not app exit)
- [ ] T068 `@Preview` renders HubScreen in both Gamified and Classic themes
