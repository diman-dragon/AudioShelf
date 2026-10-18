package com.audioshelf.app;

import android.provider.Settings;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "License")
public class LicensePlugin extends Plugin {
  private String fingerprint() {
    String id = Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.ANDROID_ID);
    return DevLicense.fingerprint(id == null ? "" : id, getContext().getPackageName());
  }

  @PluginMethod
  public void check(PluginCall call) {
    String fp = fingerprint();
    JSObject out = new JSObject();
    out.put("fingerprint", fp);
    out.put("valid", DevLicense.valid(call.getString("code", ""), fp));
    call.resolve(out);
  }
}
