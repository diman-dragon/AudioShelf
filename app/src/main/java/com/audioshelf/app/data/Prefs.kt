package com.audioshelf.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

const val KV_STORE = "kv"

private const val THEME = "audioshelf.theme"
private const val LANG = "audioshelf.lang"
private const val SKIP_BACK = "audioshelf.skipBack"
private const val SKIP_FORWARD = "audioshelf.skipForward"
private const val OFFLOAD = "audioshelf.offload"
private const val LIB_KIND = "audioshelf.libkind"
private const val LIB_SORT = "audioshelf.libsort"
private const val LIB_VIEW = "audioshelf.libview"

class Prefs(private val kv: Kv) {
    private fun read(key: String) = kv.get(KV_STORE, key)

    private fun skip(key: String, default: Int) = read(key)?.toIntOrNull()?.takeIf { it in SKIP_OPTIONS } ?: default

    private val themeFlow = MutableStateFlow(read(THEME)?.takeIf { it in THEME_IDS })
    private val langFlow = MutableStateFlow(read(LANG)?.takeIf { it in LANG_IDS } ?: LANG_AUTO)
    private val skipBackFlow = MutableStateFlow(skip(SKIP_BACK, SKIP_BACK_DEFAULT))
    private val skipForwardFlow = MutableStateFlow(skip(SKIP_FORWARD, SKIP_FORWARD_DEFAULT))
    private val offloadFlow = MutableStateFlow(read(OFFLOAD) == "1")
    private val kindFlow = MutableStateFlow(KindFilter.of(read(LIB_KIND)))
    private val sortFlow = MutableStateFlow(SortMode.of(read(LIB_SORT)))
    private val viewFlow = MutableStateFlow(LibView.of(read(LIB_VIEW)))

    val theme: StateFlow<String?> = themeFlow.asStateFlow()
    val language: StateFlow<String> = langFlow.asStateFlow()
    val skipBack: StateFlow<Int> = skipBackFlow.asStateFlow()
    val skipForward: StateFlow<Int> = skipForwardFlow.asStateFlow()
    val offload: StateFlow<Boolean> = offloadFlow.asStateFlow()
    val libKind: StateFlow<KindFilter> = kindFlow.asStateFlow()
    val libSort: StateFlow<SortMode> = sortFlow.asStateFlow()
    val libView: StateFlow<LibView> = viewFlow.asStateFlow()

    fun setTheme(id: String) {
        kv.put(KV_STORE, THEME, id)
        themeFlow.value = id
    }

    fun setLanguage(id: String) {
        kv.put(KV_STORE, LANG, id)
        langFlow.value = id
    }

    fun setSkipBack(seconds: Int) {
        kv.put(KV_STORE, SKIP_BACK, seconds.toString())
        skipBackFlow.value = seconds
    }

    fun setSkipForward(seconds: Int) {
        kv.put(KV_STORE, SKIP_FORWARD, seconds.toString())
        skipForwardFlow.value = seconds
    }

    fun setOffload(on: Boolean) {
        kv.put(KV_STORE, OFFLOAD, if (on) "1" else "0")
        offloadFlow.value = on
    }

    fun setLibKind(kind: KindFilter) {
        kv.put(KV_STORE, LIB_KIND, kind.id)
        kindFlow.value = kind
    }

    fun setLibSort(sort: SortMode) {
        kv.put(KV_STORE, LIB_SORT, sort.id)
        sortFlow.value = sort
    }

    fun setLibView(view: LibView) {
        kv.put(KV_STORE, LIB_VIEW, view.id)
        viewFlow.value = view
    }
}
