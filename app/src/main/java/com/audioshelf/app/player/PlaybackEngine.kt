@file:OptIn(UnstableApi::class)

package com.audioshelf.app.player

import com.audioshelf.app.AppLog
import android.app.Application
import android.content.Intent
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.audioshelf.app.ScreenWatch
import com.audioshelf.app.diag.SystemWatch
import com.audioshelf.app.library.Background
import com.audioshelf.app.sound.SoundOutput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TICK_MS = 250L

// Dark screen: nobody watches the position, a slow tick is enough for listening stats (the stats gap limit, MAX_GAP_MS, is well above it).
private const val DARK_TICK_MS = 4_000L

// Background work (covers, durations) stands aside while playback starts, seeks or re-buffers.
private const val START_HOLD_MS = 5_000L
private const val SEEK_HOLD_MS = 2_000L

class PlaybackEngine(
    private val app: Application,
    private val scope: CoroutineScope,
    private val sound: SoundOutput,
    private val offload: OffloadControl,
    private val background: Background,
    private val screen: ScreenWatch,
    private val system: SystemWatch,
    private val skipBackMs: () -> Long,
    private val skipForwardMs: () -> Long,
    private val onSave: (periodic: Boolean) -> Unit,
) {
    private val flow = MutableStateFlow(PlayState())
    private val failureFlow = MutableSharedFlow<PlaybackFailure>(extraBufferCapacity = 4)
    private val factory = PlayerFactory(app, sound)
    private var exo: ExoPlayer? = null
    private var fader: EdgeFader? = null
    private var chapterStop: ChapterStop? = null
    private var sleep: SleepTimer? = null
    private var saver: PositionSaver? = null
    private var diagnostics: PlaybackDiagnostics? = null
    private var focus: FocusPolicy? = null
    private var ticker: Job? = null
    private var speech = false

    val state: StateFlow<PlayState> = flow.asStateFlow()

    /** A file failed to play: shown to the user once, whatever the recovery did about it. */
    val failures: SharedFlow<PlaybackFailure> = failureFlow

    val player: ExoPlayer
        get() = exo ?: build().also { exo = it }

    val created: Boolean get() = exo != null

    private fun build(): ExoPlayer {
        val p = factory.create(speech, skipBackMs(), skipForwardMs())
        val volume = { refreshVolume() }
        fader = EdgeFader(p, volume)
        chapterStop = ChapterStop(p)
        sleep = SleepTimer(p, volume)
        ResumeRewind(p)
        ErrorRecovery(p) { failureFlow.tryEmit(it) }
        saver = PositionSaver(p, onSave)
        offload.attach(p)
        val diag = PlaybackDiagnostics(p, sound.processor.stats, screen, system, scope)
        diagnostics = diag
        p.addAnalyticsListener(diag)
        focus = FocusPolicy(app, p) { speech }
        p.addListener(Events())
        return p
    }

    private inner class Events : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            diagnostics?.setPlaying(isPlaying)
            publish()
            if (isPlaying) startTicker() else ticker?.cancel()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_BUFFERING) background.hold(SEEK_HOLD_MS)
            publish()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            publish()
        }

        override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
            background.hold(SEEK_HOLD_MS)
            publish()
        }

        override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) = publish()
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                delay(if (screen.on.value) TICK_MS else DARK_TICK_MS)
                publish()
            }
        }
    }

    private fun refreshVolume() {
        val p = exo ?: return
        p.volume = (fader?.factor() ?: 1f) * (sleep?.factor() ?: 1f)
        publish()
    }

    fun publish() {
        flow.value = now()
    }

    fun now(): PlayState {
        val p = exo ?: return PlayState()
        val item = p.currentMediaItem
        val duration = p.duration
        return PlayState(
            loaded = item != null,
            mediaId = item?.mediaId ?: "",
            index = p.currentMediaItemIndex,
            count = p.mediaItemCount,
            playing = p.isPlaying,
            loading = p.playbackState == Player.STATE_BUFFERING,
            ended = p.playbackState == Player.STATE_ENDED,
            positionMs = p.currentPosition.coerceAtLeast(0),
            durationMs = if (duration == C.TIME_UNSET) 0 else duration.coerceAtLeast(0),
            speed = p.playbackParameters.speed,
            sleepLeftMs = sleep?.left ?: 0,
            sleepAtChapterEnd = sleep?.atChapterEnd ?: false,
        )
    }

    /**
     * Speed and the sound profile (audio attributes) are set before the items go in: changing the
     * attributes of a playing player rebuilds the audio track, which was the stutter at every start.
     */
    fun load(items: List<MediaItem>, index: Int, positionMs: Long, autoplay: Boolean, speed: Float, profile: PlayProfile) {
        AppLog.i("Engine", "load ${items.size} items, start at item $index ${positionMs / 1000}s, autoplay=$autoplay, speed=$speed")
        background.hold(START_HOLD_MS)
        val p = player
        applyProfile(p, profile)
        p.setPlaybackSpeed(speed)
        p.setMediaItems(items, index, positionMs)
        p.prepare()
        p.playWhenReady = autoplay
        if (autoplay) startService()
        publish()
    }

    fun startService() {
        runCatching { app.startService(Intent(app, PlaybackService::class.java)) }
            .onFailure { AppLog.w("PlaybackEngine", "cannot start the playback service", it) }
    }

    fun play() {
        AppLog.i("Engine", "play requested")
        startService()
        val p = player
        if (p.playbackState == Player.STATE_IDLE && p.currentMediaItem != null) p.prepare()
        p.play()
    }

    fun pause() {
        AppLog.i("Engine", "pause requested")
        exo?.pause()
    }

    fun toggle() {
        AppLog.i("Engine", "toggle requested")
        val p = player
        val ended = p.playbackState == Player.STATE_ENDED
        val idle = p.playbackState == Player.STATE_IDLE
        if (p.playWhenReady && !ended && !idle) {
            p.pause()
            return
        }
        if (ended) p.seekTo(0, 0)
        play()
    }

    fun seekTo(positionMs: Long) {
        AppLog.i("Engine", "seek to ${positionMs / 1000}s")
        player.seekTo(positionMs)
        publish()
    }

    fun skipTo(index: Int, positionMs: Long = 0) {
        AppLog.i("Engine", "skip to item $index at ${positionMs / 1000}s")
        player.seekTo(index, positionMs)
        publish()
    }

    fun next() {
        AppLog.i("Engine", "next item")
        player.seekToNextMediaItem()
    }

    fun previous() {
        AppLog.i("Engine", "previous")
        player.seekToPrevious()
    }

    fun setSpeed(speed: Float) {
        AppLog.i("Engine", "speed $speed")
        exo?.setPlaybackSpeed(speed)
    }

    fun setSleep(minutes: Long, chapter: Boolean) {
        AppLog.i("Engine", "sleep timer $minutes min, at chapter end=$chapter")
        player // built on demand: the timer lives and dies with the player
        sleep?.set(minutes, chapter)
    }

    fun setProfile(profile: PlayProfile) = applyProfile(player, profile)

    private fun applyProfile(p: ExoPlayer, profile: PlayProfile) {
        p.repeatMode = profile.repeat
        p.shuffleModeEnabled = profile.shuffle
        chapterStop?.set(profile.pauseAfter)
        fader?.set(profile.fadeInMs, profile.fadeOutMs)
        if (profile.speech != speech) {
            speech = profile.speech
            p.setAudioAttributes(factory.attributes(profile.speech), false)
        }
    }

    fun flush() {
        saver?.flush()
    }

    fun stop() {
        AppLog.i("Engine", "stop")
        val p = exo ?: return
        p.stop()
        p.clearMediaItems()
        publish()
    }

    fun close() {
        AppLog.i("Engine", "close")
        flush()
        val p = exo ?: return
        p.pause()
        p.clearMediaItems()
        publish()
    }

    fun release() {
        AppLog.i("Engine", "release")
        ticker?.cancel()
        diagnostics?.release()
        diagnostics = null
        focus?.release()
        focus = null
        saver?.stop()
        fader?.stop()
        sleep?.stop()
        offload.detach()
        exo?.release()
        exo = null
        fader = null
        chapterStop = null
        sleep = null
        saver = null
        publish()
    }
}
