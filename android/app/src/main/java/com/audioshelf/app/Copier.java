package com.audioshelf.app;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/** Copies picked files into the library folder. Run off the main thread. */
final class Copier {
  private Copier() {}

  /** Returns how many files were copied; failed copies are removed. */
  static int copyAll(ContentResolver cr, List<Uri> sources, Uri parent) {
    int added = 0;
    for (Uri src : sources) if (copyOne(cr, src, parent)) added++;
    return added;
  }

  private static boolean copyOne(ContentResolver cr, Uri src, Uri parent) {
    Uri dst = null;
    try {
      String name = Saf.queryString(cr, src, OpenableColumns.DISPLAY_NAME);
      if (name == null) name = "audio";
      // The provider appends its own extension for known MIME types, so give it the full name.
      dst = DocumentsContract.createDocument(cr, parent, AudioFiles.mimeFor(name), name);
      if (dst == null) return false;
      try (InputStream in = cr.openInputStream(src);
          OutputStream out = cr.openOutputStream(dst)) {
        byte[] buf = new byte[64 * 1024];
        int len;
        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
      }
      return true;
    } catch (Exception e) {
      if (dst != null) {
        try {
          DocumentsContract.deleteDocument(cr, dst);
        } catch (Exception ignored) {
        }
      }
      return false;
    }
  }
}
