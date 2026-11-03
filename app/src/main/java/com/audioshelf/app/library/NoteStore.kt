package com.audioshelf.app.library

import com.audioshelf.app.data.Json
import com.audioshelf.app.data.Kv
import com.audioshelf.app.data.KvOp
import com.audioshelf.app.data.asList
import com.audioshelf.app.data.asMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val NOTES = "audioshelf.notes"
private const val CHARS = "audioshelf.chars"

class Person(val name: String, val note: String)

class NoteStore(private val kv: Kv) {
    private val counter = MutableStateFlow(0)

    val version: StateFlow<Int> = counter.asStateFlow()

    fun note(id: String): String = kv.get(NOTES, id) ?: ""

    fun setNote(id: String, text: String) {
        if (text.isBlank()) kv.del(NOTES, id) else kv.put(NOTES, id, text)
        counter.update { it + 1 }
    }

    fun people(id: String): List<Person> = Json.parse(kv.get(CHARS, id)).asList()?.mapNotNull {
        val m = it.asMap() ?: return@mapNotNull null
        Person(m["n"] as? String ?: return@mapNotNull null, m["d"] as? String ?: "")
    } ?: emptyList()

    private fun write(id: String, list: List<Person>) {
        if (list.isEmpty()) kv.del(CHARS, id) else kv.put(CHARS, id, Json.write(list.map { linkedMapOf("n" to it.name, "d" to it.note) }))
        counter.update { it + 1 }
    }

    fun addPerson(id: String, name: String, note: String): Boolean {
        val clean = name.trim()
        if (clean.isEmpty()) return false
        write(id, people(id) + Person(clean, note.trim()))
        return true
    }

    fun removePerson(id: String, index: Int) {
        val list = people(id)
        if (index !in list.indices) return
        write(id, list.filterIndexed { i, _ -> i != index })
    }

    fun move(from: String, to: String) {
        if (from == to) return
        val note = kv.get(NOTES, from)
        val people = kv.get(CHARS, from)
        val ops = ArrayList<KvOp>(4)
        if (note != null) {
            ops.add(KvOp.Del(NOTES, from))
            ops.add(KvOp.Put(NOTES, to, note))
        }
        if (people != null) {
            ops.add(KvOp.Del(CHARS, from))
            ops.add(KvOp.Put(CHARS, to, people))
        }
        if (ops.isNotEmpty()) kv.batch(ops)
        counter.update { it + 1 }
    }

    fun forget(id: String) {
        kv.batch(listOf(KvOp.Del(NOTES, id), KvOp.Del(CHARS, id)))
        counter.update { it + 1 }
    }
}
