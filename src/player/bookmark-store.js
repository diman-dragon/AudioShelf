import { createMapStore } from '../core/map-store.js';

// id книги -> [{ i: глава, p: позиция мс, t: когда добавлена }], по порядку в книге.
const store = createMapStore('audioshelf.marks');
const NEAR = 1000; // мс: две закладки ближе друг к другу — одна

export const getMarks = (id) => store.get(id) ?? [];

export function addMark(id, i, p) {
  const list = getMarks(id);
  if (list.some((m) => m.i === i && Math.abs(m.p - p) < NEAR)) return;
  const next = [...list, { i, p: Math.floor(p), t: Date.now() }];
  store.set(id, next.sort((a, b) => a.i - b.i || a.p - b.p));
}

export const removeMark = (id, created) => store.set(id, getMarks(id).filter((m) => m.t !== created));
export const moveMarks = store.move;
export const forgetMarks = store.del;
