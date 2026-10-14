import { readStored, writeStored } from '../core/storage.js';
import { createMapStore } from '../core/map-store.js';
import { FLAT_BANDS } from './bands.js';

// Эквалайзер: общий для всех и свой у отдельного трека (книги). { preset: id | 'custom', bands: [10 дБ] }
const GLOBAL = 'audioshelf.eq.global';
const tracks = createMapStore('audioshelf.eq.tracks');

const valid = (eq) => eq && Array.isArray(eq.bands) && eq.bands.length === FLAT_BANDS.length;

export function getGlobalEq() {
  try {
    const eq = JSON.parse(readStored(GLOBAL));
    if (valid(eq)) return eq;
  } catch {
    /* нет или повреждено — плоский */
  }
  return { preset: 'flat', bands: [...FLAT_BANDS] };
}

export const setGlobalEq = (eq) => writeStored(GLOBAL, JSON.stringify(eq));

export const getTrackEq = (id) => (valid(tracks.get(id)) ? tracks.get(id) : null);
export const setTrackEq = tracks.set;
export const clearTrackEq = tracks.del;
export const moveTrackEq = tracks.move;

// Что звучит у этой книги: её собственная настройка или общая.
export const effectiveEq = (id) => (id && getTrackEq(id)) || getGlobalEq();
