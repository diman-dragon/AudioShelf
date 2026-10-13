import { findTheme } from './themes.js';
import { loadTheme, saveTheme } from './theme-storage.js';
import { syncSystemBars } from './system-bars.js';

let current = 'amoled';

export const getTheme = () => current;

export function setTheme(id) {
  const theme = findTheme(id);
  current = theme.id;
  document.documentElement.dataset.theme = theme.id;
  syncSystemBars(theme);
  saveTheme(theme.id);
}

// Вызывается один раз при старте, до отрисовки интерфейса.
export function initTheme() {
  setTheme(loadTheme());
}
