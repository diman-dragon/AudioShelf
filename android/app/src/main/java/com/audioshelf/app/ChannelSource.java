package com.audioshelf.app;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/** A content:// file opened for random reading. */
final class ChannelSource implements ByteSource, Closeable {
  private final ParcelFileDescriptor pfd;
  private final FileInputStream in;
  private final FileChannel channel;

  private ChannelSource(ParcelFileDescriptor pfd) {
    this.pfd = pfd;
    this.in = new FileInputStream(pfd.getFileDescriptor());
    this.channel = in.getChannel();
  }

  static ChannelSource open(Context ctx, Uri uri) throws IOException {
    ParcelFileDescriptor pfd = ctx.getContentResolver().openFileDescriptor(uri, "r");
    if (pfd == null) throw new IOException("cannot open " + uri);
    return new ChannelSource(pfd);
  }

  @Override
  public long length() throws IOException {
    return channel.size();
  }

  @Override
  public int read(long pos, byte[] buf, int off, int len) throws IOException {
    return channel.read(ByteBuffer.wrap(buf, off, len), pos);
  }

  @Override
  public void close() {
    try {
      in.close();
      pfd.close();
    } catch (IOException ignored) {
      // nothing left to release
    }
  }
}
