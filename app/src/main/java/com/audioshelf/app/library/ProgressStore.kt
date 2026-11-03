package com.audioshelf.app.library

import com.audioshelf.app.data.MapStore
import com.audioshelf.app.data.Kv
import com.audioshelf.app.data.KvOp
import com.audioshelf.app.data.flag
import com.audioshelf.app.data.int
import com.audioshelf.app.data.long
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ProgressState { New, Started, Done }

class Spot(val index: Int, val positionMs: Long, val time: Long)

const val REWIND_MS = 5000L

class ProgressStore(private val kv: Kv) {
    private val spots = MapStore(kv, "pos")
    private val infos = MapStore(kv, "pinfo")
    private val counter = MutableStateFlow(0)

    val version: StateFlow<Int> = counter.asStateFlow()

    @Synchronized
    fun spot(id: String): Spot? = spots.get(id)?.let { Spot(it.int("i") ?: 0, it.long("p") ?: 0, it.long("t") ?: 0) }

    @Synchronized
    private fun done(id: String) = infos.get(id)?.flag("done") ?: false

    @Synchronized
    fun state(id: String): ProgressState {
        if (done(id)) return ProgressState.Done
        val spot = spot(id)
        return if (spot != null && (spot.index > 0 || spot.positionMs > 0)) ProgressState.Started else ProgressState.New
    }

    @Synchronized
    fun percent(id: String): Int? {
        if (done(id)) return 100
        if (state(id) != ProgressState.Started) return null
        val pc = infos.get(id)?.int("pc") ?: 0
        return pc.coerceIn(0, 99)
    }

    @Synchronized
    fun lastPlayed(id: String): Long = spot(id)?.time ?: 0

    @Synchronized
    fun save(
        id: String,
        index: Int,
        positionMs: Long,
        done: Boolean,
        percent: Int,
        expanded: Boolean = false,
        now: Long = System.currentTimeMillis(),
        notify: Boolean = true,
    ) {
        val spot = linkedMapOf<String, Any?>("i" to index, "p" to positionMs, "t" to now)
        val info = linkedMapOf<String, Any?>("done" to done, "x" to if (expanded) 1 else 0, "pc" to percent)
        spotsPutTogether(id, spot, info)
        if (notify) counter.update { it + 1 }
    }

    @Synchronized
    fun remap(id: String, map: (Int, Long) -> Pair<Int, Long>) {
        val spot = spot(id) ?: return
        val info = infos.get(id)
        if (info?.flag("x") == true) return
        val (index, position) = map(spot.index, spot.positionMs)
        val newSpot = linkedMapOf<String, Any?>("i" to index, "p" to position, "t" to spot.time)
        val newInfo = linkedMapOf<String, Any?>(
            "done" to (info?.flag("done") ?: false),
            "x" to 1,
            "pc" to (info?.int("pc") ?: 0),
        )
        spotsPutTogether(id, newSpot, newInfo)
        counter.update { it + 1 }
    }

    @Synchronized
    fun resume(id: String): Pair<Int, Long> {
        val spot = spot(id)
        if (spot == null || done(id)) return 0 to 0L
        return spot.index to maxOf(0L, spot.positionMs - REWIND_MS)
    }

    @Synchronized
    fun move(from: String, to: String) {
        if (from == to) return
        val oldSpot = spots.get(from)
        val oldInfo = infos.get(from)
        val ops = ArrayList<KvOp>(4)
        if (oldSpot != null) {
            ops.add(spots.deleteOp(from))
            ops.add(spots.putOp(to, oldSpot))
        }
        if (oldInfo != null) {
            ops.add(infos.deleteOp(from))
            ops.add(infos.putOp(to, oldInfo))
        }
        if (ops.isEmpty()) return
        kv.batch(ops)
        if (oldSpot != null) {
            spots.cacheDelete(from)
            spots.cachePut(to, oldSpot)
        }
        if (oldInfo != null) {
            infos.cacheDelete(from)
            infos.cachePut(to, oldInfo)
        }
        counter.update { it + 1 }
    }

    @Synchronized
    fun forget(id: String) {
        kv.batch(listOf(spots.deleteOp(id), infos.deleteOp(id)))
        spots.cacheDelete(id)
        infos.cacheDelete(id)
        counter.update { it + 1 }
    }

    private fun spotsPutTogether(id: String, spot: Map<String, Any?>, info: Map<String, Any?>) {
        kv.batch(listOf(spots.putOp(id, spot), infos.putOp(id, info)))
        spots.cachePut(id, spot)
        infos.cachePut(id, info)
    }
}
