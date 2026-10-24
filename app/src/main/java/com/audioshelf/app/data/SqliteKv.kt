package com.audioshelf.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SqliteKv(context: Context) : SQLiteOpenHelper(context.applicationContext, "audioshelf.db", null, 1), KvBackend {
    init {
        setWriteAheadLoggingEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE kv (s TEXT NOT NULL, k TEXT NOT NULL, v TEXT NOT NULL, PRIMARY KEY (s, k)) WITHOUT ROWID")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    @Synchronized
    override fun loadAll(): List<KvRow> {
        val rows = ArrayList<KvRow>()
        readableDatabase.rawQuery("SELECT s, k, v FROM kv", null).use { c ->
            while (c.moveToNext()) rows.add(KvRow(c.getString(0), c.getString(1), c.getString(2)))
        }
        return rows
    }

    @Synchronized
    override fun write(ops: List<KvOp>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (op in ops) {
                when (op) {
                    is KvOp.Put -> {
                        val row = ContentValues()
                        row.put("s", op.store)
                        row.put("k", op.key)
                        row.put("v", op.value)
                        db.insertWithOnConflict("kv", null, row, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                    is KvOp.Del -> db.delete("kv", "s = ? AND k = ?", arrayOf(op.store, op.key))
                    is KvOp.Clear -> db.delete("kv", "s = ?", arrayOf(op.store))
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
