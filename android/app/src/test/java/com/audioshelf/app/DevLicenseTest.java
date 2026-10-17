package com.audioshelf.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DevLicenseTest {
  private static final String FP = DevLicense.fingerprint("0123456789abcdef", "com.audioshelf.app");

  @Test
  public void fingerprintHasFourGroupsOfFour() {
    assertTrue(FP.matches("[A-Z2-7]{4}(-[A-Z2-7]{4}){3}"));
  }

  @Test
  public void fingerprintIsStableAndDependsOnDevice() {
    assertEquals(FP, DevLicense.fingerprint("0123456789abcdef", "com.audioshelf.app"));
    assertNotEquals(FP, DevLicense.fingerprint("fedcba9876543210", "com.audioshelf.app"));
  }

  @Test
  public void issuedKeyIsFourGroupsOfFiveAndValid() {
    String key = DevLicense.expected(FP);
    assertEquals(20, key.length());
    String shown = key.replaceAll("(.{5})(?=.)", "$1-");
    assertTrue(shown.matches("[A-Z2-7]{5}(-[A-Z2-7]{5}){3}"));
    assertTrue(DevLicense.valid(shown, FP));
  }

  @Test
  public void caseDashesAndSpacesAreIgnored() {
    String key = DevLicense.expected(FP).toLowerCase();
    assertTrue(DevLicense.valid(" " + key.substring(0, 5) + " - " + key.substring(5), FP));
  }

  @Test
  public void keyOfAnotherDeviceIsRejected() {
    String other = DevLicense.expected(DevLicense.fingerprint("fedcba9876543210", "com.audioshelf.app"));
    assertFalse(DevLicense.valid(other, FP));
  }

  @Test
  public void emptyOrNullIsRejected() {
    assertFalse(DevLicense.valid("", FP));
    assertFalse(DevLicense.valid(null, FP));
    assertFalse(DevLicense.valid(DevLicense.expected(FP), ""));
  }
}
