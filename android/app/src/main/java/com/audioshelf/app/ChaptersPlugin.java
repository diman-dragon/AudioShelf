package com.audioshelf.app;

import android.net.Uri;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

@CapacitorPlugin(name = "Chapters")
public class ChaptersPlugin extends Plugin {
  @PluginMethod
  public void read(PluginCall call) {
    JSONArray uris = call.getArray("uris");
    JSArray items = new JSArray();
    try {
      for (int i = 0; uris != null && i < uris.length(); i++) {
        String uri = uris.getString(i);
        JSObject item = new JSObject();
        item.put("uri", uri);
        item.put("chapters", chaptersOf(uri));
        items.put(item);
      }
      call.resolve(new JSObject().put("items", items));
    } catch (Exception e) {
      call.reject("chapters: " + e.getMessage());
    }
  }

  private JSArray chaptersOf(String uri) {
    JSArray out = new JSArray();
    try (ChannelSource src = ChannelSource.open(getContext(), Uri.parse(uri))) {
      for (Chapter c : FileChapters.read(src, false).chapters) {
        out.put(new JSONObject().put("title", c.title).put("start", c.startMs).put("end", c.endMs));
      }
    } catch (Throwable ignored) {
    }
    return out;
  }
}
