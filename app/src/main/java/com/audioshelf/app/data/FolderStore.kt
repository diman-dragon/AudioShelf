package com.audioshelf.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val FOLDER = "audioshelf.folder"

class Folder(val uri: String, val name: String)

class FolderStore(private val kv: Kv) {
    private val flow = MutableStateFlow(load())

    val current: StateFlow<Folder?> = flow.asStateFlow()

    private fun load(): Folder? {
        val json = FlatJson.parse(kv.get(KV_STORE, FOLDER)) ?: return null
        val uri = json.str("uri") ?: return null
        return Folder(uri, json.str("name") ?: "")
    }

    fun set(folder: Folder) {
        kv.put(KV_STORE, FOLDER, FlatJson.write(mapOf("uri" to folder.uri, "name" to folder.name)))
        flow.value = folder
    }

    fun clear() {
        kv.del(KV_STORE, FOLDER)
        flow.value = null
    }
}
