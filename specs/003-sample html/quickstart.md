# Quickstart: Hub-and-Spoke Navigation

## Prerequisites

```bash
cd /home/edgepoint/Edgepoint_05/Coffee/cafeos-tablet
JAVA_HOME=/home/edgepoint/jdk17/zulu17.54.21-ca-jdk17.0.13-linux_x64 ./gradlew tasks | grep -i assembleDebug
```

- JDK 17 at `/home/edgepoint/jdk17/zulu17.54.21-ca-jdk17.0.13-linux_x64`
- Android SDK at `/home/edgepoint/Android/Sdk` (from `local.properties`)
- Gradle 8.7 via `./gradlew`

## Step 1: Verify current build compiles

```bash
cd /home/edgepoint/Edgepoint_05/Coffee/cafeos-tablet
JAVA_HOME=/home/edgepoint/jdk17/zulu17.54.21-ca-jdk17.0.13-linux_x64 ./gradlew compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`

## Step 2: After implementing Phase 1 (colors + AppTopBar)

```bash
# Verify color tokens compile
./gradlew compileDebugKotlin
```

## Step 3: After implementing Phase 2 (HubScreen)

```bash
# Verify HubScreen renders
./gradlew compileDebugKotlin
```

## Step 4: After implementing Phase 3 (top bars)

```bash
# Verify all screens still compile with AppTopBar
./gradlew compileDebugKotlin
```

## Step 5: After implementing Phase 4 (MainActivity refactor)

```bash
# Full build
./gradlew assembleDebug
# Lint
./gradlew lintDebug
# Tests
./gradlew testDebugUnitTest
```

## Step 6: Run on device/emulator

```bash
# Install APK
adb install -r app/build/outputs/apk/debug/Pebot.apk
# Or use IDE run button
```

## Verification Checklist

### Navigation behavior
- [ ] Tile tap opens the correct app screen (single direction)
- [ ] Home button in top-left returns to the tile grid from any app
- [ ] Android back button returns to home (not app exit)
- [ ] Tab switching doesn't reload data (HubScreen stays mounted)
- [ ] Access-controlled tiles are hidden for unauthorized staff roles

### Visual fidelity (matches HTML mockup)
- [ ] Home screen background is espresso (#1B140F)
- [ ] Tile icons are 56×56px with rounded 16px corners
- [ ] Each tile has the correct color per the mockup (sienna, gold, sage, etc.)
- [ ] Tap feedback: `transform: scale(0.92)` equivalent (Compose `indication` or `graphicsLayer`)
- [ ] App screen top bar: espresso Home button + app title (17px Fraunces serif)
- [ ] Transition: ~220ms slide/fade
- [ ] 4-column grid, 14px gap, 22px top padding
- [ ] Greeting: "GOOD MORNING" (gold), staff name, sub-line (front counter + date)

### Gamification integration
- [ ] `GameFeedback.tap()` fires on tile tap
- [ ] Live Orders tile shows badge for pending orders
- [ ] `RarityPulse` or motion primitive used for badge animation (optional)

### Backward compatibility
- [ ] All existing routes still work (live_orders, pos, kitchen, inventory, tables, staff, analytics, settings)
- [ ] `onNavigateToLoyalty` and `onNavigateToItemShop` still functional
- [ ] Login screen unchanged
