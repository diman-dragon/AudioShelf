# AudioShelf (native Android)

Kotlin + Jetpack Compose. A rewrite of the Capacitor version: same look, no WebView.

Version lives in `version.txt`; bump the last digit for every build you want as a new release.
`versionCode = major * 10000 + minor * 100 + patch`.

## Status

1. App skeleton: theme (sand / AMOLED), header, tab bar, swipe navigation, CI.
2. Library: SAF folder scan, grouping into books and albums, search / filter / sort, list and cards, covers (embedded tags and Matroska attachments), progress, edit / hide / delete. Storage is the same `kv` SQLite as the old app.
3. Settings: folder, hidden books, skip intervals, theme, language, About.
4. Player on Media3: foreground service with notification, chapters from m4b / mka / mp3 (ID3 CHAP), bookmarks, sleep timer with fade, speed, play modes, resume with rewind, position saved in the background.
5. Sound: 10-band equalizer (global or per book), presets.

6. Statistics (Settings → Statistics): listening time per day / week / month / year, streaks, calendar, time of day, speeds, top books.
7. Library: New / Started / Finished filter, history and queue, auto-play next book.
8. Sound processing: volume boost, a tap-to-cycle speaker button whose brackets show the level (per book or global).
9. Chapters also from ogg / opus / flac (Vorbis comments), `.cue`, Libby `openbook.json` and OverDrive mp3 markers. Formats `mp4`, `webm`, `3gp` are listed.
10. Notes, characters and `info.txt` description per book; headset multi-click (1 play/pause, 2 forward, 3 back, 4+ next); backup and restore of all data to a file.

Premium / billing is not built: every feature is available. Planned model: 14 days of full access, then a one-time purchase, plus donations (text note only in About, no logic yet).

## Premium (planned, not built)

These are reserved for a future paid tier and are intentionally not implemented (there is no code for them):

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
  MainActivity, AppContainer (StorageModule, LibraryModule, PlaybackModule), BackupController, AppLog
  data/     the key-value store (Kv, SQLite), preferences, backup format
  library/  folder scan, grouping into books, stores, covers, durations
  media/    chapter and tag readers (m4b, mka, mp3, ogg, flac, cue)
  player/   Media3 engine, session, loading, progress, sleep timer
  sound/    equalizer and volume boost
  stats/    listening statistics, calendar helpers (DayMath)
  ui/       AudioShelfApp (shell), actions/ (what the buttons do), screens, components/, theme/
```

Logs: Settings → Diagnostic log → Share (a zip with one file per day, the last 7 days; `device.txt` describes the phone), or `adb logcat -s 'AudioShelf/*'`.

Finding a stutter or a raspy voice in a shared log (`grep` works well):
- `Stutter` — an audio buffer underrun: the moment the sound broke, with the power state.
- `Jitter` — `STALL` (the app thread was not scheduled), `SUSPENDED` (the device slept), main thread busy.
- `Playback` `beat:` — every 2 s while playing: `buffered-ahead`, `cpu`, `worst-thread-delay`, `sound[...]`, `power[...]`.
- `sound[... source-clipped=N ...]` high: the file itself is clipped. `limiter-hits` / `over-full-scale` high: the equalizer or boost pushes the signal into the limiter.
- `Playback` `suppression` / `AUDIO FOCUS LOSS`, `AudioStreams`: another app played a sound at that time.
- `Offload` — every switch rebuilds the audio track.
- `Bluetooth`, `AudioDevices`, `route[...]` in every `beat:` and `Stutter` line, `MediaButton` — everything about Bluetooth: a device connecting, the stream `PLAYING` / `NOT PLAYING (suspended)`, codec status, call audio (SCO) taking over from music.
