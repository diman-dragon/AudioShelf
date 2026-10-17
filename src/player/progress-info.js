import { createMapStore } from '../core/map-store.js';

// Что известно о прослушивании книги: id -> { done: дослушана, x: 1 — номера глав посчитаны с учётом глав внутри файла,
// pc: процент по всей книге }. Меняется редко (раз в процент), поэтому лежит отдельно от позиции.
const store = createMapStore('pinfo');

export const getInfo = (id) => store.get(id);

// true — запись изменилась.
export function setInfo(id, { done, x, pc }) {
  const next = { done: Boolean(done), x: x ? 1 : 0, pc: pc ?? 0 };
  const prev = store.get(id);
  if (prev && prev.done === next.done && prev.x === next.x && prev.pc === next.pc) return false;
  store.set(id, next);
  return true;
}

export const moveInfo = store.move;
export const forgetInfo = store.del;
