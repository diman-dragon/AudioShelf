package com.audioshelf.app.diag

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.audioshelf.app.AppLog
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private const val TAG = "Jitter"
private const val INTERVAL_MS = 250L
private const val STALL_MS = 300L

/**
 * While audio plays, a thread that does nothing but sleep 250 ms and measure how late it wakes up.
 * When the system takes CPU time away from the app (battery limits, a busy phone, a notification
 * waking everything up), this thread is late by the same amount the decoder is, so every stutter
 * has a line here with its length. It also tells a stall from a sleep of the whole device.
 */
class JitterWatch(private val power: () -> String, private val screenOn: () -> Boolean) {
    private val lock = ReentrantLock()
    private val awake = lock.newCondition()
    private var active = false
    private val main = Handler(Looper.getMainLooper())
    private val worstThread = AtomicLong()
    private val worstMain = AtomicLong()
    @Volatile private var probing = false

    init {
        Thread({ loop() }, "audioshelf-jitter").apply { isDaemon = true }.start()
    }

    fun setActive(on: Boolean) {
        lock.withLock {
            active = on
            if (on) awake.signalAll()
        }
    }

    /** The worst thread delay and the worst main-thread delay in ms since the last call. */
    fun drainWorst(): Pair<Long, Long> = worstThread.getAndSet(0) to worstMain.getAndSet(0)

    private fun loop() {
        while (true) {
            lock.withLock { while (!active) awake.awaitUninterruptibly() }
            val up = SystemClock.uptimeMillis()
            val real = SystemClock.elapsedRealtime()
            Thread.sleep(INTERVAL_MS)
            val late = SystemClock.uptimeMillis() - up - INTERVAL_MS
            val asleep = SystemClock.elapsedRealtime() - real - INTERVAL_MS - late
            if (late > worstThread.get()) worstThread.set(late)
            if (late > STALL_MS) {
                AppLog.w(TAG, "STALL: a thread that should wake every $INTERVAL_MS ms woke $late ms late, screen ${state()}, ${power()}")
            }
            if (asleep > STALL_MS) {
                AppLog.w(TAG, "SUSPENDED: the whole device slept for about $asleep ms during playback, screen ${state()}")
            }
            probeMain()
        }
    }

    private fun probeMain() {
        if (probing) return
        probing = true
        val posted = SystemClock.uptimeMillis()
        main.post {
            val lag = SystemClock.uptimeMillis() - posted
            probing = false
            if (lag > worstMain.get()) worstMain.set(lag)
            if (lag > STALL_MS) AppLog.w(TAG, "the main thread was busy for $lag ms, screen ${state()}")
        }
    }

    private fun state() = if (screenOn()) "on" else "off"
}
