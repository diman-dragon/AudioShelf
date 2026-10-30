@file:OptIn(UnstableApi::class)

package com.audioshelf.app.player

import android.app.Application
import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.DefaultAudioTrackBufferSizeProvider
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import com.audioshelf.app.sound.SoundOutput

// Read-ahead: plenty of audio is cheap (about 1 MB a minute), and a full buffer rides out a slow
// storage provider while covers, notifications or the system are busy.
private const val MIN_BUFFER_MS = 30_000
private const val MAX_BUFFER_MS = 60_000
private const val START_BUFFER_MS = 1_000
private const val REBUFFER_MS = 3_000

// Audio track buffer. With the screen off many phones throttle the app's CPU in slices; the decoder
// then catches up in bursts, and a roomy track buffer bridges the gaps instead of underrunning.
// A speech player needs no low latency: pause and seek flush the buffer anyway. The price is that
// an equalizer or speed change is heard about this long after the tap.
private const val TRACK_MIN_US = 2_000_000
private const val TRACK_MAX_US = 4_000_000

/** Builds the ExoPlayer and everything it is configured with, so the engine only runs it. */
class PlayerFactory(private val app: Application, private val sound: SoundOutput) {
    private fun renderers() = object : DefaultRenderersFactory(app) {
        override fun buildAudioSink(context: Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean): AudioSink =
            DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioTrackBufferSizeProvider(
                    DefaultAudioTrackBufferSizeProvider.Builder()
                        .setMinPcmBufferDurationUs(TRACK_MIN_US)
                        .setMaxPcmBufferDurationUs(TRACK_MAX_US)
                        .build(),
                )
                .setAudioProcessors(arrayOf<AudioProcessor>(sound.processor))
                .build()
    }

    fun attributes(speech: Boolean): AudioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(if (speech) C.AUDIO_CONTENT_TYPE_SPEECH else C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    fun create(speech: Boolean, skipBackMs: Long, skipForwardMs: Long): ExoPlayer {
        val extractors = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(MIN_BUFFER_MS, MAX_BUFFER_MS, START_BUFFER_MS, REBUFFER_MS)
            .build()
        return ExoPlayer.Builder(app, renderers(), DefaultMediaSourceFactory(app, extractors))
            .setLoadControl(loadControl)
            .setAudioAttributes(attributes(speech), true)
            .setHandleAudioBecomingNoisy(true)
            .setSeekParameters(SeekParameters.EXACT)
            .setSeekBackIncrementMs(skipBackMs)
            .setSeekForwardIncrementMs(skipForwardMs)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
    }
}
