package com.audioshelf.app.library

import android.os.Process
import android.os.SystemClock
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay

private const val POLL_MS = 200L

/**
 * Everything that is not playback (cover extraction, duration probing) runs here: one thread at a
 * lower priority than the UI, and it waits while the player is starting or re-buffering, so it can
 * never compete with the audio for the storage provider and the CPU.
 */
class Background(private val clock: () -> Long = { SystemClock.elapsedRealtime() }) {
    val dispatcher: CoroutineDispatcher = Executors.newSingleThreadExecutor { task ->
        Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND - 1)
            task.run()
        }, "audioshelf-background").apply { isDaemon = true }
    }.asCoroutineDispatcher()

    /** The database writer: its own low-priority thread, so a slow cover never delays a save and a save never runs on the UI thread. */
    val writer: CoroutineDispatcher = Executors.newSingleThreadExecutor { task ->
        Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND - 1)
            task.run()
        }, "audioshelf-db").apply { isDaemon = true }
    }.asCoroutineDispatcher()

    @Volatile private var until = 0L

    /** Keeps background work waiting for at least [ms] from now. */
    fun hold(ms: Long) {
        val end = clock() + ms
        if (end > until) until = end
    }

    suspend fun calm() {
        while (true) {
            val left = until - clock()
            if (left <= 0) return
            delay(minOf(left, POLL_MS))
        }
    }
}
