package com.audioshelf.app;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import org.json.JSONObject;

/** The place saved natively: JavaScript compares it with its own record at start and on return to the app. */
@CapacitorPlugin(name = "Resume")
public class ResumePlugin extends Plugin {
  /** Resolves {id, index, position, t}, or {} when nothing is saved. */
  @PluginMethod
  public void saved(PluginCall call) {
    JSObject out = new JSObject();
    String raw = ResumeStore.get(getContext(), ResumeStore.SESSION);
    long t = ResumeStore.savedAt(getContext());
    if (raw != null && t > 0) {
      try {
        out.put("id", new JSONObject(raw).optString("id"));
        out.put("index", ResumeStore.index(getContext()));
        out.put("position", ResumeStore.position(getContext()));
        out.put("t", t);
      } catch (Exception ignored) {
        // a damaged record counts as nothing saved
      }
    }
    call.resolve(out);
  }
}
