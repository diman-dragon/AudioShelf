package com.audioshelf.app.player

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TICK_MS = 250L

class PlayerViewModel : ViewModel(), PlayerActions {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val session = FakeSession(SAMPLE_BOOK)
    private val state = MutableStateFlow(session.toUi())
    private var ticker: Job? = null

    val ui: StateFlow<PlayerUi> = state.asStateFlow()

    private fun publish() {
        state.value = session.toUi()
        if (session.playing && ticker?.isActive != true) {
            ticker = scope.launch {
                while (session.playing) {
                    delay(TICK_MS)
                    session.tick(TICK_MS)
                    state.value = session.toUi()
                }
            }
        }
    }

    override fun onToggle() { session.toggle(); publish() }
    override fun onPrev() { session.previous(); publish() }
    override fun onNext() { if (session.book.chapters.size > 1) { session.step(1); publish() } }
    override fun onBack() { session.skip(false); publish() }
    override fun onForward() { session.skip(true); publish() }
    override fun onSeek(positionMs: Long) { session.seek(positionMs); publish() }
    override fun onSpeed() { session.cycleSpeed(); publish() }
    override fun onSleep() { session.cycleSleep(); publish() }
    override fun onList() {}
    override fun onMode() { session.cycleMode(); publish() }
    override fun onMarks() { session.marks++; publish() }
    override fun onToLibrary() {}

    override fun onCleared() {
        scope.cancel()
    }
}
