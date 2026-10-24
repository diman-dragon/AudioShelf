package com.audioshelf.app.sound

import android.content.Context
import android.net.Uri
import android.os.Process
import com.audioshelf.app.library.LibFile
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class NormStatus { Idle, Running, Done, Cancelled, Battery }

data class NormState(
    val status: NormStatus = NormStatus.Idle,
    val done: Int = 0,
    val total: Int = 0,
    val failed: Int = 0,
    val current: String = "",
    val paused: Boolean = false,
)

class Normalizer(
    private val scope: CoroutineScope,
    private val context: Context,
    private val files: () -> List<LibFile>,
    private val gains: GainStore,
) {
    private val meter = LoudnessMeter(context)
    private val cancel = AtomicBoolean()
    private val flow = MutableStateFlow(NormState())
    private val worker = Executors.newSingleThreadExecutor { r -> Thread(r, "loudness").apply { priority = Thread.MIN_PRIORITY } }
        .asCoroutineDispatcher()

    val state: StateFlow<NormState> = flow.asStateFlow()

    fun pending(): List<LibFile> = files().filter { !gains.has(it.uri) }

    fun start(auto: Boolean = false) {
        if (flow.value.status == NormStatus.Running) return
        val todo = pending()
        if (todo.isEmpty()) {
            flow.value = NormState(NormStatus.Done)
            return
        }
        if (auto && BatteryGuard.shouldPause(context, true)) return
        cancel.set(false)
        flow.value = NormState(NormStatus.Running, total = todo.size)
        scope.launch { run(todo, auto) }
    }

    fun stop() {
        cancel.set(true)
    }

    fun pause() {
        flow.update { it.copy(paused = true) }
        cancel.set(true)
    }

    private suspend fun run(todo: List<LibFile>, auto: Boolean) {
        val battery = BatteryWatch(context, auto)
        val stop = { cancel.get() || battery.tripped() }
        var failed = 0
        for (file in todo) {
            if (stop()) break
            flow.update { it.copy(current = file.name) }
            val gain = withContext(worker) {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                runCatching { meter.gainFor(Uri.parse(file.uri), stop) }.getOrNull()
            }
            if (stop()) break
            if (gain == null) failed++
            gains.save(file.uri, (gain ?: 0.0).toFloat())
            flow.update { it.copy(done = it.done + 1, failed = failed) }
        }
        val status = when {
            cancel.get() -> NormStatus.Cancelled
            battery.tripped() -> if (auto) NormStatus.Idle else NormStatus.Battery
            else -> NormStatus.Done
        }
        flow.update { it.copy(status = status, failed = failed, current = "") }
    }
}
