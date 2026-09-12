# Quickstart (Phase 1)

## Build the APK
```bash
export JAVA_HOME=$HOME/jdk17/zulu17.54.21-ca-jdk17.0.13-linux_x64
export PATH=$JAVA_HOME/bin:$PATH
./gradlew assembleDebug        # output: app/build/outputs/apk/debug/Pebot.apk
```
The project root must contain `local.properties` with `sdk.dir=<path to Android SDK>`
(Android SDK lives at `$HOME/Android/Sdk` on this machine).

## Run unit tests
```bash
./gradlew testDebugUnitTest
```

## Run the unit + re-skin parity tests
```bash
./gradlew testDebugUnitTest --tests "*ReceiptParityTest" --tests "*TapParityTest"
```

## Preview the theme
Open `app/src/main/java/com/cafeos/tablet/ui/theme/Theme.kt` in Android Studio; `PebotTheme` previews both a classic and a gamified swatch via `@Preview` annotations.

## Theme-mode toggle
Default at first rollout is **Fast Mode** (set `ui_mode=FAST` in the SettingsStore). Switch to **Gamified** to demo the MOBA skin; switch to **Classic** to revert the look entirely without rebuild (Settings screen).
