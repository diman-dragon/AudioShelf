import { readStored, writeStored } from '../core/storage.js';
import { LANGS } from './languages.js';

const KEY = 'audioshelf.lang';

// null — пользователь язык не выбирал, значит следуем языку устройства.
export function loadLang() {
  const saved = readStored(KEY);
  return LANGS.some((l) => l.id === saved) ? saved : null;
}

export const saveLang = (id) => writeStored(KEY, id);
