package com.audioshelf.app;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import com.getcapacitor.JSArray;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

final class FolderScanner {
  interface Progress {
    void on(int files, int folders);
  }

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

  private void submit(ExecutorService pool, ContentResolver cr, Uri tree, String dir, String path, boolean root, Progress progress) {
    pending.incrementAndGet();
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

  private void readFolder(ExecutorService pool, ContentResolver cr, Uri tree, String dir, String path, boolean root, Progress progress) throws Exception {
    tally.addFolder();
    Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, dir);
    try (Cursor c = cr.query(children, ScanFile.COLS, null, null, null)) {
      if (c == null) throw new Exception("cannot read folder");
      while (!cancelled && c.moveToNext()) {
        String name = c.getString(1);
        String mime = c.getString(2);
        if (Document.MIME_TYPE_DIR.equals(mime)) {
          String sub = path.isEmpty() ? name : path + "/" + name;
          submit(pool, cr, tree, c.getString(0), sub, false, progress);
        } else if (AudioFiles.isAudio(name, mime)) {
          tally.addFile(ScanFile.describe(tree, c, dir, path));
        }
        tally.report(progress);
      }
    }
  }
}
