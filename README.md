<h1 align="center">One UI Home Clone</h1>

<p align="center">

[![Version](https://img.shields.io/badge/version-0.2.2-4A88FF)](CHANGELOG.md)
[![License](https://img.shields.io/badge/license-MIT-4A88FF)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android%209.0%2B-4A88FF)](prototype-android/app/build.gradle.kts)
[![Stack](https://img.shields.io/badge/stack-Kotlin%20%2B%20Compose-4A88FF)](prototype-android/)

</p>

A standalone Kotlin/Compose Android launcher pursuing **Samsung One UI 7** parity — layout, motion, terminology, settings structure, and default behavior. Clone, not a port: no Samsung proprietary assets or brand names shipped.

## What makes this different

Unlike Lawnchair / Niagara / OpenLauncher, the project's north star is **Samsung parity, not launcher power-user features**. Copy behavior before adding customization. A Samsung user should pick it up without relearning navigation.

## What lives here

- [docs/product-vision.md](docs/product-vision.md) — target product definition + success criteria
- [docs/architecture.md](docs/architecture.md) — project boundaries + migration plan
- [docs/parity-checklist.md](docs/parity-checklist.md) — behavior-by-behavior Samsung parity checklist
- [docs/ui-spec.md](docs/ui-spec.md) — visual + interaction spec
- [backlog/epics.md](backlog/epics.md) — major workstreams
- [backlog/phase-01.md](backlog/phase-01.md) — first implementation milestone
- [references/clone-brief.md](references/clone-brief.md) — scope + non-goals
- [ROADMAP.md](ROADMAP.md) — planned features + competitive research (3 rounds) + implementation deep dive
- [prototype-android/](prototype-android/) — standalone Android Compose prototype (app package)

## Current state (v0.2.2)

Compose-first prototype covering:

- One UI style home surface with widget hero card, wallpaper atmosphere, page indicator
- Samsung-style app drawer shell with Finder search (grouped results, recent searches, settings hits)
- Unified edit-mode tray (Wallpaper, Themes, Widgets, Home screen settings)
- Widget picker overlay + span-aware Home widget grid for bound widgets
- Folder bubble + folder open overlay
- Hide-apps overlay (Samsung "clean view" equivalent)
- Page manager panel with reorderable preview tiles
- Notification shade overlay
- Full settings surface with Samsung section ordering + terminology

v0.2.1 adds a premium UI polish pass:

- Shared control/surface geometry with consistent 10-12dp radii instead of mixed oversized rounded containers
- Integrated in-app feedback for widget/app actions instead of abrupt Compose-owned toasts
- Cleaner user-facing copy across Finder, widgets, folders, notifications, edit mode, and settings
- Better empty-state handling in the widget picker
- More accessible semantics for page dots, app/folder targets, Finder rows, edit tiles, and settings toggles

v0.2.2 moves app inventory onto Android's launcher contract:

- Home, dock, Apps screen, Finder, and hide-apps state are now fed by a `LauncherApps` repository
- Package/profile changes refresh the inventory through `LauncherApps.Callback`
- Launch targets carry their profile-aware component and user handle, with the existing sample apps retained only as the empty/error fallback

Unreleased work moves launcher settings onto a single DataStore path:

- `LauncherDataStore.state` is collected by Compose and is now the only writer for media page, Apps button, labels, notification swipe, locked layout, home layout, drawer sort, motion preset, and folder grid toggles
- The old SharedPreferences file is used only as a one-shot migration source on first DataStore read
- Motion preset changes now feed `ProvideMotionScheme` live without requiring Activity recreation
- Crash-safe recovery mode opens after a previous crash with local reset layout, reset settings, clear widgets, sanitized diagnostics export, and continue-to-Home actions
- Default-launcher onboarding detects when Android is using another Home app and opens the system Home role/settings flow without blocking the launcher
- Long-press app and widget action sheets expose App info, Home add/remove, hide/restore, widget settings/remove, and Android dynamic shortcuts where available

v0.2.0 landed the widgets + persistence + motion primitives:

- `LauncherDataStore` — DataStore Preferences store with one-shot migration from the v0.1.0 SharedPreferences file
- `WidgetPersistence` — versioned JSON widget-ID store on its own DataStore file, bounds-checked decode (128 KB / 1024 entries) to contain corrupt-file blast radius
- `MotionScheme` + `ProvideMotionScheme` — Standard / Reduced presets exposed as raw `SpringParams`, threaded through a `LocalMotionScheme` CompositionLocal. Platform `ANIMATOR_DURATION_SCALE == 0` forces Reduced
- `WidgetBindContract` — stateless `ActivityResultContract` for `ACTION_APPWIDGET_BIND` that round-trips the allocated widget ID through an Intent extra so process death during the bind dialog still deallocates correctly on cancel
- `WidgetPreviewLoader` — `previewLayout` (API 31+) → `previewImage` → provider icon fallback
- Dep bumps: Compose BOM 2024.01 → 2024.10.01 (Compose 1.7 / Material3 1.3), Kotlin 1.9.22 → 1.9.24, core-ktx / activity-compose / lifecycle / material advanced to current stable; `datastore-preferences` 1.1.1 added

Widget bind / preview / persistence now feed live Home widget cells with move, resize, remove, and unavailable-provider recovery controls. Drop-to-edge page creation remains v0.2.x follow-up work.

## Build the prototype

From repo root:

```bash
# Git Bash / Linux / macOS
cd prototype-android
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew assembleDebug
```

```powershell
# PowerShell
cd prototype-android
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleDebug
```

APK output: `prototype-android/app/build/outputs/apk/debug/app-debug.apk`

The prototype builds with AGP 8.6, Gradle 8.7, JDK 17, and Android API 35
(`compileSdk`/`targetSdk`). Install the Android 15 SDK platform before building
on a fresh machine.

Device-backed parity/performance smoke:

```powershell
cd prototype-android
.\gradlew.bat deviceGates
```

The report lands in `prototype-android/app/build/reports/device-gates/` with
Home/Apps screenshots, cold-launch timing, drawer frame pacing, RSS, app-launch
tap timing, and pass/fail status against the roadmap thresholds. Use
`deviceGatesEnforced` on Pixel-class hardware when threshold misses should fail.

## Install as launcher

```bash
adb install -r prototype-android/app/build/outputs/apk/debug/app-debug.apk
adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME
# then select "One UI Home Clone" → "Always"
```

If Android keeps another launcher as the default Home app, One UI Home Clone shows an in-app prompt with an `Open Home settings` action. Dismissing the prompt is non-blocking; the app refreshes Home-role status when returning from Android settings.

## Roadmap

See [ROADMAP.md](ROADMAP.md). Near-term themes:

1. Real `AppWidgetHost` integration (v0.2.x)
2. Drop-to-edge page creation (v0.2.x)
3. Motion parity — 240fps reference captures → Compose `spring()` parameters (v0.3.x)
4. Landscape + foldable posture support (v0.4.x)
5. Optional `tribalfs/oneui-design` AndroidView interop for SwitchBar / ToolbarLayout fidelity (v0.5.x)

## Legal

Samsung, One UI, and related marks are trademarks of Samsung Electronics Co., Ltd. This project ships no Samsung copyrighted assets, logos, wallpapers, or glyph sets. All visual elements are original, inspired-by, or CC-licensed. See [references/clone-brief.md](references/clone-brief.md) for the trademark boundary.

## License

MIT — see [LICENSE](LICENSE).
