package com.audioshelf.app;

import android.net.Uri;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** Cover art embedded in an audio file. Rejects when the file has none. */
@CapacitorPlugin(name = "Cover")
public class CoverPlugin extends Plugin {

  @PluginMethod
  public void read(PluginCall call) {
    String u = call.getString("uri");
    if (u == null) {
      call.reject("uri required");
      return;
    }
    try {
      JSObject o = new JSObject();
      o.put("data", CoverArt.read(getContext(), Uri.parse(u), call.getInt("width", 360)));
      call.resolve(o);
    } catch (Exception e) {
      call.reject("no cover: " + e.getMessage());
    }
  }
}
