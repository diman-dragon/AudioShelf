package com.audioshelf.app.player

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.Player

private const val TICK_MS = 1000L
private const val FADE_TICK_MS = 200L
private const val FADE_MS = 15_000L

class SleepTimer(
    private val player: Player,
    private val onChange: () -> Unit,
    private val pauseAtEnd: PauseAtEnd,
) : Player.Listener, Runnable {
    private val handler = Handler(Looper.getMainLooper())
    @Volatile var left = 0L
        private set
    @Volatile var atChapterEnd = false
        private set
    private var lastTick = 0L

    init {
        player.addListener(this)
    }

    fun set(minutes: Long, chapter: Boolean) {
        handler.post {
            handler.removeCallbacks(this)
            left = minutes * 60_000L
            atChapterEnd = chapter && left > 0
            pauseAtEnd.setForSleep(atChapterEnd)
            lastTick = SystemClock.elapsedRealtime()
            onChange()
            if (left > 0) handler.postDelayed(this, TICK_MS)
        }
    }

    fun factor(): Float {
        val l = left
        return if (l in 1 until FADE_MS) l.toFloat() / FADE_MS else 1f
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        left = 0
        atChapterEnd = false
        pauseAtEnd.setForSleep(false)
    }

    override fun run() {
        val now = SystemClock.elapsedRealtime()
        if (player.isPlaying) left -= now - lastTick
        lastTick = now
        if (left <= 0) {
            left = 0
            atChapterEnd = false
            pauseAtEnd.setForSleep(false)
            player.pause()
            onChange()
            return
        }
        onChange()
        handler.postDelayed(this, if (left < FADE_MS) FADE_TICK_MS else TICK_MS)
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM && atChapterEnd) {
            atChapterEnd = false
            pauseAtEnd.setForSleep(false)
            onChange()
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_ENDED && atChapterEnd) {
            atChapterEnd = false
            pauseAtEnd.setForSleep(false)
            onChange()
        }
    }
}
