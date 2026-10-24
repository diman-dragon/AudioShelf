package com.audioshelf.app.data

fun coalesce(ops: List<KvOp>): List<KvOp> {
    val out = ArrayList<KvOp?>()
    val at = HashMap<Pair<String, String>, Int>()

    fun place(key: Pair<String, String>, op: KvOp) {
        at[key]?.let { out[it] = null }
        at[key] = out.size
        out.add(op)
    }

    for (op in ops) {
        when (op) {
            is KvOp.Clear -> {
                val entries = at.iterator()
                while (entries.hasNext()) {
                    val entry = entries.next()
                    if (entry.key.first == op.store) {
                        out[entry.value] = null
                        entries.remove()
                    }
                }
                out.add(op)
            }
            is KvOp.Put -> place(op.store to op.key, op)
            is KvOp.Del -> place(op.store to op.key, op)
        }
    }
    return out.filterNotNull()
}
