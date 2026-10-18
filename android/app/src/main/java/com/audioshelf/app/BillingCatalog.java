package com.audioshelf.app;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class BillingCatalog {
  private final Map<String, ProductDetails> details = new HashMap<>();

  ProductDetails get(String id) {
    return details.get(id);
  }

  void load(BillingClient client, Runnable next) {
    List<QueryProductDetailsParams.Product> list = new ArrayList<>();
    list.add(product(BillingProducts.PREMIUM));
    for (String tip : BillingProducts.TIPS) list.add(product(tip));
    client.queryProductDetailsAsync(
        QueryProductDetailsParams.newBuilder().setProductList(list).build(),
        (result, found) -> {
          details.clear();
          for (ProductDetails d : found.getProductDetailsList()) details.put(d.getProductId(), d);
          next.run();
        });
  }

  private static QueryProductDetailsParams.Product product(String id) {
    return QueryProductDetailsParams.Product.newBuilder().setProductId(id).setProductType(BillingClient.ProductType.INAPP).build();
  }

  JSObject state(boolean premium) {
    JSObject out = new JSObject();
    out.put("premium", premium);
    JSArray prices = new JSArray();
    for (Map.Entry<String, ProductDetails> e : details.entrySet()) {
      ProductDetails.OneTimePurchaseOfferDetails offer = e.getValue().getOneTimePurchaseOfferDetails();
      if (offer == null) continue;
      JSObject item = new JSObject();
      item.put("id", e.getKey());
      item.put("price", offer.getFormattedPrice());
      prices.put(item);
    }
    out.put("products", prices);
    return out;
  }
}
