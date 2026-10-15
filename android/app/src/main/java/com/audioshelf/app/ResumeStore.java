package com.audioshelf.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** What survives the death of the process: the last book, where we stopped, and the settings to restore. */
final class ResumeStore {
  static final String SESSION = "session"; // {id, title, artist, items: [{uri, title}]}
  static final String PROFILE = "profile"; // last setProfile arguments
  static final String SOUND = "sound"; // last setSound arguments
  static final String SPEED = "speed";
  static final String OFFLOAD = "offload"; // "1" when hardware playback is allowed

  private ResumeStore() {}

  private static SharedPreferences prefs(Context ctx) {
    return ctx.getApplicationContext().getSharedPreferences("audioshelf.resume", Context.MODE_PRIVATE);
  }

  static String get(Context ctx, String key) {
    return prefs(ctx).getString(key, null);
  }

  static void put(Context ctx, String key, String value) {
    prefs(ctx).edit().putString(key, value).apply();
  }

  /** A book was loaded: remember what it is and where we start. Items are [{uri, title}] as sent by JavaScript. */
  static void saveSession(Context ctx, String id, String title, String artist, JSONArray items, int index, long position)
      throws JSONException {
    JSONObject session = new JSONObject();
    session.put("id", id);
    session.put("title", title);
    session.put("artist", artist);
    session.put("items", items);
    put(ctx, SESSION, session.toString());
    putSpot(ctx, index, position);
  }

  /** Where playback stopped: chapter index and position inside it. */
  static void putSpot(Context ctx, int index, long position) {
    prefs(ctx)
        .edit()
        .putInt("index", index)
        .putLong("position", position)
        .putLong("t", System.currentTimeMillis())
        .apply();
  }

  static int index(Context ctx) {
    return prefs(ctx).getInt("index", 0);
  }

  static long position(Context ctx) {
    return prefs(ctx).getLong("position", 0L);
  }

  static long savedAt(Context ctx) {
    return prefs(ctx).getLong("t", 0L);
  }

  /** The book was deleted or closed on purpose: nothing to resume. */
  static void forgetSession(Context ctx) {
    prefs(ctx).edit().remove(SESSION).remove("index").remove("position").remove("t").apply();
  }
}
