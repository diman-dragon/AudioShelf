package com.audioshelf.app.player

import com.audioshelf.app.data.Json
import com.audioshelf.app.data.Kv
import com.audioshelf.app.data.asList
import com.audioshelf.app.data.asMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val STORE = "audioshelf.marks"
private const val NEAR_MS = 1000L

class Mark(val index: Int, val positionMs: Long, val created: Long, val expanded: Boolean)

class MarkStore(private val kv: Kv) {
    private val counter = MutableStateFlow(0)

    val version: StateFlow<Int> = counter.asStateFlow()

    fun list(id: String): List<Mark> =
        Json.parse(kv.get(STORE, id)).asList()?.mapNotNull { row(it.asMap()) } ?: emptyList()

    fun count(id: String): Int = list(id).size

    fun add(id: String, index: Int, positionMs: Long, expanded: Boolean, now: Long = System.currentTimeMillis()): Boolean {
        val current = list(id)
        if (current.any { it.index == index && kotlin.math.abs(it.positionMs - positionMs) < NEAR_MS }) return false
        write(id, current + Mark(index, positionMs, now, expanded))
        return true
    }

    fun remove(id: String, created: Long) = write(id, list(id).filter { it.created != created })

    fun remap(id: String, map: (Int, Long) -> Pair<Int, Long>) {
        val current = list(id)
        if (current.none { !it.expanded }) return
        write(
            id,
            current.map {
                if (it.expanded) it else map(it.index, it.positionMs).let { (i, p) -> Mark(i, p, it.created, true) }
            },
        )
    }

    fun move(from: String, to: String) {
        if (from == to) return
        val value = kv.get(STORE, from) ?: return
        kv.del(STORE, from)
        kv.put(STORE, to, value)
        counter.value++
    }

    fun forget(id: String) {
        kv.del(STORE, id)
        counter.value++
    }

    private fun write(id: String, marks: List<Mark>) {
        val sorted = marks.sortedWith(compareBy<Mark>({ it.index }, { it.positionMs }))
        val rows = sorted.map { linkedMapOf("i" to it.index, "p" to it.positionMs, "t" to it.created, "x" to if (it.expanded) 1 else 0) }
        kv.put(STORE, id, Json.write(rows))
        counter.value++
    }

    private fun row(m: Map<String, Any?>?): Mark? {
        m ?: return null
        val flag = m["x"]
        return Mark(
            (m["i"] as? Number)?.toInt() ?: return null,
            (m["p"] as? Number)?.toLong() ?: return null,
            (m["t"] as? Number)?.toLong() ?: 0,
            flag == true || (flag as? Number)?.toInt() == 1,
        )
    }
}
