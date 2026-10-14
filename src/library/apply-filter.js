import { lastPlayed } from '../player/progress-store.js';
import { kindOf, titleOf } from './file-info.js';
import { getMeta } from './meta-store.js';

const norm = (text) => String(text ?? '').toLowerCase();

// Где ищем: название, имя папки/файла, автор, чтец, год.
function haystack(book) {
  const { author, narrator, year } = getMeta(book.id);
  return norm([titleOf(book), book.name, author, narrator, year].join(' '));
}

const SORTERS = {
  date: (a, b) => b.modified - a.modified,
  name: (a, b) => titleOf(a).localeCompare(titleOf(b), undefined, { numeric: true, sensitivity: 'base' }),
  played: (a, b) => lastPlayed(b.id) - lastPlayed(a.id) || b.modified - a.modified,
};

export function filterBooks(books, { kind, sort, query }) {
  const q = norm(query).trim();
  return books
    .filter((b) => (kind === 'all' || kindOf(b) === kind) && (!q || haystack(b).includes(q)))
    .sort(SORTERS[sort]);
}
