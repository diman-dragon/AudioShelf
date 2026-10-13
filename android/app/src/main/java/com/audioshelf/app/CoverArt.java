package com.audioshelf.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Base64;
import java.io.ByteArrayOutputStream;

/** Embedded cover from audio tags, as a small JPEG data URL. */
final class CoverArt {
  private CoverArt() {}

  static String read(Context ctx, Uri uri, int width) throws Exception {
    return "data:image/jpeg;base64," + Base64.encodeToString(jpeg(ctx, uri, width), Base64.NO_WRAP);
  }

  /** Small JPEG of the embedded cover (also used as notification artwork). */
  static byte[] jpeg(Context ctx, Uri uri, int width) throws Exception {
    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
    try {
      mmr.setDataSource(ctx, uri);
      byte[] raw = mmr.getEmbeddedPicture();
      if (raw == null) throw new Exception("no cover");
      Bitmap bmp = decode(raw, width);
      if (bmp == null) throw new Exception("bad cover");
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      bmp.compress(Bitmap.CompressFormat.JPEG, 82, out);
      bmp.recycle();
      return out.toByteArray();
    } finally {
      try {
        mmr.release();
      } catch (Exception ignored) {
      }
    }
  }

  private static Bitmap decode(byte[] raw, int width) {
    BitmapFactory.Options bounds = new BitmapFactory.Options();
    bounds.inJustDecodeBounds = true;
    BitmapFactory.decodeByteArray(raw, 0, raw.length, bounds);
    int sample = 1;
    while (bounds.outWidth / (sample * 2) >= width) sample *= 2;
    BitmapFactory.Options opts = new BitmapFactory.Options();
    opts.inSampleSize = sample;
    return BitmapFactory.decodeByteArray(raw, 0, raw.length, opts);
  }
}
