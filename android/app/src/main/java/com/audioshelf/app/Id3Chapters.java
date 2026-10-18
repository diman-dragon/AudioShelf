package com.audioshelf.app;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class Id3Chapters {
  private static final int MAX_TAG = 32 << 20;

  private Id3Chapters() {}

  static boolean matches(ByteSource src) throws IOException {
    byte[] b = src.bytes(0, 3);
    return b != null && b[0] == 'I' && b[1] == 'D' && b[2] == '3';
  }

  static List<Chapter> read(ByteSource src) throws IOException {
    List<Chapter> out = new ArrayList<>();
    byte[] h = src.bytes(0, 10);
    if (h == null) return out;
    int version = h[3] & 0xff;
    int size = syncsafe(h, 6);
    if ((version != 3 && version != 4) || size <= 0 || size > MAX_TAG) return out;
    byte[] d = src.bytes(10, size);
    if (d == null) return out;
    int p = 0;
    if ((h[5] & 0x40) != 0 && d.length >= 4) p = version == 4 ? syncsafe(d, 0) : 4 + beInt(d, 0);
    while (p + 10 <= d.length && d[p] != 0) {
      int len = version == 4 ? syncsafe(d, p + 4) : beInt(d, p + 4);
      if (len < 0 || p + 10 + len > d.length) break;
      if (d[p] == 'C' && d[p + 1] == 'H' && d[p + 2] == 'A' && d[p + 3] == 'P') chapter(d, p + 10, p + 10 + len, version, out);
      p += 10 + len;
    }
    return Chapter.tidy(out);
  }

  private static void chapter(byte[] d, int from, int to, int version, List<Chapter> out) {
    int p = from;
    while (p < to && d[p] != 0) p++;
    p++;
    if (p + 16 > to) return;
    long start = beInt(d, p) & 0xffffffffL;
    p += 16;
    String title = "";
    while (p + 10 <= to) {
      int len = version == 4 ? syncsafe(d, p + 4) : beInt(d, p + 4);
      if (len < 1 || p + 10 + len > to) break;
      if (d[p] == 'T' && d[p + 1] == 'I' && d[p + 2] == 'T' && d[p + 3] == '2') title = text(d, p + 10, p + 10 + len);
      p += 10 + len;
    }
    out.add(new Chapter(title, start));
  }

  private static String text(byte[] d, int from, int to) {
    Charset cs = new Charset[] {StandardCharsets.ISO_8859_1, StandardCharsets.UTF_16, StandardCharsets.UTF_16BE, StandardCharsets.UTF_8}[Math.min(3, d[from] & 0xff)];
    return new String(d, from + 1, to - from - 1, cs).replace("﻿", "").replace("\0", "");
  }

  private static int syncsafe(byte[] d, int at) {
    return ((d[at] & 0x7f) << 21) | ((d[at + 1] & 0x7f) << 14) | ((d[at + 2] & 0x7f) << 7) | (d[at + 3] & 0x7f);
  }

  private static int beInt(byte[] d, int at) {
    return ((d[at] & 0xff) << 24) | ((d[at + 1] & 0xff) << 16) | ((d[at + 2] & 0xff) << 8) | (d[at + 3] & 0xff);
  }
}
