import { findTheme, systemTheme } from './themes.js';
import { loadTheme, saveTheme } from './theme-storage.js';
import { syncSystemBars } from './system-bars.js';

let current = 'dark';

export const getTheme = () => current;

function apply(id) {
  const theme = findTheme(id);
  current = theme.id;
  document.documentElement.dataset.theme = theme.id;
  syncSystemBars(theme);
}

export function setTheme(id) {
  apply(id);
  saveTheme(current);
}

export function initTheme() {
  apply(loadTheme() ?? systemTheme());
}
