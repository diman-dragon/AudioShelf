@file:androidx.annotation.OptIn(markerClass = [UnstableApi::class])

package com.audioshelf.app.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.audioshelf.app.AppLog

private const val TAG = "Focus"
private const val CALL_FIRST_CHECK_MS = 100L
private const val CALL_CHECK_MS = 300L

/** What to do when the system tells us the audio focus changed. */
enum class FocusAction { Ignore, PauseAndResume, PauseForGood, Resume }

/**
 * The rules, kept apart from Android so they can be tested: a notification, a message sound, a
 * navigation prompt or an alarm must not touch the playback; a call (phone, or a voice call in an
 * app) pauses it and it carries on when the call is over; another app that takes the audio for good
 * (a video, a music player) pauses it and it stays paused.
 */
object FocusRules {
    fun onChange(change: Int, inCall: Boolean, pausedByFocus: Boolean): FocusAction = when (change) {
        AudioManager.AUDIOFOCUS_GAIN -> if (pausedByFocus) FocusAction.Resume else FocusAction.Ignore
        AudioManager.AUDIOFOCUS_LOSS -> FocusAction.PauseForGood
        // Because the request says "I pause when ducked", every short request arrives as a transient
        // loss, a notification's as well as a call's. Only a call is allowed to stop the playback.
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
        -> if (inCall) FocusAction.PauseAndResume else FocusAction.Ignore
        else -> FocusAction.Ignore
    }

    /** Ringing, in a call, in a voice call of an app, or screening a call. */
    fun isCall(audioMode: Int): Boolean = audioMode == AudioManager.MODE_RINGTONE ||
        audioMode == AudioManager.MODE_IN_CALL ||
        audioMode == AudioManager.MODE_IN_COMMUNICATION ||
        audioMode == 4 // MODE_CALL_SCREENING, API 29
}

/**
 * Audio focus handled by the app instead of the player. The player's own handling pauses a book
 * (speech) for every notification and lowers the volume of an album; here the playback goes on at full
 * volume under a notification, and stops only for a call.
 *
 * The request says it pauses when ducked. That makes the system leave the volume alone and deliver
 * every short request as a transient loss; [FocusRules] then looks at the audio mode to tell a call
 * from a notification. A call can start a moment after its focus request, so while the focus is lost the
 * mode is checked again after 100 ms and then every 300 ms.
 */
class FocusPolicy(
    context: Context,
    private val player: Player,
    private val speech: () -> Boolean,
) : Player.Listener, AudioManager.OnAudioFocusChangeListener {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private var request: AudioFocusRequest? = null
    private var holding = false
    private var pausedByFocus = false
    private var lost = false

    private val callCheck = object : Runnable {
        override fun run() {
            if (!lost) return
            val mode = audio.mode
            if (!pausedByFocus && player.playWhenReady && FocusRules.isCall(mode)) {
                AppLog.i(TAG, "a call started while the focus was lost (audio mode $mode): pausing")
                pauseForFocus()
            }
            handler.postDelayed(this, CALL_CHECK_MS)
        }
    }

    init {
        player.addListener(this)
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (playWhenReady) {
            pausedByFocus = false
            acquire()
        } else if (!pausedByFocus) {
            release()
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_IDLE) release() else if (player.playWhenReady && !pausedByFocus) acquire()
    }

    override fun onAudioFocusChange(change: Int) {
        // One read: the mode can change between two reads, and the log must show what the decision used.
        val mode = audio.mode
        val call = FocusRules.isCall(mode)
        val action = FocusRules.onChange(change, call, pausedByFocus)
        AppLog.i(TAG, "focus ${name(change)}: audio mode $mode, call=$call, playing=${player.isPlaying} -> $action")
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                lost = false
                handler.removeCallbacks(callCheck)
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                lost = false
                handler.removeCallbacks(callCheck)
            }
            else -> if (!lost) {
                lost = true
                handler.postDelayed(callCheck, CALL_FIRST_CHECK_MS)
            }
        }
        when (action) {
            FocusAction.Ignore -> Unit
            FocusAction.PauseAndResume -> pauseForFocus()
            FocusAction.PauseForGood -> {
                pausedByFocus = false
                player.pause()
                release()
            }
            FocusAction.Resume -> {
                pausedByFocus = false
                player.play()
            }
        }
    }

    fun release() {
        handler.removeCallbacks(callCheck)
        lost = false
        if (!holding) return
        holding = false
        if (Build.VERSION.SDK_INT >= 26) {
            request?.let { audio.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audio.abandonAudioFocus(this)
        }
        request = null
        AppLog.d(TAG, "focus released")
    }

    private fun pauseForFocus() {
        pausedByFocus = true
        player.pause()
        AppLog.i(TAG, "paused for a call, will carry on when it is over")
    }

    private fun acquire() {
        if (holding) return
        val result = if (Build.VERSION.SDK_INT >= 26) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(if (speech()) AudioAttributes.CONTENT_TYPE_SPEECH else AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val built = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener(this, handler)
                .setWillPauseWhenDucked(true)
                .build()
            request = built
            audio.requestAudioFocus(built)
        } else {
            @Suppress("DEPRECATION")
            audio.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            holding = true
            AppLog.d(TAG, "focus granted")
        } else {
            AppLog.w(TAG, "focus refused (result $result, audio mode ${audio.mode}): not playing")
            request = null
            pausedByFocus = false
            player.pause()
        }
    }

    private fun name(change: Int) = when (change) {
        AudioManager.AUDIOFOCUS_GAIN -> "GAIN (back)"
        AudioManager.AUDIOFOCUS_LOSS -> "LOSS (another app took the audio for good)"
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> "LOSS_TRANSIENT (a short sound, a notification or a call)"
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> "LOSS_TRANSIENT_CAN_DUCK (a notification)"
        else -> "change $change"
    }
}
