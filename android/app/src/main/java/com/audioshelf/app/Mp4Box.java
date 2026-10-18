package com.audioshelf.app;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class Mp4Box {
  final int start;
  final int end;

  Mp4Box(int start, int end) {
    this.start = start;
    this.end = end;
  }

  static Mp4Box whole(byte[] d) {
    return new Mp4Box(0, d.length);
  }

  List<Mp4Box> all(byte[] d, String type) {
    List<Mp4Box> out = new ArrayList<>();
    for (int p = start; p + 8 <= end; ) {
      long size = ((long) (d[p] & 0xff) << 24) | ((d[p + 1] & 0xff) << 16) | ((d[p + 2] & 0xff) << 8) | (d[p + 3] & 0xff);
      int head = 8;
      if (size == 1 && p + 16 <= end) {
        size = ByteBuffer.wrap(d, p + 8, 8).getLong();
        head = 16;
      } else if (size == 0) {
        size = end - p;
      }
      if (size < head || p + size > end) break;
      if (new String(d, p + 4, 4, StandardCharsets.ISO_8859_1).equals(type)) out.add(new Mp4Box(p + head, (int) (p + size)));
      p += (int) size;
    }
    return out;
  }

  Mp4Box one(byte[] d, String type) {
    List<Mp4Box> list = all(d, type);
    return list.isEmpty() ? null : list.get(0);
  }

  Mp4Box path(byte[] d, String path) {
    Mp4Box box = this;
    for (String part : path.split("/")) {
      box = box == null ? null : box.one(d, part);
    }
    return box;
  }
}
