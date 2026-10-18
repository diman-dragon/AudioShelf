import { Sound } from '../folder/plugins.js';
import { isPremium } from '../billing/entitlement.js';
import { FLAT_BANDS } from './bands.js';
import { effectiveEq } from './eq-store.js';
import { getGain } from './gain-store.js';
import { getBoost } from './boost-pref.js';
import { isNormOn } from './norm-pref.js';

let sent = '';

export function applySound(book) {
  const trims = {};
  if (book && isNormOn() && isPremium()) {
    book.files.forEach((f) => {
      const db = getGain(f.uri);
      if (db) trims[f.uri] = db;
    });
  }
  const sound = { bands: isPremium() ? effectiveEq(book?.id).bands : FLAT_BANDS, trims, boost: getBoost() / 100 };
  const json = JSON.stringify(sound);
  if (json === sent) return Promise.resolve();
  sent = json;
  return Sound.setSound(sound).catch(() => {
    sent = '';
  });
}
