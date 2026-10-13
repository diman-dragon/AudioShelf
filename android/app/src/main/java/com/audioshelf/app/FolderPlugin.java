package com.audioshelf.app;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract.Document;
import androidx.activity.result.ActivityResult;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

/** Audio folder via Storage Access Framework: pick a folder and list its audio files. */
@CapacitorPlugin(name = "Folder")
public class FolderPlugin extends Plugin {

  private volatile FolderScanner scanner;

  private ContentResolver cr() {
    return getContext().getContentResolver();
  }

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
    String t = call.getString("tree");
    if (t == null) {
      call.reject("tree required");
      return;
    }
    if (scanner != null) scanner.cancel();
    FolderScanner mine = new FolderScanner();
    scanner = mine;
    new Thread(
            () -> {
              try {
                JSArray files =
                    mine.scan(
                        cr(),
                        Uri.parse(t),
                        (found, folders) -> {
                          JSObject p = new JSObject();
                          p.put("files", found);
                          p.put("folders", folders);
                          notifyListeners("scan", p);
                        });
                JSObject r = new JSObject();
                r.put("files", files);
                r.put("cancelled", mine.isCancelled());
                call.resolve(r);
              } catch (Exception e) {
                call.reject("scan failed: " + e.getMessage());
              }
            })
        .start();
  }

  @PluginMethod
  public void cancelScan(PluginCall call) {
    if (scanner != null) scanner.cancel();
    call.resolve();
  }
}
