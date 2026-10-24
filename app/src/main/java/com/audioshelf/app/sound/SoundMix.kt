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
    private val output: SoundOutput,
) {
    fun start() {
        scope.launch {
            combine(track, eq.version) { book, _ -> output.apply(eq.effective(book?.id).bands) }.collect { }
        }
    }
}
