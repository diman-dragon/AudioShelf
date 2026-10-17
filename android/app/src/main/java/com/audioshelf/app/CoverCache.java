package com.audioshelf.app;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;

/** Small covers cached on disk (cache dir, the system may clear it). A file without a cover is remembered as ".none". */
final class CoverCache {
  private static final int MAX_FILES = 300;
  private static final int DROP = 60;

  private CoverCache() {}

  static String key(String uri, String sig, int width) throws Exception {
    byte[] h = MessageDigest.getInstance("SHA-1").digest((uri + "|" + sig + "|" + width).getBytes("UTF-8"));
    StringBuilder sb = new StringBuilder();
    for (byte b : h) sb.append(String.format("%02x", b));
    return sb.toString();
  }

  private static File dir(Context ctx) {
    File d = new File(ctx.getCacheDir(), "covers");
    d.mkdirs();
    return d;
  }

  /** The cached cover file (a .jpg so that the WebView takes it for an image), or null. */
  static File file(Context ctx, String key) {
    File f = new File(dir(ctx), key + ".jpg");
    return f.isFile() ? f : null;
  }

  static byte[] get(Context ctx, String key) {
    File f = file(ctx, key);
    if (f == null) return null;
    try (FileInputStream in = new FileInputStream(f)) {
      byte[] data = new byte[(int) f.length()];
      int n = 0;
      while (n < data.length) {
        int r = in.read(data, n, data.length - n);
        if (r < 0) return null;
        n += r;
      }
      return data;
    } catch (IOException e) {
      return null;
    }
  }

  static boolean none(Context ctx, String key) {
    return new File(dir(ctx), key + ".none").isFile();
  }

  static void put(Context ctx, String key, byte[] jpeg) {
    write(new File(dir(ctx), key + ".jpg"), jpeg, ctx);
  }

  static void putNone(Context ctx, String key) {
    write(new File(dir(ctx), key + ".none"), new byte[0], ctx);
  }

  private static void write(File f, byte[] data, Context ctx) {
    try (FileOutputStream out = new FileOutputStream(f)) {
      out.write(data);
    } catch (IOException ignored) {
      return;
    }
    prune(dir(ctx));
  }

  /** Keeps the cache small: past MAX_FILES the oldest files go. */
  private static void prune(File dir) {
    File[] all = dir.listFiles();
    if (all == null || all.length <= MAX_FILES) return;
    Arrays.sort(all, Comparator.comparingLong(File::lastModified));
    for (int i = 0; i < DROP && i < all.length; i++) all[i].delete();
  }
}
