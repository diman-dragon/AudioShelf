import { readStored, writeStored } from '../core/storage.js';
import { createMapStore } from '../core/map-store.js';

// id книги -> { i: номер файла (главы), p: позиция в нём мс, d: его длительность мс, done: дослушана }
const store = createMapStore('audioshelf.progress2');
const LAST = 'audioshelf.last2'; // последняя книга целиком
const SPEED = 'audioshelf.speed';

export const saveProgress = (id, { i, p, d, done }) =>
  store.set(id, { i, p: Math.floor(p), d: Math.floor(d), done: Boolean(done) });

// new — не открывали, started — начата, done — прослушана до конца.
export function progressState(id) {
  const r = store.get(id);
  if (!r) return 'new';
  if (r.done) return 'done';
  return r.i > 0 || r.p > 0 ? 'started' : 'new';
}

// С какого места продолжать: прослушанное начинаем сначала.
export function resumeAt(id) {
  const r = store.get(id);
  return !r || r.done ? { i: 0, p: 0 } : { i: r.i, p: r.p };
}

export const moveProgress = store.move;
export const forgetProgress = store.del;

const parse = (key, fallback) => {
  try {
    return JSON.parse(readStored(key)) ?? fallback;
  } catch {
    return fallback;
  }
};

export const loadLast = () => parse(LAST, null);
export const saveLast = (book) => writeStored(LAST, JSON.stringify(book));

export const loadSpeed = () => parse(SPEED, 1);
export const saveSpeed = (speed) => writeStored(SPEED, JSON.stringify(speed));
