import { Sound } from '../folder/plugins.js';
import { effectiveEq } from './eq-store.js';
import { getGain } from './gain-store.js';
import { isNormOn } from './norm-pref.js';

// Отправляет плееру эквалайзер книги и поправки громкости её файлов.
export function applySound(book) {
  const trims = {};
  if (book && isNormOn()) {
    book.files.forEach((f) => {
      const db = getGain(f.uri);
      if (db) trims[f.uri] = db;
    });
  }
  return Sound.setSound({ bands: effectiveEq(book?.id).bands, trims }).catch(() => {});
}
