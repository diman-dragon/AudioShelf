package com.audioshelf.app.sound

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

private const val FULL_SCALE = 32768f

/**
 * What the sound processor saw since the last [drain]: how loud the source is, how often the source
 * itself is clipped, and how often the limiter had to bend the signal. A raspy voice is either already
 * in the file (source clipped), or made by the equalizer and boost pushing the signal into the limiter.
 */
class SoundStats {
    class Snapshot(
        val samples: Long,
        val sourceClipped: Long,
        val limited: Long,
        val overLimit: Long,
        val peakIn: Int,
        val peakOut: Int,
    ) {
        fun describe(): String =
            "sound[in-peak=${percent(peakIn)}% out-peak=${percent(peakOut)}% source-clipped=$sourceClipped " +
                "limiter-hits=$limited over-full-scale=$overLimit of $samples samples]"

        private fun percent(peak: Int) = (peak * 100f / FULL_SCALE).toInt()
    }

    private val samples = AtomicLong()
    private val sourceClipped = AtomicLong()
    private val limited = AtomicLong()
    private val overLimit = AtomicLong()
    private val peakIn = AtomicInteger()
    private val peakOut = AtomicInteger()

    fun record(sampleCount: Int, sourceClippedCount: Int, limitedCount: Int, overCount: Int, inPeak: Int, outPeak: Int) {
        samples.addAndGet(sampleCount.toLong())
        sourceClipped.addAndGet(sourceClippedCount.toLong())
        limited.addAndGet(limitedCount.toLong())
        overLimit.addAndGet(overCount.toLong())
        raise(peakIn, inPeak)
        raise(peakOut, outPeak)
    }

    fun drain(): Snapshot = Snapshot(
        samples.getAndSet(0), sourceClipped.getAndSet(0), limited.getAndSet(0), overLimit.getAndSet(0),
        peakIn.getAndSet(0), peakOut.getAndSet(0),
    )

    private fun raise(target: AtomicInteger, value: Int) {
        while (true) {
            val seen = target.get()
            if (value <= seen || target.compareAndSet(seen, value)) return
        }
    }
}
