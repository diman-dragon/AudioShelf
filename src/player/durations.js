import { createMapStore } from '../core/map-store.js';

// id книги -> { номер главы: длительность мс }. Узнаём по мере прослушивания: нужны для общего прогресса книги.
const store = createMapStore('audioshelf.durs');
let seen = '';

export function noteDuration(id, index, ms) {
  const key = `${id}|${index}|${ms}`;
  if (key === seen) return;
  seen = key;
  const known = store.get(id) ?? {};
  if (known[index] !== ms) store.set(id, { ...known, [index]: ms });
}

export const getDurations = (id) => store.get(id) ?? {};
export const moveDurations = store.move;
export const forgetDurations = store.del;

// Длины глав, которые известны заранее (из оглавления файла).
export const seedDurations = (id, known) => store.set(id, { ...getDurations(id), ...known });
