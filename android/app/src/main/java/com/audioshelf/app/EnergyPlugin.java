package com.audioshelf.app;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** Battery settings: hardware playback (audio offload). */
@CapacitorPlugin(name = "Energy")
public class EnergyPlugin extends Plugin {
  /** enabled: let the phone's audio chip play when no equalizer or loudness trim is needed. */
  @PluginMethod
  public void setOffload(PluginCall call) {
    boolean enabled = Boolean.TRUE.equals(call.getBoolean("enabled", false));
    ResumeStore.put(getContext(), ResumeStore.OFFLOAD, enabled ? "1" : "0");
    PlayerOffload.setAllowed(enabled);
    call.resolve();
  }
}
