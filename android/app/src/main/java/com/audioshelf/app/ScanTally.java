package com.audioshelf.app;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Thread-safe counters and results of a running scan, with throttled progress reports. */
final class ScanTally {
  private static final long REPORT_EVERY_MS = 150;

  private final Queue<JSObject> found = new ConcurrentLinkedQueue<>();
  private final AtomicInteger folders = new AtomicInteger();
  private final AtomicLong lastReport = new AtomicLong();

  void addFile(JSObject file) {
    found.add(file);
  }

  void addFolder() {
    folders.incrementAndGet();
  }

  /** Reports at most every 150 ms, whichever worker thread gets there first. */
  void report(FolderScanner.Progress progress) {
    long now = System.currentTimeMillis();
    long last = lastReport.get();
    if (now - last >= REPORT_EVERY_MS && lastReport.compareAndSet(last, now)) reportNow(progress);
  }

  void reportNow(FolderScanner.Progress progress) {
    progress.on(found.size(), folders.get());
  }

  JSArray files() {
    JSArray files = new JSArray();
    for (JSObject o : found) files.put(o);
    return files;
  }
}
