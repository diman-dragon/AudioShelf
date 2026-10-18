package com.audioshelf.app;

import android.content.Intent;
import android.net.Uri;
import java.util.ArrayList;
import java.util.List;

final class PickedUris {
  private PickedUris() {}

  static List<Uri> from(Intent data) {
    List<Uri> uris = new ArrayList<>();
    if (data.getClipData() != null) {
      for (int k = 0; k < data.getClipData().getItemCount(); k++) {
        uris.add(data.getClipData().getItemAt(k).getUri());
      }
    } else if (data.getData() != null) {
      uris.add(data.getData());
    }
    return uris;
  }
}
