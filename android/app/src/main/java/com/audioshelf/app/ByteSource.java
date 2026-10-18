package com.audioshelf.app;

import java.io.IOException;

interface ByteSource {
  long length() throws IOException;

  int read(long pos, byte[] buf, int off, int len) throws IOException;

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
