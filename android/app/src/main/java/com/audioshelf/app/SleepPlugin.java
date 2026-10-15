package com.audioshelf.app;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** Sleep timer from Settings. Time left and the chapter flag come back in the player state. */
@CapacitorPlugin(name = "Sleep")
public class SleepPlugin extends Plugin {
  /** minutes: 0 = off; chapter: stop at the end of the current chapter. */
  @PluginMethod
  public void setSleep(PluginCall call) {
    long minutes = call.getData().optLong("minutes", 0L);
    boolean chapter = Boolean.TRUE.equals(call.getBoolean("chapter", false));
    PlayerProfile.setSleep(minutes, chapter);
    call.resolve();
  }
}
