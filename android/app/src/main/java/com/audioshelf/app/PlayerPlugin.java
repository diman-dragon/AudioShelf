package com.audioshelf.app;

import android.os.Handler;
import android.os.Looper;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import com.getcapacitor.JSArray;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.List;
import java.util.function.Consumer;

/** Audio playback on Media3 ExoPlayer: load, play, pause, seek, speed, mode, stop, state. */
@CapacitorPlugin(name = "Player")
public class PlayerPlugin extends Plugin {
  private final Handler main = new Handler(Looper.getMainLooper());

  /** ExoPlayer lives on the main thread; plugin calls arrive on another one. Resolves with the new state. */
  private void onPlayer(PluginCall call, Consumer<ExoPlayer> job) {
    main.post(
        () -> {
          try {
            ExoPlayer p = PlayerHolder.get(getContext());
            job.accept(p);
            call.resolve(PlayerState.of(p));
          } catch (Exception e) {
            call.reject("player: " + e.getMessage());
          }
        });
  }

  private void startService() {
    PlaybackStarter.start(getContext(), getActivity());
  }

  /** items: [{uri, title}] — the files of one book; index/position say where to start. */
  @PluginMethod
  public void load(PluginCall call) {
    JSArray items = call.getArray("items");
    if (items == null || items.length() == 0) {
      call.reject("items required");
      return;
    }
    int index = call.getInt("index", 0);
    long position = call.getLong("position", 0L);
    boolean autoplay = Boolean.TRUE.equals(call.getBoolean("autoplay", false));
    List<MediaItem> list;
    try {
      // Reads tags (title, cover): not on the main thread.
      list = Playlist.from(getContext(), items, call.getString("title", ""), call.getString("artist", ""));
    } catch (Exception e) {
      call.reject("bad items: " + e.getMessage());
      return;
    }
    onPlayer(
        call,
        p -> {
          p.setMediaItems(list, index, position);
          p.prepare();
          p.setPlayWhenReady(autoplay);
          if (autoplay) startService();
        });
  }

  @PluginMethod
  public void play(PluginCall call) {
    onPlayer(
        call,
        p -> {
          startService();
          p.play();
        });
  }

  @PluginMethod
  public void pause(PluginCall call) {
    onPlayer(call, ExoPlayer::pause);
  }

  @PluginMethod
  public void seek(PluginCall call) {
    long position = call.getLong("position", 0L);
    onPlayer(call, p -> p.seekTo(position));
  }

  @PluginMethod
  public void setSpeed(PluginCall call) {
    float speed = call.getFloat("speed", 1f);
    onPlayer(call, p -> p.setPlaybackSpeed(speed));
  }

  @PluginMethod
  public void next(PluginCall call) {
    onPlayer(call, ExoPlayer::seekToNextMediaItem);
  }

  @PluginMethod
  public void previous(PluginCall call) {
    onPlayer(call, ExoPlayer::seekToPreviousMediaItem);
  }

  /** Jump to chapter number index (from its start). */
  @PluginMethod
  public void skipTo(PluginCall call) {
    int index = call.getInt("index", 0);
    onPlayer(call, p -> p.seekTo(index, 0));
  }

  /** Mode and file-edge fades: repeat off|all|one, shuffle, pauseAfter (chapter), fadeIn/fadeOut in ms. */
  @PluginMethod
  public void setProfile(PluginCall call) {
    String repeat = call.getString("repeat", "off");
    boolean shuffle = Boolean.TRUE.equals(call.getBoolean("shuffle", false));
    boolean pauseAfter = Boolean.TRUE.equals(call.getBoolean("pauseAfter", false));
    int fadeIn = call.getInt("fadeIn", 0);
    int fadeOut = call.getInt("fadeOut", 0);
    onPlayer(call, p -> PlayerProfile.apply(p, repeat, shuffle, pauseAfter, fadeIn, fadeOut));
  }

  /** The file was deleted or closed: stop and forget it. */
  @PluginMethod
  public void stop(PluginCall call) {
    onPlayer(
        call,
        p -> {
          p.stop();
          p.clearMediaItems();
        });
  }

  @PluginMethod
  public void state(PluginCall call) {
    onPlayer(call, p -> {});
  }
}
