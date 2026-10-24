package com.audioshelf.app.player

import com.audioshelf.app.data.Json
import com.audioshelf.app.data.Kv
import com.audioshelf.app.data.asList
import com.audioshelf.app.data.asMap
import com.audioshelf.app.library.LibFile

private const val STORE = "audioshelf.chapters"

class ChapterRec(val title: String, val start: Long, val end: Long)

class ChapterStore(private val kv: Kv) {
    private fun sig(file: LibFile) = "${file.size}|${file.modified}"

    fun of(file: LibFile): List<ChapterRec>? {
        val m = Json.parse(kv.get(STORE, file.uri)).asMap() ?: return null
        if (m["sig"] != sig(file)) return null
        return m["list"].asList()?.mapNotNull { rec(it.asMap()) }
    }

    fun set(file: LibFile, list: List<ChapterRec>) {
        val rows = list.map { linkedMapOf("t" to it.title, "s" to it.start, "e" to it.end) }
        kv.put(STORE, file.uri, Json.write(linkedMapOf("sig" to sig(file), "list" to rows)))
    }

    fun forget(uri: String) = kv.del(STORE, uri)

    private fun rec(m: Map<String, Any?>?): ChapterRec? {
        m ?: return null
        return ChapterRec(
            m["t"] as? String ?: "",
            (m["s"] as? Number)?.toLong() ?: return null,
            (m["e"] as? Number)?.toLong() ?: -1,
        )
    }
}
