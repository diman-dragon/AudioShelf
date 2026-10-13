package com.audioshelf.app;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.exoplayer.ExoPlayer;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.function.Consumer;

/** Audio playback on Media3 ExoPlayer: load, play, pause, seek, speed, state. */
@CapacitorPlugin(name = "Player")
public class PlayerPlugin extends Plugin {
  private final Handler main = new Handler(Looper.getMainLooper());

  /** ExoPlayer lives on the main thread; plugin calls arrive on another one. */
  private void onPlayer(PluginCall call, Consumer<ExoPlayer> job) {
    main.post(
        () -> {
          try {
            job.accept(PlayerHolder.get(getContext()));
          } catch (Exception e) {
            call.reject("player: " + e.getMessage());
          }
        });
  }

  private void startService() {
    getContext().startService(new Intent(getContext(), PlaybackService.class));
    // Android 13+: without this permission the media notification is not shown.
    if (Build.VERSION.SDK_INT >= 33
        && ContextCompat.checkSelfPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(
          getActivity(), new String[] {Manifest.permission.POST_NOTIFICATIONS}, 0);
    }
  }

  @PluginMethod
  public void load(PluginCall call) {
    String uri = call.getString("uri");
    if (uri == null) {
      call.reject("uri required");
      return;
    }
    String title = call.getString("title", "");
    String artist = call.getString("artist", "");
    long position = call.getLong("position", 0L);
    boolean autoplay = Boolean.TRUE.equals(call.getBoolean("autoplay", false));
    MediaMetadata meta = TrackMeta.of(getContext(), uri, title, artist); // reads tags: not on the main thread
    onPlayer(
        call,
        p -> {
          p.setMediaItem(
              new MediaItem.Builder()
                  .setUri(Uri.parse(uri))
                  .setMediaId(uri)
                  .setMediaMetadata(meta)
                  .build(),
              position);
          p.prepare();
          p.setPlayWhenReady(autoplay);
          if (autoplay) startService();
          call.resolve(PlayerState.of(p));
        });
  }

  @PluginMethod
  public void play(PluginCall call) {
    onPlayer(
        call,
        p -> {
          startService();
          p.play();
          call.resolve(PlayerState.of(p));
        });
  }

  @PluginMethod
  public void pause(PluginCall call) {
    onPlayer(
        call,
        p -> {
          p.pause();
          call.resolve(PlayerState.of(p));
        });
  }

  @PluginMethod
  public void seek(PluginCall call) {
    long position = call.getLong("position", 0L);
    onPlayer(
        call,
        p -> {
          p.seekTo(position);
          call.resolve(PlayerState.of(p));
        });
  }

  @PluginMethod
  public void setSpeed(PluginCall call) {
    Float speed = call.getFloat("speed", 1f);
    onPlayer(
        call,
        p -> {
          p.setPlaybackSpeed(speed);
          call.resolve(PlayerState.of(p));
        });
  }

  /** The file was deleted or closed: stop and forget it. */
  @PluginMethod
  public void stop(PluginCall call) {
    onPlayer(
        call,
        p -> {
          p.stop();
          p.clearMediaItems();
          call.resolve(PlayerState.of(p));
        });
  }

  @PluginMethod
  public void state(PluginCall call) {
    onPlayer(call, p -> call.resolve(PlayerState.of(p)));
  }
}
