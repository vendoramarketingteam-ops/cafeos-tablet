# Phase 0 Research: Gamified POS — Audit Transcript

Source of truth: live repo at `app/src/main/` (Kotlin, Jetpack Compose). Verified by `./gradlew assembleDebug` → BUILD SUCCESSFUL (Gradle 8.7 / AGP 8.5.2 / Kotlin 1.9.24 / JDK 17).

## Stack
- Native Android, Kotlin 1.9.24, JVM 17 (Zulu 17.0.13).
- UI: Jetpack Compose (Material 3), Compose compiler 1.5.14.
- Architecture: single-activity MVVM. `MainActivity` -> `PebotTheme` -> `MainScreen` (shared `CafeViewModel`).
- Navigation: `navigation-compose` NavHost, 17 routes (see below).
- Persistence: Room 2.6.1 (+KSP), coroutine `Dispatchers.IO`, `Flow`/`StateFlow`.
- Networking/offline: `SocketSyncManager` (Socket.IO), `LocalWebServer` (nanohttpd), `PcBridgeClient` (websocket), `MenuImportParser`. Sync is opportunistic; local SQLite is source of truth.
- DI: none ( ViewModel created via `viewModel()` in `MainScreen`).

## Existing theme layer (re-skin extension point — NOT a fork)
- `ui/theme/Color.kt`: `PosInk`, `PosPaper`, `PosSurface`, `PosAccent` (green #4A5D3A), `PosGold` (#B58A4A), `PosCoffee`/`PosCoffeeLight`/`PosCoffeeDeep`, `PosCream`, `PosDanger`, `PosInfo`.
- `ui/theme/Type.kt`: Material 3 `Typography` (display/title/body/label) using `FontFamily.SansSerif` with tight letter-spacing.
- `ui/theme/Theme.kt`: `PebotTheme(darkTheme=false, dynamicColor=false)` mapping tokens to color scheme; sets status/navigation bar colors.

## Screen / flow enumeration (existing surfaces)
Login -> MainScreen (rail nav) -> `live_orders` | `pos` (cart -> checkout -> payment -> receipt) | `kitchen` | `analytics` | `products` | `inventory` (+ `stock_history`) | `expenses` | `loyalty` | `order_history` | `reviews` | `tables` | `stations` | `staff` | `settings` | `audit_logs` | `connection`. Plus `AccessDeniedScreen`.

## Business logic that MUST NOT change (single source of truth = `CafeViewModel`)
Pricing/tax/discount/change: `calculateTotals()`, `calculateBirPrice()`, `getVoucherDiscountAmount()`, `setDiscountType()`, `setSelectedVoucher()`.
Cart: `addToCart()`, `removeFromCart()`, `updateCartItemQuantity()`, `clearCart()`, `openProductOptions()`/`closeProductOptions()`.
Checkout/order: `placeOrder()`, `openCheckout()`/`closeCheckout()`, `setOrderType()`, `setCustomerName()`, `setDeliveryAddress()`, `setSelectedTableId()`.
Payment: `setPaymentMethod()`, `setAmountTendered()`, `setScreenshotPath()`/`clearScreenshot()`.
Inventory/receipting: `adjustStock()`, `saveProductRecipe()`, `ReceiptPrinter`, `ReceiptNumbers`.
Access: `AccessControl.canAccessRoute()`, `Staff` role normalization (ADMIN/STAFF/KITCHEN).
Offline/sync: `SocketSyncManager`, `LocalWebServer`, `PcBridgeClient`, `MenuImportParser`.

## Coupling observations
- Screens already receive the shared `CafeViewModel` (good separation) — re-skin is presentation-only.
- Money rendering uses `NumberFormat` in many composables; re-skin may change style (color/font) but must preserve the numeric value and contrast.
- `AccessControl` is a single object — do not fragment permission logic.
- Only `SharedPreferences("pebot_sync")` exists; the Fast Mode toggle is net-new, SharedPreferences-backed (no DataStore migration).

## Risks / fragility
1. Receipt BIR fields (rendered in `POSScreen`/`ReceiptPrinter`) must diff to zero before/after.
2. Numeric legibility of price/tax/change/totals under new theme colors.
3. No DataStore — keep the new toggle on the existing SharedPreferences pattern.
4. Build already compiles with warnings (unused params, deprecated APIs, label shadowing in `CafeViewModel`); do not introduce new errors.
5. Offline-first: theme additions add no runtime/network dependency.
