import { delRow, putRow, readRows } from '../core/storage.js';

// Длины глав книги (мс): строка на каждую главу, ключ «id книги, номер главы». Узнаём по мере прослушивания:
// нужны для общего прогресса книги. Новая длина пишет одну строку, а не весь список.
const STORE = 'durs';
const SEP = '\u0001';

let byBook = null; // id -> { номер главы: мс }

function load() {
  if (byBook) return byBook;
  byBook = new Map();
  Object.entries(readRows(STORE)).forEach(([key, ms]) => {
    const at = key.lastIndexOf(SEP);
    const id = key.slice(0, at);
    byBook.set(id, { ...byBook.get(id), [key.slice(at + 1)]: Number(ms) });
  });
  return byBook;
}

const put = (id, index, ms) => {
  const book = load().get(id) ?? {};
  if (book[index] === ms) return;
  load().set(id, { ...book, [index]: ms });
  putRow(STORE, `${id}${SEP}${index}`, String(ms));
};

export const noteDuration = put;
export const getDurations = (id) => load().get(id) ?? {};

export function forgetDurations(id) {
  Object.keys(getDurations(id)).forEach((i) => delRow(STORE, `${id}${SEP}${i}`));
  load().delete(id);
}

export function moveDurations(from, to) {
  const known = getDurations(from);
  forgetDurations(from);
  Object.entries(known).forEach(([i, ms]) => put(to, i, ms));
}

// Длины глав, которые известны заранее (из оглавления файла).
export const seedDurations = (id, known) => Object.entries(known).forEach(([i, ms]) => put(id, i, ms));
