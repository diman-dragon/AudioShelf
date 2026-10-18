package com.audioshelf.app;

import android.content.ContentResolver;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;

abstract class SafPlugin extends Plugin {

  protected ContentResolver cr() {
    return getContext().getContentResolver();
  }

  protected static String need(PluginCall call, String key) {
    String value = call.getString(key);
    if (value == null) call.reject(key + " required");
    return value;
  }
}
