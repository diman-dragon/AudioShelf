package com.audioshelf.app;

import android.provider.Settings;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** Developer code: device fingerprint and code check (the check itself lives in DevLicense). */
@CapacitorPlugin(name = "License")
public class LicensePlugin extends Plugin {
  private String fingerprint() {
    String id = Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.ANDROID_ID);
    return DevLicense.fingerprint(id == null ? "" : id, getContext().getPackageName());
  }

  /** Returns { fingerprint }, and { valid } when a code is given. */
  @PluginMethod
  public void check(PluginCall call) {
    String fp = fingerprint();
    JSObject out = new JSObject();
    out.put("fingerprint", fp);
    out.put("valid", DevLicense.valid(call.getString("code", ""), fp));
    call.resolve(out);
  }
}
