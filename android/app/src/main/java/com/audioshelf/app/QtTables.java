package com.audioshelf.app;

import java.nio.ByteBuffer;

final class QtTables {
  private QtTables() {}

  static long[] sizes(byte[] d, Mp4Box stsz) {
    if (stsz == null) return new long[0];
    ByteBuffer b = ByteBuffer.wrap(d, stsz.start, stsz.end - stsz.start).slice();
    long fixed = b.getInt(4) & 0xffffffffL;
    int count = (int) Math.min(b.getInt(8) & 0xffffffffL, 100000);
    long[] out = new long[count];
    for (int i = 0; i < count; i++) out[i] = fixed != 0 ? fixed : (12 + 4 * i + 4 <= b.limit() ? b.getInt(12 + 4 * i) & 0xffffffffL : 0);
    return out;
  }

  static long[] offsets(byte[] d, Mp4Box stbl, int samples) {
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

  static long[] starts(byte[] d, Mp4Box stts, int samples, long scale) {
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
