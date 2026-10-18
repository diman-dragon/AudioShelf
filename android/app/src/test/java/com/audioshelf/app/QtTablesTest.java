package com.audioshelf.app;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import org.junit.Test;

public class QtTablesTest {
  private static byte[] ints(int... values) {
    ByteBuffer b = ByteBuffer.allocate(values.length * 4);
    for (int v : values) b.putInt(v);
    return b.array();
  }

  private static byte[] box(String type, byte[] payload) {
    ByteBuffer b = ByteBuffer.allocate(8 + payload.length);
    b.putInt(8 + payload.length).put(type.getBytes()).put(payload);
    return b.array();
  }

  private static byte[] join(byte[]... parts) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (byte[] p : parts) out.write(p, 0, p.length);
    return out.toByteArray();
  }

  private static final byte[] STSZ = box("stsz", ints(0, 0, 3, 10, 20, 30));

  @Test
  public void sizesAreReadPerSample() {
    byte[] payload = ints(0, 0, 3, 10, 20, 30);
    assertArrayEquals(new long[] {10, 20, 30}, QtTables.sizes(payload, new Mp4Box(0, payload.length)));
  }

  @Test
  public void fixedSampleSizeIsRepeated() {
    byte[] payload = ints(0, 7, 4);
    assertArrayEquals(new long[] {7, 7, 7, 7}, QtTables.sizes(payload, new Mp4Box(0, payload.length)));
  }

  @Test
  public void startTimesComeFromTheTimeToSampleTable() {
    byte[] payload = ints(0, 2, 2, 1000, 1, 500);
    assertArrayEquals(new long[] {0, 1000, 2000}, QtTables.starts(payload, new Mp4Box(0, payload.length), 3, 1000));
  }

  @Test
  public void startTimesNeedAScale() {
    byte[] payload = ints(0, 1, 3, 1000);
    assertNull(QtTables.starts(payload, new Mp4Box(0, payload.length), 3, 0));
  }

  @Test
  public void offsetsFollowChunksAndSampleSizes() {
    byte[] stsc = box("stsc", ints(0, 1, 1, 2, 1));
    byte[] stco = box("stco", ints(0, 2, 1000, 2000));
    byte[] d = join(stsc, stco, STSZ);
    long[] offsets = QtTables.offsets(d, Mp4Box.whole(d), 3);
    assertArrayEquals(new long[] {1000, 1010, 2000}, offsets);
  }
}
