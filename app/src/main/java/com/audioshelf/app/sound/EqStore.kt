package com.audioshelf.app.sound

import com.audioshelf.app.data.KV_STORE
import com.audioshelf.app.data.Kv
import com.audioshelf.app.data.KvOp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val GLOBAL = "audioshelf.eq.global"
private const val TRACKS = "audioshelf.eq.tracks"

class EqStore(private val kv: Kv) {
    private val counter = MutableStateFlow(0)

    val version: StateFlow<Int> = counter.asStateFlow()

    fun global(): Eq = Eq.decode(kv.get(KV_STORE, GLOBAL)) ?: Eq.Flat

    fun setGlobal(eq: Eq) {
        kv.put(KV_STORE, GLOBAL, Eq.encode(eq))
        counter.update { it + 1 }
    }

    fun track(id: String): Eq? = Eq.decode(kv.get(TRACKS, id))

    fun setTrack(id: String, eq: Eq) {
        kv.put(TRACKS, id, Eq.encode(eq))
        counter.update { it + 1 }
    }

    fun clearTrack(id: String) {
        kv.del(TRACKS, id)
        counter.update { it + 1 }
    }

    fun moveTrack(from: String, to: String) {
        if (from == to) return
        val eq = track(from) ?: return
        kv.batch(listOf(KvOp.Del(TRACKS, from), KvOp.Put(TRACKS, to, Eq.encode(eq))))
        counter.update { it + 1 }
    }

    fun effective(id: String?): Eq = (id?.let { track(it) }) ?: global()
}
