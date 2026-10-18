import { Capacitor, SystemBars, SystemBarsStyle } from '@capacitor/core';

export function syncSystemBars(theme) {
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', theme.bg);
  if (!Capacitor.isNativePlatform()) return;
  SystemBars.setStyle({ style: theme.light ? SystemBarsStyle.Light : SystemBarsStyle.Dark });
}
