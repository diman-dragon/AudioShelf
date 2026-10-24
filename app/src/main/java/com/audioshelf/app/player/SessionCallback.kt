@file:OptIn(UnstableApi::class)

package com.audioshelf.app.player

import android.content.Context
import android.os.Bundle
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
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.Executors

private val CLOSE = SessionCommand("audioshelf.close", Bundle.EMPTY)
private val IO = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())

class SessionCallback(
    private val context: Context,
    private val resume: () -> MediaSession.MediaItemsWithStartPosition,
    private val onClose: () -> Unit,
) : MediaSession.Callback {
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
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = IO.submit<MediaSession.MediaItemsWithStartPosition> { resume() }

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
