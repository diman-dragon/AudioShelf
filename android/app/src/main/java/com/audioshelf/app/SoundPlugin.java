package com.audioshelf.app;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import org.json.JSONObject;

/** Sound settings from the Sound tab: equalizer bands and per-file loudness trims. */
@CapacitorPlugin(name = "Sound")
public class SoundPlugin extends Plugin {
  /** bands: [10 dB values]; trims: {fileUri: dB}. Remembered, so a resumed player sounds the same. */
  @PluginMethod
  public void setSound(PluginCall call) {
    JSONObject data = call.getData();
    ResumeStore.put(getContext(), ResumeStore.SOUND, data.toString());
    PlayerSound.applyJson(data);
    call.resolve();
  }
}
