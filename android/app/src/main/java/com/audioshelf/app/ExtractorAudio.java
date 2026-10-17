package com.audioshelf.app;

import android.media.MediaExtractor;
import android.media.MediaFormat;

/** Picking the audio track of a file and reading its basic parameters. */
final class ExtractorAudio {
  private ExtractorAudio() {}

  /** Selects the first audio track; null if there is none. */
  static MediaFormat select(MediaExtractor extractor) {
    for (int i = 0; i < extractor.getTrackCount(); i++) {
      MediaFormat f = extractor.getTrackFormat(i);
      String mime = f.getString(MediaFormat.KEY_MIME);
      if (mime != null && mime.startsWith("audio/")) {
        extractor.selectTrack(i);
        return f;
      }
    }
    return null;
  }

  /** Where to measure: the middle of a long file (the start often has an intro or music), otherwise the beginning. */
  static long windowStartUs(MediaFormat f, long windowUs) {
    long total = f.containsKey(MediaFormat.KEY_DURATION) ? f.getLong(MediaFormat.KEY_DURATION) : 0;
    return total > 3 * windowUs ? total / 2 - windowUs / 2 : 0;
  }

  static int rate(MediaFormat f) {
    return f.containsKey(MediaFormat.KEY_SAMPLE_RATE) ? f.getInteger(MediaFormat.KEY_SAMPLE_RATE) : 44100;
  }

  static int channels(MediaFormat f) {
    return f.containsKey(MediaFormat.KEY_CHANNEL_COUNT) ? f.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 2;
  }
}
