# One UI Home Clone Prototype

Standalone Android Compose prototype for the Samsung One UI 7 parity launcher.

## Purpose

- validate the visual language
- prototype home, drawer, Finder, folders, edit mode flows
- iterate without touching Lawnchair Lite app code

## Build

From the `prototype-android/` directory:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\openjdk\jdk-21.0.8'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleDebug
```

```bash
JAVA_HOME="/c/Program Files/Android/openjdk/jdk-21.0.8" ./gradlew assembleDebug
```

## Install + set as launcher

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME
# pick "One UI Home Clone" → "Always"
```

## Architecture

Single-file Compose prototype — see `app/src/main/java/com/oneuihomeclone/ui/OneUiHomeCloneApp.kt` (~3700 lines).

- **MainActivity** — `singleTask` launcher activity with `onNewIntent` observer so HOME re-entry resets overlay state
- **LauncherPreferences** (`data/`) — SharedPreferences-backed persistence for 8 user-facing toggles
- **OneUiHomeCloneTheme** (`ui/theme/`) — full Material 3 day/night color scheme + One UI type scale
- **OneUiHomeCloneApp** (`ui/`) — home surface, Finder drawer, folders, widget picker, page manager, notification shade, settings overlay, edit-mode tray

Split into per-surface files staged for v0.2.0 once behavior stabilizes.

## Notes

- Not included in the Lawnchair Lite root Gradle build — intentional
- Has its own Gradle settings + app module
- Shipping target: standalone app, decide at v1.0 whether to merge selected work back into Lawnchair Lite
