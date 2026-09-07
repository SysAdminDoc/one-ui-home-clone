# One UI Home Clone Android project

This directory contains the standalone Kotlin and Jetpack Compose launcher. It builds independently and does not depend on Lawnchair Lite.

## Requirements

- Android Studio with Android API 37 and platform tools
- JDK 17 or newer
- An Android 9 or newer emulator for connected checks
- A private release keystore for signed packaging

## Build

From this directory in PowerShell:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Connected checks

Use an emulator so launcher-role changes do not disturb a daily-use device:

```powershell
$env:ANDROID_SERIAL='emulator-5590'
.\gradlew.bat :app:connectedDebugAndroidTest
```

The connected suite covers Home, Apps, Finder, settings persistence, widgets, backup and restore, recovery actions, RTL layout, and the pseudo-locale fixture.

Local device gates capture launch time, resident memory, drawer frame pacing, app-launch latency, Home and Back behavior, Finder keyboard behavior, landscape layout, and system-bar contrast:

```powershell
$env:ANDROID_SERIAL='emulator-5590'
.\gradlew.bat deviceGates
```

Reports are written to `app/build/reports/device-gates/`. `deviceGatesEnforced` returns a failure when a measured threshold is missed.

## Signed package

Copy `../keystore.properties.example` to `keystore.properties` and enter the local keystore values. Never commit that file or the keystore.

```powershell
.\gradlew.bat clean :app:releaseChannelPackage
```

The output directory is `app/build/outputs/release-channel/`. It contains a versioned signed APK plus JSON metadata with the SDK levels, signing configuration, file size, and SHA-256 digest.

## Marketing assets

Rebuild the project mark, app icon, adaptive icon layers, downloadable icon pack, wordmark, banner, and social preview:

```powershell
.\tools\build-brand-assets.ps1
```

After creating the signed package, capture the release on an isolated emulator:

```powershell
.\tools\capture-marketing.ps1 `
  -DeviceSerial emulator-5590 `
  -ApkPath .\app\build\outputs\release-channel\one-ui-home-clone-v0.2.5-release.apk
```

The capture tool rejects physical-device serials, installs the supplied APK, records each product surface, and restores the emulator's prior Home app and theme mode.

## Architecture

- `MainActivity.kt` owns the launcher activity and Android role flows.
- `ui/OneUiHomeCloneApp.kt` coordinates state, persistence, overlays, widgets, and app inventory.
- `ui/HomeSurface.kt`, `DrawerUi.kt`, `EditModeUi.kt`, `WidgetPickerUi.kt`, and `SettingsUi.kt` render the main product surfaces.
- `data/` holds bounded local stores, backup and restore, and diagnostics export.
- `notifications/` provides local aggregate badge counts after Android grants access.
- `baselineprofile/` exercises cold Home start, Apps, Finder, and an app launch for release optimization.

The manifest does not request internet access. Contacts and notification access are optional, and both features default to off.
