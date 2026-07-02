# One UI Home Clone Prototype

Standalone Android Compose prototype for the Samsung One UI 7 parity launcher.

## Purpose

- Validate the visual language
- Prototype home, drawer, Finder, folders, edit mode, widgets, and settings flows
- Iterate without touching Lawnchair Lite app code

## Build

From the `prototype-android/` directory:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleDebug
```

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew assembleDebug
```

## Install + set as launcher

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME
# pick "One UI Home Clone" -> "Always"
```

## Device gates

With one emulator or device attached:

```powershell
.\gradlew.bat deviceGates
```

The task builds and installs the debug APK, cold-launches the launcher, captures
Home and Apps screen screenshots, records RSS, drawer frame pacing, an app-launch
tap probe, and a Perfetto trace when the device allows it. Reports are written to
`app/build/reports/device-gates/`. Use `.\gradlew.bat deviceGatesEnforced` on
Pixel-class hardware when the ROADMAP thresholds should fail the command.

## Architecture

Compose-first launcher prototype split into surface-focused UI files under
`app/src/main/java/com/oneuihomeclone/ui/`.

- **MainActivity** - `singleTask` launcher activity with `onNewIntent` observer so HOME re-entry resets overlay state
- **LauncherPreferences** (`data/`) - SharedPreferences-backed persistence for user-facing toggles
- **OneUiHomeCloneTheme** (`ui/theme/`) - full Material 3 day/night color scheme + One UI type scale
- **OneUiHomeCloneApp** (`ui/`) - state orchestration, persistence, overlay routing, and widget/app loading
- **HomeSurface / DrawerUi / FolderUi / WidgetPickerUi / SettingsUi / EditModeUi / NotificationUi** (`ui/`) - focused Compose surfaces
- **SharedComponents** (`ui/`) - shared controls, app icons, settings rows, and in-app feedback

## Notes

- Not included in the Lawnchair Lite root Gradle build - intentional
- Has its own Gradle settings + app module
- Shipping target: standalone app; decide at v1.0 whether to merge selected work back into Lawnchair Lite
