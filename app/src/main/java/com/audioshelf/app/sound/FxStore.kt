package com.audioshelf.app.sound

import com.audioshelf.app.data.KV_STORE
import com.audioshelf.app.data.Kv
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val GLOBAL = "audioshelf.fx.global"
private const val TRACKS = "audioshelf.fx.tracks"

class FxStore(private val kv: Kv) {
    private val counter = MutableStateFlow(0)

    val version: StateFlow<Int> = counter.asStateFlow()

    fun global(): Fx = Fx.decode(kv.get(KV_STORE, GLOBAL)) ?: Fx.Off

    fun setGlobal(fx: Fx) {
        kv.put(KV_STORE, GLOBAL, Fx.encode(fx))
        counter.value++
    }

    fun track(id: String): Fx? = Fx.decode(kv.get(TRACKS, id))

    fun setTrack(id: String, fx: Fx) {
        kv.put(TRACKS, id, Fx.encode(fx))
        counter.value++
    }

    fun clearTrack(id: String) {
        kv.del(TRACKS, id)
        counter.value++
    }

    fun effective(id: String?): Fx = (id?.let { track(it) }) ?: global()
}
