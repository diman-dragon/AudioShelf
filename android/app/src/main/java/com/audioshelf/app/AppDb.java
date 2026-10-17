package com.audioshelf.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONObject;

/** The app's own database: one table of (store, key) -> JSON text. Progress, bookmarks, chapters, durations, gains, settings. */
final class AppDb extends SQLiteOpenHelper {
  private static AppDb instance;

  static synchronized AppDb get(Context ctx) {
    if (instance == null) instance = new AppDb(ctx.getApplicationContext());
    return instance;
  }

  private AppDb(Context ctx) {
    super(ctx, "audioshelf.db", null, 1);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE kv (s TEXT NOT NULL, k TEXT NOT NULL, v TEXT NOT NULL, PRIMARY KEY (s, k)) WITHOUT ROWID");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int from, int to) {}

  synchronized JSONArray all() {
    JSONArray rows = new JSONArray();
    try (Cursor c = getReadableDatabase().rawQuery("SELECT s, k, v FROM kv", null)) {
      while (c.moveToNext()) rows.put(new JSONArray().put(c.getString(0)).put(c.getString(1)).put(c.getString(2)));
    }
    return rows;
  }

  /** ops: [["p", store, key, value] | ["d", store, key] | ["c", store]] in one transaction. */
  synchronized void apply(JSONArray ops) throws Exception {
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      for (int i = 0; i < ops.length(); i++) {
        JSONArray op = ops.getJSONArray(i);
        String kind = op.getString(0);
        if (kind.equals("p")) {
          ContentValues row = new ContentValues();
          row.put("s", op.getString(1));
          row.put("k", op.getString(2));
          row.put("v", op.getString(3));
          db.insertWithOnConflict("kv", null, row, SQLiteDatabase.CONFLICT_REPLACE);
        } else if (kind.equals("d")) {
          db.delete("kv", "s = ? AND k = ?", new String[] {op.getString(1), op.getString(2)});
        } else if (kind.equals("c")) {
          db.delete("kv", "s = ?", new String[] {op.getString(1)});
        }
      }
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }
}
