package com.audioshelf.app;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Lifetime developer key: a short HMAC of the device fingerprint, issued by the developer's generator.
 * No Android classes here, so the logic can be checked on a plain JVM.
 */
final class DevLicense {
  private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
  private static final String PREFIX = "audioshelf-dev:2:";

  private DevLicense() {}

  /** Device fingerprint, "ABCD-EFGH-JKLM-NPQR": the first 80 bits of SHA-256(androidId:package). */
  static String fingerprint(String androidId, String packageName) {
    try {
      byte[] hash = MessageDigest.getInstance("SHA-256").digest((androidId + ":" + packageName).getBytes(StandardCharsets.UTF_8));
      StringBuilder out = new StringBuilder();
      int bits = 0;
      int acc = 0;
      for (int i = 0; i < 10; i++) {
        acc = (acc << 8) | (hash[i] & 0xff);
        bits += 8;
        while (bits >= 5) {
          out.append(ALPHABET.charAt((acc >> (bits - 5)) & 31));
          bits -= 5;
        }
        acc &= (1 << bits) - 1;
      }
      String s = out.toString();
      return s.substring(0, 4) + "-" + s.substring(4, 8) + "-" + s.substring(8, 12) + "-" + s.substring(12, 16);
    } catch (Exception e) {
      return "";
    }
  }

  /** The key a developer issues for this fingerprint: 100 bits of HMAC-SHA256 as "XXXXX-XXXXX-XXXXX-XXXXX". */
  static String expected(String fingerprint) {
    try {
      byte[] secret = new byte[LicenseKey.SECRET_HEX.length() / 2];
      for (int i = 0; i < secret.length; i++) secret[i] = (byte) Integer.parseInt(LicenseKey.SECRET_HEX.substring(2 * i, 2 * i + 2), 16);
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret, "HmacSHA256"));
      byte[] h = mac.doFinal((PREFIX + fingerprint).getBytes(StandardCharsets.UTF_8));
      StringBuilder out = new StringBuilder();
      int bits = 0;
      int acc = 0;
      for (int i = 0; i < 13 && out.length() < 20; i++) {
        acc = (acc << 8) | (h[i] & 0xff);
        bits += 8;
        while (bits >= 5 && out.length() < 20) {
          out.append(ALPHABET.charAt((acc >> (bits - 5)) & 31));
          bits -= 5;
        }
        acc &= (1 << bits) - 1;
      }
      return out.toString();
    } catch (Exception e) {
      return "";
    }
  }

  /** True when code is the key for this device's fingerprint. Case, dashes and spaces are ignored. */
  static boolean valid(String code, String fingerprint) {
    if (code == null || fingerprint == null || fingerprint.isEmpty()) return false;
    String given = code.toUpperCase().replaceAll("[^A-Z2-7]", "");
    String want = expected(fingerprint);
    return want.length() == 20 && MessageDigest.isEqual(given.getBytes(StandardCharsets.UTF_8), want.getBytes(StandardCharsets.UTF_8));
  }
}
