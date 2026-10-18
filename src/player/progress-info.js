import { createMapStore } from '../core/map-store.js';

const store = createMapStore('pinfo');

export const getInfo = (id) => store.get(id);

export function setInfo(id, { done, x, pc }) {
  const next = { done: Boolean(done), x: x ? 1 : 0, pc: pc ?? 0 };
  const prev = store.get(id);
  if (prev && prev.done === next.done && prev.x === next.x && prev.pc === next.pc) return false;
  store.set(id, next);
  return true;
}

export const moveInfo = store.move;
export const forgetInfo = store.del;
