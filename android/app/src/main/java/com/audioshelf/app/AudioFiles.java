package com.audioshelf.app;

import android.webkit.MimeTypeMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Which files count as audio, and their MIME types. */
final class AudioFiles {
  private static final Set<String> EXT =
      new HashSet<>(
          Arrays.asList(
              "mp3", "m4a", "m4b", "aac", "ogg", "oga", "opus", "flac", "wav", "wma", "aif", "aiff",
              "mka"));

  private AudioFiles() {}

  static String extension(String name) {
    int dot = name == null ? -1 : name.lastIndexOf('.');
    return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
  }

  static boolean isAudio(String name, String mime) {
    if (mime != null && mime.startsWith("audio/")) return true;
    return EXT.contains(extension(name));
  }

  /** MIME by extension; falls back to a generic audio type. */
  static String mimeFor(String name) {
    String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension(name));
    return mime != null && mime.startsWith("audio/") ? mime : "audio/*";
  }
}
