import { readStored, writeStored } from '../core/storage.js';
import { DEFAULT_THEME, THEMES } from './themes.js';

const KEY = 'audioshelf.theme';

export function loadTheme() {
  const saved = readStored(KEY);
  return THEMES.some((t) => t.id === saved) ? saved : DEFAULT_THEME;
}

export const saveTheme = (id) => writeStored(KEY, id);
