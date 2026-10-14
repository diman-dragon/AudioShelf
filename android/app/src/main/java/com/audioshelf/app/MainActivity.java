package com.audioshelf.app;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    registerPlugin(FolderPlugin.class);
    registerPlugin(FileOpsPlugin.class);
    registerPlugin(CoverPlugin.class);
    registerPlugin(PlayerPlugin.class);
    registerPlugin(SoundPlugin.class);
    registerPlugin(LoudnessPlugin.class);
    super.onCreate(savedInstanceState);
  }
}
