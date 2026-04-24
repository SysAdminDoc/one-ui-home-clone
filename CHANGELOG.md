# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## v0.1.0 — 2026-04-24 (unreleased)

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

### Notes
- Samsung trademarks, logos, wallpapers, and branded glyph sets remain off-limits by design — this is a clone, not a port
- Landscape grid and foldable posture support staged for v0.2.x
- Magisk-module install path and oneui-design `AndroidView` interop deferred to v0.3.x
