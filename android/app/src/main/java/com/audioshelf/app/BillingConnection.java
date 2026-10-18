package com.audioshelf.app;

import android.content.Context;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.PurchasesUpdatedListener;
import java.util.function.Consumer;

final class BillingConnection {
  final BillingClient client;

  BillingConnection(Context ctx, PurchasesUpdatedListener listener) {
    client =
        BillingClient.newBuilder(ctx)
            .setListener(listener)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build();
  }

  void run(Runnable task, Consumer<String> onFail) {
    if (client.isReady()) {
      task.run();
      return;
    }
    client.startConnection(
        new BillingClientStateListener() {
          @Override
          public void onBillingSetupFinished(BillingResult result) {
            if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) task.run();
            else onFail.accept("unavailable");
          }

          @Override
          public void onBillingServiceDisconnected() {}
        });
  }
}
