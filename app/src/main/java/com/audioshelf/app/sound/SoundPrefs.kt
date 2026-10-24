package com.audioshelf.app.sound

import com.audioshelf.app.data.KV_STORE
import com.audioshelf.app.data.Kv
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val SCOPE = "audioshelf.eq.scope"

enum class EqScope(val id: String) {
    Global("global"), Track("track");

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id } ?: Global
    }
}

class SoundPrefs(private val kv: Kv) {
    private val scopeFlow = MutableStateFlow(EqScope.of(kv.get(KV_STORE, SCOPE)))

    val scope: StateFlow<EqScope> = scopeFlow.asStateFlow()

    fun setScope(scope: EqScope) {
        kv.put(KV_STORE, SCOPE, scope.id)
        scopeFlow.value = scope
    }
}
