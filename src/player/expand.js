import { chaptersOf } from './chapter-store.js';
import { forgetDurations, seedDurations } from './durations.js';
import { remapMarks } from './bookmark-store.js';
import { remapProgress } from './progress-store.js';

// Книга -> книга, где файл с главами внутри заменён своими главами: каждая — «файл» с началом и концом (мс) в том же uri.
// Дальше всё работает как с обычными главами: список, «следующая», пауза после главы, таймер до конца главы.
export function expandBook(book) {
  if (!book?.files) return book;
  let changed = false;
  const files = book.files.flatMap((f) => {
    const list = chaptersOf(f);
    if (!list || list.length < 2) return [f];
    changed = true;
    const ext = f.name.match(/\.[^.]*$/)?.[0] ?? '';
    return list.map((c) => ({ ...f, name: `${c.t}${ext}`, start: c.s, end: c.e }));
  });
  if (!changed) return book;
  const expanded = { ...book, files, expanded: true, base: book };
  migrate(book, expanded);
  return expanded;
}

// Прежние записи (прогресс, закладки) хранили номер файла и позицию в нём; теперь это номер главы и позиция в главе.
function migrate(book, expanded) {
  const map = (i, p) => {
    const file = book.files[i];
    const parts = file ? expanded.files.map((f, k) => [f, k]).filter(([f]) => f.uri === file.uri) : [];
    if (!parts.length) return { i: 0, p };
    if (parts[0][0].start === undefined) return { i: parts[0][1], p }; // обычный файл: сдвинулся только номер
    const hit = parts.filter(([f]) => f.start <= p).pop() ?? parts[0];
    return { i: hit[1], p: Math.max(0, p - hit[0].start) };
  };
  remapProgress(book.id, map);
  remapMarks(book.id, map);
  forgetDurations(book.id);
  const known = {};
  expanded.files.forEach((f, k) => {
    if (f.end > 0) known[k] = f.end - f.start;
  });
  seedDurations(book.id, known);
}
