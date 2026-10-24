package com.audioshelf.app.sound

import com.audioshelf.app.library.LibStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val WAIT_MS = 4000L
private const val RETRY_MS = 10 * 60_000L
private const val LATER_MS = 30 * 60_000L

class AutoNormalize(
    private val scope: CoroutineScope,
    private val normalizer: Normalizer,
    private val normOn: StateFlow<Boolean>,
    private val libraryStatus: StateFlow<LibStatus>,
    private val scanning: StateFlow<Boolean>,
    private val playing: StateFlow<Boolean>,
    private val triggers: Flow<Any>,
) {
    private var planned: Job? = null
    private var retry: Job? = null
    private var blockedUntil = 0L

    private fun now() = System.currentTimeMillis()

    private fun ready(): Boolean =
        normOn.value &&
            libraryStatus.value == LibStatus.Ok &&
            !scanning.value &&
            !playing.value &&
            now() >= blockedUntil &&
            normalizer.state.value.status != NormStatus.Running &&
            normalizer.pending().isNotEmpty()

    private fun plan() {
        planned?.cancel()
        planned = scope.launch {
            delay(WAIT_MS)
            if (ready()) normalizer.start(auto = true)
        }
    }

    fun start() {
        scope.launch { merge(triggers.map { }, normOn.map { }, libraryStatus.map { }).collect { plan() } }
        scope.launch {
            playing.collect { isPlaying ->
                if (isPlaying && normalizer.state.value.status == NormStatus.Running) normalizer.pause()
                plan()
            }
        }
        scope.launch { scanning.drop(1).collect { blockedUntil = 0 } }
        scope.launch { normalizer.state.drop(1).collect { onNorm(it) } }
        plan()
    }

    private fun onNorm(state: NormState) {
        val pending = normalizer.pending().isNotEmpty()
        if (state.status == NormStatus.Idle && !state.paused && pending) {
            blockedUntil = now() + RETRY_MS
            retry?.cancel()
            retry = scope.launch {
                delay(RETRY_MS + 500)
                plan()
            }
        }
        if ((state.status == NormStatus.Cancelled && !state.paused) || state.status == NormStatus.Battery) blockedUntil = Long.MAX_VALUE
        if (state.status == NormStatus.Done && pending) blockedUntil = now() + LATER_MS
        plan()
    }
}
