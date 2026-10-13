package com.audioshelf.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import androidx.activity.result.ActivityResult;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.ArrayList;
import java.util.List;

/** Operations on library files: add (copy in), remove, rename, open in another app. */
@CapacitorPlugin(name = "FileOps")
public class FileOpsPlugin extends Plugin {

  private ContentResolver cr() {
    return getContext().getContentResolver();
  }

  @PluginMethod
  public void add(PluginCall call) {
    if (call.getString("tree") == null) {
      call.reject("tree required");
      return;
    }
    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    i.addCategory(Intent.CATEGORY_OPENABLE);
    i.setType("audio/*");
    i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
    startActivityForResult(call, i, "addResult");
  }

  @ActivityCallback
  private void addResult(PluginCall call, ActivityResult result) {
    if (call == null) return;
    Intent data = result.getData();
    if (result.getResultCode() != Activity.RESULT_OK || data == null) {
      call.reject("cancelled");
      return;
    }
    List<Uri> sources = new ArrayList<>();
    if (data.getClipData() != null) {
      for (int k = 0; k < data.getClipData().getItemCount(); k++) {
        sources.add(data.getClipData().getItemAt(k).getUri());
      }
    } else if (data.getData() != null) {
      sources.add(data.getData());
    }
    Uri parent = Saf.treeRoot(Uri.parse(call.getString("tree")));
    // Files can be large: copy off the main thread.
    new Thread(
            () -> {
              JSObject o = new JSObject();
              o.put("added", Copier.copyAll(cr(), sources, parent));
              call.resolve(o);
            })
        .start();
  }

  @PluginMethod
  public void remove(PluginCall call) {
    String u = call.getString("uri");
    if (u == null) {
      call.reject("uri required");
      return;
    }
    try {
      if (DocumentsContract.deleteDocument(cr(), Uri.parse(u))) call.resolve();
      else call.reject("delete failed");
    } catch (Exception e) {
      call.reject("delete failed: " + e.getMessage());
    }
  }

  @PluginMethod
  public void rename(PluginCall call) {
    String u = call.getString("uri");
    String name = call.getString("name");
    if (u == null || name == null) {
      call.reject("uri and name required");
      return;
    }
    try {
      Uri n = DocumentsContract.renameDocument(cr(), Uri.parse(u), name);
      if (n == null) {
        call.reject("rename failed");
        return;
      }
      JSObject o = new JSObject();
      o.put("uri", n.toString());
      call.resolve(o);
    } catch (Exception e) {
      call.reject("rename failed: " + e.getMessage());
    }
  }

  @PluginMethod
  public void open(PluginCall call) {
    String u = call.getString("uri");
    if (u == null) {
      call.reject("uri required");
      return;
    }
    Uri uri = Uri.parse(u);
    String mime = cr().getType(uri);
    Intent i = new Intent(Intent.ACTION_VIEW);
    i.setDataAndType(uri, mime != null && mime.startsWith("audio/") ? mime : "audio/*");
    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
    try {
      getContext().startActivity(i);
      call.resolve();
    } catch (ActivityNotFoundException e) {
      call.reject("no_viewer");
    }
  }
}
