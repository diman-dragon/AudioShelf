package com.audioshelf.app;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** Access to the app database (see AppDb). JS keeps a copy in memory and writes changes in batches. */
@CapacitorPlugin(name = "Db")
public class DbPlugin extends Plugin {
  /** -> { rows: [[store, key, value]] } */
  @PluginMethod
  public void all(PluginCall call) {
    try {
      call.resolve(new JSObject().put("rows", AppDb.get(getContext()).all()));
    } catch (Exception e) {
      call.reject("db: " + e.getMessage());
    }
  }

  /** ops: [["p", store, key, value] | ["d", store, key] | ["c", store]] */
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
