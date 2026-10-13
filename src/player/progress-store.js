import { readStored, writeStored } from '../core/storage.js';
import { createMapStore } from '../core/map-store.js';

const store = createMapStore('audioshelf.progress'); // uri -> { p: позиция мс, d: длительность мс }
const LAST = 'audioshelf.last'; // последний файл целиком
const SPEED = 'audioshelf.speed';
const END_GAP = 3000; // «дослушано», если до конца осталось меньше 3 с

export const savePos = (uri, p, d) => store.set(uri, { p: Math.floor(p), d: Math.floor(d) });

// new — ещё не открывали, started — начато, done — прослушано до конца.
export function progressState(uri) {
  const { p = 0, d = 0 } = store.get(uri) ?? {};
  if (d > 0 && p >= d - END_GAP) return 'done';
  return p > 0 ? 'started' : 'new';
}

// С какого места продолжать: прослушанное начинаем сначала.
export const loadPos = (uri) => (progressState(uri) === 'done' ? 0 : store.get(uri)?.p ?? 0);

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
export const saveLast = (file) => writeStored(LAST, JSON.stringify(file));

export const loadSpeed = () => parse(SPEED, 1);
export const saveSpeed = (speed) => writeStored(SPEED, JSON.stringify(speed));
