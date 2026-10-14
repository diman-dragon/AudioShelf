package com.audioshelf.app;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import android.provider.OpenableColumns;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/** Copies picked files into the library folder. Run off the main thread. */
final class Copier {
  private Copier() {}

  /** Returns the files that were copied (uri, name, size, modified); failed copies are removed. */
  static JSArray copyAll(ContentResolver cr, List<Uri> sources, Uri parent) {
    JSArray added = new JSArray();
    for (Uri src : sources) {
      JSObject file = copyOne(cr, src, parent);
      if (file != null) added.put(file);
    }
    return added;
  }

  private static JSObject copyOne(ContentResolver cr, Uri src, Uri parent) {
    Uri dst = null;
    try {
      String name = Saf.queryString(cr, src, OpenableColumns.DISPLAY_NAME);
      if (name == null) name = "audio";
      // The provider appends its own extension for known MIME types, so give it the full name.
      dst = DocumentsContract.createDocument(cr, parent, AudioFiles.mimeFor(name), name);
      if (dst == null) return null;
      try (InputStream in = cr.openInputStream(src);
          OutputStream out = cr.openOutputStream(dst)) {
        byte[] buf = new byte[64 * 1024];
        int len;
        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
      }
      return describe(cr, dst);
    } catch (Exception e) {
      if (dst != null) {
        try {
          DocumentsContract.deleteDocument(cr, dst);
        } catch (Exception ignored) {
        }
      }
      return null;
    }
  }

  private static JSObject describe(ContentResolver cr, Uri doc) {
    String[] cols = {Document.COLUMN_DISPLAY_NAME, Document.COLUMN_SIZE, Document.COLUMN_LAST_MODIFIED};
    try (Cursor c = cr.query(doc, cols, null, null, null)) {
      if (c == null || !c.moveToFirst()) return null;
      JSObject o = new JSObject();
      o.put("uri", doc.toString());
      o.put("name", c.getString(0));
      o.put("size", c.getLong(1));
      o.put("modified", c.getLong(2));
      return o;
    }
  }
}
