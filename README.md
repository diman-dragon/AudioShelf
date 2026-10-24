# AudioShelf (native Android)

Kotlin + Jetpack Compose. A rewrite of the Capacitor version: same look, no WebView.

Version lives in `version.txt` (`1.0.0`); bump the last digit for every build you want as a new release.
`versionCode = major * 10000 + minor * 100 + patch`.

## Status

1. App skeleton: theme (sand / AMOLED), header, tab bar, swipe navigation, CI.
2. Library: SAF folder scan, grouping into books and albums, search / filter / sort, list and cards, covers (embedded tags and Matroska attachments), progress, edit / hide / delete, add files. Storage is the same `kv` SQLite as the old app.
3. Settings: folder, hidden books, skip intervals, theme, language, About.
4. Player on Media3: foreground service with notification, chapters from m4b / mka / mp3 (ID3 CHAP), bookmarks, sleep timer with fade, speed, play modes, resume with rewind, position saved in the background.
5. Sound: 10-band equalizer (global or per book), presets.

Premium / billing is not ported: every feature is available.

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
