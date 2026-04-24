# One UI Home Clone Prototype

This is a standalone Android Compose prototype for the Samsung-style launcher clone.

## Purpose

- validate the visual language
- prototype home, drawer, settings, and edit mode flows
- iterate without touching Lawnchair Lite app code

## Build

From the repo root:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\openjdk\jdk-21.0.8'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat -p one-ui-home-clone/prototype-android assembleDebug
```

## Notes

- This project is intentionally not included in the root repo `settings.gradle.kts`
- It has its own nested Gradle settings and app module
- It is meant to be a prototype shell first, then a real launcher project later
