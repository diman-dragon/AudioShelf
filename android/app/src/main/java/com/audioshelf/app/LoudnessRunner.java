package com.audioshelf.app;

import android.content.Context;
import android.net.Uri;
import android.os.Process;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

final class LoudnessRunner {
  interface Listener {
    void onStart(String name);

    void onFile(String uri, String name, double gain);

    void onFinish(String status, int failed);
  }

  private final AtomicBoolean cancel = new AtomicBoolean();
  private volatile boolean running;

  void cancel() {
    cancel.set(true);
  }

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
    BooleanSupplier stop = () -> cancel.get() || battery.tripped();
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
      if (stop.getAsBoolean()) break;
      if (Double.isNaN(gain)) failed++;
      listener.onFile(file[0], file[1], gain);
    }
    running = false;
    String status = cancel.get() ? "cancelled" : battery.tripped() ? (onlyCharging ? "unplugged" : "battery") : "done";
    listener.onFinish(status, failed);
  }
}
