package com.audioshelf.app;

/** Product ids from Play Console: one non-consumable and three consumable tips. */
final class BillingProducts {
  static final String PREMIUM = "premium";
  static final String[] TIPS = {"tip_small", "tip_medium", "tip_large"};

  private BillingProducts() {}

  static boolean isTip(String id) {
    for (String tip : TIPS) if (tip.equals(id)) return true;
    return false;
  }
}
