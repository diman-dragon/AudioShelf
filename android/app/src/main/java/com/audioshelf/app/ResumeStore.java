package com.audioshelf.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class ResumeStore {
  static final String SESSION = "session";
  static final String PROFILE = "profile";
  static final String SOUND = "sound";
  static final String SPEED = "speed";
  static final String OFFLOAD = "offload";

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

  static void forgetSession(Context ctx) {
    prefs(ctx).edit().remove(SESSION).remove("index").remove("position").remove("t").apply();
  }
}
