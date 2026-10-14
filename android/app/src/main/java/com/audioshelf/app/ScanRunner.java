package com.audioshelf.app;

import android.content.ContentResolver;
import android.net.Uri;
import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;

/** Runs a folder scan on its own thread and settles the call when it ends. */
final class ScanRunner {
  private ScanRunner() {}

  static void start(
      FolderScanner scanner, ContentResolver cr, Uri tree, FolderScanner.Progress progress, PluginCall call) {
    new Thread(
            () -> {
              try {
                JSObject result = new JSObject();
                result.put("files", scanner.scan(cr, tree, progress));
                result.put("cancelled", scanner.isCancelled());
                call.resolve(result);
              } catch (Exception e) {
                call.reject("scan failed: " + e.getMessage());
              }
            })
        .start();
  }
}
