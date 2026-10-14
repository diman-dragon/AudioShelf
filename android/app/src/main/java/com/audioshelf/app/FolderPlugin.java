package com.audioshelf.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract.Document;
import androidx.activity.result.ActivityResult;
import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

/** Audio folder via Storage Access Framework: pick a folder and list its audio files. */
@CapacitorPlugin(name = "Folder")
public class FolderPlugin extends SafPlugin {

  private volatile FolderScanner scanner;

  @PluginMethod
  public void pickFolder(PluginCall call) {
    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
    i.addFlags(
        Intent.FLAG_GRANT_READ_URI_PERMISSION
            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
    startActivityForResult(call, i, "pickFolderResult");
  }

  @ActivityCallback
  private void pickFolderResult(PluginCall call, ActivityResult result) {
    if (call == null) return;
    Intent data = result.getData();
    if (result.getResultCode() != Activity.RESULT_OK || data == null || data.getData() == null) {
      call.reject("cancelled");
      return;
    }
    Uri tree = data.getData();
    cr().takePersistableUriPermission(
        tree, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    String name = Saf.queryString(cr(), Saf.treeRoot(tree), Document.COLUMN_DISPLAY_NAME);
    JSObject o = new JSObject();
    o.put("uri", tree.toString());
    o.put("name", name == null ? "" : name);
    call.resolve(o);
  }

  /** Scans the folder and its subfolders in the background; progress arrives as "scan" events. */
  @PluginMethod
  public void scan(PluginCall call) {
    String tree = need(call, "tree");
    if (tree == null) return;
    if (scanner != null) scanner.cancel();
    scanner = new FolderScanner();
    ScanRunner.start(
        scanner,
        cr(),
        Uri.parse(tree),
        (found, folders) -> {
          JSObject p = new JSObject();
          p.put("files", found);
          p.put("folders", folders);
          notifyListeners("scan", p);
        },
        call);
  }

  /** The folder is no longer tracked: give the system permission back. */
  @PluginMethod
  public void release(PluginCall call) {
    String tree = need(call, "uri");
    if (tree == null) return;
    try {
      cr().releasePersistableUriPermission(
          Uri.parse(tree), Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    } catch (Exception ignored) {
      // permission was already gone
    }
    call.resolve();
  }

  @PluginMethod
  public void cancelScan(PluginCall call) {
    if (scanner != null) scanner.cancel();
    call.resolve();
  }
}
