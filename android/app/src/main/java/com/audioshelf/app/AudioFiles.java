package com.audioshelf.app;

import android.webkit.MimeTypeMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Which files count as audio, and their MIME types. */
final class AudioFiles {
  // Formats Media3 can read (containers) and Android can decode out of the box: MP3/MP2, AAC (M4A, M4B, ADTS, 3GP),
  // Ogg (Vorbis, Opus, FLAC), FLAC, WAV, AMR, Matroska/WebM audio. AC-3/E-AC-3 play only where the phone has a decoder.
  private static final Set<String> EXT =
      new HashSet<>(
          Arrays.asList(
              "mp3", "mp2", "mpga", "m4a", "m4b", "m4r", "aac", "adts", "ogg", "oga", "opus", "flac", "wav", "wave",
              "mka", "weba", "amr", "awb", "3ga", "ac3", "eac3", "ec3"));

  // Audio the system labels audio/* but that Media3 cannot open (no extractor or decoder): showing them in the
  // library would only lead to a playback error, so they are skipped.
  private static final Set<String> UNPLAYABLE =
      new HashSet<>(
          Arrays.asList(
              "wma", "asf", "aif", "aiff", "aifc", "au", "snd", "mid", "midi", "kar", "xmf", "rtttl", "rtx", "ota",
              "imy", "dts", "dtshd", "ape", "wv", "tta", "mpc", "dsf", "dff", "ra", "ram", "caf", "spx"));

  private AudioFiles() {}

  static String extension(String name) {
    int dot = name == null ? -1 : name.lastIndexOf('.');
    return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
  }

  static boolean isAudio(String name, String mime) {
    String ext = extension(name);
    if (UNPLAYABLE.contains(ext)) return false;
    if (mime != null && mime.startsWith("audio/")) return true;
    return EXT.contains(ext);
  }

  /** MIME by extension; falls back to a generic audio type. */
  static String mimeFor(String name) {
    String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension(name));
    return mime != null && mime.startsWith("audio/") ? mime : "audio/*";
  }
}
