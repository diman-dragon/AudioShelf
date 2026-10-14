package com.audioshelf.app;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/** Sound settings from the Sound tab: equalizer bands and per-file loudness trims. */
@CapacitorPlugin(name = "Sound")
public class SoundPlugin extends Plugin {
  /** bands: [10 dB values]; trims: {fileUri: dB}. */
  @PluginMethod
  public void setSound(PluginCall call) {
    JSONArray list = call.getData().optJSONArray("bands");
    float[] bands = new float[SoundProcessor.BANDS];
    for (int i = 0; list != null && i < bands.length && i < list.length(); i++) {
      bands[i] = (float) list.optDouble(i, 0);
    }
    Map<String, Float> trims = new HashMap<>();
    JSONObject raw = call.getData().optJSONObject("trims");
    if (raw != null) {
      Iterator<String> keys = raw.keys();
      while (keys.hasNext()) {
        String uri = keys.next();
        trims.put(uri, (float) raw.optDouble(uri, 0));
      }
    }
    PlayerSound.apply(bands, trims);
    call.resolve();
  }
}
