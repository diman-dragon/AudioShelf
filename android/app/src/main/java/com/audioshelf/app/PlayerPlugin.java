package com.audioshelf.app;

import androidx.media3.exoplayer.ExoPlayer;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import org.json.JSONObject;

/** Audio playback on Media3 ExoPlayer: load, play, pause, seek, speed, mode, stop, state. */
@CapacitorPlugin(name = "Player")
public class PlayerPlugin extends Plugin {
  private PlayerRunner runner;

  @Override
  public void load() {
    runner = new PlayerRunner(getContext(), this::notifyListeners);
  }

  private void startService() {
    PlaybackStarter.start(getContext(), getActivity());
  }

  @PluginMethod
  public void load(PluginCall call) {
    PlayerLoad.run(getContext(), call, runner, this::startService);
  }

  @PluginMethod
  public void play(PluginCall call) {
    runner.run(
        call,
        p -> {
          startService();
          p.play();
        });
  }

  /** Pause when it is playing (or about to), play otherwise: decided here, on the real player state, so a tap is never lost. */
  @PluginMethod
  public void toggle(PluginCall call) {
    runner.run(
        call,
        p -> PlayerToggle.apply(p, this::startService));
  }

  @PluginMethod
  public void pause(PluginCall call) {
    runner.run(call, ExoPlayer::pause);
  }

  @PluginMethod
  public void seek(PluginCall call) {
    long position = call.getData().optLong("position", 0L);
    runner.run(call, p -> p.seekTo(position));
  }

  @PluginMethod
  public void setSpeed(PluginCall call) {
    float speed = call.getFloat("speed", 1f);
    ResumeStore.put(getContext(), ResumeStore.SPEED, String.valueOf(speed));
    runner.run(call, p -> p.setPlaybackSpeed(speed));
  }

  @PluginMethod
  public void next(PluginCall call) {
    runner.run(call, ExoPlayer::seekToNextMediaItem);
  }

  @PluginMethod
  public void previous(PluginCall call) {
    runner.run(call, ExoPlayer::seekToPreviousMediaItem);
  }

  /** Jump to chapter number index (from its start). */
  @PluginMethod
  public void skipTo(PluginCall call) {
    int index = call.getInt("index", 0);
    runner.run(call, p -> p.seekTo(index, 0));
  }

  /** Mode and file edges: repeat off|all|one, shuffle, pauseAfter, fadeIn/fadeOut in ms, speech (a book). */
  @PluginMethod
  public void setProfile(PluginCall call) {
    JSONObject data = call.getData();
    ResumeStore.put(getContext(), ResumeStore.PROFILE, data.toString());
    runner.run(call, p -> PlayerProfile.apply(p, data));
  }

  /** The file was deleted or closed: stop and forget it. */
  @PluginMethod
  public void stop(PluginCall call) {
    runner.run(call, p -> PlayerStop.apply(getContext(), p));
  }

  @PluginMethod
  public void state(PluginCall call) {
    runner.run(call, p -> {});
  }
}
