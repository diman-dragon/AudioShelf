import { REWIND_MS } from './rewind.js';
import { forgetInfo, getInfo, moveInfo, setInfo } from './progress-info.js';
import { forgetSpot, getSpot, moveSpot, setSpot } from './spot-store.js';

const changed = (id) => document.dispatchEvent(new CustomEvent('progresschange', { detail: id }));

export const saveProgress = (id, { i, p, done, x, pc }) => {
  const before = progressState(id);
  setSpot(id, i, p);
  const infoChanged = setInfo(id, { done, x, pc });
  if (infoChanged || before !== progressState(id)) changed(id);
};

export function percentOf(id) {
  const info = getInfo(id);
  if (info?.done) return 100;
  const pc = info?.pc ?? 0;
  return progressState(id) === 'started' ? Math.max(0, Math.min(99, pc)) : null;
}

export function remapProgress(id, map) {
  const info = getInfo(id);
  const spot = getSpot(id);
  if (!spot || info?.x) return;
  const next = map(spot.i, spot.p);
  setSpot(id, next.i, next.p, spot.t);
  setInfo(id, { done: info?.done, x: 1, pc: info?.pc });
}

export const lastPlayed = (id) => getSpot(id)?.t ?? 0;

export function progressState(id) {
  if (getInfo(id)?.done) return 'done';
  const spot = getSpot(id);
  return spot && (spot.i > 0 || spot.p > 0) ? 'started' : 'new';
}

export function resumeAt(id) {
  const spot = getSpot(id);
  return !spot || getInfo(id)?.done ? { i: 0, p: 0 } : { i: spot.i, p: Math.max(0, spot.p - REWIND_MS) };
}

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
