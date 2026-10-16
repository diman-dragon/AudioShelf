import { readStored, writeStored } from '../core/storage.js';

// Усиление громкости выше 100 %: во сколько процентов усиливается сигнал. Не зависит от Премиума и от книги.
const KEY = 'audioshelf.boost';
export const BOOSTS = [100, 125, 150, 200, 250, 300];

export function getBoost() {
  const n = Number(readStored(KEY));
  return BOOSTS.includes(n) ? n : 100;
}

export function setBoost(percent) {
  writeStored(KEY, String(percent));
  document.dispatchEvent(new Event('boostchange'));
}
