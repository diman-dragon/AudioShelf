package com.audioshelf.app;

import android.content.ContentResolver;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;

/** Shared helpers for plugins that work with files. */
abstract class SafPlugin extends Plugin {

  protected ContentResolver cr() {
    return getContext().getContentResolver();
  }

  /** The string argument, or null after rejecting the call because it is missing. */
  protected static String need(PluginCall call, String key) {
    String value = call.getString(key);
    if (value == null) call.reject(key + " required");
    return value;
  }
}
