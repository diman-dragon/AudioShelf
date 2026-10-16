package com.audioshelf.app;

import java.io.IOException;

/** Random access to a file's bytes (a SAF descriptor on the phone, a plain file in tests). */
interface ByteSource {
  long length() throws IOException;

  /** Reads up to len bytes at pos; returns the count, or -1 at the end. */
  int read(long pos, byte[] buf, int off, int len) throws IOException;

  /** Exactly len bytes at pos, or null when the file is shorter (or len is unreasonable). */
  default byte[] bytes(long pos, int len) throws IOException {
    if (pos < 0 || len < 0 || len > (96 << 20)) return null;
    byte[] out = new byte[len];
    int got = 0;
    while (got < len) {
      int n = read(pos + got, out, got, len - got);
      if (n <= 0) return null;
      got += n;
    }
    return out;
  }
}
