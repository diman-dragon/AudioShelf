package com.audioshelf.app.library

import com.audioshelf.app.data.FlatJson
import com.audioshelf.app.data.Kv
import com.audioshelf.app.data.MapStore
import com.audioshelf.app.data.str
import com.audioshelf.app.player.Kind
import kotlinx.coroutines.flow.StateFlow

data class Meta(
    val kind: Kind? = null,
    val title: String = "",
    val author: String = "",
    val narrator: String = "",
    val year: String = "",
)

private fun kindOf(id: String?) = when (id) {
    "book" -> Kind.Book
    "album" -> Kind.Album
    else -> null
}

fun decodeMeta(m: Map<String, Any?>) = Meta(
    kind = kindOf(m.str("kind")),
    title = m.str("title") ?: "",
    author = m.str("author") ?: "",
    narrator = m.str("narrator") ?: "",
    year = m.str("year") ?: "",
)

fun encodeMeta(meta: Meta): Map<String, Any?> = linkedMapOf(
    "kind" to meta.kind?.name?.lowercase(),
    "title" to meta.title,
    "author" to meta.author,
    "narrator" to meta.narrator,
    "year" to meta.year,
)

class MetaStore(kv: Kv) {
    private val map = MapStore(kv, "audioshelf.meta")

    val version: StateFlow<Int> get() = map.version

    fun get(id: String): Meta = map.get(id)?.let(::decodeMeta) ?: Meta()

    fun set(id: String, meta: Meta) = map.set(id, encodeMeta(meta))

    fun forget(id: String) = map.del(id)

    fun move(from: String, to: String) = map.move(from, to)
}
