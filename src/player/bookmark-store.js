import { createMapStore } from '../core/map-store.js';

// id книги -> [{ i: глава, p: позиция мс, t: когда добавлена, x: 1 — глава внутри файла }], по порядку в книге.
const store = createMapStore('audioshelf.marks');
const NEAR = 1000; // мс: две закладки ближе друг к другу — одна

export const getMarks = (id) => store.get(id) ?? [];

export function addMark(id, i, p, x = false) {
  const list = getMarks(id);
  if (list.some((m) => m.i === i && Math.abs(m.p - p) < NEAR)) return;
  const next = [...list, { i, p: Math.floor(p), t: Date.now(), x: x ? 1 : 0 }];
  store.set(id, next.sort((a, b) => a.i - b.i || a.p - b.p));
}

// Книга стала раскладываться на главы внутри файла: закладки без пометки x переносим на новые номера.
export function remapMarks(id, map) {
  const list = getMarks(id);
  if (!list.some((m) => !m.x)) return;
  store.set(id, list.map((m) => (m.x ? m : { ...m, ...map(m.i, m.p), x: 1 })).sort((a, b) => a.i - b.i || a.p - b.p));
}

export const removeMark = (id, created) => store.set(id, getMarks(id).filter((m) => m.t !== created));
export const moveMarks = store.move;
export const forgetMarks = store.del;
