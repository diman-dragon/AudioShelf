package com.audioshelf.app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Minimal EBML (Matroska) element reading. */
final class Ebml {
  static final long UNKNOWN = -1;

  /** An element header: id, where its data starts, and its size (UNKNOWN for live streams). */
  static final class Element {
    final long id;
    final long dataPos;
    final long size;

    Element(long id, long dataPos, long size) {
      this.id = id;
      this.dataPos = dataPos;
      this.size = size;
    }

    long end(long limit) {
      return size == UNKNOWN ? limit : Math.min(limit, dataPos + size);
    }
  }

  private Ebml() {}

  static Element at(ByteSource src, long pos) throws IOException {
    byte[] b = src.bytes(pos, 1);
    if (b == null) return null;
    int idLen = Integer.numberOfLeadingZeros((b[0] & 0xff) << 24) + 1;
    if (idLen > 4) return null;
    byte[] idBytes = src.bytes(pos, idLen);
    byte[] first = src.bytes(pos + idLen, 1);
    if (idBytes == null || first == null) return null;
    long id = 0;
    for (byte x : idBytes) id = (id << 8) | (x & 0xff);
    int sizeLen = Integer.numberOfLeadingZeros((first[0] & 0xff) << 24) + 1;
    if (sizeLen > 8) return null;
    byte[] sizeBytes = src.bytes(pos + idLen, sizeLen);
    if (sizeBytes == null) return null;
    long size = (sizeBytes[0] & 0xff) & ((1 << (8 - sizeLen)) - 1);
    boolean allOnes = size == ((1 << (8 - sizeLen)) - 1);
    for (int i = 1; i < sizeLen; i++) {
      int v = sizeBytes[i] & 0xff;
      size = (size << 8) | v;
      allOnes &= v == 0xff;
    }
    return new Element(id, pos + idLen + sizeLen, allOnes ? UNKNOWN : size);
  }

  static long uint(ByteSource src, Element e) throws IOException {
    byte[] b = e.size > 8 || e.size < 0 ? null : src.bytes(e.dataPos, (int) e.size);
    long v = 0;
    if (b != null) for (byte x : b) v = (v << 8) | (x & 0xff);
    return v;
  }

  static String text(ByteSource src, Element e) throws IOException {
    byte[] b = e.size < 0 || e.size > 4096 ? null : src.bytes(e.dataPos, (int) e.size);
    return b == null ? "" : new String(b, StandardCharsets.UTF_8).replace("\0", "");
  }
}
