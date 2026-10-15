import { Sound } from '../folder/plugins.js';
import { isPremium } from '../billing/entitlement.js';
import { FLAT_BANDS } from './bands.js';
import { effectiveEq } from './eq-store.js';
import { getGain } from './gain-store.js';
import { isNormOn } from './norm-pref.js';

// Отправляет плееру эквалайзер книги и поправки громкости её файлов. Без Премиума звук не обрабатывается.
export function applySound(book) {
  const trims = {};
  if (book && isNormOn() && isPremium()) {
    book.files.forEach((f) => {
      const db = getGain(f.uri);
      if (db) trims[f.uri] = db;
    });
  }
  return Sound.setSound({ bands: isPremium() ? effectiveEq(book?.id).bands : FLAT_BANDS, trims }).catch(() => {});
}
