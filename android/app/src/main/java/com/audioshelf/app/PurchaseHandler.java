package com.audioshelf.app;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryPurchasesParams;
import java.util.List;
import java.util.function.Consumer;

/** What the purchases mean: Premium is granted and confirmed to Google, tips are used up so they can be bought again. */
final class PurchaseHandler {
  private final BillingClient client;
  private boolean premium;

  PurchaseHandler(BillingClient client) {
    this.client = client;
  }

  boolean premium() {
    return premium;
  }

  void grantPremium() {
    premium = true;
  }

  /** Asks Google Play what is owned and applies it (Premium can also be taken away, e.g. after a refund). */
  void refresh(Runnable next, Consumer<String> onFail) {
    client.queryPurchasesAsync(
        QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
        (result, purchases) -> {
          if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            onFail.accept("unavailable");
            return;
          }
          premium = false;
          handle(purchases);
          next.run();
        });
  }

  /** Grants Premium, confirms it to Google, and uses up tips. Returns true if a purchase is still pending. */
  boolean handle(List<Purchase> purchases) {
    boolean pending = false;
    if (purchases == null) return false;
    for (Purchase p : purchases) {
      if (p.getPurchaseState() == Purchase.PurchaseState.PENDING) {
        pending = true;
        continue;
      }
      if (p.getPurchaseState() != Purchase.PurchaseState.PURCHASED) continue;
      for (String id : p.getProducts()) {
        if (BillingProducts.PREMIUM.equals(id)) {
          premium = true;
          if (!p.isAcknowledged()) acknowledge(p);
        } else if (BillingProducts.isTip(id)) {
          consume(p);
        }
      }
    }
    return pending;
  }

  private void acknowledge(Purchase p) {
    client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.getPurchaseToken()).build(), (r) -> {});
  }

  private void consume(Purchase p) {
    client.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(p.getPurchaseToken()).build(), (r, token) -> {});
  }
}
