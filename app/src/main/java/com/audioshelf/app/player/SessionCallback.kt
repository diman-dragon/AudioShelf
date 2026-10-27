@file:OptIn(UnstableApi::class)

package com.audioshelf.app.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.core.content.IntentCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.audioshelf.app.R
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

private const val CLICK_WINDOW_MS = 400L
private val CLOSE = SessionCommand("audioshelf.close", Bundle.EMPTY)

class SessionCallback(
    private val context: Context,
    private val resume: () -> MediaSession.MediaItemsWithStartPosition,
    private val onClose: () -> Unit,
    private val onClicks: (Int) -> Unit = {},
) : MediaSession.Callback {
    private val handler = Handler(Looper.getMainLooper())
    private var clicks = 0
    private val fire = Runnable {
        val n = clicks
        clicks = 0
        if (n > 0) onClicks(n)
    }

    override fun onMediaButtonEvent(session: MediaSession, controllerInfo: MediaSession.ControllerInfo, intent: Intent): Boolean {
        val event = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_KEY_EVENT, KeyEvent::class.java) ?: return false
        val key = event.keyCode
        if (key != KeyEvent.KEYCODE_HEADSETHOOK && key != KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) return false
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            clicks++
            handler.removeCallbacks(fire)
            handler.postDelayed(fire, CLICK_WINDOW_MS)
        }
        return true
    }

    override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
        val close = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setCustomIconResId(R.drawable.ic_close)
            .setDisplayName(context.getString(R.string.notif_close))
            .setSessionCommand(CLOSE)
            .setEnabled(true)
            .build()
        val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon().add(CLOSE).build()
        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(commands)
            .setMediaButtonPreferences(ImmutableList.of(close))
            .build()
    }

    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> =
        // Runs on the player's thread: building the items is a few map lookups now (the cover is a
        // pointer, not bytes) and the sound profile has to be in place before playback starts.
        try {
            Futures.immediateFuture(resume())
        } catch (e: Exception) {
            Futures.immediateFailedFuture(e)
        }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        if (customCommand.customAction == CLOSE.customAction) onClose()
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }
}
