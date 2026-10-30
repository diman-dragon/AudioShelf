package com.audioshelf.app.player

import com.audioshelf.app.ScreenWatch
import com.audioshelf.app.library.Background
import com.audioshelf.app.library.DurationReader
import com.audioshelf.app.library.DurationStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Learns the length of every file of the current book in the background, so the book's total and percent are known. */
class DurationFiller(
    private val scope: CoroutineScope,
    private val durations: DurationStore,
    private val background: Background,
    private val screen: ScreenWatch,
    private val durationReader: DurationReader,
    private val current: CurrentBook,
    private val publisher: PlayerUiPublisher,
) {
    private var job: Job? = null

    fun fill(t: PlayBook) {
        job?.cancel()
        job = scope.launch {
            t.items.forEachIndexed { index, item ->
                if (durations.get(t.id)[index] != null) return@forEachIndexed
                background.calm()
                screen.on.first { it }
                val total = durationReader.read(item.uri) ?: return@forEachIndexed
                val ms = if (item.clipped) total - item.startMs else total
                if (ms <= 0) return@forEachIndexed
                durations.note(t.id, index, ms)
                if (current.get()?.id == t.id) publisher.publish()
            }
        }
    }
}
