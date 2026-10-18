package com.audioshelf.app;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import java.util.function.Consumer;

final class StateEvents implements Player.Listener {
  private final Player player;
  private final Consumer<Player> send;

  StateEvents(Player player, Consumer<Player> send) {
    this.player = player;
    this.send = send;
    player.addListener(this);
  }

  @Override
  public void onIsPlayingChanged(boolean playing) {
    send.accept(player);
  }

  @Override
  public void onPlaybackStateChanged(int state) {
    send.accept(player);
  }

  @Override
  public void onMediaItemTransition(MediaItem item, int reason) {
    send.accept(player);
  }

  @Override
  public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
    send.accept(player);
  }

  @Override
  public void onPlaybackParametersChanged(PlaybackParameters parameters) {
    send.accept(player);
  }
}
