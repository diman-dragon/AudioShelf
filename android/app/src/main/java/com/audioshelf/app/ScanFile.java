package com.audioshelf.app;

import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import com.getcapacitor.JSObject;

/** What the scanner asks the provider for, and how a found file is described to JavaScript. */
final class ScanFile {
  static final String[] COLS = {
    Document.COLUMN_DOCUMENT_ID,
    Document.COLUMN_DISPLAY_NAME,
    Document.COLUMN_MIME_TYPE,
    Document.COLUMN_SIZE,
    Document.COLUMN_LAST_MODIFIED
  };

  private ScanFile() {}

  /** path is the folder the file lies in; the files of one folder make one book. */
  static JSObject describe(Uri tree, Cursor c, String dir, String path) {
    JSObject o = new JSObject();
    o.put("dir", path);
    o.put("dirUri", DocumentsContract.buildDocumentUriUsingTree(tree, dir).toString());
    o.put("uri", DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0)).toString());
    o.put("name", c.getString(1));
    o.put("size", c.getLong(3));
    o.put("modified", c.getLong(4));
    return o;
  }
}
