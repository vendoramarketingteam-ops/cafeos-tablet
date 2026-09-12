# Research: Hub-and-Spoke Mobile Navigation (Spec 003)

## Source Material

### HTML Mockup
- **File**: `specs/003-sample html/pebot-navigation-mockup.html` (502 lines)
- **Design system**: CSS custom properties for a coffee-shop color palette
- **JS behavior**: Single-level toggle between `.view-home` and `.view-app[data-view="..."]`
- **No nested routing**: One entry point (Home) to any app; one Home button back

### Visual Design (from CSS)

| Token | Value | Usage |
|-------|-------|-------|
| `--espresso` | `#1B140F` | Home bg, Home button bg |
| `--espresso-2` | `#241B14` | (unused in mockup) |
| `--espresso-3` | `#3A2C1F` | Tables tile bg |
| `--cream` | `#F4EDE0` | Home text, Home button text |
| `--cream-2` | `#EAE1CF` | App screen bg, home indicator |
| `--ink` | `#241A12` | App screen body text |
| `--ink-muted` | `#8A7A68` | Secondary text |
| `--sienna` | `#B5541E` | New Order tile bg |
| `--sienna-dark` | `#8F3F14` | HR tile bg |
| `--sage` | `#6B7A5E` | Inventory tile bg |
| `--sage-dark` | `#4E5C44` | Reports tile bg |
| `--gold` | `#C9A227` | Kitchen tile bg, greeting accent |
| `--gold-dark` | `#9C7D1D` | Admin tile bg |

### Layout (from CSS — iPhone SE proportions: 375×780)

**Home screen**:
- Status bar (top): time + signal icons, 13px font
- Greeting: "GOOD MORNING" eyebrow (gold, 11.5px), name (Fraunces serif, 22px), sub (12.5px muted)
- Tile grid: 4-column `grid-template-columns: repeat(4, 1fr)`, 14px gap, 22px top padding
- Each tile: 56×56px icon (rounded 16px), 11px label
- Tap feedback: `transform: scale(0.92)`

**App screen**:
- Top bar: Home button (espresso rounded-20, cream text) + app title (17px Fraunces serif)
- Content area: 16px horizontal padding, 90px bottom padding
- Transition: `opacity 220ms ease, transform 220ms ease` + `translateY(10px)` → `translateY(0)`

### App tiles (from HTML)

| # | Label | Tile class | Icon (SVG) | Route |
|---|-------|-----------|------------|-------|
| 1 | New Order | `t-order` | Shopping cart | `pos` |
| 2 | Kitchen | `t-kitchen` | Restaurant | `kitchen` |
| 3 | Inventory | `t-inventory` | Cube/blocks | `inventory` |
| 4 | Tables | `t-tables` | 4 squares | `tables` |
| 5 | HR | `t-hr` | People | `staff` |
| 6 | Reports | `t-reports` | Bar chart | `analytics` |
| 7 | Settings | `t-settings` | Gear | `settings` |
| 8 | Admin | `t-admin` | Shield | `admin` (new route) |

## Current State: MainActivity.kt (343 lines)

### Architecture
- **Single-activity**: `MainActivity` → `MainScreen()` (Composable)
- **Surface + Scaffold**: Light gray background, dark espresso sidebar
- **NavigationRail**: Left rail, 250dp (expanded) / 88dp (collapsed), 15 items
- **NavHost**: Right content area, all routes as `composable()` blocks
- **Access control**: `AccessControl.canAccessRoute(currentStaff, route)` filters items

### Current sidebar items (15 routes, 3 groups)

| Route | Label | Icon | Group |
|-------|-------|------|-------|
| `live_orders` | Live Orders | `FormatListBulleted` | OPERATIONS |
| `pos` | New Order | `ShoppingCart` | OPERATIONS |
| `kitchen` | Kitchen | `Restaurant` | OPERATIONS |
| `analytics` | Analytics | `BarChart` | OPERATIONS |
| `products` | Products | `Coffee` | OPERATIONS |
| `inventory` | Inventory | `Inventory` | OPERATIONS |
| `stock_history` | Stock History | `History` | OPERATIONS |
| `expenses` | Expenses | `AttachMoney` | OPERATIONS |
| `loyalty` | Loyalty | `LocalOffer` | MANAGEMENT |
| `order_history` | Order History | `TableChart` | MANAGEMENT |
| `reviews` | Reviews | `Star` | MANAGEMENT |
| `tables` | Tables | `TableBar` | MANAGEMENT |
| `stations` | Stations | `Build` | MANAGEMENT |
| `staff` | Staff | `Group` | MANAGEMENT |
| `settings` | Settings | `Settings` | MANAGEMENT |
| `audit_logs` | Audit Logs | `List` | MANAGEMENT |
| `connection` | Connection | `Wifi` | SYSTEM |

### What stays in the HTML mockup (8 tiles)
The HTML has 8 tiles. The current app has 17 routes. The mapping:
- The 8 HTML tiles map to the 8 most "primary" routes
- The remaining 9 routes (Live Orders, Products, Stock History, Expenses, Order History, Reviews, Stations, Audit Logs, Connection) need to be accessible but not on the home grid

**Decision**: The 8 home tiles match the HTML mockup. The remaining routes either:
- (a) Are absorbed into existing app screens as in-app features, OR
- (b) Are accessible via a "More" or overflow action in the Home screen

### Current navigation behavior
- **Bidirectional**: Any sidebar item can navigate to any other
- **Back stack**: `navController.navigate(route)` with `popUpTo(startDestination)` + `launchSingleTop`
- **No back press handling**: Android back closes the app
- **Collapse/expand**: `navCollapsed` state toggles rail width and label visibility

### Existing gamification hooks
- `GameFeedback.tap()`, `GameFeedback.confirm()` — haptics + SFX
- `RarityPulse`, `CurrencyCountUp` — motion primitives
- `SettingsStore` — reads `THEME_MODE` and `SOUND_ENABLED` from `pebot_sync` SharedPreferences
- Default theme mode: GAMIFIED

## Key Constraints from Constitution

1. **Offline-first**: Navigation must work with zero connectivity — purely client-side
2. **Touch targets ≥ 48dp**: 56×56px tile icons pass
3. **Motion < 300ms**: 220ms transition matches
4. **Single-activity + NavHost**: Already matches the hub-and-spoke model
5. **Material 3**: Home button uses `RoundedCornerShape(20.dp)`, tiles use `RoundedCornerShape(16.dp)`

## Migration Challenges

### 1. NavHost back stack
Current: `navigate(route) { popUpTo(startDestination); launchSingleTop = true }`
Required: Tile tap → navigate to app route (push). Home button → navigate back to home.

**Solution**: Change NavHost `startDestination` to `"home"`, add `composable("home") { HubScreen(...) }`, and each app route returns to home via `navController.popBackStack()` or `navController.navigate("home") { popUpTo("home") }`.

### 2. Access control
Currently filtered in `MainActivity` via `AccessControl`. The HubScreen needs to do the same — only show tiles for permitted routes.

### 3. Live Orders tile
The HTML mockup doesn't include "Live Orders" as a tile, but the current app treats it as the default/home destination for non-kitchen staff. The HubScreen IS the new home, so Live Orders can be accessed from within the POS app or as a sub-feature.

Actually — re-reading the HTML, the 8 tiles are: New Order, Kitchen, Inventory, Tables, HR, Reports, Settings, Admin. "Reports" maps to `analytics` (current). Live Orders would need its own tile or be accessible from POS.

**Decision**: Add a "Live Orders" tile (9th tile, 3rd column in a 4-column grid) OR fold it into POS. Given the spec says "grid of app tiles, one per module," Live Orders is its own module. I'll add it as a 9th tile with the `FormatListBulleted` icon.

### 4. The "Admin" tile
The current app has `audit_logs` and `connection` under "SYSTEM" group with restricted access. The HTML mockup adds "Admin" as a tile. This could be a new combined "Admin" screen or map to the existing staff management.

**Decision**: "Admin" tile = `settings` route (Settings already exists). Audit Logs and Connection can be sub-sections within Settings.

### 5. Transition animation
The mockup uses CSS `opacity` + `translateY` with 220ms ease. In Compose, this requires `AnimatedVisibility` or `AnimatedContent` around the NavHost, or per-route enter/exit animations.

**Option A**: Wrap each app composable's root in `AnimatedVisibility(visible = true, enter = slideIn + fadeIn, exit = ...)`.
**Option B**: Use `NavHost` with custom `enterTransition`/`exitTransition` per composable (requires `AnimatedNavHost`).
**Option C**: Simple `fadeIn() + slideIn()` via NavHost builder.

**Decision**: Option B (using `AnimatedNavHost` from `androidx.navigation.compose`) — standard approach, declarative, and the `fadeOut() + slideOut()` on exit gives the reverse transition when pressing Home.
