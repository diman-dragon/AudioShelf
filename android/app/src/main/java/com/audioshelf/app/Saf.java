package com.audioshelf.app;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

/** Small helpers for Storage Access Framework URIs. */
final class Saf {
  private Saf() {}

  /** Document URI of the folder itself (a tree URI points at the tree, not at its root document). */
  static Uri treeRoot(Uri tree) {
    return DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree));
  }

  /** Reads one string column of a document, or null if unavailable. */
  static String queryString(ContentResolver cr, Uri uri, String column) {
    try (Cursor c = cr.query(uri, new String[] {column}, null, null, null)) {
      if (c != null && c.moveToFirst()) return c.getString(0);
    } catch (Exception ignored) {
    }
    return null;
  }
}
