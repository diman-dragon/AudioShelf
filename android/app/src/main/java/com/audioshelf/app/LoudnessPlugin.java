package com.audioshelf.app;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

@CapacitorPlugin(name = "Loudness")
public class LoudnessPlugin extends Plugin {
  private final LoudnessRunner runner = new LoudnessRunner();

  private void send(String type, String name, String uri, Double gain, Integer failed, String status) {
    JSObject o = new JSObject();
    o.put("type", type);
    if (name != null) o.put("name", name);
    if (uri != null) o.put("uri", uri);
    if (gain != null) o.put("gain", gain);
    if (failed != null) o.put("failed", failed);
    if (status != null) o.put("status", status);
    notifyListeners("loudness", o);
  }

  @PluginMethod
  public void start(PluginCall call) {
    JSArray raw = call.getArray("files");
    List<String[]> files = new ArrayList<>();
    try {
      for (int i = 0; raw != null && i < raw.length(); i++) {
        JSONObject f = raw.getJSONObject(i);
        files.add(new String[] {f.getString("uri"), f.optString("name", "")});
      }
    } catch (Exception e) {
      call.reject("bad files: " + e.getMessage());
      return;
    }
    boolean onlyCharging = Boolean.TRUE.equals(call.getBoolean("onlyCharging", false));
    if (onlyCharging && BatteryGuard.shouldPause(getContext(), true)) {
      call.resolve(new JSObject().put("started", false).put("reason", "unplugged"));
      return;
    }
    boolean started =
        runner.start(
            getContext(),
            files,
            onlyCharging,
            new LoudnessRunner.Listener() {
              @Override
              public void onStart(String name) {
                send("start", name, null, null, null, null);
              }

              @Override
              public void onFile(String uri, String name, double gain) {
                send("file", name, uri, Double.isNaN(gain) ? null : gain, null, null);
              }

              @Override
              public void onFinish(String status, int failed) {
                send("finish", null, null, null, failed, status);
              }
            });
    JSObject result = new JSObject();
    result.put("started", started);
    call.resolve(result);
  }

  @PluginMethod
  public void cancel(PluginCall call) {
    runner.cancel();
    call.resolve();
  }
}
