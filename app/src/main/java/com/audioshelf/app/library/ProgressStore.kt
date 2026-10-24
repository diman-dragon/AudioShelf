package com.audioshelf.app.library

import com.audioshelf.app.data.Kv
import com.audioshelf.app.data.MapStore
import com.audioshelf.app.data.flag
import com.audioshelf.app.data.int
import com.audioshelf.app.data.long
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ProgressState { New, Started, Done }

class Spot(val index: Int, val positionMs: Long, val time: Long)

const val REWIND_MS = 5000L

class ProgressStore(kv: Kv) {
    private val spots = MapStore(kv, "pos")
    private val infos = MapStore(kv, "pinfo")
    private val counter = MutableStateFlow(0)

    val version: StateFlow<Int> = counter.asStateFlow()

    fun spot(id: String): Spot? = spots.get(id)?.let { Spot(it.int("i") ?: 0, it.long("p") ?: 0, it.long("t") ?: 0) }

    private fun done(id: String) = infos.get(id)?.flag("done") ?: false

    fun state(id: String): ProgressState {
        if (done(id)) return ProgressState.Done
        val spot = spot(id)
        return if (spot != null && (spot.index > 0 || spot.positionMs > 0)) ProgressState.Started else ProgressState.New
    }

    fun percent(id: String): Int? {
        if (done(id)) return 100
        if (state(id) != ProgressState.Started) return null
        val pc = infos.get(id)?.int("pc") ?: 0
        return pc.coerceIn(0, 99)
    }

    fun lastPlayed(id: String): Long = spot(id)?.time ?: 0

    fun save(
        id: String,
        index: Int,
        positionMs: Long,
        done: Boolean,
        percent: Int,
        expanded: Boolean = false,
        now: Long = System.currentTimeMillis(),
    ) {
        spots.set(id, linkedMapOf("i" to index, "p" to positionMs, "t" to now))
        infos.set(id, linkedMapOf("done" to done, "x" to if (expanded) 1 else 0, "pc" to percent))
        counter.value++
    }

    fun remap(id: String, map: (Int, Long) -> Pair<Int, Long>) {
        val spot = spot(id) ?: return
        val info = infos.get(id)
        if (info?.flag("x") == true) return
        val (index, position) = map(spot.index, spot.positionMs)
        spots.set(id, linkedMapOf("i" to index, "p" to position, "t" to spot.time))
        infos.set(id, linkedMapOf("done" to (info?.flag("done") ?: false), "x" to 1, "pc" to (info?.int("pc") ?: 0)))
        counter.value++
    }

    fun resume(id: String): Pair<Int, Long> {
        val spot = spot(id)
        if (spot == null || done(id)) return 0 to 0L
        return spot.index to maxOf(0L, spot.positionMs - REWIND_MS)
    }

    fun move(from: String, to: String) {
        spots.move(from, to)
        infos.move(from, to)
        counter.value++
    }

    fun forget(id: String) {
        spots.del(id)
        infos.del(id)
        counter.value++
    }
}
