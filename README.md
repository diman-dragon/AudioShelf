# AudioShelf (native Android)

Kotlin + Jetpack Compose. A rewrite of the Capacitor version: same look, no WebView.

Version lives in `version.txt` (`1.0.0`); bump the last digit for every build you want as a new release.
`versionCode = major * 10000 + minor * 100 + patch`.

## Status

Stages 1–2 of the rewrite:

1. App skeleton: theme (sand / AMOLED, follows the system), header, tab bar with colour emoji icons, swipe navigation between the four tabs, system Back to Library, CI.
2. Player window on fake data (`player/FakePlayer.kt`): cover, title, seek bar, book progress, controls, speed / sleep / list / mode / bookmarks. Library, Sound and Settings are placeholders.

## Build

Everything is built by GitHub Actions (`.github/workflows/audioshelf-android.yml`):

- `build` → debug APK `AudioShelf-<version>.apk` (Artifacts, and a GitHub Release on push to main)
- `test` → JUnit tests and Robolectric screenshots of the player (artifact `screenshots`)

Signing uses the same secrets as before: `AUDIOSHELF_DEBUG_KEYSTORE_BASE64`, `AUDIOSHELF_DEBUG_KEYSTORE_PASSWORD`, `AUDIOSHELF_DEBUG_KEY_ALIAS`, `AUDIOSHELF_DEBUG_KEY_PASSWORD`. Without them the APK is signed with a temporary debug key.

Local build (optional): JDK 17+, Android SDK, then `./gradlew assembleDebug`.

## Layout

```
app/src/main/java/com/audioshelf/app/
  MainActivity.kt
  player/   state model, fake session, view model, time formatting
  ui/       AppFrame, Screen, theme/, components/ (header, tab bar, swipe, texts), player/, screens/
```
