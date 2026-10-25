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
    private val fxStore: FxStore,
    private val output: SoundOutput,
    private val onPlayer: (pitch: Float, skipSilence: Boolean) -> Unit = { _, _ -> },
) {
    fun start() {
        scope.launch {
            combine(track, eq.version, fxStore.version) { book, _, _ ->
                val fx = fxStore.effective(book?.id)
                output.apply(eq.effective(book?.id).bands, fx)
                onPlayer(fx.pitchFactor, fx.skipSilence)
            }.collect { }
        }
    }
}
