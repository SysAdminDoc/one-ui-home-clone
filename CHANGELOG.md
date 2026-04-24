# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

### Added

- DataStore Preferences mirror (`LauncherDataStore`) with one-shot `SharedPreferencesMigration` from the v0.1.0 `one_ui_home_clone_prefs` file. Forward-compat plumbing for v0.2.x — `LauncherPreferences` (SharedPreferences) remains the single writer until the monolith split lands
- `WidgetPersistence` — DataStore-backed, JSON-encoded widget ID store with `schemaVersion=1` dispatch on decode. Scaffolding for real `AppWidgetHost` binding in a follow-up iter; not yet wired to the picker UI
- `MotionScheme` + `LocalMotionScheme` CompositionLocal — Standard / Reduced motion presets exposed as raw `SpringParams` so callers build typed `spring<T>()` at the call site. `ProvideMotionScheme` top-level provider OR's the persisted user preset with the system-level `ANIMATOR_DURATION_SCALE == 0` signal (system "Remove animations" wins)
- `motionPreset` persisted toggle (`standard` / `reduced`) added to both `LauncherPreferences` and `LauncherDataStore`; included in the SP→DS migration key set
- `WidgetBindContract` — stateless `ActivityResultContract<WidgetBindRequest, WidgetBindResult>` for `ACTION_APPWIDGET_BIND`. Round-trips the allocated widget id through an Intent extra so process death during the bind dialog still deallocates correctly on cancel. Falls back to `EXTRA_APPWIDGET_ID` on OEM forks that strip non-framework extras
- `WidgetPreviewLoader` — API-31+ `previewLayout` → `previewImage` → provider icon fallback hierarchy. `PreviewSource` sealed type (`RemoteLayout` / `PreviewImage` / `ProviderIcon` / `Empty`) so callers don't have to re-derive the precedence
- `LauncherApp.requestWidgetBind(request, callback)` helper + lifecycle-aware `ActivityResultLauncher` registration in `MainActivity` — bind requests from anywhere in the tree dispatch through the Activity-scoped launcher

### Changed

- Dependency bumps for v0.2.0: `core-ktx` 1.12.0 → 1.13.1, `activity-compose` 1.8.2 → 1.9.3, `lifecycle-runtime-ktx` 2.7.0 → 2.8.7 (added `lifecycle-runtime-compose`), `material` 1.11.0 → 1.12.0, Compose BOM 2024.01.00 → 2024.10.01 (Compose 1.7.x / Material3 1.3.0), `kotlinCompilerExtensionVersion` 1.5.8 → 1.5.14, Kotlin plugin 1.9.22 → 1.9.24. `datastore-preferences` 1.1.1 added as a new dep. Closes the `GradleDependency` lint disable (removed from the suppress list)
- `LauncherState` gained `motionPreset: MotionPresetKey` (default `STANDARD`) — callers constructing `LauncherState` directly keep compiling
- Enum `fromRaw` accessors in `HomeLayoutKey` / `DrawerSortKey` switched from `values()` to `entries` for parity with the new `MotionPresetKey`

### Fixed

- Widget bind cancel path no longer leaks allocated widget ids on process death — the allocated id is now encoded in the outbound Intent so `parseResult` can recover it from the result Intent even when the contract instance is recreated
- `MainActivity.onDestroy` now flushes any pending widget-bind callback so a dying Activity's closure can't pin Compose state on a rotation during the bind dialog
- `WidgetPersistence.decode` now honours the stored `schema_version` — reserves the version-dispatch branch for when a future shape change ships
- `WidgetPersistence.clear()` wipes the schema stamp alongside the JSON so an empty store doesn't carry a stale version marker

### Notes

- This is scaffolding + primitives for v0.2.0 "widgets + persistence + motion". Behavioural wire-up (drop-to-edge page creation in the grid drag detector, widget resize handles, picker UI using the new preview loader, settings toggle for motion preset) is staged for the next iteration
- Motion preset live-switch without Activity recreate is deferred — `ProvideMotionScheme` seeds from `LauncherPreferences.snapshot()` today; a Flow-based observation ships when the motion toggle lands in settings
- Physical-device interactive validation for this iteration was not possible (no device attached). Build, lint, and static audit are the ship gates; the new surfaces will be exercised on device during the v0.2.0 release smoke test

## v0.1.0 — 2026-04-24

### Added
- Standalone Compose prototype shell carved out of Lawnchair Lite — home surface, app drawer with Finder search, edit-mode tray, settings overlay, folders, widget picker, page manager, notification shade
- Full Samsung One UI 7 parity research baked into ROADMAP (Rounds 1–3): 900+ icon catalog reference, tribalfs sesl7 interop path, Launcher3 forking strategy, motion parity reference captures
- Gradle wrapper (8.4) + standalone Android build, not wired into Lawnchair Lite Gradle
- Adaptive launcher icon, light-first One UI palette, Samsung-style blue accent
- GitHub Actions release workflow (`release.yml`) — signed release APK + debug APK + AAB attached per tag
- Keystore signing config gated on `keystore.properties`; debug builds work without it
- MIT license, shields.io README badges, repo `CLAUDE.md` working notes
- `LauncherPreferences` (SharedPreferences-backed) persisting 8 user-facing toggles: media page, apps button, app/widget labels, notification swipe, lock layout, home-layout mode, drawer sort
- Launcher `HOME` + `DEFAULT` intent filter + `singleTask` + `onNewIntent` observer so HOME re-entry collapses overlays and scrolls to default page
- `BackHandler` absorbing back-press on home — overlays collapse first, then search clears, then page resets to default, then press is absorbed
- Real `WallpaperManager.peekDrawable()` integration inside `WallpaperAtmosphere` — off-main-thread decode via `produceState(Dispatchers.IO)`, immutable bitmap copy, gradient fallback when access denied
- Complete `darkColorScheme` — every surface / text / outline / container slot overridden, no more half-dark UI surfaces in night mode
- Full Material 3 typography scale (13 slots) tuned to One UI metrics — tighter letter-spacing on display tier, medium-weight bias, sentence-case body
- `windowShowWallpaper=true` + transparent window background — Compose surface floats over the user's wallpaper, matching real launcher behaviour
- Lint config — `warningsAsErrors=false`, disables `OldTargetApi`/`GradleDependency`/`ObsoleteSdkInt`/`ObsoleteLintCustomCheck` (scheduled for Iteration 2)

### Fixed
- Dropped hardcoded `windowLightStatusBar`/`windowLightNavigationBar` theme overrides — `enableEdgeToEdge()` now auto-flips system-bar icon colour with system dark mode
- First SharedPreferences write on composition entry skipped via `snapshotFlow.drop(1)` — no wasted I/O on cold start
- System wallpaper bitmap copied with `Bitmap.copy(ARGB_8888, false)` so the cached `ImageBitmap` survives wallpaper change / live wallpaper frame recycling

### Added (Iteration 2 carry-over — still v0.1.0 scope)
- `LauncherApp : Application` — global uncaught-exception handler writes a minimal crash log to `filesDir/crash-log.txt`, next cold start surfaces it as a toast and clears the file atomically
- `AppWidgetHost` lifecycle stub — host id 2048 allocated at `Application.onCreate`, `startListening()` / `stopListening()` matched to `MainActivity.onStart()` / `onStop()` so v0.2.x widget binding has a solid hook point
- `Haptics` helper — `HapticFeedbackConstants.CONFIRM` / `DRAG_START` on API 30+, `LONG_PRESS` fallback on API 28-29, `FLAG_IGNORE_VIEW_SETTING` so haptics fire even when the host view disables them
- `android:allowBackup="false"` — keeps the user's widget-binding IDs (persisted in v0.2.x) + SharedPreferences out of ADB backups by default

### Fixed (Iteration 2 counter-audit)
- `LauncherApp.onCreate` — `widgetHost` now published before `instance` so no reader sees a non-null `LauncherApp` without a companion-visible `AppWidgetHost`
- `installCrashHandler` falls back to `Process.killProcess` + `exitProcess(10)` when no prior `UncaughtExceptionHandler` is installed — prevents zombie process with a frozen home surface
- `Haptics.dragPickup` pre-API-30 fallback uses `LONG_PRESS` instead of `CONTEXT_CLICK` (which mapped to a right-click feel on OEM skins, wrong semantic for a grab)

### Notes
- Samsung trademarks, logos, wallpapers, and branded glyph sets remain off-limits by design — this is a clone, not a port
- Landscape grid and foldable posture support staged for v0.2.x
- Magisk-module install path and oneui-design `AndroidView` interop deferred to v0.3.x
