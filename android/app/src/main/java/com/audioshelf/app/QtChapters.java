package com.audioshelf.app;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class QtChapters {
  private QtChapters() {}

  static List<Chapter> read(ByteSource src, byte[] d, Mp4Box moov) throws IOException {
    List<Chapter> out = new ArrayList<>();
    long wanted = -1;
    for (Mp4Box trak : moov.all(d, "trak")) {
      Mp4Box chap = trak.path(d, "tref/chap");
      if (chap != null && chap.end - chap.start >= 4) wanted = ByteBuffer.wrap(d, chap.start, 4).getInt() & 0xffffffffL;
    }
    if (wanted < 0) return out;
    for (Mp4Box trak : moov.all(d, "trak")) {
      Mp4Box tkhd = trak.one(d, "tkhd");
      if (tkhd == null || tkhd.end - tkhd.start < 24) continue;
      int idAt = tkhd.start + (d[tkhd.start] == 1 ? 20 : 12);
      if ((ByteBuffer.wrap(d, idAt, 4).getInt() & 0xffffffffL) == wanted) return samples(src, d, trak);
    }
    return out;
  }

  private static List<Chapter> samples(ByteSource src, byte[] d, Mp4Box trak) throws IOException {
    List<Chapter> out = new ArrayList<>();
    Mp4Box mdhd = trak.path(d, "mdia/mdhd");
    Mp4Box stbl = trak.path(d, "mdia/minf/stbl");
    if (mdhd == null || stbl == null) return out;
    long scale = ByteBuffer.wrap(d, mdhd.start + (d[mdhd.start] == 1 ? 20 : 12), 4).getInt() & 0xffffffffL;
    long[] sizes = QtTables.sizes(d, stbl.one(d, "stsz"));
    long[] offsets = QtTables.offsets(d, stbl, sizes.length);
    long[] starts = QtTables.starts(d, stbl.one(d, "stts"), sizes.length, scale);
    for (int i = 0; i < sizes.length && offsets != null && starts != null; i++) {
      byte[] raw = src.bytes(offsets[i], (int) Math.min(sizes[i], 4096));
      if (raw != null && raw.length >= 2) out.add(new Chapter(text(raw), starts[i]));
    }
    return out;
  }

  private static String text(byte[] raw) {
    int len = Math.min(((raw[0] & 0xff) << 8) | (raw[1] & 0xff), raw.length - 2);
    boolean utf16 = len >= 2 && (raw[2] & 0xff) == 0xFE && (raw[3] & 0xff) == 0xFF;
    return new String(raw, 2, len, utf16 ? StandardCharsets.UTF_16 : StandardCharsets.UTF_8).replace("﻿", "");
  }
}
