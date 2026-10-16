package com.audioshelf.app;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Chapters of an MP4 / M4B / M4A: the Nero "chpl" list, or a QuickTime chapter track. */
final class Mp4Chapters {
  private static final int MAX_MOOV = 64 << 20;

  private Mp4Chapters() {}

  static boolean matches(ByteSource src) throws IOException {
    byte[] b = src.bytes(4, 4);
    return b != null && "ftyp".equals(new String(b, StandardCharsets.ISO_8859_1));
  }

  static List<Chapter> read(ByteSource src) throws IOException {
    long length = src.length();
    for (long p = 0; p + 8 <= length; ) {
      byte[] head = src.bytes(p, 16 <= length - p ? 16 : (int) (length - p));
      if (head == null || head.length < 8) break;
      long size = ByteBuffer.wrap(head, 0, 4).getInt() & 0xffffffffL;
      int hlen = 8;
      if (size == 1 && head.length >= 16) {
        size = ByteBuffer.wrap(head, 8, 8).getLong();
        hlen = 16;
      } else if (size == 0) {
        size = length - p;
      }
      if (size < hlen) break;
      if ("moov".equals(new String(head, 4, 4, StandardCharsets.ISO_8859_1))) {
        if (size - hlen > MAX_MOOV) break;
        byte[] moov = src.bytes(p + hlen, (int) (size - hlen));
        return moov == null ? new ArrayList<>() : fromMoov(src, moov);
      }
      p += size;
    }
    return new ArrayList<>();
  }

  private static List<Chapter> fromMoov(ByteSource src, byte[] d) throws IOException {
    Mp4Box moov = Mp4Box.whole(d);
    List<Chapter> list = Chapter.tidy(chpl(d, moov.path(d, "udta/chpl")));
    return list.isEmpty() ? Chapter.tidy(QtChapters.read(src, d, moov)) : list;
  }

  /** Nero chapter list: version+flags (4), reserved (4), count (1), then start (8, units of 100 ns), length (1), title. */
  private static List<Chapter> chpl(byte[] d, Mp4Box box) {
    List<Chapter> out = new ArrayList<>();
    if (box == null || box.end - box.start < 9) return out;
    ByteBuffer b = ByteBuffer.wrap(d, box.start, box.end - box.start);
    b.position(box.start + 8);
    int count = b.get() & 0xff;
    for (int i = 0; i < count && b.remaining() >= 9; i++) {
      long start = b.getLong() / 10_000L;
      int len = b.get() & 0xff;
      if (len > b.remaining()) break;
      byte[] t = new byte[len];
      b.get(t);
      out.add(new Chapter(new String(t, StandardCharsets.UTF_8), start));
    }
    return out;
  }
}
