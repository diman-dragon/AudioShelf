import { readStored, writeStored } from '../core/storage.js';
import { createMapStore } from '../core/map-store.js';
import { REWIND_MS } from './rewind.js';

// id книги -> { i: номер файла (главы), p: позиция в нём мс, d: его длительность мс, done: дослушана, t: когда слушали,
//   x: 1 — номера глав посчитаны с учётом глав внутри файла }
const store = createMapStore('audioshelf.progress2');
const LAST = 'audioshelf.last2'; // последняя книга целиком
const SPEED = 'audioshelf.speed';

// pc — процент прослушанного по всей книге (для полоски в библиотеке).
export const saveProgress = (id, { i, p, d, done, x, pc }) => {
  store.set(id, { i, p: Math.floor(p), d: Math.floor(d), done: Boolean(done), x: x ? 1 : 0, pc: pc ?? 0, t: Date.now() });
  document.dispatchEvent(new Event('progresschange'));
};

// Сколько процентов книги прослушано: null — не начата. Для записей без pc считаем по одному файлу.
export function percentOf(id) {
  const r = store.get(id);
  if (!r) return null;
  if (r.done) return 100;
  const pc = r.pc ?? (!r.x && r.i === 0 && r.d > 0 ? Math.floor((100 * r.p) / r.d) : 0);
  return pc > 0 || r.i > 0 || r.p > 1 ? Math.max(1, Math.min(99, pc)) : null; // начатая книга: хотя бы 1%
}

// Книга стала раскладываться на главы внутри файла: запись без пометки x переносим на новые номера.
export function remapProgress(id, map) {
  const r = store.get(id);
  if (!r || r.x) return;
  store.set(id, { ...r, ...map(r.i, r.p), d: 0, x: 1 });
}

export const lastPlayed = (id) => store.get(id)?.t ?? 0;

// new — не открывали, started — начата, done — прослушана до конца.
export function progressState(id) {
  const r = store.get(id);
  if (!r) return 'new';
  if (r.done) return 'done';
  return r.i > 0 || r.p > 0 ? 'started' : 'new';
}

// С какого места продолжать: на 5 с раньше, чем остановились; прослушанное начинаем сначала.
export function resumeAt(id) {
  const r = store.get(id);
  return !r || r.done ? { i: 0, p: 0 } : { i: r.i, p: Math.max(0, r.p - REWIND_MS) };
}

// Нативный плеер записал место, пока JS спал или был убит: берём его, если оно новее нашей записи.
export function adoptSpot(id, { i, p, t, x }) {
  const r = store.get(id);
  if (r && r.t >= t) return false;
  store.set(id, { i, p: Math.floor(p), d: r && r.i === i ? r.d : 0, done: false, x: x ? 1 : (r?.x ?? 0), pc: r?.pc ?? 0, t });
  return true;
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
