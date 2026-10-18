package com.audioshelf.app;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import org.json.JSONObject;

@CapacitorPlugin(name = "Sound")
public class SoundPlugin extends Plugin {
  @PluginMethod
  public void setSound(PluginCall call) {
    JSONObject data = call.getData();
    ResumeStore.put(getContext(), ResumeStore.SOUND, data.toString());
    PlayerSound.applyJson(data);
    call.resolve();
  }
}
