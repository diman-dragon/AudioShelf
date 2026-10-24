@file:OptIn(UnstableApi::class)

package com.audioshelf.app.player

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import com.audioshelf.app.sound.SoundOutput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TICK_MS = 250L

class PlaybackEngine(
    private val app: Application,
    private val scope: CoroutineScope,
    private val sound: SoundOutput,
    private val offload: OffloadControl,
    private val skipBackMs: () -> Long,
    private val skipForwardMs: () -> Long,
    private val onSave: () -> Unit,
) {
    private val flow = MutableStateFlow(PlayState())
    private var exo: ExoPlayer? = null
    private var fader: EdgeFader? = null
    private var chapterStop: ChapterStop? = null
    private var sleep: SleepTimer? = null
    private var saver: PositionSaver? = null
    private var ticker: Job? = null
    private var speech = false

    val state: StateFlow<PlayState> = flow.asStateFlow()

    val player: ExoPlayer
        get() = exo ?: build().also { exo = it }

    val created: Boolean get() = exo != null

    private fun renderers() = object : DefaultRenderersFactory(app) {
        override fun buildAudioSink(context: Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean): AudioSink =
            DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessors(arrayOf<AudioProcessor>(sound.processor))
                .build()
    }

    private fun attributes(speech: Boolean) = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(if (speech) C.AUDIO_CONTENT_TYPE_SPEECH else C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    private fun build(): ExoPlayer {
        val extractors = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)
        val p = ExoPlayer.Builder(app, renderers(), DefaultMediaSourceFactory(app, extractors))
            .setAudioAttributes(attributes(false), true)
            .setHandleAudioBecomingNoisy(true)
            .setSeekParameters(SeekParameters.EXACT)
            .setSeekBackIncrementMs(skipBackMs())
            .setSeekForwardIncrementMs(skipForwardMs())
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        val volume = { refreshVolume() }
        fader = EdgeFader(p, volume)
        chapterStop = ChapterStop(p)
        sleep = SleepTimer(p, volume)
        ResumeRewind(p)
        saver = PositionSaver(p, onSave)
        offload.attach(p)
        p.addListener(Events())
        return p
    }

    private inner class Events : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            publish()
            if (isPlaying) startTicker() else ticker?.cancel()
        }

        override fun onPlaybackStateChanged(playbackState: Int) = publish()

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            sound.useTrimOf(mediaItem?.mediaId)
            publish()
        }

        override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) = publish()

        override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) = publish()
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                delay(TICK_MS)
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

    fun load(items: List<MediaItem>, index: Int, positionMs: Long, autoplay: Boolean, speed: Float) {
        val p = player
        p.setMediaItems(items, index, positionMs)
        p.prepare()
        p.setPlaybackSpeed(speed)
        p.playWhenReady = autoplay
        sound.useTrimOf(items.getOrNull(index)?.mediaId)
        if (autoplay) startService()
        publish()
    }

    fun startService() {
        runCatching { app.startService(Intent(app, PlaybackService::class.java)) }
    }

    fun play() {
        startService()
        player.play()
    }

    fun pause() {
        exo?.pause()
    }

    fun toggle() {
        val p = player
        val ended = p.playbackState == Player.STATE_ENDED
        if (p.playWhenReady && !ended) {
            p.pause()
            return
        }
        if (ended) p.seekTo(0, 0)
        play()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        publish()
    }

    fun skipTo(index: Int, positionMs: Long = 0) {
        player.seekTo(index, positionMs)
        publish()
    }

    fun next() = player.seekToNextMediaItem()

    fun previous() = player.seekToPrevious()

    fun setSpeed(speed: Float) {
        exo?.setPlaybackSpeed(speed)
    }

    fun setSleep(minutes: Long, chapter: Boolean) {
        val ready = player
        if (ready.mediaItemCount >= 0) sleep?.set(minutes, chapter)
    }

    fun setProfile(profile: PlayProfile) {
        val p = player
        p.repeatMode = profile.repeat
        p.shuffleModeEnabled = profile.shuffle
        chapterStop?.set(profile.pauseAfter)
        fader?.set(profile.fadeInMs, profile.fadeOutMs)
        if (profile.speech != speech) {
            speech = profile.speech
            p.setAudioAttributes(attributes(profile.speech), true)
        }
    }

    fun flush() {
        saver?.flush()
    }

    fun stop() {
        val p = exo ?: return
        p.stop()
        p.clearMediaItems()
        publish()
    }

    fun close() {
        flush()
        val p = exo ?: return
        p.pause()
        p.clearMediaItems()
        publish()
    }

    fun release() {
        ticker?.cancel()
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
