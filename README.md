<h1 align="center">One UI Home Clone</h1>

<p align="center">

[![Version](https://img.shields.io/badge/version-0.2.3-4A88FF)](CHANGELOG.md)
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

## Current state (v0.2.3)

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

v0.2.3 adds local data-safety, widget, Finder, layout, packaging, and gate hardening:

- `LauncherDataStore.state` is collected by Compose and is now the only writer for media page, Apps button, labels, notification swipe, locked layout, home layout, drawer sort, motion preset, and folder grid toggles
- The old SharedPreferences file is used only as a one-shot migration source on first DataStore read
- Motion preset changes now feed `ProvideMotionScheme` live without requiring Activity recreation
- Crash-safe recovery mode opens after a previous crash with local reset layout, reset settings, clear widgets, sanitized diagnostics export, and continue-to-Home actions
- Default-launcher onboarding detects when Android is using another Home app and opens the system Home role/settings flow without blocking the launcher
- Long-press app and widget action sheets expose App info, Home add/remove, hide/restore, widget settings/remove, and Android app shortcuts where available
- Launcher icons and widget previews load lazily through bounded caches, and release builds consume a generated Baseline Profile for the core launcher journey
- Widget picker supports local provider/title search, explicit empty search results, health labels, setup-required provider configuration, and add/bind cleanup for ready/template/missing-provider/preview-unavailable widgets
- Widget picker enumerates Android widget providers across launcher-visible profiles, shows Work/Private/Clone-style profile badges when available, and keeps profile/provider diagnostics aggregate-only
- Bound widgets receive active-grid size options on bind, restore, move, and resize so providers redraw against the current phone/landscape/foldable/tablet layout contract
- Apps screen Custom order supports long-press reorder in the paged drawer, persists through launcher layout/backups, reconciles installed-app changes, and keeps Alphabetical order generated separately
- Finder surfaces local Android app shortcuts in a dedicated App shortcuts group when this launcher is the Home role holder
- Finder supports simple typo-tolerant matching plus bounded local usage ranking for apps, settings, actions, and app shortcuts; ranking history can be cleared from Home screen settings and diagnostics stay aggregate-only
- Finder can optionally search local Contacts after the Contacts in Finder setting and Android Contacts permission are both enabled; contact names remain live-only and are excluded from recent searches, backups, and diagnostics
- Count labels use plural resources, and connected Compose smoke tests cover `en-XA` pseudo-locale plus RTL layout direction on Home, Drawer/Finder, Settings, and Widget Picker
- Android toolchain and stable AndroidX dependencies now run on AGP 9.2.1, Gradle 9.4.1, API 37, built-in Kotlin, Compose compiler plugin 2.4.0, and Compose BOM 2026.06.01
- Compact phone widget pages keep page indicators, Finder, the dock, and the Apps button visible above system navigation even when the page contains multiple large widgets
- Home screen settings can export/import `one-ui-home-clone-backup.json` for settings, pages, folders, hidden apps, recent searches, and bound-widget metadata; restore writes an app-private pre-restore snapshot, validates counts/provider availability, rolls back failed storage writes, reports changed/restored/missing counts, and keeps unavailable apps/widgets visible as repairable placeholders
- Home screen settings can export `one-ui-home-clone-diagnostics.txt` with sanitized version, SDK, launcher-role, crash-summary, app-inventory, layout, and widget counts without app names or search history
- Home screen settings persist "Add new apps to Home screen" and privacy-gated notification badge modes; badges stay local, default off, and require Android notification-listener access before dots or counts appear on Home, dock, drawer, and folders
- Responsive layout contracts now select explicit phone portrait, phone landscape, foldable-width, and tablet grids; Home/Apps pagination, widget placement, Settings, folders, Widget Picker, edit tray, and default-launcher prompt are bounded per form factor
- Connected Compose UI smoke tests cover settings toggle persistence, Finder action results, widget picker search and health states, backup file export/import, default-launcher prompt dismissal, and safe-recovery reset actions

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

The prototype builds with AGP 9.2.1, Gradle 9.4.1, JDK 17+, and Android API 37
(`compileSdk`/`targetSdk`). Install the Android API 37 SDK platform before building
on a fresh machine.

Release-channel package with metadata:

```powershell
cd prototype-android
Copy-Item ..\keystore.properties.example .\keystore.properties
# fill in storeFile/storePassword/keyAlias/keyPassword
.\gradlew.bat :app:releaseChannelPackage
```

Output lands in `prototype-android/app/build/outputs/release-channel/` as a
versioned signed APK plus a JSON metadata file containing version, SDK, signing,
size, SHA-256, and upgrade-install information. Upgrade an existing install with
`adb install -r app/build/outputs/release-channel/one-ui-home-clone-v0.2.3-release.apk`.

Device-backed parity/performance smoke:

```powershell
cd prototype-android
.\gradlew.bat deviceGates
```

The report lands in `prototype-android/app/build/reports/device-gates/` with
Home/Apps screenshots, cold-launch timing, drawer frame pacing, RSS, app-launch
tap timing, HOME/back retention, drawer swipe-down close, Finder IME back
behavior, landscape Finder visibility, edge-to-edge system-bar contrast, and
pass/fail status against the roadmap thresholds. Use `deviceGatesEnforced` on
Pixel-class hardware when threshold misses should fail.

Connected Compose UI smoke:

```powershell
cd prototype-android
.\gradlew.bat :app:connectedDebugAndroidTest
```

This installs the debug build on a connected device and runs the focused
Settings/Finder/Widget Picker/backup/default-prompt/recovery/locale smoke suite.

Generate the release Baseline Profile after changing startup, drawer, Finder, or
app-launch behavior:

```powershell
cd prototype-android
.\gradlew.bat :app:generateBaselineProfile
```

The profile generator installs the non-minified release variant on a connected
device or emulator, exercises cold Home start, Apps drawer open, Finder search,
and an app launch path, then writes
`prototype-android/app/src/release/generated/baselineProfiles/baseline-prof.txt`.

## Install as launcher

```bash
adb install -r prototype-android/app/build/outputs/apk/debug/app-debug.apk
adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME
# then select "One UI Home Clone" → "Always"
```

If Android keeps another launcher as the default Home app, One UI Home Clone shows an in-app prompt with an `Open Home settings` action. Dismissing the prompt is non-blocking; the app refreshes Home-role status when returning from Android settings.

## Privacy and data safety

One UI Home Clone is local-only in the current prototype:

- Network: the manifest declares no `INTERNET` permission, so the app cannot upload launcher data through its own process.
- Installed apps: Android `LauncherApps` and package-visibility queries are used locally to render Home, Apps screen, Finder, folders, hide-apps state, labels, icons, profile badges, and launch targets.
- Searches and layout: recent Finder searches, hidden apps, Home pages, folders, settings, backup metadata, and widget IDs are stored in app-private DataStore/files. `android:allowBackup="false"` keeps this data out of Android backup.
- Contacts: local contact matches are off by default and require both the Contacts in Finder setting and Android Contacts permission. Contact names are shown only in live Finder results and are not written to recent searches, backups, or diagnostics.
- Notification badges: badge mode is off by default. If enabled, Android notification access is required and the launcher uses aggregate per-app counts only; notification titles and text are not stored or exported.
- Widgets and wallpaper: `EXPAND_STATUS_BAR`, `SET_WALLPAPER`, `VIBRATE`, `AppWidgetHost`, and wallpaper APIs are used for launcher behavior only.
- Exports: `one-ui-home-clone-backup.json` intentionally contains launcher layout/search/widget data for restore. Restore also writes an app-private pre-restore snapshot for rollback. `one-ui-home-clone-diagnostics.txt` contains only version, SDK, default-launcher state, sanitized crash fields, and aggregate app/layout/widget counts.

Store listing copy: no data is collected, shared, or transmitted by the app; launcher data remains on device unless the user manually exports a backup or diagnostics file.

## Roadmap

See [ROADMAP.md](ROADMAP.md) for the active implementation queue.

## Legal

Samsung, One UI, and related marks are trademarks of Samsung Electronics Co., Ltd. This project ships no Samsung copyrighted assets, logos, wallpapers, or glyph sets. All visual elements are original, inspired-by, or CC-licensed. See [references/clone-brief.md](references/clone-brief.md) for the trademark boundary.

## License

MIT — see [LICENSE](LICENSE).
