package com.audioshelf.app;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import java.util.ArrayDeque;
import java.util.Deque;

/** Walks a SAF folder tree (subfolders included) and collects audio files, reporting progress. */
final class FolderScanner {
  interface Progress {
    void on(int files, int folders);
  }

  private static final String[] COLS = {
    Document.COLUMN_DOCUMENT_ID,
    Document.COLUMN_DISPLAY_NAME,
    Document.COLUMN_MIME_TYPE,
    Document.COLUMN_SIZE,
    Document.COLUMN_LAST_MODIFIED
  };
  private static final long REPORT_EVERY_MS = 150;

  private volatile boolean cancelled;

  void cancel() {
    cancelled = true;
  }

  boolean isCancelled() {
    return cancelled;
  }

  /** The root must be readable (otherwise throws); unreadable subfolders are skipped. */
  JSArray scan(ContentResolver cr, Uri tree, Progress progress) throws Exception {
    JSArray files = new JSArray();
    Deque<String> dirs = new ArrayDeque<>();
    dirs.push(DocumentsContract.getTreeDocumentId(tree));
    int found = 0;
    int folders = 0;
    long lastReport = 0;
    while (!dirs.isEmpty() && !cancelled) {
      boolean root = folders == 0;
      folders++;
      Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, dirs.pop());
      try (Cursor c = cr.query(children, COLS, null, null, null)) {
        if (c == null && root) throw new Exception("cannot read folder");
        while (c != null && !cancelled && c.moveToNext()) {
          String name = c.getString(1);
          String mime = c.getString(2);
          if (Document.MIME_TYPE_DIR.equals(mime)) {
            dirs.push(c.getString(0));
          } else if (AudioFiles.isAudio(name, mime)) {
            JSObject o = new JSObject();
            o.put("uri", DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0)).toString());
            o.put("name", name);
            o.put("size", c.getLong(3));
            o.put("modified", c.getLong(4));
            files.put(o);
            found++;
          }
          long now = System.currentTimeMillis();
          if (now - lastReport >= REPORT_EVERY_MS) {
            lastReport = now;
            progress.on(found, folders);
          }
        }
      } catch (Exception e) {
        if (root) throw e;
      }
    }
    progress.on(found, folders);
    return files;
  }
}
