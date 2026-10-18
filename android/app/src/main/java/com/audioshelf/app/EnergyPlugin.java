package com.audioshelf.app;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "Energy")
public class EnergyPlugin extends Plugin {
  @PluginMethod
  public void setOffload(PluginCall call) {
    boolean enabled = Boolean.TRUE.equals(call.getBoolean("enabled", false));
    ResumeStore.put(getContext(), ResumeStore.OFFLOAD, enabled ? "1" : "0");
    PlayerOffload.setAllowed(enabled);
    call.resolve();
  }

  @PluginMethod
  public void status(PluginCall call) {
    JSObject o = new JSObject();
    o.put("supported", OffloadSupport.supported(getContext()));
    o.put("allowed", PlayerOffload.isAllowed());
    o.put("neutral", PlayerOffload.isNeutral());
    call.resolve(o);
  }
}
