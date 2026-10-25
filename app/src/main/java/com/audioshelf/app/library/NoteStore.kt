package com.audioshelf.app.library

import com.audioshelf.app.data.Json
import com.audioshelf.app.data.Kv
import com.audioshelf.app.data.asList
import com.audioshelf.app.data.asMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val NOTES = "audioshelf.notes"
private const val CHARS = "audioshelf.chars"

class Person(val name: String, val note: String)

class NoteStore(private val kv: Kv) {
    private val counter = MutableStateFlow(0)

    val version: StateFlow<Int> = counter.asStateFlow()

    fun note(id: String): String = kv.get(NOTES, id) ?: ""

    fun setNote(id: String, text: String) {
        if (text.isBlank()) kv.del(NOTES, id) else kv.put(NOTES, id, text)
        counter.value++
    }

    fun people(id: String): List<Person> = Json.parse(kv.get(CHARS, id)).asList()?.mapNotNull {
        val m = it.asMap() ?: return@mapNotNull null
        Person(m["n"] as? String ?: return@mapNotNull null, m["d"] as? String ?: "")
    } ?: emptyList()

    private fun write(id: String, list: List<Person>) {
        if (list.isEmpty()) kv.del(CHARS, id) else kv.put(CHARS, id, Json.write(list.map { linkedMapOf("n" to it.name, "d" to it.note) }))
        counter.value++
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
        kv.get(NOTES, from)?.let {
            kv.del(NOTES, from)
            kv.put(NOTES, to, it)
        }
        kv.get(CHARS, from)?.let {
            kv.del(CHARS, from)
            kv.put(CHARS, to, it)
        }
        counter.value++
    }

    fun forget(id: String) {
        kv.del(NOTES, id)
        kv.del(CHARS, id)
        counter.value++
    }
}
