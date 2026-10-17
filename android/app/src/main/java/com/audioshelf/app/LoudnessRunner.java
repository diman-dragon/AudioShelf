package com.audioshelf.app;

import android.content.Context;
import android.net.Uri;
import android.os.Process;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/** Measures files one after another on a low-priority thread, so playback and the screen are not disturbed. */
final class LoudnessRunner {
  interface Listener {
    void onStart(String name);

    /** gain is NaN when the file could not be measured. */
    void onFile(String uri, String name, double gain);

    /** status: "done", "cancelled", "battery" (stopped to save a weak battery) or "unplugged" (automatic run: the charger was removed). */
    void onFinish(String status, int failed);
  }

  private final AtomicBoolean cancel = new AtomicBoolean();
  private volatile boolean running;

  boolean isRunning() {
    return running;
  }

  void cancel() {
    cancel.set(true);
  }

  /** files: {uri, name} pairs. Returns false if a run is already going. */
  synchronized boolean start(Context ctx, List<String[]> files, boolean onlyCharging, Listener listener) {
    if (running) return false;
    running = true;
    cancel.set(false);
    Context app = ctx.getApplicationContext();
    Thread thread = new Thread(() -> run(app, files, onlyCharging, listener), "loudness");
    thread.setPriority(Thread.MIN_PRIORITY);
    thread.start();
    return true;
  }

  private void run(Context app, List<String[]> files, boolean onlyCharging, Listener listener) {
    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
    BatteryWatch battery = new BatteryWatch(app, onlyCharging);
    BooleanSupplier stop = () -> cancel.get() || battery.tripped(); // also checked inside the decoding of a file
    int failed = 0;
    for (String[] file : files) {
      if (stop.getAsBoolean()) break;
      listener.onStart(file[1]);
      double gain;
      try {
        gain = LoudnessMeter.gainFor(app, Uri.parse(file[0]), stop);
      } catch (Exception e) {
        gain = Double.NaN;
      }
      if (stop.getAsBoolean()) break; // half-measured file does not count
      if (Double.isNaN(gain)) failed++;
      listener.onFile(file[0], file[1], gain);
    }
    running = false;
    String status = cancel.get() ? "cancelled" : battery.tripped() ? (onlyCharging ? "unplugged" : "battery") : "done";
    listener.onFinish(status, failed);
  }
}
