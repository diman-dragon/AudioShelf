package com.audioshelf.app;

import android.net.Uri;
import android.util.Base64;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.File;

/**
 * Cover art embedded in an audio file, cached on disk. Resolves with the cached file's path (the WebView loads it itself),
 * or with a data URL when inline is asked for. Rejects when the file has no cover.
 */
@CapacitorPlugin(name = "Cover")
public class CoverPlugin extends Plugin {

  @PluginMethod
  public void read(PluginCall call) {
    String uri = call.getString("uri");
    if (uri == null) {
      call.reject("uri required");
      return;
    }
    try {
      String key = CoverCache.key(uri, call.getString("sig", ""), call.getInt("width", 360));
      File file = cached(uri, key, call.getInt("width", 360));
      JSObject out = new JSObject();
      if (Boolean.TRUE.equals(call.getBoolean("inline", false))) {
        out.put("data", "data:image/jpeg;base64," + Base64.encodeToString(CoverCache.get(getContext(), key), Base64.NO_WRAP));
      } else {
        out.put("path", file.getAbsolutePath());
      }
      call.resolve(out);
    } catch (Exception e) {
      call.reject("no cover: " + e.getMessage());
    }
  }

  /** The cover in the cache; read from the audio file and cached when it is not there yet (also "no cover"). */
  private File cached(String uri, String key, int width) throws Exception {
    File file = CoverCache.file(getContext(), key);
    if (file != null) return file;
    if (CoverCache.none(getContext(), key)) throw new Exception("none");
    try {
      CoverCache.put(getContext(), key, CoverArt.jpeg(getContext(), Uri.parse(uri), width));
    } catch (Exception e) {
      CoverCache.putNone(getContext(), key);
      throw e;
    }
    return CoverCache.file(getContext(), key);
  }
}
