# CaféOS Premium Mobile UI — Handoff Plan

## Goal

Complete the request:

> Fix all UI/UX to be premium looking and all of the pages, it should work properly in mobile phone.

Preserve pricing, tax, inventory, payments, receipts, access control, offline persistence, Room data, ViewModel logic, and navigation behavior.

## Repository and PR

- Repository: `vendoramarketingteam-ops/cafeos-tablet`
- Base branch: `main`
- Working branch: `devin/1789232276-premium-mobile-ui`
- Implemented commit: `9cf9aac`
- Pull request: https://github.com/vendoramarketingteam-ops/cafeos-tablet/pull/1
- Local checkout in the original session: `/home/ubuntu/repos/cafeos-tablet`

To continue from another Devin account:

```bash
git clone https://github.com/vendoramarketingteam-ops/cafeos-tablet.git
cd cafeos-tablet
git fetch origin devin/1789232276-premium-mobile-ui
git checkout devin/1789232276-premium-mobile-ui
```

## Completed implementation

The PR currently changes 31 files with a shared responsive redesign:

1. Added a warm espresso, cream, sage, and gold palette.
2. Made shared premium screens, headers, panels, and top bars responsive.
3. Made login/setup cards width-safe and vertically scrollable.
4. Changed the hub to responsive 2/3/4-column layouts.
5. Rebuilt compact POS as full-screen `Menu` and `Order` panes instead of two compressed panes.
6. Reworked the item-shop compact layout with scrollable categories, tabs, an adaptive item grid, stacked build details, and a compact purchase bar.
7. Added adaptive grids, flexible dialogs, scroll-safe filters/actions, and compact stacking across product, inventory, analytics, customer analytics, tables, stations, vouchers, option groups, customers, floor plan, guests, stock history, loyalty, reviews, audit, connection, history, live orders, expenses, and bulk-adjust screens.
8. Increased important cart and recipe controls to 48dp touch targets.
9. Set FAST as the default low-motion premium mode.
10. Made `gradlew` executable and reduced the Gradle heap from 8 GiB to 4 GiB so builds succeed on a standard 7.8 GiB VM.

## Verification already completed

These commands passed:

```bash
export ANDROID_HOME="$HOME/.android-sdk"
export ANDROID_SDK_ROOT="$HOME/.android-sdk"
./gradlew --no-daemon --max-workers=2 :app:compileDebugKotlin
./gradlew --no-daemon --max-workers=2 testDebugUnitTest
./gradlew --no-daemon --max-workers=2 lintDebug
./gradlew --no-daemon --max-workers=2 assembleDebug
git diff --check
```

Debug APK:

```text
app/build/outputs/apk/debug/Pebot.apk
```

The GitHub PR currently has no configured CI checks.

## Phone-emulator results at commit `9cf9aac`

Tested on Android API 35 at 360×640dp.

Passed:

- First-run administrator creation.
- Returning six-digit PIN login.
- Hub renders three columns and all 10 modules are reachable by scrolling.
- Product Groups dialog can create a `Coffee` category.
- Creating a category enables the Add Product action.

Issues found:

1. `ProductScreen.kt`: in New Product → Pricing, the Margin Type dropdown consumes the row and Margin Value collapses into a narrow unusable strip.
2. `AppTopBar.kt`: the Home icon overlaps the `Home` label because an `IconButton` is being used for icon plus text.
3. `HubScreen.kt`: staff name and role/date use cream text in FAST mode on a cream background, producing almost no contrast.

Untested:

- Saving a product after the pricing fix.
- Compact POS menu-to-cart flow.
- Item-shop build details.
- Inventory.
- Reports.
- Tables.
- Hub at 320dp.

## Current uncommitted state in the original session

Two fixes were already applied locally after the first emulator run but were not committed or pushed:

- `AppTopBar.kt`: replaced the icon-only `IconButton` with a properly padded Material `Button`.
- `HubScreen.kt`: changed staff name and role/date colors to dark ink in non-gamified modes.

The Product pricing fix was not yet applied.

If continuing on a fresh machine/account, reapply all three fixes rather than relying on that original working tree.

## Exact next implementation

### 1. Fix Product pricing fields

In `app/src/main/java/com/cafeos/tablet/ui/screens/ProductScreen.kt`:

- Detect compact width inside `ProductFormDialog`.
- On compact screens, render Margin Type and Margin Value in a vertical `Column`, each using `fillMaxWidth()`.
- On larger screens, keep a two-column `Row`.
- Put `Modifier.weight(1f)` on the `ExposedDropdownMenuBox` itself, not only its child field; its child should use `fillMaxWidth().menuAnchor()`.
- Prefer a small private `MarginTypeField` composable to avoid duplicating dropdown behavior.

### 2. Verify and push the three fixes

```bash
./gradlew --no-daemon --max-workers=2 :app:compileDebugKotlin
./gradlew --no-daemon --max-workers=2 testDebugUnitTest
./gradlew --no-daemon --max-workers=2 lintDebug
git diff --check
git add app/src/main/java/com/cafeos/tablet/ui/components/AppTopBar.kt \
        app/src/main/java/com/cafeos/tablet/ui/screens/HubScreen.kt \
        app/src/main/java/com/cafeos/tablet/ui/screens/ProductScreen.kt
git commit -m "Fix compact product and navigation layouts"
git push
```

Do not amend commit `9cf9aac`.

### 3. Re-run mobile UI testing

Test at 360×640dp and 320dp:

1. First-run or PIN login.
2. Confirm hub staff name/role contrast.
3. Open Products and confirm the Home icon and label do not overlap.
4. Create/open a category.
5. Open Add Product, scroll to Pricing, enter Capital Cost and Margin Value, switch Margin Type, and save a product.
6. Open New Order, add a product, confirm automatic switch to Order, change quantity, and verify checkout controls remain reachable.
7. Open Item Shop and inspect categories, item grid, details, and purchase bar.
8. Smoke-test Inventory, Reports, Tables, Settings, and remaining hub modules for clipping, crashes, or unreachable controls.

### 4. Update the existing PR

Keep using PR #1. Do not open a second PR.

If code changes after PR creation, refresh its description if the scope materially changes. Attach final emulator evidence or post one concise PR comment only after the fixed commit passes.

## Android emulator setup for a fresh Devin VM

The original session installed:

- Android command-line tools
- `platform-tools`
- `platforms;android-34`
- `build-tools;34.0.0`
- `emulator`
- `system-images;android-35;google_apis;x86_64`

Use Java 17. Set:

```bash
export ANDROID_HOME="$HOME/.android-sdk"
export ANDROID_SDK_ROOT="$HOME/.android-sdk"
```

Create/launch a compact AVD with hardware acceleration when available. The successful run used:

- AVD: `cafeos_compact`
- API 35 Google APIs x86_64
- SwiftShader
- no snapshots
- logical display: `720x1280`
- density: `320`

Useful commands after launch:

```bash
adb shell wm size 720x1280
adb shell wm density 320
adb shell settings put secure stylus_handwriting_enabled 0
adb install -r app/build/outputs/apk/debug/Pebot.apk
```

The original emulator retained a disposable local administrator with PIN `864209`; a fresh emulator will require first-run setup.

## Environment blueprint follow-up

No repository blueprint currently exists. Add a repo-level environment blueprint after confirming the Android SDK installation commands are repeatable. It should:

- install Java 17 and Android command-line tools;
- install platform-tools, Android 34 platform/build tools, emulator, and the API 35 x86_64 Google APIs system image;
- export `ANDROID_HOME` and `ANDROID_SDK_ROOT`;
- record the Gradle test, lint, and build commands as knowledge.

Do not start the emulator in the blueprint because background processes do not survive snapshot creation.

## Guardrails

- Keep business logic unchanged.
- Do not modify tests just to make them pass.
- Do not push directly to `main`.
- Do not amend commits.
- Do not skip Git hooks.
- Do not use destructive Git commands.
- Keep edits limited to the remaining responsive defects.
