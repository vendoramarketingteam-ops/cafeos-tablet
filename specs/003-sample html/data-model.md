# Data Model: Hub-and-Spoke Navigation

## Overview

The hub-and-spoke navigation is **purely a UI-layer concern**. No new database entities,
DAO methods, or schema migrations are required. All data comes from existing sources:

- Staff member + role (determines tile visibility via `AccessControl`)
- `CafeViewModel` (shared ViewModel — orders, products, inventory — already wired)
- `ItemShopViewModel` (already created for the gamified POS checkout)
- `SettingsStore` (theme mode, sound enabled — from `pebot_sync` SharedPreferences)

## New UI-Only Data Structures

### HomeTile (in-memory only)

```kotlin
data class HomeTile(
    val label: String,
    val icon: ImageVector,
    val route: String,
    val tint: Color,          // tile-specific accent (sienna, gold, sage, etc.)
    val badgeCount: Int = 0,   // e.g., Live Orders shows pending count
)
```

### Tile → Route Mapping

| Tile Label | Icon | Route | Access Control | Badge |
|---|---|---|---|---|
| Live Orders | `FormatListBulleted` | `live_orders` | `canAccessRoute(staff, "live_orders")` | Pending order count from `CafeViewModel.allOrders` |
| New Order | `ShoppingCart` | `pos` | `canAccessRoute(staff, "pos")` | None |
| Kitchen | `Restaurant` | `kitchen` | `canAccessRoute(staff, "kitchen")` | Pending prep count |
| Inventory | `Inventory` | `inventory` | `canAccessRoute(staff, "inventory")` | Low-stock count |
| Tables | `TableBar` | `tables` | `canAccessRoute(staff, "tables")` | Occupied count |
| HR | `Group` | `staff` | `canAccessRoute(staff, "staff")` | None |
| Reports | `BarChart` | `analytics` | `canAccessRoute(staff, "analytics")` | None |
| Settings | `Settings` | `settings` | `canAccessRoute(staff, "settings")` | None |
| Admin | `Shield` (new) | `admin` | `canAccessRoute(staff, "settings") && staff.role == "ADMIN"` | None |

**Note**: The HTML mockup shows 8 tiles (New Order, Kitchen, Inventory, Tables, HR, Reports, Settings, Admin). Live Orders is added as a 9th tile because the current app treats it as the primary home screen for non-kitchen staff. The grid is 4-column, so 8 tiles = 2 rows, 9 tiles = 3 rows (last row has 1 tile).

### New Route: `admin`

Currently the app has no `admin` route — Settings (`settings`) serves admin functions. The HTML mockup shows "Admin" as a distinct tile. **Decision**: Map the "Admin" tile to the existing `settings` route. Audit Logs and Connection remain accessible within Settings as sub-sections.

## No Database Changes

- No new entities
- No new DAO methods
- No migrations
- Navigation state is entirely handled by Jetpack Navigation Compose's back stack

## Session State (ViewModel)

### HubViewModel (new — lightweight)

```kotlin
class HubViewModel(application: Application) : AndroidViewModel(application) {
    val visibleTiles: List<HomeTile>     // filtered by AccessControl + live badge data
    val pendingOrderCount: StateFlow<Int> // from CafeDao via transaction
    val lowStockCount: StateFlow<Int>
    val occupiedTableCount: StateFlow<Int>
}
```

**Alternative (rejected)**: Derive tiles directly in `HubScreen` from `CafeViewModel`. This would create tight coupling. A dedicated `HubViewModel` keeps the Home screen testable in isolation (Constitution §V: tests are the deliverable).

## StateFlow Contract

All badge data in the HubScreen is read from `StateFlow`:
- `CafeViewModel.allOrders: StateFlow<List<Order>>` → pending count
- `CafeViewModel.lowStockItems: StateFlow<Int>` → badge (if exists, else omit)
- `CafeViewModel.tableStatus: StateFlow<...>` → occupied count (if exists, else omit)

**Constraint**: HubScreen never calls DAO directly (Constitution §III: Room DAOs are the sole owners of database access; UI layer reads from StateFlow only).
