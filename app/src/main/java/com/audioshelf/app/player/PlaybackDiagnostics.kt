@file:androidx.annotation.OptIn(markerClass = [UnstableApi::class])

package com.audioshelf.app.player

import android.os.Debug
import android.os.Process
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import com.audioshelf.app.AppLog
import com.audioshelf.app.ScreenWatch
import com.audioshelf.app.diag.JitterWatch
import com.audioshelf.app.diag.SystemWatch
import com.audioshelf.app.sound.SoundStats
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "Playback"
private const val BEAT_MS = 2_000L
private const val ENVIRONMENT_SAMPLE_MS = 5_000L

private data class Underrun(
    val positionMs: Long,
    val bufferBytes: Int,
    val bufferMs: Long,
    val elapsedSinceFeedMs: Long,
    val elapsedRealtimeMs: Long,
)

/**
 * Everything the player says about itself, in the log: states, the decoder and audio track in use,
 * buffer underruns (each one is an audible stutter), seeks, errors. While playing, a line every
 * 2 s sums up the buffer, CPU, garbage collections, the sound levels and the power state, so the
 * seconds before a stutter can be read back.
 */
class PlaybackDiagnostics(
    private val player: ExoPlayer,
    private val stats: SoundStats,
    private val screen: ScreenWatch,
    private val system: SystemWatch,
    private val scope: CoroutineScope,
) : AnalyticsListener {
    private val jitter = JitterWatch(system::powerLine) { screen.on.value }
    private var beat: Job? = null
    private var environment: Job? = null
    private val underruns = AtomicInteger()
    private val underrunsTotal = AtomicInteger()
    private val firstUnderrun = AtomicReference<Underrun?>()
    private val lastUnderrun = AtomicReference<Underrun?>()
    @Volatile private var environmentSnapshot = "system-state=pending"
    private var lastCpuMs = Process.getElapsedCpuTime()
    private var lastWallMs = SystemClock.elapsedRealtime()
    private var lastGc = gcCount("art.gc.gc-count")
    private var lastBlockingGc = gcCount("art.gc.blocking-gc-count")

    fun setPlaying(on: Boolean) {
        jitter.setActive(on)
        beat?.cancel()
        environment?.cancel()
        if (!on) {
            logUnderruns("playback stopped")
            return
        }
        environment = scope.launch {
            while (isActive) {
                environmentSnapshot = withContext(Dispatchers.IO) { sampleEnvironment() }
                delay(ENVIRONMENT_SAMPLE_MS)
            }
        }
        lastCpuMs = Process.getElapsedCpuTime()
        lastWallMs = SystemClock.elapsedRealtime()
        beat = scope.launch {
            while (isActive) {
                delay(BEAT_MS)
                heartbeat()
            }
        }
    }

    fun release() {
        beat?.cancel()
        environment?.cancel()
        jitter.setActive(false)
        jitter.close()
    }

    private fun heartbeat() {
        val position = player.currentPosition
        val ahead = (player.bufferedPosition - position).coerceAtLeast(0)
        val cpuNow = Process.getElapsedCpuTime()
        val wallNow = SystemClock.elapsedRealtime()
        val wall = (wallNow - lastWallMs).coerceAtLeast(1)
        val cpu = (cpuNow - lastCpuMs) * 100 / wall
        lastCpuMs = cpuNow
        lastWallMs = wallNow
        val gc = gcCount("art.gc.gc-count")
        val blocking = gcCount("art.gc.blocking-gc-count")
        val gcLine = "gc=${gc - lastGc}(blocking ${blocking - lastBlockingGc})"
        lastGc = gc
        lastBlockingGc = blocking
        val (threadLate, mainLate) = jitter.drainWorst()
        AppLog.i(
            TAG,
            "beat: pos=${position / 1000}s buffered-ahead=${ahead}ms state=${stateName(player.playbackState)} " +
                "loading=${player.isLoading} speed=${player.playbackParameters.speed} volume=${"%.2f".format(player.volume)} " +
                "screen=${if (screen.on.value) "on" else "off"} underruns=${underruns.get()} cpu=$cpu% $gcLine " +
                "worst-thread-delay=${threadLate}ms worst-main-delay=${mainLate}ms " +
                "${stats.drain().describe()} $environmentSnapshot",
        )
        logUnderruns("heartbeat")
    }

    private fun sampleEnvironment(): String = listOf(
        "power" to system::powerLine,
        "device" to system::resourceLine,
        "route" to system::routeLine,
    ).joinToString(" ") { (name, read) ->
        try {
            read()
        } catch (e: Exception) {
            AppLog.w(TAG, "could not sample $name state for playback diagnostics", e)
            "$name=unavailable"
        }
    }

    private fun logUnderruns(reason: String) {
        val count = underruns.getAndSet(0)
        if (count == 0) return
        val first = firstUnderrun.getAndSet(null)
        val last = lastUnderrun.getAndSet(null)
        val now = SystemClock.elapsedRealtime()
        // This line is written at the next beat (or when playback stops), so say when the burst really was.
        val timing = if (first != null && last != null) {
            "burst lasted ${last.elapsedRealtimeMs - first.elapsedRealtimeMs}ms, ended ${now - last.elapsedRealtimeMs}ms before this line, " +
                "first at pos=${first.positionMs}ms"
        } else {
            "timing unavailable"
        }
        AppLog.w(
            "Stutter",
            "UNDERRUN burst: count=$count total=${underrunsTotal.get()} reason=$reason $timing " +
                "last=${last?.let { "pos=${it.positionMs}ms buffer=${it.bufferBytes}B/${it.bufferMs}ms " +
                    "feed-gap=${it.elapsedSinceFeedMs}ms" } ?: "details unavailable"} " +
                "output=${outputName()} screen=${if (screen.on.value) "on" else "off"} $environmentSnapshot",
        )
    }

    private fun outputName(): String = try {
        system.mediaOutput()
    } catch (e: Exception) {
        "unavailable"
    }

    /** A fresh reading, for the lines that mark a change (the periodic one can be seconds old, or not taken yet). */
    private fun refreshEnvironment() {
        environmentSnapshot = sampleEnvironment()
    }

    private fun gcCount(key: String): Long = Debug.getRuntimeStat(key)?.toLongOrNull() ?: 0L

    private fun stateName(state: Int) = when (state) {
        Player.STATE_IDLE -> "IDLE"
        Player.STATE_BUFFERING -> "BUFFERING"
        Player.STATE_READY -> "READY"
        Player.STATE_ENDED -> "ENDED"
        else -> "state $state"
    }

    private fun at(time: AnalyticsListener.EventTime) = "at ${time.currentPlaybackPositionMs / 1000}s"

    override fun onPlaybackStateChanged(eventTime: AnalyticsListener.EventTime, state: Int) {
        AppLog.i(TAG, "state ${stateName(state)} ${at(eventTime)}, screen ${if (screen.on.value) "on" else "off"}")
    }

    override fun onPlayWhenReadyChanged(eventTime: AnalyticsListener.EventTime, playWhenReady: Boolean, reason: Int) {
        val why = when (reason) {
            Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST -> "user"
            Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS -> "AUDIO FOCUS LOSS (another app took the audio)"
            Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY -> "audio becoming noisy"
            Player.PLAY_WHEN_READY_CHANGE_REASON_REMOTE -> "remote (headset, notification, car)"
            Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM -> "end of item"
            Player.PLAY_WHEN_READY_CHANGE_REASON_SUPPRESSED_TOO_LONG -> "suppressed too long"
            else -> "reason $reason"
        }
        AppLog.i(TAG, "play-when-ready=$playWhenReady because $why ${at(eventTime)}")
    }

    override fun onPlaybackSuppressionReasonChanged(eventTime: AnalyticsListener.EventTime, playbackSuppressionReason: Int) {
        val why = if (playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS) {
            "TRANSIENT AUDIO FOCUS LOSS (another app is playing a sound, e.g. a notification)"
        } else {
            "none"
        }
        AppLog.w(TAG, "playback suppression: $why ${at(eventTime)}")
    }

    override fun onIsPlayingChanged(eventTime: AnalyticsListener.EventTime, isPlaying: Boolean) {
        refreshEnvironment()
        AppLog.i(
            TAG,
            "playing=$isPlaying ${at(eventTime)}, screen ${if (screen.on.value) "on" else "off"}, $environmentSnapshot",
        )
    }

    override fun onIsLoadingChanged(eventTime: AnalyticsListener.EventTime, isLoading: Boolean) {
        AppLog.d(TAG, "loading=$isLoading ${at(eventTime)}")
    }

    override fun onAudioUnderrun(eventTime: AnalyticsListener.EventTime, bufferSize: Int, bufferSizeMs: Long, elapsedSinceLastFeedMs: Long) {
        val underrun = Underrun(
            eventTime.currentPlaybackPositionMs,
            bufferSize,
            bufferSizeMs,
            elapsedSinceLastFeedMs,
            SystemClock.elapsedRealtime(),
        )
        firstUnderrun.compareAndSet(null, underrun)
        lastUnderrun.set(underrun)
        underrunsTotal.incrementAndGet()
        underruns.incrementAndGet()
    }

    override fun onVolumeChanged(eventTime: AnalyticsListener.EventTime, volume: Float) {
        AppLog.d(TAG, "player volume ${"%.2f".format(volume)} ${at(eventTime)}")
    }

    override fun onDeviceVolumeChanged(eventTime: AnalyticsListener.EventTime, volume: Int, muted: Boolean) {
        AppLog.i(TAG, "phone media volume $volume, muted=$muted ${at(eventTime)}")
    }

    override fun onMediaItemTransition(eventTime: AnalyticsListener.EventTime, mediaItem: MediaItem?, reason: Int) {
        AppLog.i(TAG, "item ${mediaItem?.mediaId ?: "none"} (reason $reason) ${at(eventTime)}")
    }

    override fun onAudioPositionAdvancing(eventTime: AnalyticsListener.EventTime, playoutStartSystemTimeMs: Long) {
        AppLog.d(TAG, "audio started coming out of ${outputName()} ${at(eventTime)}")
    }

    override fun onAudioSinkError(eventTime: AnalyticsListener.EventTime, audioSinkError: Exception) {
        AppLog.e(TAG, "audio sink error ${at(eventTime)}", audioSinkError)
    }

    override fun onAudioCodecError(eventTime: AnalyticsListener.EventTime, audioCodecError: Exception) {
        AppLog.e(TAG, "audio codec error ${at(eventTime)}", audioCodecError)
    }

    override fun onAudioDecoderInitialized(
        eventTime: AnalyticsListener.EventTime,
        decoderName: String,
        initializedTimestampMs: Long,
        initializationDurationMs: Long,
    ) {
        AppLog.i(TAG, "decoder: $decoderName (initialized in $initializationDurationMs ms)")
    }

    override fun onAudioInputFormatChanged(
        eventTime: AnalyticsListener.EventTime,
        format: Format,
        decoderReuseEvaluation: DecoderReuseEvaluation?,
    ) {
        AppLog.i(
            TAG,
            "audio format: ${format.sampleMimeType} ${format.codecs.orEmpty()} ${format.sampleRate} Hz, " +
                "${format.channelCount} ch, bitrate ${format.bitrate}",
        )
    }

    override fun onAudioTrackInitialized(eventTime: AnalyticsListener.EventTime, audioTrackConfig: AudioSink.AudioTrackConfig) {
        AppLog.i(
            TAG,
            "audio track created: ${audioTrackConfig.sampleRate} Hz, encoding ${audioTrackConfig.encoding}, " +
                "buffer ${audioTrackConfig.bufferSize} bytes, offload=${audioTrackConfig.offload}, tunneling=${audioTrackConfig.tunneling}",
        )
    }

    override fun onAudioTrackReleased(eventTime: AnalyticsListener.EventTime, audioTrackConfig: AudioSink.AudioTrackConfig) {
        AppLog.d(TAG, "audio track released ${at(eventTime)}")
    }

    override fun onAudioSessionIdChanged(eventTime: AnalyticsListener.EventTime, audioSessionId: Int) {
        AppLog.d(TAG, "audio session id $audioSessionId")
    }

    override fun onAudioAttributesChanged(eventTime: AnalyticsListener.EventTime, audioAttributes: AudioAttributes) {
        AppLog.i(TAG, "audio attributes: usage ${audioAttributes.usage}, content type ${audioAttributes.contentType}")
    }

    override fun onPlaybackParametersChanged(eventTime: AnalyticsListener.EventTime, playbackParameters: PlaybackParameters) {
        AppLog.i(TAG, "speed ${playbackParameters.speed}, pitch ${playbackParameters.pitch} ${at(eventTime)}")
    }

    override fun onPositionDiscontinuity(
        eventTime: AnalyticsListener.EventTime,
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
        val why = when (reason) {
            Player.DISCONTINUITY_REASON_AUTO_TRANSITION -> "next item"
            Player.DISCONTINUITY_REASON_SEEK -> "seek"
            Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT -> "seek adjustment"
            Player.DISCONTINUITY_REASON_SKIP -> "skip"
            Player.DISCONTINUITY_REASON_REMOVE -> "item removed"
            Player.DISCONTINUITY_REASON_INTERNAL -> "internal"
            else -> "reason $reason"
        }
        AppLog.d(TAG, "jump ($why): ${oldPosition.positionMs / 1000}s -> ${newPosition.positionMs / 1000}s")
    }

    override fun onPlayerError(eventTime: AnalyticsListener.EventTime, error: PlaybackException) {
        AppLog.e(TAG, "player error ${error.errorCodeName} ${at(eventTime)}", error)
    }

    override fun onLoadError(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData,
        error: IOException,
        wasCanceled: Boolean,
    ) {
        AppLog.w(TAG, "load error (canceled=$wasCanceled) ${at(eventTime)}", error)
    }
}
