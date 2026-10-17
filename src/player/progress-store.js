import { REWIND_MS } from './rewind.js';
import { forgetInfo, getInfo, moveInfo, setInfo } from './progress-info.js';
import { forgetSpot, getSpot, moveSpot, setSpot } from './spot-store.js';

// Прогресс книги = позиция (spot-store, пишется часто) + сведения (progress-info, пишутся редко).
// Каждая часть пишется только когда изменилась именно она.
const changed = (id) => document.dispatchEvent(new CustomEvent('progresschange', { detail: id }));

export const saveProgress = (id, { i, p, done, x, pc }) => {
  const before = progressState(id);
  setSpot(id, i, p);
  const infoChanged = setInfo(id, { done, x, pc });
  if (infoChanged || before !== progressState(id)) changed(id); // полоска и значок книги меняются, только когда изменился процент или состояние
};

// Процент по записи: null — не начата.
export function percentOf(id) {
  const info = getInfo(id);
  const spot = getSpot(id);
  if (info?.done) return 100;
  const pc = info?.pc ?? 0;
  return progressState(id) === 'started' ? Math.max(0, Math.min(99, pc)) : null; // та же граница «начата», что и у значка книги
}

// Книга стала раскладываться на главы внутри файла: позицию без пометки x переносим на новые номера.
export function remapProgress(id, map) {
  const spot = getSpot(id);
  const info = getInfo(id);
  if (!spot || info?.x) return;
  const next = map(spot.i, spot.p);
  setSpot(id, next.i, next.p, spot.t);
  setInfo(id, { done: info?.done, x: 1, pc: info?.pc });
}

export const lastPlayed = (id) => getSpot(id)?.t ?? 0;

// new — не открывали, started — начата, done — прослушана до конца.
export function progressState(id) {
  if (getInfo(id)?.done) return 'done';
  const spot = getSpot(id);
  return spot && (spot.i > 0 || spot.p > 0) ? 'started' : 'new';
}

// С какого места продолжать: на 5 с раньше, чем остановились; прослушанное начинаем сначала.
export function resumeAt(id) {
  const spot = getSpot(id);
  return !spot || getInfo(id)?.done ? { i: 0, p: 0 } : { i: spot.i, p: Math.max(0, spot.p - REWIND_MS) };
}

// Нативный плеер записал место, пока JS спал или был убит: берём его, если оно новее нашей записи.
export function adoptSpot(id, { i, p, t, x }) {
  const spot = getSpot(id);
  if (spot && spot.t >= t) return false;
  setSpot(id, i, p, t);
  const info = getInfo(id);
  setInfo(id, { done: false, x: x ? 1 : info?.x, pc: info?.pc });
  return true;
}

export const moveProgress = (from, to) => {
  moveSpot(from, to);
  moveInfo(from, to);
};
export const forgetProgress = (id) => {
  forgetSpot(id);
  forgetInfo(id);
};
