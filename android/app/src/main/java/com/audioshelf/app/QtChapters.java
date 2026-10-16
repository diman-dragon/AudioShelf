package com.audioshelf.app;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** QuickTime chapter track: a text track that the audio track points to with tref/chap. */
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
    long[] sizes = sizes(d, stbl.one(d, "stsz"));
    long[] offsets = offsets(d, stbl, sizes.length);
    long[] starts = starts(d, stbl.one(d, "stts"), sizes.length, scale);
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

  private static long[] sizes(byte[] d, Mp4Box stsz) {
    if (stsz == null) return new long[0];
    ByteBuffer b = ByteBuffer.wrap(d, stsz.start, stsz.end - stsz.start).slice();
    long fixed = b.getInt(4) & 0xffffffffL;
    int count = (int) Math.min(b.getInt(8) & 0xffffffffL, 100000);
    long[] out = new long[count];
    for (int i = 0; i < count; i++) out[i] = fixed != 0 ? fixed : (12 + 4 * i + 4 <= b.limit() ? b.getInt(12 + 4 * i) & 0xffffffffL : 0);
    return out;
  }

  /** File offset of every sample, from the chunk table (stsc) and chunk offsets (stco / co64). */
  private static long[] offsets(byte[] d, Mp4Box stbl, int samples) {
    Mp4Box stsc = stbl.one(d, "stsc");
    Mp4Box stco = stbl.one(d, "stco");
    Mp4Box co64 = stbl.one(d, "co64");
    Mp4Box co = stco != null ? stco : co64;
    if (stsc == null || co == null) return null;
    ByteBuffer c = ByteBuffer.wrap(d, co.start, co.end - co.start).slice();
    int chunks = c.getInt(4);
    ByteBuffer s = ByteBuffer.wrap(d, stsc.start, stsc.end - stsc.start).slice();
    int entries = s.getInt(4);
    long[] sizes = sizes(d, stbl.one(d, "stsz"));
    long[] out = new long[samples];
    int sample = 0;
    for (int chunk = 1; chunk <= chunks && sample < samples; chunk++) {
      int per = 1;
      for (int e = 0; e < entries; e++) if ((s.getInt(8 + 12 * e) & 0xffffffffL) <= chunk) per = s.getInt(12 + 12 * e);
      long at = stco != null ? c.getInt(8 + 4 * (chunk - 1)) & 0xffffffffL : c.getLong(8 + 8 * (chunk - 1));
      for (int k = 0; k < per && sample < samples; k++) {
        out[sample] = at;
        at += sizes[sample++];
      }
    }
    return out;
  }

  private static long[] starts(byte[] d, Mp4Box stts, int samples, long scale) {
    if (stts == null || scale <= 0) return null;
    ByteBuffer b = ByteBuffer.wrap(d, stts.start, stts.end - stts.start).slice();
    int entries = b.getInt(4);
    long[] out = new long[samples];
    long time = 0;
    int i = 0;
    for (int e = 0; e < entries && i < samples; e++) {
      int count = b.getInt(8 + 8 * e);
      long delta = b.getInt(12 + 8 * e) & 0xffffffffL;
      for (int k = 0; k < count && i < samples; k++) {
        out[i++] = time * 1000 / scale;
        time += delta;
      }
    }
    return out;
  }
}
