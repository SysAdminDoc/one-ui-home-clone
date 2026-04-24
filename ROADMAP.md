# ROADMAP

Separate Gradle-independent workspace for building a Samsung One UI Home parity clone. Standalone Compose prototype + parity research docs; not wired into the Lawnchair Lite build.

## Iteration 1 — v0.1.0 "actually-a-launcher" (P0/P1)

Ship a debug APK that a Samsung user can install and select as their default home app, with persistent toggles and real wallpaper behind the home surface.

- [x] **P0**: Transparent window + `windowShowWallpaper=true` so system wallpaper renders behind the Compose home surface instead of our gradient placeholder
- [x] **P0**: `BackHandler` on home that collapses overlays first and then absorbs the back press (launcher HOME semantics — back never leaves home)
- [x] **P0**: `onNewIntent` wiring — when user presses HOME while already inside launcher, collapse every overlay and scroll back to the default home page
- [x] **P0**: Complete `Typography` scale (display → label, 13 slots) matching One UI metrics instead of empty default
- [x] **P0**: Complete `DarkColorScheme` so dark mode renders every surface/text variant, not just `primary`
- [x] **P0**: `LauncherPreferences` repo (SharedPreferences-backed) persisting 8 user-facing toggles: `mediaPageEnabled`, `appsButtonEnabled`, `appLabelsEnabled`, `widgetLabelsEnabled`, `swipeDownForNotifications`, `homeLayoutMode`, `lockHomeScreenLayout`, `drawerSortMode`. Launcher launches retain state across restarts
- [x] **P0**: Real `WallpaperManager.peekFastDrawable()` integration inside `WallpaperAtmosphere`, gradient fallback if permission denied (auto for non-system apps that haven't requested READ_EXTERNAL_STORAGE)
- [x] **P1**: `./gradlew lintDebug` baseline — 0 lint errors, only warnings allowed; lint-baseline.xml shipped for legacy warnings
- [x] **P1**: Adaptive-icon monochrome layer (Android 13+ themed-icon support) — foreground vector reused as monochrome drawable
- [x] **P1**: Signed-release keystore scaffold + release workflow gated on `KEYSTORE_BASE64` secret

## Iteration 2 — v0.2.0 "widgets + persistence + motion" (planned)

- [ ] **P0**: DataStore migration from SharedPreferences for typed, async persistence
- [ ] **P0**: Real `AppWidgetHost` scaffolding (host id 2048, allocate + bind + listen lifecycle)
- [ ] **P0**: `AppWidgetProviderInfo.previewLayout` (API 31+) with `previewImage` fallback in widget picker
- [ ] **P0**: Drop-to-edge page creation with haptic tick + 350 ms hold threshold from ROADMAP capture research
- [ ] **P0**: Widget resize handles with Samsung dashed outline, 8dp grid snap
- [ ] **P1**: Motion preset switcher (Standard / Reduced) wired through a `MotionScheme` object
- [ ] **P1**: Split `OneUiHomeCloneApp.kt` into per-surface files once behavior stabilizes (home, drawer, edit, finder, settings, folders, widgets, notifications)
- [ ] **P1**: App launch crash guard — global uncaught exception handler writes crash log to app files dir + shows toast on next launch

## Iteration 3 — v0.3.0 "parity & polish" (planned)

- [ ] **P0**: `tribalfs/oneui-design` sesl7 interop wrapper — `AndroidView` host for `SeslSwitchBar`
- [ ] **P0**: Folder grid settings (3×4 / 4×4 / 5×5 options) with Samsung defaults
- [ ] **P0**: Alphabet scrubber on drawer right edge with pull-out affordance
- [ ] **P1**: `IconPackManager` runtime contract compatible with Launcher3 icon packs (including our own iOSIconPack)
- [ ] **P1**: Adaptive-icon shape switcher (squircle / square / rounded-square / circle / teardrop / cylinder)
- [ ] **P1**: Lock-layout toggle wired globally to disable long-press reorder

## Iteration 4 — v0.4.0 "device class" (planned)

- [ ] **P0**: Landscape home grid (5×3 per One UI) separate from portrait (4×5)
- [ ] **P0**: Tablet layout (larger cell / wider dock / denser widget picker)
- [ ] **P1**: Foldable posture listener (`WindowInfoTracker`) — half-open dock mode

## Iteration 5 — v0.5.0 "pre-1.0 hardening" (planned)

- [ ] **P0**: `./gradlew testDebug` unit test harness — at least `buildSeedWidgets`, `alphabeticalAppSections`, `reorderHomeGridItems`, `applyHiddenAppsToPages`
- [ ] **P0**: Espresso scenario: tap drawer button → drawer opens → type search → Finder results render
- [ ] **P1**: `ProGuard` rules audit for Compose + AppWidgetHost in release build
- [ ] **P1**: Play Store AAB build path + signing config separation from APK path

## Cumulative metrics target (v1.0)

- Cold launch time: ≤ 800 ms (home visible) on Pixel-class devices
- Frame pacing: ≤ 5% dropped frames during drawer open animation (Perfetto capture)
- App launch time: ≤ 250 ms from tap to target `onCreate`
- Memory: resident set ≤ 140 MB with 40 installed apps + 6 widgets bound



## Planned Features

### Home screen
- Exact One UI 7 grid: 4x5 default with 4x6 option, correct icon padding and label baseline
- Two-row page indicator (top-centered) with swipe affordance matching Samsung's hit-test geometry
- Drop-to-edge page creation with haptic tick and the correct 350ms hold threshold
- Widget resize handles with the Samsung dashed outline and 8dp grid snap
- Wallpaper motion parallax: 2-axis device tilt driving a `graphicsLayer { translationX/Y }` offset

### App drawer
- Vertical-scroll A-Z drawer with alphabet scrubber on the right edge (pull-out style)
- Folder creation by drag-onto inside the drawer (matches One UI, not Pixel behavior)
- Search bar with recency + Finder-style suggestions
- Clean/customize view: hide apps, not uninstall, with `clean_apps` persisted to DataStore
- Horizontal-page drawer toggle in Home Up-equivalent settings for users coming from older One UI

### Edit mode
- Unified edit-mode tray with Wallpaper, Themes, Widgets, Home screen settings buttons
- Widget picker grouped by app with live preview (`AppWidgetProviderInfo.previewLayout` when available, fallback to `previewImage`)
- Grid-size slider with live re-layout and the subtle bounce Samsung uses
- Dual-home on/off toggle wired to an optional drawer suppression

### Settings & motion
- "Home screen settings" surface with the exact Samsung section ordering
- Motion preset switcher (Standard / Reduced) that remaps all transitions via a `MotionScheme` object
- Rotate-to-landscape support — One UI landscape grid differs from portrait, track it separately
- Lock-layout toggle that disables long-press reorder globally

## Competitive Research

- **Samsung One UI Home** — Ground truth. Capture reference videos at 240fps for each interaction to measure timings
- **Lawnchair 13 / Lawnchair Lite** — Closest OSS baseline; reuse their AOSP Launcher3 fork strategy, deviate only where parity demands
- **Niagara / Smart Launcher** — Alternative drawer paradigms; useful negative examples of what the clone should not do
- **Microsoft Launcher (EOL)** — Historical reference for cross-device handoff; not a current target but their widget stack was polished

## Nice-to-Haves

- One UI clock/weather widget look-alikes as first-party in-app widgets
- Good Lock-style plugin surface — tiny APKs that extend the launcher (e.g., NavStar-like gesture tuning)
- Icon Kitchen integration so users can pull icon packs without leaving the app
- AOD-adjacent "Always on Home" screen mode for tablets in stand
- Automated parity regression harness — Espresso scripts that record and replay reference device interactions

## Open-Source Research (Round 2)

### Related OSS Projects
- https://github.com/OneUIProject/oneui-core — "sesl", heavily modified fork of Jetpack + Material Components that replaces stock libraries with OneUI-styled versions; the OSS foundation closest to Samsung's actual look
- https://github.com/OneUIProject/oneui-design — drop-in OneUI components (AppBarLayout, ToolbarLayout, SwitchBar, etc.) targeting Material Components
- https://github.com/OneUIProject/OneUI-Kotlin-Template — ready-to-use Kotlin template wired against oneui-design
- https://github.com/AyraHikari/SamsungLauncherPort — Samsung Launcher port Magisk module for compatible Android 13 ROMs
- https://github.com/reiryuki/One-UI-Home-35-Magisk-Module — reiryuki's SDK 35 port; useful reference for which system permissions the stock launcher assumes
- https://github.com/ShabdVasudeva/one-ui-launcher-port — no-root OneUI 2 launcher port, low API min
- https://github.com/OpenLauncherTeam/openlauncher — OSS launcher reference; discontinued but clean architecture for workspace/folder/dock/icon-pack plumbing
- https://github.com/amirzaidi/Launcher3 — Rootless Pixel Launcher, canonical Launcher3 fork with adaptive-icon and icon-pack plumbing

### Features to Borrow
- oneui-design component library integration (OneUIProject) — instead of hand-rolling OneUI widgets in Compose, interop with the oneui-design View widgets via `AndroidView` for high-fidelity pixel parity where Compose can't match
- oneui-icons catalog (OneUIProject) — 900+ OneUI-style icons; legal-grey but reasonable for personal/OSS parity effort as long as Samsung-trademark glyphs are excluded
- Kotlin template scaffolding (OneUI-Kotlin-Template) — use as the starting point for prototype-android instead of a blank Compose project
- Magisk-module install path (reiryuki / AyraHikari) — optional power-user install route that replaces system launcher without the permission gymnastics
- Icon-pack runtime loader contract (Launcher3 fork) — match IconPackManager's contract so existing OSS icon packs (including our own iOSIconPack) drop in cleanly
- Adaptive-icon shape switcher (Launcher3 fork) — squircle / square / rounded square / circle / teardrop / cylinder; OneUI default is squircle, expose the others as a hidden power-user option
- Motion parity via MotionLayout + physics spring specs — Samsung's feel is distinctive because of overshoot/spring on open/close; replicate via Compose `spring(dampingRatio, stiffness)` tuned to reference captures

### Patterns & Architectures Worth Studying
- Compose + `AndroidView` interop for OneUI fidelity — where pure Compose can't hit the exact look (OneUI's SwitchBar, AppBarLayout with big-title collapsing behavior), wrap the oneui-design View; budget the interop cost against perfect fidelity
- Launcher3 forking strategy — Launcher3's architecture is proven for real launcher needs (hotseat, workspace pages, folders, predictive row); a Compose-first clone that ignores Launcher3's lessons will relearn them the hard way
- Motion-parity research harness — capture reference interactions on a physical Samsung device at 240fps, compare frame-by-frame against the clone, convert deltas into physics parameters; essential for "it feels right" rather than "it looks right"
- Legal boundary between "clone" and "port" — Samsung trademarks + copyrighted assets are off-limits; OneUIProject threads this carefully by shipping equivalent-but-not-identical widgets. Our clone should document the same boundary in CONTRIBUTING.md

## Implementation Deep Dive (Round 3)

### Reference Implementations to Study
- **OneUIProject/oneui-core/sesl/appcompat/src/main/java/androidx/appcompat/widget/SeslSwitchBar.java** — https://github.com/OneUIProject/oneui-core/tree/sesl4 — SeslSwitchBar, the canonical OneUI switch widget. Use via `AndroidView` wrapper in Compose until a pure-Compose port is ready.
- **tribalfs/oneui-design** — https://github.com/tribalfs/oneui-design — actively maintained OneUI Design fork with sesl6/7/8 support (covers OneUI 6/7/8). Prefer this over the upstream OneUIProject repo which lags.
- **tribalfs/sesl-androidx** — https://github.com/tribalfs/sesl-androidx — modified AndroidX modules; drops in for `androidx.appcompat`/`androidx.core`. Required dependency-exclusion block in `build.gradle.kts` — see README.
- **Launcher3-dev/Launcher3/src/com/android/launcher3/Workspace.java** — https://github.com/Launcher3-dev/Launcher3/blob/main/src/com/android/launcher3/Workspace.java — canonical Workspace implementation (page management, cell layout occupancy, drop animation). Template for one-ui-home-clone's workspace.
- **Launcher3-dev/Launcher3/src/com/android/launcher3/Hotseat.java** — https://github.com/Launcher3-dev/Launcher3/blob/main/src/com/android/launcher3/Hotseat.java — Hotseat extends CellLayout; QSB_CENTER_FACTOR = 0.325f, mHasVerticalHotseat flag. Reference for dock emulation.
- **cs.android.com/android/platform/superproject/+/master:packages/apps/Launcher3/** — https://cs.android.com/android/platform/superproject/+/master:packages/apps/Launcher3/ — upstream AOSP Launcher3 full tree. Use this as the ground truth; GitHub mirrors lag behind.
- **LawnchairLauncher/lawnchair/lawnchair/src/app/lawnchair/LawnchairApp.kt** — https://github.com/LawnchairLauncher/lawnchair — OSS rebase-onto-Launcher3 pattern. Required reading before deciding Compose-first vs Launcher3-fork architecture.
- **AyraHikari/SamsungLauncherPort (Magisk module)** — https://github.com/AyraHikari/SamsungLauncherPort — reference for power-user Magisk install path. Shows which system-level permissions Samsung launcher expects.
- **reiryuki/One-UI-Home-35-Magisk-Module** — https://github.com/reiryuki/One-UI-Home-35-Magisk-Module — SDK 35 port Magisk module; useful reference for which system permissions the stock launcher assumes.
- **OneUIProject/OneUI-Kotlin-Template** — https://github.com/OneUIProject/OneUI-Kotlin-Template — ready-to-use Kotlin template with oneui-design pre-wired. Better starting point than a blank Compose project.

### Known Pitfalls from Similar Projects
- **oneui-core requires excluding `androidx.appcompat` and `androidx.core`** — OneUIProject README — any transitive dep pulling those in causes duplicate-class errors. Use `configurations.all { exclude(group = "androidx.appcompat", module = "appcompat") }` in `build.gradle.kts`.
- **OneUIProject main repo has stalled — use tribalfs fork** — upstream activity dropped off in 2024; tribalfs/oneui-design publishes sesl6/7/8 updates. Plan to switch early, not at v1.0 cutover.
- **Samsung trademarks/copyrighted assets are off-limits** — cannot ship Samsung's actual glyph set, logos, or wallpapers. Reference competitive assets must be "inspired-by" or CC-licensed. Document in CONTRIBUTING.md early.
- **oneui-icons 900+ icon catalog is legal grey — exclude Samsung-branded glyphs** — reference OneUIProject catalog before bundling to avoid take-down requests.
- **Launcher3 forking at main-branch HEAD is a moving target** — AOSP rebases every Android version; Lawnchair tracks versions in subdirs (`lawnchair/versions/`). One-ui-home-clone should establish the same pattern from v0.
- **OneUI landscape grid ≠ portrait grid** — separate layout specs. Samsung uses 5×3 in landscape vs 4×5 portrait. Track both in `default_workspace_*.xml`.
- **`AppWidgetProviderInfo.previewLayout` Android 15+; `previewImage` fallback for older** — feature-detect, don't assume. Widget picker must handle both.
- **BIND_APPWIDGET permission only grantable to system apps** — non-system launcher must use `AppWidgetManager.ACTION_APPWIDGET_BIND` Activity intent with result. Plan for permission-denial UX.
- **Compose `AndroidView` wrapping SeslSwitchBar leaks Lifecycle if bound to Activity lifecycle instead of Composition** — use `LifecycleRegistry` + `rememberCompositionLifecycle` pattern. Bug class common in Compose-interop projects.
- **Motion parity requires 240fps reference captures — phone camera insufficient** — need an external high-speed camera or screen recording via Android's `adb shell screencap`/`screenrecord` isn't 240fps. Budget a Samsung device + external capture rig.

### Library Integration Checklist
- **oneui-core (tribalfs fork for OneUI 7)** — `implementation("io.github.tribalfs:sesl7-appcompat:1.0.x")` + `implementation("io.github.tribalfs:sesl7-material:1.0.x")` — entry: replace theme parent with `Theme.SeslMaterialComponents.Light.NoActionBar` in `styles.xml`. Gotcha: must `configurations.all { exclude(group = "androidx.appcompat"); exclude(group = "com.google.android.material") }` globally or ClassCastException on `SeslSwitchBar` inflate.
- **AppWidgetHost (framework)** — no dep; instantiate `AppWidgetHost(context, HOST_ID=2048)`. Entry: `host.startListening(); val widgetId = host.allocateAppWidgetId(); startActivityForResult(Intent(ACTION_APPWIDGET_BIND).putExtra(EXTRA_APPWIDGET_ID, widgetId), REQUEST_BIND)`. Gotcha: `HOST_ID` must be stable across app versions; pick a number > 1024 (Lawnchair uses 1024). Choose 2048 to avoid Lawnchair collision if user migrates.
- **Launcher3 CellLayout (vendored)** — no dep; copy `Launcher3/src/com/android/launcher3/CellLayout.java` + dependencies into `app/src/main/java/`. Entry: `<CellLayout android:id="@+id/workspace_cell" app:containerType="workspace"/>` in XML. Gotcha: CellLayout uses dozens of transitive helper classes (ShortcutAndWidgetContainer, DragController, WorkspaceItemInfo). Either vendor the whole tree or write a minimal Compose replacement — half-measures break on drag/drop.


