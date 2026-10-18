package com.audioshelf.app;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "Db")
public class DbPlugin extends Plugin {
  @PluginMethod
  public void all(PluginCall call) {
    try {
      call.resolve(new JSObject().put("rows", AppDb.get(getContext()).all()));
    } catch (Exception e) {
      call.reject("db: " + e.getMessage());
    }
  }

  @PluginMethod
  public void apply(PluginCall call) {
    try {
      AppDb.get(getContext()).apply(call.getArray("ops"));
      call.resolve();
    } catch (Exception e) {
      call.reject("db: " + e.getMessage());
    }
  }
}
