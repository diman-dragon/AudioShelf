package com.audioshelf.app;

import android.app.PendingIntent;
import android.content.Intent;
import androidx.annotation.Nullable;
import androidx.media3.common.Player;
import androidx.media3.session.DefaultMediaNotificationProvider;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

/** Keeps playback alive in the background and shows the system media notification. */
public class PlaybackService extends MediaSessionService {
  private MediaSession session;

  @Override
  public void onCreate() {
    super.onCreate();
    Intent open = new Intent(this, MainActivity.class);
    PendingIntent tap =
        PendingIntent.getActivity(
            this, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    session =
        new MediaSession.Builder(this, PlayerHolder.get(this))
            .setCallback(new SessionCallback(this, this::close))
            .setSessionActivity(tap)
            .build();
    DefaultMediaNotificationProvider provider =
        new DefaultMediaNotificationProvider.Builder(this).build();
    provider.setSmallIcon(R.drawable.ic_stat_player);
    setMediaNotificationProvider(provider);
    // Nobody binds to the service (the player is driven by the plugin), so without this
    // the session is never attached and no notification is ever shown.
    addSession(session);
  }

  /** ✕ in the notification: stop, forget the item and remove the notification. */
  private void close() {
    Player p = session.getPlayer();
    p.pause();
    p.clearMediaItems();
    stopForeground(STOP_FOREGROUND_REMOVE);
    stopSelf();
  }

  @Nullable
  @Override
  public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
    return session;
  }

  @Override
  public void onTaskRemoved(@Nullable Intent rootIntent) {
    // App swiped away from recents: stop playback and remove the notification.
    pauseAllPlayersAndStopSelf();
  }

  @Override
  public void onDestroy() {
    session.release();
    PlayerHolder.release();
    session = null;
    super.onDestroy();
  }
}
