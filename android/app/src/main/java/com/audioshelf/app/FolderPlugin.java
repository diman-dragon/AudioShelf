package com.audioshelf.app;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
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

  @PluginMethod
  public void list(PluginCall call) {
    String t = call.getString("tree");
    if (t == null) {
      call.reject("tree required");
      return;
    }
    Uri tree = Uri.parse(t);
    Uri children =
        DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree));
    String[] cols = {
      Document.COLUMN_DOCUMENT_ID,
      Document.COLUMN_DISPLAY_NAME,
      Document.COLUMN_MIME_TYPE,
      Document.COLUMN_SIZE,
      Document.COLUMN_LAST_MODIFIED
    };
    JSArray files = new JSArray();
    try (Cursor c = cr().query(children, cols, null, null, null)) {
      while (c != null && c.moveToNext()) {
        String name = c.getString(1);
        String mime = c.getString(2);
        if (Document.MIME_TYPE_DIR.equals(mime) || !AudioFiles.isAudio(name, mime)) continue;
        JSObject o = new JSObject();
        o.put("uri", DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0)).toString());
        o.put("name", name);
        o.put("size", c.getLong(3));
        o.put("modified", c.getLong(4));
        files.put(o);
      }
    } catch (Exception e) {
      call.reject("list failed: " + e.getMessage());
      return;
    }
    JSObject r = new JSObject();
    r.put("files", files);
    call.resolve(r);
  }
}
