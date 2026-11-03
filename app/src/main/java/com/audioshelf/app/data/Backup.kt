package com.audioshelf.app.data

private const val TAG = "audioshelf-backup"
private const val VERSION = 1

private val SKIP_STORES = setOf("lib", "audioshelf.chapters")
private val SKIP_KV_KEYS = setOf("audioshelf.folder", "audioshelf.libtree", "audioshelf.last2")

object Backup {
    fun rows(kv: Kv): List<KvRow> {
        val out = ArrayList<KvRow>()
        for ((store, rows) in kv.snapshot()) {
            if (store in SKIP_STORES) continue
            for ((key, value) in rows) {
                if (store == KV_STORE && key in SKIP_KV_KEYS) continue
                out.add(KvRow(store, key, value))
            }
        }
        return out
    }

    fun encode(rows: List<KvRow>, now: Long = System.currentTimeMillis()): String = Json.write(
        linkedMapOf(
            "app" to TAG,
            "version" to VERSION,
            "created" to now,
            "rows" to rows.map { linkedMapOf("s" to it.store, "k" to it.key, "v" to it.value) },
        ),
    )

    fun decode(text: String?): List<KvRow>? {
        val root = Json.parse(text).asMap() ?: return null
        if (root["app"] != TAG) return null
        val list = root["rows"].asList() ?: return null
        val out = ArrayList<KvRow>(list.size)
        for (item in list) {
            val m = item.asMap() ?: continue
            val s = m["s"] as? String ?: continue
            val k = m["k"] as? String ?: continue
            val v = m["v"] as? String ?: continue
            if (s in SKIP_STORES) continue
            if (s == KV_STORE && k in SKIP_KV_KEYS) continue
            out.add(KvRow(s, k, v))
        }
        return out
    }

    /**
     * Restore, not merge: every store the backup contains is emptied first, so rows from the current
     * data never mix with the backed-up ones (a day of statistics would otherwise be overwritten by an
     * older version of itself). The settings store holds device keys too, so there only the
     * backed-up keys are replaced.
     */
    fun apply(kv: Kv, rows: List<KvRow>): Int {
        val ops = ArrayList<KvOp>()
        rows.mapTo(LinkedHashSet()) { it.store }.filter { it != KV_STORE }.forEach { ops.add(KvOp.Clear(it)) }
        rows.forEach { ops.add(KvOp.Put(it.store, it.key, it.value)) }
        kv.batch(ops)
        kv.flushNow()
        return rows.size
    }
}
