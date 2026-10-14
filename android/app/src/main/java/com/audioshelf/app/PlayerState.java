package com.audioshelf.app;

import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import com.getcapacitor.JSObject;

/** Snapshot of the player for JavaScript. */
final class PlayerState {
  private PlayerState() {}

  static JSObject of(Player p) {
    MediaItem item = p.getCurrentMediaItem();
    JSObject o = new JSObject();
    o.put("loaded", item != null);
    o.put("uri", item == null ? "" : item.mediaId);
    o.put("index", p.getCurrentMediaItemIndex());
    o.put("count", p.getMediaItemCount());
    o.put("playing", p.isPlaying());
    o.put("loading", p.getPlaybackState() == Player.STATE_BUFFERING);
    o.put("ended", p.getPlaybackState() == Player.STATE_ENDED);
    o.put("position", p.getCurrentPosition());
    o.put("duration", p.getDuration() < 0 ? 0 : p.getDuration());
    o.put("speed", p.getPlaybackParameters().speed);
    return o;
  }
}
