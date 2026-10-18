package com.audioshelf.app;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "Sleep")
public class SleepPlugin extends Plugin {
  @PluginMethod
  public void setSleep(PluginCall call) {
    long minutes = call.getData().optLong("minutes", 0L);
    boolean chapter = Boolean.TRUE.equals(call.getBoolean("chapter", false));
    PlayerProfile.setSleep(minutes, chapter);
    call.resolve();
  }
}
