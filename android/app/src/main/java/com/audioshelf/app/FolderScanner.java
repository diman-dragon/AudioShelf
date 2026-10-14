package com.audioshelf.app;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Walks a SAF folder tree (subfolders included) on a small thread pool: every folder is its own task,
 * so slow provider calls overlap. Reports progress while it works.
 */
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
  private static final int THREADS = 4;

  private volatile boolean cancelled;
  private final ScanTally tally = new ScanTally();
  private final AtomicInteger pending = new AtomicInteger();
  private final AtomicReference<Exception> rootError = new AtomicReference<>();
  private final CountDownLatch done = new CountDownLatch(1);

  void cancel() {
    cancelled = true;
  }

  boolean isCancelled() {
    return cancelled;
  }

  /** The root must be readable (otherwise throws); unreadable subfolders are skipped. */
  JSArray scan(ContentResolver cr, Uri tree, Progress progress) throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(THREADS);
    try {
      submit(pool, cr, tree, DocumentsContract.getTreeDocumentId(tree), "", true, progress);
      done.await();
    } finally {
      pool.shutdownNow();
    }
    if (rootError.get() != null) throw rootError.get();
    tally.reportNow(progress);
    return tally.files();
  }

  /** dir is the document id, path is the folder path relative to the chosen root ("" for the root). */
  private void submit(
      ExecutorService pool,
      ContentResolver cr,
      Uri tree,
      String dir,
      String path,
      boolean root,
      Progress progress) {
    pending.incrementAndGet(); // counted before the parent task ends, so it can't reach 0 early
    pool.execute(
        () -> {
          try {
            if (!cancelled) readFolder(pool, cr, tree, dir, path, root, progress);
          } catch (Exception e) {
            if (root) rootError.set(e);
          } finally {
            if (pending.decrementAndGet() == 0) done.countDown();
          }
        });
  }

  private void readFolder(
      ExecutorService pool,
      ContentResolver cr,
      Uri tree,
      String dir,
      String path,
      boolean root,
      Progress progress)
      throws Exception {
    tally.addFolder();
    Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, dir);
    try (Cursor c = cr.query(children, COLS, null, null, null)) {
      if (c == null) throw new Exception("cannot read folder");
      while (!cancelled && c.moveToNext()) {
        String name = c.getString(1);
        String mime = c.getString(2);
        if (Document.MIME_TYPE_DIR.equals(mime)) {
          String sub = path.isEmpty() ? name : path + "/" + name;
          submit(pool, cr, tree, c.getString(0), sub, false, progress);
        } else if (AudioFiles.isAudio(name, mime)) {
          tally.addFile(describe(tree, c, dir, path));
        }
        tally.report(progress);
      }
    }
  }

  private static JSObject describe(Uri tree, Cursor c, String dir, String path) {
    JSObject o = new JSObject();
    o.put("dir", path); // the folder the file lies in; files of one folder make one book
    o.put("dirUri", DocumentsContract.buildDocumentUriUsingTree(tree, dir).toString());
    o.put("uri", DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0)).toString());
    o.put("name", c.getString(1));
    o.put("size", c.getLong(3));
    o.put("modified", c.getLong(4));
    return o;
  }
}
