package com.audioshelf.app;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.Collections;
import java.util.List;

@CapacitorPlugin(name = "Billing")
public class BillingPlugin extends Plugin implements PurchasesUpdatedListener {
  private BillingConnection connection;
  private final BillingCatalog catalog = new BillingCatalog();
  private PurchaseHandler purchases;
  private PluginCall buyCall;

  @Override
  public void load() {
    connection = new BillingConnection(getContext(), this);
    purchases = new PurchaseHandler(connection.client);
  }

  @PluginMethod
  public void getState(PluginCall call) {
    connection.run(
        () -> catalog.load(connection.client, () -> purchases.refresh(() -> call.resolve(state()), (why) -> call.reject(why))),
        (why) -> call.reject(why));
  }

  private JSObject state() {
    return catalog.state(purchases.premium());
  }

  @PluginMethod
  public void buy(PluginCall call) {
    ProductDetails d = catalog.get(call.getString("id", ""));
    if (d == null || getActivity() == null || buyCall != null) {
      call.resolve(status("error"));
      return;
    }
    buyCall = call;
    BillingFlowParams.ProductDetailsParams one = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build();
    BillingFlowParams flow = BillingFlowParams.newBuilder().setProductDetailsParamsList(Collections.singletonList(one)).build();
    BillingResult started = connection.client.launchBillingFlow(getActivity(), flow);
    if (started.getResponseCode() != BillingClient.BillingResponseCode.OK) finishBuy("error");
  }

  @Override
  public void onPurchasesUpdated(BillingResult result, List<Purchase> list) {
    int code = result.getResponseCode();
    if (code == BillingClient.BillingResponseCode.OK) {
      finishBuy(purchases.handle(list) ? "pending" : "ok");
    } else if (code == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
      purchases.grantPremium();
      finishBuy("ok");
    } else {
      finishBuy(code == BillingClient.BillingResponseCode.USER_CANCELED ? "cancelled" : "error");
    }
    notifyListeners("billing", state());
  }

  private void finishBuy(String status) {
    PluginCall call = buyCall;
    buyCall = null;
    if (call != null) call.resolve(status(status));
  }

  private JSObject status(String status) {
    JSObject out = state();
    out.put("status", status);
    return out;
  }
}
