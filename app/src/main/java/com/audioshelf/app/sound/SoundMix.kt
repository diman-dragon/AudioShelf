package com.audioshelf.app.sound

import com.audioshelf.app.player.PlayBook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SoundMix(
    private val scope: CoroutineScope,
    private val track: StateFlow<PlayBook?>,
    private val eq: EqStore,
    private val gains: GainStore,
    private val prefs: SoundPrefs,
    private val output: SoundOutput,
) {
    fun start() {
        scope.launch {
            combine(track, eq.version, gains.version, prefs.boost, prefs.normOn) { book, _, _, boost, norm ->
                apply(book, boost, norm)
            }.collect { }
        }
    }

    private fun apply(book: PlayBook?, boost: Int, normOn: Boolean) {
        val trims = HashMap<String, Float>()
        if (book != null && normOn) {
            book.base.files.forEach { file ->
                val db = gains.get(file.uri)
                if (db != 0f) trims[file.uri] = db
            }
        }
        output.apply(eq.effective(book?.id).bands, trims, boost / 100f)
    }
}
