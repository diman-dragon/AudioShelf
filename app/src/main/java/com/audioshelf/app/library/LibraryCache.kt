package com.audioshelf.app.library

import com.audioshelf.app.data.KV_STORE
import com.audioshelf.app.data.Kv

private const val STORE = "lib"
private const val TREE = "audioshelf.libtree"

class LibraryCache(private val kv: Kv) {
    fun load(tree: String): List<LibFile>? {
        if (kv.get(KV_STORE, TREE) != tree) return null
        return kv.all(STORE).values.mapNotNull(::decodeFile).sortedBy { it.first }.map { it.second }
    }

    fun save(tree: String, files: List<LibFile>) {
        kv.put(KV_STORE, TREE, tree)
        val keep = files.mapTo(HashSet()) { it.uri }
        kv.all(STORE).keys.filter { it !in keep }.forEach { kv.del(STORE, it) }
        files.forEachIndexed { n, file -> kv.put(STORE, file.uri, encodeFile(file, n)) }
    }
}
