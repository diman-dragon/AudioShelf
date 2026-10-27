# Changelog

## 1.3.2
Playback stutter, cold start and covers.
- The sound profile (speech or music audio attributes) and speed are set before the first item is loaded. Changing them after the start rebuilt the audio track and caused a hiccup at every start.
- Covers never block or compete with playback. The notification cover is a small pointer resolved later by a custom bitmap loader instead of image bytes inside every playlist item. Cover extraction and duration probing run on one low-priority thread and wait while playback starts, seeks or re-buffers.
- Resuming from the notification or a headset button builds the items on the player thread in a few lookups (no cover work, no cross-thread reads of the storage maps).
- The 10-second position autosave no longer rebuilds the whole library list on the main thread; the list refreshes on pause, seek, chapter change and when the app goes to the background.
- Dark screen: nothing is written to the database and nothing is redrawn. Changes stay in memory, the position is saved and flushed once when the screen goes off, and everything that piled up is written when it comes on. The playback tick slows down and duration probing waits for the screen. The library and Sound lists are computed only while their screen is shown, so switching to the library pulls fresh data.
- Larger read-ahead (30-60 s) and a roomier audio track buffer (0.6-1.5 s).
- Removed noise reduction and pitch everywhere (code, settings, strings, premium plan).
- Volume boost is a tap-to-cycle setting next to skip back and forward shown as a speaker with brackets (none, 1 to 4). An old global boost is carried over; per-book boost is gone.

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
