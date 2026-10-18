package com.audioshelf.app;

final class BillingProducts {
  static final String PREMIUM = "premium";
  static final String[] TIPS = {"tip_small", "tip_medium", "tip_large"};

  private BillingProducts() {}

  static boolean isTip(String id) {
    for (String tip : TIPS) if (tip.equals(id)) return true;
    return false;
  }
}
