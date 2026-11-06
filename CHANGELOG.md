# Changelog

## 1.4.6
Found in the log of 5 November (headset, 12 hours, no stutter, 4 calls and a dozen notifications).
- Calls in an app (Telegram, WhatsApp): they take the audio focus "for good" with the audio mode still normal, so the book was stopped for good and waited for a manual start (twice that day, after 2 and 10 minutes). Now, when the focus is lost and a ringtone or a voice-call stream is playing on the phone, the book pauses and carries on 2 seconds after that stream is gone (it rewinds 5 seconds as after any pause). A phone call already worked.
- Notifications: a notification sound made the headset suspend the music stream for a few seconds (`A2DP stream NOT PLAYING (suspended)` in the log), which is where the book "disappeared"; the rewind of 1.4.5 covers it. Two limits added after another app held the focus for 10 minutes: the book is not kept lowered for longer than 15 seconds, and it is not rewound after a "notification" longer than 30 seconds.
- Main thread: `playing=` lines took the power, device and route reading on the main thread, which cost 0.5-1.3 s (`the main thread was busy`, 8 times that day, always right after a play or pause). The reading is now taken in the background and the line is written when it is ready.
- Log: a burst of Bluetooth events (a call switching the headset) gave 6 identical `connected devices` lines each; one check is made after the burst. The repeated identical `battery_level_changed` lines are written once.
- Seen and working: Bluetooth events, the headset's charge (100%, 90%), the codec (AAC), the call pause and resume, the rewind after a notification.

## 1.4.5
- Notifications: two new switches in Settings. "Lower the book for notifications" (off by default, the behavior so far): while a notification sounds the book plays at a fifth of its volume and then comes back. "Back 10 s after a notification" (on): when the notification is over the book goes back 10 seconds, so nothing is missed. The log says `notification over after N ms: back ...` when it does.
- Bluetooth: the charge of the headphones or speaker is read when the device reports it (a hidden Android call; where Android refuses it, nothing is shown). It is written to the log with the device (`battery 80%`, also at every change of the charge) and shown in Settings under "Connected headphones and speakers".
- Log: the quick database writes (`wrote 1 ops`, every few seconds, a third of the log) are now one line a minute (`wrote N ops in M batches over 60 s, slowest X ms`); slow writes are still logged one by one.

## 1.4.4
Found in the log of 4 November (7 hours on a Bluetooth headset: no stutter, no underruns).
- Bluetooth: not one Bluetooth event reached the log in those 7 hours (the headset connecting, the stream state, the codec, the disconnect at 22:47), though the permission was given. The receiver was registered as not exported, and Android drops the broadcasts that the Bluetooth app sends to such a receiver. It is now registered as exported; the broadcasts are protected (only the system can send them) and the receiver only writes a log line.

## 1.4.3
Fixes found by reading the logs of 3-4 November.
- Calls: the audio focus handler read the audio mode twice, so a ringing phone could be logged as "audio mode 1, call=false" and the pause waited for the next check. The mode is now read once, and the first check for a call that starts a moment after the focus loss runs after 100 ms (it was 300 ms): the book stops with the first ring instead of about 0.4 s later.
- Log: `playing=` lines were written with a stale (or, at the start, empty `system-state=pending`) power, device and route reading, so after a call they still showed `mode=ringtone`. The reading is taken again at every play / pause.
- Log: `audio started coming out of the speaker` was written for every output, Bluetooth included; it now names the real output (`bluetooth A2DP "..."`, `speaker`, ...).
- Log: the `UNDERRUN burst` line is written at the next beat, so it now says how long the burst lasted, how long ago it ended and where it began, names the output, and no longer prints the meaningless `event-time`.
- Log: `doze (light idle)` now says whether the phone entered or left it (Android 13+), and `power[...]` carries `light-idle=`.
- Bluetooth: the permission ("Nearby devices") is asked at the next two starts while it is missing (it was asked once, and the one refusal on 3 November left the log without the headset state during the only stutter). Settings shows a "Bluetooth headphones" button that opens the app settings once Android stops showing the dialog, and the permission given there is picked up when the app comes back.

## 1.4.1
Diagnostic log, fixes and cleanup.
- Diagnostic log: Settings → Diagnostic log → Share / Erase logs. One file per day (`audioshelf-2026-11-03.log`, the next day `audioshelf-2026-11-04.log`, and so on), the last 7 days are kept, a day stops at 30 MB. Share sends one zip with every day and a `device.txt` about the phone. Lines also go to logcat as `AudioShelf/<part>`. Uncaught crashes are logged. It records:
  - every audio buffer underrun (`Stutter` tag) with the power state at that moment;
  - a late-wake watchdog (`Jitter` tag): the app thread not scheduled for 300+ ms, the main thread blocked, the whole device suspended during playback;
  - a line every 2 s while playing: buffered audio ahead, CPU, garbage collections, speed, volume, screen, sound levels, battery, memory, power state;
  - sound levels: how loud the source is, how many samples of the file itself are clipped, how often the limiter bent the signal (a raspy voice is either in the file or made by the equalizer/boost);
  - decoder, audio format, audio track (offload, buffer size), player and phone volume, audio focus loss, speed changes, seeks, item changes, player errors;
  - every user action (play, pause, seek, speed, sleep timer, library and settings buttons, screen changes);
  - Bluetooth, in full (`Bluetooth`, `AudioDevices`, `MediaButton` tags): adapter on/off, every device connecting and disconnecting with the reason, pairing, the A2DP music link and its codec status, whether the stream is playing or suspended, the call-quality SCO link, connected devices at start and after every change, the audio route (mode, SCO, outputs with address, sample rates and encodings), and every headset/car media button press. On Android 12+ the app asks once for the Bluetooth permission so the events carry device names; without it the log says so and still records the routes;
  - the system around the app: screen on/off, app in front/background, doze, battery saver, app standby bucket, heat, network changes, audio outputs (Bluetooth), ringer and do-not-disturb, and the audio streams other apps start (a "silent" notification may still play one);
  - storage: every write to the database with its time, and any write that blocked a thread.
- Notifications no longer interrupt the playback: a message sound, a notification, a navigation prompt or an alarm plays over the book or music at full volume, nothing pauses and nothing is ducked. Only a call (phone or a voice call in an app) pauses it, and it carries on when the call is over; another app that takes the audio for good (a video, a music player) still pauses it. Cause: a book is "speech", and the player pauses speech for every notification, and lowers the volume of music. The app now handles the audio focus itself (`FocusPolicy`), and the log shows every decision (`Focus` tag).
- Settings: the Auto-next switch showed "off" whatever was saved; it now shows the real value.
- Statistics no longer drop listening time when the dark-screen tick is late: the gap limit is 10 s instead of 5 s.
- Storage: writes can no longer reach the disk out of order, and a failed batch is retried; a write queued while the flusher was finishing is no longer left behind.
- Restore replaces the stored data of the backup instead of mixing it with the current one (a day of statistics was overwritten by an older copy). The app still has to be started again afterwards; the language picker no longer stops the app from closing cleanly.
- CI: failing tests now stop the release.
- Removed: the unreachable "add files" code (`LibraryController.addFiles`, `FileOps.add`), content time and speed fields of the statistics, `Premium.kt`, unused library flows, unused imports and the duplicated calendar helpers. `AudioShelfApp` is split into `ui/actions`, `BackupLaunchers` and `BackupController`.

## 1.3.4
Sound screen, library buttons and playback races.
- Volume boost stays on the Sound screen (per book or global). It is one tap-to-cycle speaker button: the brackets show the level (crossed out = off, 1-4 brackets = +3/+6/+9/+12 dB).
- Skip silence is removed (code, setting, strings); old saved records still load.
- Library buttons (history, refresh, sort, view) use the same pill style as the filters; queue and history headers in the dialog carry matching icons.
- Race fixes: audio offload is switched once after the settings stop changing (it used to flip twice at start and rebuilt the audio track); a second tap on a book cancels the unfinished first open; only one duration probe runs at a time.

## 1.3.3
Playback stutter, cold start and covers.
- The sound profile (speech or music audio attributes) and speed are set before the first item is loaded. Changing them after the start rebuilt the audio track and caused a hiccup at every start.
- Covers never block or compete with playback. The notification cover is a small pointer resolved later by a custom bitmap loader instead of image bytes inside every playlist item. Cover extraction and duration probing run on one low-priority thread and wait while playback starts, seeks or re-buffers.
- Resuming from the notification or a headset button builds the items on the player thread in a few lookups (no cover work, no cross-thread reads of the storage maps).
- The 10-second position autosave no longer rebuilds the whole library list on the main thread; the list refreshes on pause, seek, chapter change and when the app goes to the background.
- Position is saved every 5 seconds (the resume rewind), screen on or off, so a kill never loses more than the rewind covers. Saves go through one low-priority database thread that waits while playback starts, seeks or re-buffers; nothing is written on the UI thread.
- Dark screen: the player is not redrawn, the playback tick slows to 4 s (listening stats still count), duration probing waits for the screen, and one safe save is made when it goes off.
- The library list is built off the main thread and only while the library or settings screen is shown, so switching to the library pulls fresh data. Stores are thread-safe for that.
- The player state is rebuilt only for a real change or a 200 ms position step, and the bookmark count is cached instead of parsed on every tick.
- Language in Settings is a tap-to-cycle button like the other settings.
- History and queue use the library rows (cover, title, author, progress).
- Garbled text in the notification: ID3 chapter titles in windows-1251 are decoded correctly (cached titles are read again), and the notification shows only the metadata the app set, not the raw file tags.
- Statistics rebuilt around numbers and charts: all-time / month / week totals, streak and best streak; books finished and in progress with percent; habits (favorite time of day, average and longest session, best weekday, hours and weekday charts); calendar; period summary with top 3 books and authors. Speed statistics are gone.
- Larger read-ahead (30-60 s) and a roomier audio track buffer (0.6-1.5 s).
- Removed noise reduction and pitch everywhere (code, settings, strings, premium plan).

## 1.2.4
Fix: switching the language crashed the app (the localized context hid the activity from back handling and the folder picker).
Removed volume boost and loudness normalization (with the background analysis, battery guard and their settings). The Sound screen is the equalizer only.
Settings is one screen; About is a compact card at the bottom.

## 1.2.3
Settings controls.
- Real switches for power saving, dark theme and loudness normalization.
- Skip back and forward are tap-to-cycle buttons, like the sleep timer.

## 1.2.2
Launcher icon: the three bars above the book are replaced by the Datalou D (adaptive, round, legacy and monochrome).

## 1.2.1
Covers in the player and next to audio files.
- The player shows the real cover instead of only the placeholder.
- Images beside tracks: same name as the track (01. Name.jpg for 01. Name.mp3) and a general one (00.jpg, cover, folder, front, or a single unmatched image).
- The player prefers the track image, the library prefers the general cover.

## 1.2.0
Real playback and the Sound screen.
- Media3 player in a media session service: notification, lock screen, headset buttons.
- Chapters from m4b (Nero / QuickTime), mka and ID3 CHAP, chapter list, bookmarks.
- Sleep timer (minutes or end of chapter) with fade, speed, play modes, edge fades for albums.
- Equalizer (10 bands, presets, per book), volume boost with soft limiter, loudness normalization.
- Covers from Matroska attachments.
- Premium and billing are not ported.

## 1.1.0
Library and Settings.
- Library: folder scan, book / album grouping, search, filters, sorting, list / cards, covers, progress, edit, hide, delete, add files.
- Settings: folder, hidden books, skip intervals, theme, language, About.
- Unit tests for storage, grouping, sorting, filtering; screenshots of Library and Settings.

## 1.0.0
Start of the native rewrite.
- Project skeleton, GitHub Actions build, signing through the existing secrets.
- Theme, header, tab bar with colour emoji icons, swipe navigation.
- Player window on fake data, ru / en strings.
