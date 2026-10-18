package com.audioshelf.app;

import android.content.Context;
import android.os.Bundle;
import androidx.media3.session.CommandButton;
import androidx.media3.session.MediaSession;
import androidx.media3.session.SessionCommand;
import androidx.media3.session.SessionCommands;
import androidx.media3.session.SessionResult;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

final class SessionCallback implements MediaSession.Callback {
  private static final SessionCommand CLOSE = new SessionCommand("audioshelf.close", Bundle.EMPTY);

  private final Context ctx;
  private final Runnable onClose;

  SessionCallback(Context ctx, Runnable onClose) {
    this.ctx = ctx;
    this.onClose = onClose;
  }

  @Override
  public MediaSession.ConnectionResult onConnect(
      MediaSession session, MediaSession.ControllerInfo controller) {
    CommandButton close =
        new CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setCustomIconResId(R.drawable.ic_close)
            .setDisplayName(ctx.getString(R.string.notif_close))
            .setSessionCommand(CLOSE)
            .setEnabled(true)
            .build();
    SessionCommands commands =
        MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon().add(CLOSE).build();
    return new MediaSession.ConnectionResult.AcceptedResultBuilder(session)
        .setAvailableSessionCommands(commands)
        .setMediaButtonPreferences(ImmutableList.of(close))
        .build();
  }

  @Override
  public ListenableFuture<MediaSession.MediaItemsWithStartPosition> onPlaybackResumption(
      MediaSession session, MediaSession.ControllerInfo controller) {
    return ResumeLoader.load(ctx);
  }

  @Override
  public ListenableFuture<SessionResult> onCustomCommand(
      MediaSession session,
      MediaSession.ControllerInfo controller,
      SessionCommand command,
      Bundle args) {
    if (CLOSE.customAction.equals(command.customAction)) onClose.run();
    return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
  }
}
