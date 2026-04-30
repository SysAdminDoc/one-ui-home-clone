<h1 align="center">One UI Home Clone</h1>

<p align="center">

[![Version](https://img.shields.io/badge/version-0.2.0-4A88FF)](CHANGELOG.md)
[![License](https://img.shields.io/badge/license-MIT-4A88FF)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-4A88FF)](prototype-android/app/build.gradle.kts)
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

## Current state (v0.2.0)

Compose-first prototype covering:

- One UI style home surface with widget hero card, wallpaper atmosphere, page indicator
- Samsung-style app drawer shell with Finder search (grouped results, recent searches, settings hits)
- Unified edit-mode tray (Wallpaper, Themes, Widgets, Home screen settings)
- Widget picker overlay + widget preview strip
- Folder bubble + folder open overlay
- Hide-apps overlay (Samsung "clean view" equivalent)
- Page manager panel with reorderable preview tiles
- Notification shade overlay
- Full settings surface with Samsung section ordering + terminology

v0.2.0 landed the widgets + persistence + motion primitives:

- `LauncherDataStore` — DataStore Preferences mirror with one-shot migration from the v0.1.0 SharedPreferences file (live DataStore flow; `LauncherPreferences` remains the sole writer until the monolith split cuts call sites over)
- `WidgetPersistence` — versioned JSON widget-ID store on its own DataStore file, bounds-checked decode (128 KB / 1024 entries) to contain corrupt-file blast radius
- `MotionScheme` + `ProvideMotionScheme` — Standard / Reduced presets exposed as raw `SpringParams`, threaded through a `LocalMotionScheme` CompositionLocal. Platform `ANIMATOR_DURATION_SCALE == 0` forces Reduced
- `WidgetBindContract` — stateless `ActivityResultContract` for `ACTION_APPWIDGET_BIND` that round-trips the allocated widget ID through an Intent extra so process death during the bind dialog still deallocates correctly on cancel
- `WidgetPreviewLoader` — `previewLayout` (API 31+) → `previewImage` → provider icon fallback
- Dep bumps: Compose BOM 2024.01 → 2024.10.01 (Compose 1.7 / Material3 1.3), Kotlin 1.9.22 → 1.9.24, core-ktx / activity-compose / lifecycle / material advanced to current stable; `datastore-preferences` 1.1.1 added

Motion preset wiring is seeded at composition — live switch without Activity recreate ships when the settings toggle lands. Widget bind / preview / persistence are plumbed but not yet consumed by the picker UI; that's v0.2.x follow-up work alongside drop-to-edge page creation and widget resize handles.

## Build the prototype

From repo root:

```bash
# Git Bash / Linux / macOS
cd prototype-android
JAVA_HOME="/c/Program Files/Android/openjdk/jdk-21.0.8" ./gradlew assembleDebug
```

```powershell
# PowerShell
cd prototype-android
$env:JAVA_HOME='C:\Program Files\Android\openjdk\jdk-21.0.8'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleDebug
```

APK output: `prototype-android/app/build/outputs/apk/debug/app-debug.apk`

## Install as launcher

```bash
adb install -r prototype-android/app/build/outputs/apk/debug/app-debug.apk
adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME
# then select "One UI Home Clone" → "Always"
```

## Roadmap

See [ROADMAP.md](ROADMAP.md). Near-term themes:

1. Real `AppWidgetHost` integration (v0.2.x)
2. Drop-to-edge page creation + widget resize handles (v0.2.x)
3. Motion parity — 240fps reference captures → Compose `spring()` parameters (v0.3.x)
4. Landscape + foldable posture support (v0.4.x)
5. Optional `tribalfs/oneui-design` AndroidView interop for SwitchBar / ToolbarLayout fidelity (v0.5.x)

## Legal

Samsung, One UI, and related marks are trademarks of Samsung Electronics Co., Ltd. This project ships no Samsung copyrighted assets, logos, wallpapers, or glyph sets. All visual elements are original, inspired-by, or CC-licensed. See [references/clone-brief.md](references/clone-brief.md) for the trademark boundary.

## License

MIT — see [LICENSE](LICENSE).
