package com.audioshelf.app.library

import com.audioshelf.app.data.KV_STORE
import com.audioshelf.app.data.Kv
import com.audioshelf.app.data.KvOp

private const val STORE = "lib"
private const val TREE = "audioshelf.libtree"

class LibraryCache(private val kv: Kv) {
    fun load(tree: String): List<LibFile>? {
        val snapshot = kv.snapshot()
        if (snapshot[KV_STORE]?.get(TREE) != tree) return null
        return snapshot[STORE].orEmpty().values.mapNotNull(::decodeFile).sortedBy { it.first }.map { it.second }
    }

    fun save(tree: String, files: List<LibFile>) {
        val ops = ArrayList<KvOp>(files.size + 2)
        ops.add(KvOp.Clear(STORE))
        ops.add(KvOp.Put(KV_STORE, TREE, tree))
        files.forEachIndexed { n, file -> ops.add(KvOp.Put(STORE, file.uri, encodeFile(file, n))) }
        kv.batch(ops)
    }
}
