![One UI Home Clone](assets/brand/one-ui-home-banner.png)

[![Version](https://img.shields.io/badge/version-0.2.6-2586FF)](https://github.com/SysAdminDoc/one-ui-home-clone/releases/latest)
[![License](https://img.shields.io/badge/license-MIT-2586FF)](LICENSE)
[![Platform](https://img.shields.io/badge/Android-9%2B-2586FF?logo=android&logoColor=white)](prototype-android/app/build.gradle.kts)
[![Kotlin](https://img.shields.io/badge/Kotlin-Compose-2586FF?logo=kotlin&logoColor=white)](prototype-android/)

One UI Home Clone is an open-source Android launcher prototype inspired by Samsung's Home and Apps workflow. Organize installed apps into folders, find them with local search, and experiment with home-screen layouts. It's built in Kotlin and Compose, without Samsung code or proprietary artwork.

[Try the signed APK](https://github.com/SysAdminDoc/one-ui-home-clone/releases/latest/download/one-ui-home-clone-v0.2.6-release.apk) · [Release notes](https://github.com/SysAdminDoc/one-ui-home-clone/releases/latest) · [Original logo concepts](assets/brand/concepts/README.md)

This is an independent project, not a Samsung product or a complete One UI replacement. Keep your current launcher installed while you try it.

## See it in action

These are unretouched captures of the signed v0.2.6 APK on an isolated Android 15 emulator. The [capture record](assets/screenshots/capture-report.json) identifies the exact APK and screenshot files by SHA-256.

Apps and Finder now use an opaque background, so Home-screen text doesn't show through search results or app labels.

The Home screen's Calendar and Weather cards are built-in layout previews, not live calendar or weather feeds. The widget picker shows previews supplied by installed widget providers. Live widgets require a provider app and Android's binding or setup flow.

| Home | Apps | Finder |
| --- | --- | --- |
| <img src="assets/screenshots/home.png" alt="One UI Home Clone home screen" width="280"> | <img src="assets/screenshots/apps.png" alt="One UI Home Clone apps screen" width="280"> | <img src="assets/screenshots/finder.png" alt="Finder search results" width="280"> |

| Edit mode | Widgets | Home settings |
| --- | --- | --- |
| <img src="assets/screenshots/edit-mode.png" alt="Home screen edit mode" width="280"> | <img src="assets/screenshots/widgets.png" alt="Widget picker" width="280"> | <img src="assets/screenshots/settings.png" alt="Home screen settings" width="280"> |

## What you can try

- The Home and Apps screens follow the interaction model Samsung users already know.
- Finder searches installed apps, launcher settings, actions, shortcuts, and optional local contacts.
- Home pages support folders, app shortcuts, notification badges, widgets, grid controls, and a dedicated edit surface.
- Phone portrait, landscape, foldable-width, and tablet layouts use separate responsive grid contracts.
- Standard and reduced motion modes respond immediately without restarting the activity.
- Local backup, restore, and sanitized diagnostics tools make experimentation recoverable.

The current build is for people who want to test an independent launcher or contribute to its development. It doesn't provide Samsung services, bundled weather data, or full One UI feature parity.

## Install

1. Download the [latest signed APK](https://github.com/SysAdminDoc/one-ui-home-clone/releases/latest/download/one-ui-home-clone-v0.2.6-release.apk).
2. Open the file on your Android device and allow installation from that source if Android asks.
3. Launch One UI Home Clone, then tap **Open Home settings**.
4. Choose **One UI Home Clone** as the Home app.

Android may place the Home app setting under **Settings > Apps > Default apps > Home app**. Existing installs can be upgraded without clearing launcher data:

```powershell
adb install -r one-ui-home-clone-v0.2.6-release.apk
```

## Privacy

One UI Home Clone has no `INTERNET` permission. Backup and diagnostics exports are local files that you choose to create. Widget providers are separate apps with their own permissions and data policies.

- Installed-app inventory is used locally to draw icons, folders, search results, and launch targets.
- Contact search is off by default. It needs both the in-app setting and Android's Contacts permission. Names are never added to recent searches or exports.
- Notification badges are also off by default. When enabled, the launcher keeps only aggregate per-app counts.
- Settings, pages, recent Finder searches, hidden apps, and widget IDs live in app-private storage. The manifest opts out of Android backup.

The diagnostics export contains version, Android SDK, launcher state, sanitized crash fields, and aggregate counts. It does not include app names, contact names, notification text, or search history.

## Current capabilities

| Area | Included |
| --- | --- |
| Home | Multiple pages, folders, dock, Apps button, page management, wallpaper atmosphere |
| Apps | Custom or alphabetical order, paged grid, hide-app controls, local Finder |
| Widgets | Android provider discovery, binding, setup flow, resize, move, recovery |
| Personalization | Home and Apps grids, folder grid, labels, media page, motion, badges |
| Reliability | Crash recovery, bounded local stores, backup rollback, Baseline Profile |
| Accessibility | TalkBack semantics, RTL fixtures, pseudo-locale checks, reduced motion |

## Build and verify

The Android project lives in [`prototype-android/`](prototype-android/). It needs Android Studio with API 37 installed and JDK 17 or newer.

```powershell
cd prototype-android
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Run the connected Compose checks on an emulator:

```powershell
$env:ANDROID_SERIAL='emulator-5590'
.\gradlew.bat :app:connectedDebugAndroidTest
```

Signed packaging uses a local `prototype-android/keystore.properties` file. Copy [`keystore.properties.example`](keystore.properties.example), add your keystore values, then run:

```powershell
.\gradlew.bat clean :app:releaseChannelPackage
```

The release task writes a versioned APK, a `.sha256` checksum file, and JSON metadata under `prototype-android/app/build/outputs/release-channel/`. The original project signing key is required to publish an update that existing installs can accept. A build signed with your own key requires a separate installation.

Brand files and the downloadable [`icon pack`](one_ui_home_clone_icon_pack/) are reproduced with [`build-brand-assets.ps1`](prototype-android/tools/build-brand-assets.ps1). The [concept archive](assets/brand/concepts/README.md) preserves all five original directions and the selected master. Use the production icon pack for installation assets, not the unprocessed studies.

Capture release screenshots with [`capture-marketing.ps1`](prototype-android/tools/capture-marketing.ps1), then run [`verify-marketing.ps1`](prototype-android/tools/verify-marketing.ps1). Verification checks the original concepts, README links, release checksums, and screenshot provenance against the final APK. See the [Android project guide](prototype-android/README.md#marketing-assets) for commands.

## Legal

Samsung and One UI are trademarks of Samsung Electronics Co., Ltd. This project is not affiliated with or endorsed by Samsung. All shipped visual assets are original project artwork.

## License

[MIT](LICENSE)
