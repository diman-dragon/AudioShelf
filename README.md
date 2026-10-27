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

6. Statistics (Settings → Statistics): listening time per day / week / month / year, streaks, calendar, time of day, speeds, top books.
7. Library: New / Started / Finished filter, history and queue, auto-play next book.
8. Sound processing: volume boost and skip silence (per book or global).
9. Chapters also from ogg / opus / flac (Vorbis comments), `.cue`, Libby `openbook.json` and OverDrive mp3 markers. Formats `mp4`, `webm`, `3gp` are listed.
10. Notes, characters and `info.txt` description per book; headset multi-click (1 play/pause, 2 forward, 3 back, 4+ next); backup and restore of all data to a file.

Premium / billing is not built: every feature is available. Planned model: 14 days of full access, then a one-time purchase, plus donations (text note only in About, no logic yet).

## Premium (planned, not built)

These are reserved for a future paid tier and are intentionally not implemented (see `Premium.kt`):

- Tasker / automation intents.
- Android Auto.

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
