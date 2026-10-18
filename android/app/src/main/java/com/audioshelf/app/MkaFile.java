package com.audioshelf.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class MkaFile {
  private static final long SEGMENT = 0x18538067L;
  private static final long ATTACHMENTS = 0x1941A469L;
  private static final long FILE = 0x61A7L;
  private static final long NAME = 0x466EL;
  private static final long MIME = 0x4660L;
  private static final long DATA = 0x465CL;
  private static final long MAX_PICTURE = 8L << 20;

  List<Chapter> chapters = new ArrayList<>();
  byte[] cover;

  static boolean matches(ByteSource src) throws IOException {
    byte[] b = src.bytes(0, 4);
    return b != null && (b[0] & 0xff) == 0x1A && (b[1] & 0xff) == 0x45 && (b[2] & 0xff) == 0xDF && (b[3] & 0xff) == 0xA3;
  }

  static MkaFile read(ByteSource src, boolean wantCover) throws IOException {
    MkaFile out = new MkaFile();
    long length = src.length();
    Ebml.Element head = Ebml.at(src, 0);
    long p = head == null || head.size == Ebml.UNKNOWN ? length : head.end(length);
    Ebml.Element segment = Ebml.at(src, p);
    if (segment == null || segment.id != SEGMENT) return out;
    long end = segment.end(length);
    boolean gotChapters = false;
    boolean gotCover = !wantCover;
    p = segment.dataPos;
    for (int guard = 0; p < end && guard < 400000 && !(gotChapters && gotCover); guard++) {
      Ebml.Element e = Ebml.at(src, p);
      if (e == null || e.size == Ebml.UNKNOWN) break;
      if (e.id == MkaChapters.chaptersId() && !gotChapters) {
        out.chapters = MkaChapters.parse(src, e);
        gotChapters = true;
      } else if (e.id == ATTACHMENTS && !gotCover) {
        out.cover = cover(src, e);
        gotCover = true;
      }
      p = e.end(end);
    }
    return out;
  }

  private static byte[] cover(ByteSource src, Ebml.Element attachments) throws IOException {
    byte[] best = null;
    boolean bestNamed = false;
    long end = attachments.end(src.length());
    for (long p = attachments.dataPos; p < end; ) {
      Ebml.Element file = Ebml.at(src, p);
      if (file == null || file.size == Ebml.UNKNOWN) break;
      if (file.id == FILE) {
        String name = "";
        String mime = "";
        Ebml.Element data = null;
        for (long q = file.dataPos; q < file.end(end); ) {
          Ebml.Element e = Ebml.at(src, q);
          if (e == null || e.size == Ebml.UNKNOWN) break;
          if (e.id == NAME) name = Ebml.text(src, e).toLowerCase();
          else if (e.id == MIME) mime = Ebml.text(src, e).toLowerCase();
          else if (e.id == DATA) data = e;
          q = e.end(file.end(end));
        }
        boolean image = mime.startsWith("image/") || name.matches(".*\\.(jpe?g|png|webp)$");
        boolean named = name.contains("cover") || name.contains("folder") || name.contains("front");
        if (image && data != null && data.size > 0 && data.size <= MAX_PICTURE && (best == null || (named && !bestNamed))) {
          best = src.bytes(data.dataPos, (int) data.size);
          bestNamed = named;
        }
      }
      p = file.end(end);
    }
    return best;
  }
}
