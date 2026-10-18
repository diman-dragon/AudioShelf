package com.audioshelf.app;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

final class Saf {
  private Saf() {}

  static Uri treeRoot(Uri tree) {
    return DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree));
  }

  static String queryString(ContentResolver cr, Uri uri, String column) {
    try (Cursor c = cr.query(uri, new String[] {column}, null, null, null)) {
      if (c != null && c.moveToFirst()) return c.getString(0);
    } catch (Exception ignored) {
    }
    return null;
  }
}
