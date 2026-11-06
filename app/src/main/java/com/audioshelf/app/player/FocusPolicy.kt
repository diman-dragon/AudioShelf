@file:androidx.annotation.OptIn(markerClass = [UnstableApi::class])

package com.audioshelf.app.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.audioshelf.app.AppLog

private const val TAG = "Focus"
private const val CALL_FIRST_CHECK_MS = 100L
private const val CALL_CHECK_MS = 300L
private const val NOTIFICATION_MIN_MS = 300L
private const val NOTIFICATION_REWIND_MS = 10_000L

// A notification is a few seconds. One that holds the focus longer is another app that forgot to let it go
// (a 10-minute one was seen): the book is not kept lowered, and nothing was missed that a rewind would give back.
private const val NOTIFICATION_MAX_DUCK_MS = 15_000L
private const val NOTIFICATION_MAX_REWIND_MS = 30_000L

// A call in an app (Telegram, WhatsApp) takes the focus "for good" with the audio mode still normal. It is told
// apart by what plays on the phone: a ringtone or a voice-call stream. The focus is gone for good, so no GAIN
// comes when the call ends; the streams are watched instead.
private const val CALL_WATCH_MS = 1_000L
private const val CALL_GONE_POLLS = 2
private const val CALL_WATCH_LIMIT_MS = 3L * 60 * 60 * 1000

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
    private val duckForNotification: () -> Boolean,
    private val rewindAfterNotification: () -> Boolean,
    private val onDuck: (Boolean) -> Unit,
) : Player.Listener, AudioManager.OnAudioFocusChangeListener {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private var request: AudioFocusRequest? = null
    private var holding = false
    private var pausedByFocus = false
    private var lost = false
    private var notificationSince = 0L
    private var ducked = false
    private var callGonePolls = 0
    private var callWatchSince = 0L

    private val duckTimeout = Runnable {
        if (ducked) {
            ducked = false
            onDuck(false)
            AppLog.i(TAG, "notification still holds the focus after $NOTIFICATION_MAX_DUCK_MS ms: the book is back to full volume")
        }
    }

    private val callWatch = object : Runnable {
        override fun run() {
            if (!pausedByFocus || player.playWhenReady) return
            callGonePolls = if (voiceCallPlaying()) 0 else callGonePolls + 1
            if (callGonePolls >= CALL_GONE_POLLS) {
                AppLog.i(TAG, "the call is over: carrying on")
                player.play()
                return
            }
            if (SystemClock.elapsedRealtime() - callWatchSince > CALL_WATCH_LIMIT_MS) {
                AppLog.i(TAG, "no end of the call seen for 3 hours: staying paused")
                pausedByFocus = false
                return
            }
            handler.postDelayed(this, CALL_WATCH_MS)
        }
    }

    /** A ringtone or a voice call is playing on the phone (any app). */
    private fun voiceCallPlaying(): Boolean = Build.VERSION.SDK_INT >= 26 && try {
        audio.activePlaybackConfigurations.any { isCallStream(it) }
    } catch (e: Exception) {
        false
    }

    private fun isCallStream(config: AudioPlaybackConfiguration): Boolean = when (config.audioAttributes.usage) {
        AudioAttributes.USAGE_VOICE_COMMUNICATION,
        AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING,
        AudioAttributes.USAGE_NOTIFICATION_RINGTONE,
        -> true
        else -> false
    }

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
            handler.removeCallbacks(callWatch)
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
        val appCall = change == AudioManager.AUDIOFOCUS_LOSS && player.playWhenReady && voiceCallPlaying()
        val action = if (appCall) FocusAction.PauseAndResume else FocusRules.onChange(change, call, pausedByFocus)
        AppLog.i(
            TAG,
            "focus ${name(change)}: audio mode $mode, call=$call${if (appCall) " (a voice call of an app is playing)" else ""}, " +
                "playing=${player.isPlaying} -> $action",
        )
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                lost = false
                handler.removeCallbacks(callCheck)
                val ms = endNotification()
                if (action == FocusAction.Ignore && ms in NOTIFICATION_MIN_MS..NOTIFICATION_MAX_REWIND_MS &&
                    player.playWhenReady && rewindAfterNotification()
                ) {
                    rewindForNotification(ms)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                lost = false
                handler.removeCallbacks(callCheck)
                endNotification()
            }
            else -> {
                if (!lost) {
                    lost = true
                    handler.postDelayed(callCheck, CALL_FIRST_CHECK_MS)
                }
                if (action == FocusAction.Ignore && player.playWhenReady) startNotification()
            }
        }
        when (action) {
            FocusAction.Ignore -> Unit
            FocusAction.PauseAndResume -> {
                pauseForFocus()
                if (appCall) {
                    // The focus is gone for good, so let the request go and watch the call instead.
                    release()
                    callGonePolls = 0
                    callWatchSince = SystemClock.elapsedRealtime()
                    handler.removeCallbacks(callWatch)
                    handler.postDelayed(callWatch, CALL_WATCH_MS)
                }
            }
            FocusAction.PauseForGood -> {
                handler.removeCallbacks(callWatch)
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

    /** A notification took the focus for a moment: remember when, and lower the book if the user wants that. */
    private fun startNotification() {
        if (notificationSince == 0L) notificationSince = SystemClock.elapsedRealtime()
        if (duckForNotification() && !ducked) {
            ducked = true
            onDuck(true)
            handler.removeCallbacks(duckTimeout)
            handler.postDelayed(duckTimeout, NOTIFICATION_MAX_DUCK_MS)
            AppLog.i(TAG, "notification: the book is lowered")
        }
    }

    /** The notification is over (or the focus went elsewhere): the volume comes back. Returns how long it lasted, 0 if none. */
    private fun endNotification(): Long {
        val since = notificationSince
        notificationSince = 0L
        handler.removeCallbacks(duckTimeout)
        if (ducked) {
            ducked = false
            onDuck(false)
        }
        return if (since == 0L) 0L else SystemClock.elapsedRealtime() - since
    }

    private fun rewindForNotification(ms: Long) {
        val from = player.currentPosition
        val to = (from - NOTIFICATION_REWIND_MS).coerceAtLeast(0L)
        AppLog.i(TAG, "notification over after $ms ms: back ${from - to} ms ($from -> $to)")
        player.seekTo(to)
    }

    fun release() {
        endNotification()
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
        endNotification()
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
