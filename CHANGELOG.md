# Changelog

## 1.4.15
New app icon; the logs of 20-27 November (1.4.14) read and checked.
- Icon: the sand-coloured headphones-and-book icon replaces the navy one. Adaptive icon (background `#F3E7CF`, foreground with the art inside the 66 dp safe zone), monochrome layer for themed icons (Android 13+), and the legacy square and round icons for all five densities. The art was cut out of the supplied 1024 px image with its transparency recovered, so it sits on the background without a halo.
- Logs, 20-27 November (1.4.14, vivo V2025, Android 13): no error lines, no crashes, no ANR; 9 starts of the process, all clean. The only warnings are slow database writes (54-116 ms, background thread) and two main-thread pauses of 378-450 ms. 19 stutter bursts, all on the JLab GO Air Sport over A2DP, screen off, phone charging, buffer full (2000 ms) and the feed gaps 0-176 ms: the same picture as in 1.4.13, the headphones' link, not the app. No code change for it.
- Checked: English and Russian strings match (260 each), every `R.string` used in code is defined, no TODO / FIXME left in the sources.
- Not compiled here (no Android SDK): only resources changed in this version.

## 1.4.14
Debt from the audits that can be closed without guessing.
- Deleting a book: when a later file of a book fails to delete, the files that are already gone are removed from the library too (before, the exception left them listed until the next scan and their progress in place). A book is forgotten (meta, progress, duration, hidden mark) only when everything was deleted; a folder book whose folder could not be deleted stays unchanged.
- `SqliteKv.onUpgrade` no longer does nothing silently: it writes a warning (there is no migration yet, the database is version 1).
- The screenshot test no longer swallows a `ComposeTimeoutException` without a trace: the skipped screenshot is named in the test output.
- Not done on purpose: renaming (file rename, then the moves of meta, progress, duration, hidden in four stores) is still not one transaction; making it one needs the stores to share a write batch, a larger change that cannot be checked without a build.
- Not compiled here (no Android SDK).

## 1.4.13
From the end of the 16 November log (1.4.8): three stutter episodes with the screen off, the phone charging and the JLab GO Air Sport on AAC (19:25:32-46, 22:46:37-45 and 22:47:20-30, 22:49:17-35; 291 underruns in the last one) and the Bluetooth link dropping 14 s after the last of them (22:49:49). In all of them the buffer was full (2000 ms, 35-50 s ahead) and the feed gaps small, the CPU 16-32%, the main thread idle: the app fed the sound in time, the headphones' link did not take it. Not an app-side stall.
- Every underrun burst now opens an `AudioTrace` window (route, devices, players, volumes, A2DP/SCO state, 4 times a second), so the next stutter comes with the full picture of the link around it.
- Not compiled here (no Android SDK).

## 1.4.12
Full audio-route logging, and a fix after reading the logs of 6-16 November (all from 1.4.8).
- Seen in all the logs: 33 of 40 notifications with the book playing (39 of them on the JLab GO Air Sport, 33 of those 39; the one on the KP-599 had none; the three of 16 November 16:29-16:41 had none on the JLab either) were followed, about 1 s later, by the A2DP stream being suspended until the notification ended (3-7 s). Low battery is no different from other notifications. The logs did not say where the book's sound goes in that time.
- New `AudioTrace` (`diag/AudioTrace.kt`): a window opens on a focus loss, on an A2DP stream change while the book plays, or when another app starts a notification / call / alarm / navigation stream. In the window, 4 times a second (a line on a change, one a second otherwise): the book (playing, position, volume, session, attributes), the devices the system would use for media, a notification and a notification event (Android 13+), every audio player on the phone with its owner, state and the device it is routed to (from the system's own description of the player, also written raw at the start), the volumes and mutes of the streams, the mode, the ringer, speakerphone, the communication device, the A2DP and SCO state. Events (A2DP, SCO, volume, mute, mode, headset, Bluetooth link) are written with their milliseconds since the start; the window ends with a summary of the events and of the route changes.
- `AudioStreams` now writes the system's text of each player (device id, owner, state) on every change of the players.
- After a call the book is no longer rewound twice: the extra 5 s seek of 1.4.10 is removed, `ResumeRewind` already goes 5 s back after any stop of 3 s or more (seen in the log of 16 November, a call over Bluetooth: pause 0.4 s after the focus loss, 5 s back on the end).
- Not compiled here (no Android SDK): `AudioTrace`, `HeadphonePin`, `FocusPolicy`. The system's text of a player (`deviceId:`, `u/pid:`, `state:`) is read with regular expressions; if the format differs on the phone, the raw text in the log still shows everything.

## 1.4.11
Found in the logs of 11-14 November (1.4.8): one notification on 13 November, 20:44, with JLab GO Air Sport. The notification took the focus at 20:44:28, the A2DP stream was suspended at 20:44:29 and came back at 20:44:34 (when the notification ended) while the book went on playing; this is the "the sound moves to the phone and comes back" the user hears. The other 16 notifications in the logs show no such thing.
- Experimental: the book is pinned to the Bluetooth headphones (`HeadphonePin`, `ExoPlayer.setPreferredAudioDevice`) for as long as they are connected, so that the system has no reason to move it to the speaker under a notification. Without the headphones the preference is dropped and the output is chosen by the system as before. Whether the phone (vivo, Android 13) honours it under a notification is not known: if it does not, the log of the episode will say so (`HeadphonePin`, `AudioDevices`, `Bluetooth`, `Focus` lines).
- Not compiled here (no Android SDK).

## 1.4.10
Behaviour around notifications and calls.
- Notifications of any kind (messages, social networks, low battery and the like) no longer touch the book: it is not paused, not lowered and not rewound. The two settings that existed for this ("Lower the book for notifications", "Back 10 s after a notification") are removed together with their code, strings and the 0.2 volume factor. Saved values of the two settings stay in the database unused. The loudness of the notification sound itself is the system's, not the app's.
- Voice calls (phone, and VoIP such as Telegram or WhatsApp): the book pauses as soon as the call rings, stays paused for the whole call, and after the hang-up goes back 5 seconds and plays on. The call is now also recognised by what plays on the phone (ringtone / voice-call stream), not only by the audio mode, for every kind of focus loss, so a VoIP ring that takes only a short focus is caught too.
- Another app that takes the audio for good (a video, a music player) still pauses the book and it stays paused.
- Cleanup: removed `ComingSoonScreen` and its two strings (only a screenshot test used it), `Fx.nextGain()` and `EqStore.moveTrack()` (only tests called them), an unused import in `EqStore.kt`; `ArraySource` moved from main to the test sources. Checked and left: `StatsReport.finishedBooks` is computed and tested but not shown anywhere (the data is recorded; removing it touches the stored statistics).
- Not compiled here (no Android SDK): `FocusPolicy`, the settings screen and the tests need the CI build to confirm.

## 1.4.9
Found in the logs of 11-13 November (1.4.8, 7 hours on Bluetooth A2DP, 3.8 of them with the screen off).
- Log: `boost follows the limiter` was written 4800 times in a day (a quarter of the log): on speech the cut moved between 0.45 and 0.5 dB and the rounded value flipped between -1 and -0 every quarter of a second. It is now written only when the cut has moved by a whole dB since the last line, and shows one decimal.
- Seen and working: no underruns in 13000 beats (7 hours on KP-599 and JLab GO Air Sport, the battery limit lifted); the boost cut was 0 dB half of the time, 0.7 dB at the 90th percentile, 3.2 dB at most (boost 3 dB, 6 dB); the limiter hit more than 0.2% of the samples in 112 of 13000 beats (before: thousands of hits per beat); no error lines.

## 1.4.8
From a second review of 1.4.7.
- Sound: the shown boost cut now follows a change of the boost setting at once (it could show the old value), and tests cover stereo frames, a lowered boost and a new track keeping the cut. The first quarter of a second after the app starts or the boost is raised is still limited by the soft limiter alone, until the first window is measured.
- Cleanup: unused imports (`Uri` in `SiblingFiles.kt` and `LibraryRepo.kt`, `update` in `Kv.kt`) and the unused `AppColors.isLight`.
- Checked and left as it is: `flushNow()` held the main thread once in three days of logs (65 ms), the slow writes (50-650 ms) are on the database thread, so it is not moved off the main thread; the focus flag and the A2DP debounce (see 1.4.7).

## 1.4.7
Found in the logs of 6-10 November (about 20 hours on a Bluetooth headset, 4 stutter episodes, loud music with the boost on).
- Sound: the boost is now adaptive. When the signal keeps reaching the limiter (loud music, a clipped file) the boost gives way by up to its own size and comes back slowly when the signal is quiet again; quiet speech keeps all of it. The gain is smoothed, so the change is not heard. The `beat` line shows `boost-cut=X.XdB`, and `boost follows the limiter: ...` is written when the cut changes by a whole dB. Before this, a +6 or +9 dB boost on loud material pushed thousands of samples a second into the limiter (up to 6500 in 2 seconds on 6 November).
- Log: `audio track created` now says the channels and the buffer in seconds (22050 Hz mono in 88200 bytes is 2 s, not 1 s).
- Battery: an underrun with the screen off is remembered, and Settings under "Screen-off playback" says whether the battery limit is lifted, still on, or on while a stutter was recorded.
- Focus: `setWillPauseWhenDucked(true)` stays, with the reason written next to it (with `false` the system lowers music by itself and does not tell the app).
- Seen: all 4 stutter episodes were on Bluetooth A2DP with the screen off and 30-90 s of audio buffered, so the app was not starving; the phone's threads were delayed for 30-60 s before each one. Only one episode followed an A2DP suspend after a notification.

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
