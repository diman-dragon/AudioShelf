package com.audioshelf.app.sound

import com.audioshelf.app.data.KV_STORE
import com.audioshelf.app.data.Kv
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val BOOST = "audioshelf.boost"
private const val NORM_ON = "audioshelf.normOn"
private const val SCOPE = "audioshelf.eq.scope"

val BOOSTS = listOf(100, 125, 150, 200, 250, 300)

enum class EqScope(val id: String) {
    Global("global"), Track("track");

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id } ?: Global
    }
}

class SoundPrefs(private val kv: Kv) {
    private val boostFlow = MutableStateFlow(kv.get(KV_STORE, BOOST)?.toIntOrNull()?.takeIf { it in BOOSTS } ?: 100)
    private val normFlow = MutableStateFlow(kv.get(KV_STORE, NORM_ON) != "0")
    private val scopeFlow = MutableStateFlow(EqScope.of(kv.get(KV_STORE, SCOPE)))

    val boost: StateFlow<Int> = boostFlow.asStateFlow()
    val normOn: StateFlow<Boolean> = normFlow.asStateFlow()
    val scope: StateFlow<EqScope> = scopeFlow.asStateFlow()

    fun setBoost(percent: Int) {
        kv.put(KV_STORE, BOOST, percent.toString())
        boostFlow.value = percent
    }

    fun setNormOn(on: Boolean) {
        kv.put(KV_STORE, NORM_ON, if (on) "1" else "0")
        normFlow.value = on
    }

    fun setScope(scope: EqScope) {
        kv.put(KV_STORE, SCOPE, scope.id)
        scopeFlow.value = scope
    }
}
