# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## v0.2.4 (2026-08-29)

### Added

- Functional "Add new apps to Home screen" setting backed by DataStore, Finder settings results, backup/export state, and package-change placement that adds newly installed launchable apps to the first available Home page slot or creates a new Home page only when every existing page is full.
- Privacy-gated notification badge modes backed by DataStore, backup/export state, a `NotificationListenerService` aggregate count source, Home/dock/drawer/folder badge projection, settings permission guidance, and sanitized diagnostics fields that exclude notification text.
- Setup-required widgets now allocate and bind through Android's widget host flow, launch provider configuration before committing, deallocate on cancel/failure, and persist pending host IDs so process loss does not leave orphaned allocations.
- Bound widgets now compute size options from the active launcher layout contract and update Android widget manager/host-view sizing after bind, restore, move, and resize.
- Widget provider catalogs now enumerate launcher-visible profiles, badge provider cards for non-personal profiles, catch per-profile provider query failures, and export only aggregate profile/provider counts in diagnostics.
- Connected Compose UI smoke coverage now exercises settings toggle persistence, Finder action results, widget picker search and health states, backup file export/import, default-launcher prompt dismissal, and safe-recovery reset actions through `:app:connectedDebugAndroidTest`.
- Apps screen Custom order now supports long-press reorder in the paged drawer, persists drawer app IDs in launcher layout/backups, reconciles installed-app changes, and leaves Alphabetical order generated separately.
- Finder now supports simple typo-tolerant matching plus bounded local usage ranking for apps, settings, actions, and app shortcuts; Home screen settings can clear the ranking history, and diagnostics expose only aggregate Finder index/history counts.
- Finder can optionally search local Contacts after an explicit Home screen settings toggle plus Android `READ_CONTACTS` grant; contact names are live-result only and are excluded from recent searches, backups, and diagnostics.
- Plural resources now back visible launcher count labels, with `en-XA` pseudo-locale and Arabic RTL regression fixtures covered by connected Compose smoke tests for Home, Drawer/Finder, Settings, and Widget Picker.
- Widget persistence round-trip coverage now runs as an Android instrumentation test, keeping JVM unit coverage focused on pure codecs while avoiding Windows/Robolectric DataStore rename false failures.

### Changed

- New app icon: adaptive, themed (monochrome) and legacy variants regenerated from the 2026-08 icon set.
- Android toolchain now targets API 37 with AGP 9.2.1, Gradle 9.4.1, built-in Kotlin, Compose compiler plugin 2.4.0, Compose BOM 2026.06.01, and current stable AndroidX core/activity/lifecycle/datastore/test/UIAutomator dependencies.
- Stable AndroidX Baseline Profile Gradle Plugin remains on 1.4.1 with the AGP 9 `android.newDsl=false` compatibility flag until the plugin's AGP 9 support is stable.
- Compact phone widget pages reduce widget preview height and suppress the separate app grid when needed so page indicators, Finder, the dock, and the Apps button remain visible above system navigation.

### Fixed

- Backup restore now writes an app-private pre-restore snapshot, rejects invalid restore counts/widget records before applying, rolls settings/layout/widgets back on storage failure, distinguishes corrupt backups from missing backups, and reports changed/restored/missing counts after successful import.

## v0.2.3 (2026-07-02)

### Added

- Release Baseline Profile generation for cold Home start, Apps drawer open, Finder search, and app launch coverage.
- Local Home settings backup/restore actions that write and read `one-ui-home-clone-backup.json` with launcher settings, Home pages, folders, hidden apps, recent searches, and bound-widget metadata.
- Sanitized Home settings diagnostics export that writes `one-ui-home-clone-diagnostics.txt` with app version, SDK/target SDK, build type, default-launcher state, previous-crash summary, app/layout/widget counts, and privacy markers without raw app names or search history.
- Privacy and permission disclosure in Home settings plus README data-safety wording for installed-app inventory, local search/layout/widget storage, backup/diagnostics exports, no internet access, and `allowBackup=false`.
- Widget picker search by provider/title plus provider health states for ready, built-in template, setup-required, missing-provider, and preview-unavailable widgets.
- Finder now surfaces Android manifest, dynamic, and pinned app shortcuts in a dedicated App shortcuts group when the launcher has shortcut host access.
- Responsive launcher layout contracts for phone portrait, phone landscape, foldable-width, and tablet states, including explicit Home/Apps grid values, drawer pagination, widget placement bounds, and capped settings/folder/widget/edit/default-launcher surfaces.
- `:app:releaseChannelPackage` task that requires the local release keystore, copies a versioned signed APK, and writes JSON release metadata with SDK/version/signing/size/SHA-256/upgrade-install fields.
- SESL interop assessment for Home screen settings toggle rows, recording binary/dependency/lifecycle cost and keeping the current Compose `SettingsToggleCard` path until fidelity gain outweighs GitHub Packages and `AndroidView` maintenance risk.
- One UI-style long-press action sheets for Home/dock/drawer/folder/hide-app entries and widgets, including App info, Add/Remove Home, hide/restore, widget settings/remove, and dynamic shortcut rows when Android exposes them.
- Default-launcher onboarding prompt and settings row that detect when Android is using another Home app and open the system Home role/settings flow.
- Crash-safe recovery mode opens after a consumed crash log with local reset layout, reset settings, clear widgets, export diagnostics, and continue-to-Home actions.
- Launcher app inventory now surfaces work/profile badged labels and icons, installing progress, and unavailable/suspended app states across Home, drawer, folders, and previews.
- `deviceGates` and `deviceGatesEnforced` local Gradle tasks for attached-device launcher smoke reports with Home/Apps screenshots, cold-launch timing, RSS, drawer frame pacing, app-launch timing, HOME/back retention, drawer swipe-down close, Finder IME behavior, landscape Finder visibility, edge-to-edge system-bar contrast, and Perfetto trace capture.
- `LauncherLayoutStore` persists Home pages, folders, default page, hidden apps, recent searches, and next ID counters in a bounded JSON DataStore.
- Widget recovery action in Home screen settings that clears AppWidgetHost state, removes bound widgets from visible pages, and clears persisted widget IDs.
- Bound widgets now render in a span-aware Home widget grid with persisted cells, move controls, resize controls, remove handling, and unavailable-provider recovery copy.

### Changed

- Launcher app icons and widget previews now load lazily with bounded in-memory caches instead of decoding every app icon and widget preview during inventory reads.
- Home, drawer/Finder, settings, edit mode, folders, notifications, widgets, and launcher feedback now render visible static copy from Android string resources, with localized enum labels and resource-backed Finder/settings rows.
- Android toolchain now targets API 35 with AGP 8.6.0, Gradle 8.7, `compileSdk` 35, and `targetSdk` 35; the obsolete target-SDK lint suppression was removed.
- Launcher settings now use `LauncherDataStore.state` as the single source of truth; the legacy SharedPreferences writer was removed and UI/DataStore enum mapping is unit-tested.

### Fixed

- Default-launcher role and settings checks now run off the main thread so slow RoleManager/PackageManager binder calls cannot block prompt input.
- MainActivity now gates normal launcher composition behind the crash-log check so recovery actions remain reachable before corrupt state can remount Home.
- Unavailable, suspended, and still-installing launcher apps are visually disabled and produce explicit in-app feedback instead of silently attempting a launch.
- Accessibility semantics now describe page indicators, Finder action/setting rows, settings title/value rows, folder/open targets, hide-app state rows, and widget move/resize/remove controls without relying on visual context.
- Launcher layout restore now reconciles persisted app IDs with the current `LauncherApps` inventory so removed apps are pruned and surviving apps/folders use fresh launch targets.
- Backup and layout restore now preserve missing apps and missing widget providers as disabled repair placeholders instead of silently dropping the restored Home records.
- Previous-crash recovery now consumes the crash file on `Dispatchers.IO`, logs only sanitized summary fields, deletes the full local file after consumption, and surfaces the recovery through the in-app feedback banner.
- Widget bind requests now resolve and pin a system bind handler before launch, release allocated widget IDs on declined/canceled/superseded/unavailable flows, and reject setup-required providers until configuration handling is implemented.

## v0.2.2 (2026-06-28)

### Added

- `LauncherAppInventory` repository backed by Android `LauncherApps`, including profile-aware app IDs and package-change callbacks.
- Unit coverage for launcher app identity, host filtering, duplicate handling, and display-label sorting.

### Changed

- Home, dock, Apps screen, Finder, and hide-apps surfaces now consume `LauncherApps` inventory instead of the simplified `PackageManager.queryIntentActivities` path.
- App launches now prefer `LauncherApps.startMainActivity` with the original `UserHandle`, while retaining the existing intent fallback and sample-app empty/error fallback.

## v0.2.1 (2026-06-16)

### Added

- Motion preset settings toggle (Standard / Reduced) in home screen settings, with live `ProvideMotionScheme` recomposition without Activity restart, persisted to SharedPreferences
- Folder grid settings selector (3x4 / 4x4 / 5x5) in home screen settings, with dynamic folder grid columns/rows, persisted to SharedPreferences, wired into FolderOverlay
- `SettingsSelectorCard` generic composable for multi-option settings (reused for motion and folder grid)
- `FolderGridKey` persistence enum in `LauncherPreferences` / `LauncherDataStore` with SP-to-DS migration support
- Shared in-app feedback banner for widget/app action success and failure states
- Widget picker empty state when a selected category has no widget providers

### Changed

- `ProvideMotionScheme` moved from `MainActivity` wrapping into `OneUiHomeCloneApp` content, so it responds to live state changes instead of seeding from snapshot once
- `ProvideMotionScheme` now accepts an optional `presetOverride` parameter for explicit preset control
- Premium-polish pass across home, Finder, edit mode, folders, widgets, notifications, and settings: consistent 10-12dp control geometry, calmer surface elevation, clearer microcopy, and reduced decorative background noise
- Build docs now use the Android Studio JBR path used on this workstation

### Fixed

- Widget span calculation used non-existent `minSpanX`/`minSpanY` API; replaced with `targetCellWidth`/`targetCellHeight` (API 31+) with `minWidth`/`minHeight` fallback
- Accessibility semantics for page dots, Finder rows, app/folder launch targets, edit tiles, and settings toggles

## v0.2.0: 2026-04-24

### Added

- DataStore Preferences mirror (`LauncherDataStore`) with one-shot `SharedPreferencesMigration` from the v0.1.0 `one_ui_home_clone_prefs` file. Forward-compat plumbing for v0.2.x: `LauncherPreferences` (SharedPreferences) remains the single writer until the monolith split lands
- `WidgetPersistence`: DataStore-backed, JSON-encoded widget ID store with `schemaVersion=1` dispatch on decode. Scaffolding for real `AppWidgetHost` binding in a follow-up iter; not yet wired to the picker UI
- `MotionScheme` + `LocalMotionScheme` CompositionLocal: Standard / Reduced motion presets exposed as raw `SpringParams` so callers build typed `spring<T>()` at the call site. `ProvideMotionScheme` top-level provider OR's the persisted user preset with the system-level `ANIMATOR_DURATION_SCALE == 0` signal (system "Remove animations" wins)
- `motionPreset` persisted toggle (`standard` / `reduced`) added to both `LauncherPreferences` and `LauncherDataStore`; included in the SP→DS migration key set
- `WidgetBindContract`: stateless `ActivityResultContract<WidgetBindRequest, WidgetBindResult>` for `ACTION_APPWIDGET_BIND`. Round-trips the allocated widget id through an Intent extra so process death during the bind dialog still deallocates correctly on cancel. Falls back to `EXTRA_APPWIDGET_ID` on OEM forks that strip non-framework extras
- `WidgetPreviewLoader`: API-31+ `previewLayout` → `previewImage` → provider icon fallback hierarchy. `PreviewSource` sealed type (`RemoteLayout` / `PreviewImage` / `ProviderIcon` / `Empty`) so callers don't have to re-derive the precedence
- `LauncherApp.requestWidgetBind(request, callback)` helper + lifecycle-aware `ActivityResultLauncher` registration in `MainActivity`: bind requests from anywhere in the tree dispatch through the Activity-scoped launcher

### Changed

- Dependency bumps for v0.2.0: `core-ktx` 1.12.0 → 1.13.1, `activity-compose` 1.8.2 → 1.9.3, `lifecycle-runtime-ktx` 2.7.0 → 2.8.7 (added `lifecycle-runtime-compose`), `material` 1.11.0 → 1.12.0, Compose BOM 2024.01.00 → 2024.10.01 (Compose 1.7.x / Material3 1.3.0), `kotlinCompilerExtensionVersion` 1.5.8 → 1.5.14, Kotlin plugin 1.9.22 → 1.9.24. `datastore-preferences` 1.1.1 added as a new dep. Closes the `GradleDependency` lint disable (removed from the suppress list)
- `LauncherState` gained `motionPreset: MotionPresetKey` (default `STANDARD`): callers constructing `LauncherState` directly keep compiling
- Enum `fromRaw` accessors in `HomeLayoutKey` / `DrawerSortKey` switched from `values()` to `entries` for parity with the new `MotionPresetKey`

### Fixed

- Widget bind cancel path no longer leaks allocated widget ids on process death: the allocated id is now encoded in the outbound Intent so `parseResult` can recover it from the result Intent even when the contract instance is recreated
- `MainActivity.onDestroy` now flushes any pending widget-bind callback so a dying Activity's closure can't pin Compose state on a rotation during the bind dialog
- `WidgetPersistence.decode` now honours the stored `schema_version`: reserves the version-dispatch branch for when a future shape change ships
- `WidgetPersistence.clear()` wipes the schema stamp alongside the JSON so an empty store doesn't carry a stale version marker

### Notes

- This is scaffolding + primitives for v0.2.0 "widgets + persistence + motion". Behavioural wire-up (drop-to-edge page creation in the grid drag detector, widget resize handles, picker UI using the new preview loader, settings toggle for motion preset) is staged for the next iteration
- Motion preset live-switch without Activity recreate is deferred: `ProvideMotionScheme` seeds from `LauncherPreferences.snapshot()` today; a Flow-based observation ships when the motion toggle lands in settings
- Physical-device interactive validation for this iteration was not possible (no device attached). Build, lint, and static audit are the ship gates; the new surfaces will be exercised on device during the v0.2.0 release smoke test

## v0.1.0: 2026-04-24

### Added
- Standalone Compose prototype shell carved out of Lawnchair Lite: home surface, app drawer with Finder search, edit-mode tray, settings overlay, folders, widget picker, page manager, notification shade
- Full Samsung One UI 7 parity research baked into ROADMAP (Rounds 1 to 3): 900+ icon catalog reference, tribalfs sesl7 interop path, Launcher3 forking strategy, motion parity reference captures
- Gradle wrapper (8.4) + standalone Android build, not wired into Lawnchair Lite Gradle
- Adaptive launcher icon, light-first One UI palette, Samsung-style blue accent
- GitHub Actions release workflow (`release.yml`): signed release APK + debug APK + AAB attached per tag
- Keystore signing config gated on `keystore.properties`; debug builds work without it
- MIT license, shields.io README badges, repo `CLAUDE.md` working notes
- `LauncherPreferences` (SharedPreferences-backed) persisting 8 user-facing toggles: media page, apps button, app/widget labels, notification swipe, lock layout, home-layout mode, drawer sort
- Launcher `HOME` + `DEFAULT` intent filter + `singleTask` + `onNewIntent` observer so HOME re-entry collapses overlays and scrolls to default page
- `BackHandler` absorbing back-press on home: overlays collapse first, then search clears, then page resets to default, then press is absorbed
- Real `WallpaperManager.peekDrawable()` integration inside `WallpaperAtmosphere`: off-main-thread decode via `produceState(Dispatchers.IO)`, immutable bitmap copy, gradient fallback when access denied
- Complete `darkColorScheme`: every surface / text / outline / container slot overridden, no more half-dark UI surfaces in night mode
- Full Material 3 typography scale (13 slots) tuned to One UI metrics: tighter letter-spacing on display tier, medium-weight bias, sentence-case body
- `windowShowWallpaper=true` + transparent window background: Compose surface floats over the user's wallpaper, matching real launcher behaviour
- Lint config: `warningsAsErrors=false`, disables `OldTargetApi`/`GradleDependency`/`ObsoleteSdkInt`/`ObsoleteLintCustomCheck` (scheduled for Iteration 2)

### Fixed
- Dropped hardcoded `windowLightStatusBar`/`windowLightNavigationBar` theme overrides: `enableEdgeToEdge()` now auto-flips system-bar icon colour with system dark mode
- First SharedPreferences write on composition entry skipped via `snapshotFlow.drop(1)`: no wasted I/O on cold start
- System wallpaper bitmap copied with `Bitmap.copy(ARGB_8888, false)` so the cached `ImageBitmap` survives wallpaper change / live wallpaper frame recycling

### Added (Iteration 2 carry-over: still v0.1.0 scope)
- `LauncherApp : Application`: global uncaught-exception handler writes a minimal crash log to `filesDir/crash-log.txt`, next cold start surfaces it as a toast and clears the file atomically
- `AppWidgetHost` lifecycle stub: host id 2048 allocated at `Application.onCreate`, `startListening()` / `stopListening()` matched to `MainActivity.onStart()` / `onStop()` so v0.2.x widget binding has a solid hook point
- `Haptics` helper: `HapticFeedbackConstants.CONFIRM` / `DRAG_START` on API 30+, `LONG_PRESS` fallback on API 28-29, `FLAG_IGNORE_VIEW_SETTING` so haptics fire even when the host view disables them
- `android:allowBackup="false"`: keeps the user's widget-binding IDs (persisted in v0.2.x) + SharedPreferences out of ADB backups by default

### Fixed (Iteration 2 counter-audit)
- `LauncherApp.onCreate`: `widgetHost` now published before `instance` so no reader sees a non-null `LauncherApp` without a companion-visible `AppWidgetHost`
- `installCrashHandler` falls back to `Process.killProcess` + `exitProcess(10)` when no prior `UncaughtExceptionHandler` is installed: prevents zombie process with a frozen home surface
- `Haptics.dragPickup` pre-API-30 fallback uses `LONG_PRESS` instead of `CONTEXT_CLICK` (which mapped to a right-click feel on OEM skins, wrong semantic for a grab)

### Notes
- Samsung trademarks, logos, wallpapers, and branded glyph sets remain off-limits by design: this is a clone, not a port
- Landscape grid and foldable posture support staged for v0.2.x
- Magisk-module install path and oneui-design `AndroidView` interop deferred to v0.3.x

## Roadmap archive: 2026-08-10: ROADMAP.md

<details>
<summary>Original roadmap snapshot</summary>

```markdown
# ROADMAP

Separate Gradle-independent workspace for building a Samsung One UI Home parity clone. Standalone Compose prototype + parity research docs; not wired into the Lawnchair Lite build.

Blocked items live in [Roadmap_Blocked.md](Roadmap_Blocked.md).

## Cumulative metrics target (v1.0)

- Cold launch time: <= 800 ms (home visible) on Pixel-class devices
- Frame pacing: <= 5% dropped frames during drawer open animation (Perfetto capture)
- App launch time: <= 250 ms from tap to target `onCreate`
- Memory: resident set <= 140 MB with 40 installed apps + 6 widgets bound

## Open-Source Research

### Related OSS Projects
- https://github.com/OneUIProject/oneui-core: "sesl", heavily modified fork of Jetpack + Material Components that replaces stock libraries with OneUI-styled versions
- https://github.com/OneUIProject/oneui-design: drop-in OneUI components (AppBarLayout, ToolbarLayout, SwitchBar, etc.)
- https://github.com/tribalfs/oneui-design: actively maintained fork with sesl6/7/8 support
- https://github.com/Launcher3-dev/Launcher3: canonical Launcher3 implementation
- https://github.com/LawnchairLauncher/lawnchair: OSS rebase-onto-Launcher3 pattern

### Implementation Deep Dive

#### Reference Implementations
- **tribalfs/oneui-design**: sesl6/7/8 support. Prefer over upstream OneUIProject which lags.
- **tribalfs/sesl-androidx**: modified AndroidX modules. Required dependency-exclusion block.
- **Launcher3 Workspace.java**: canonical page management, cell layout, drop animation.
- **Launcher3 Hotseat.java**: dock emulation reference.

#### Known Pitfalls
- oneui-core requires excluding `androidx.appcompat` and `androidx.core` globally.
- Samsung trademarks/copyrighted assets are off-limits: "inspired-by" or CC-licensed only.
- OneUI landscape grid != portrait grid: 5x3 vs 4x5. Track both.
- `BIND_APPWIDGET` permission only grantable to system apps: use `ACTION_APPWIDGET_BIND` Activity intent.
- Compose `AndroidView` wrapping SeslSwitchBar leaks Lifecycle if bound to Activity lifecycle.

## Research-Driven Additions
```

</details>
