import { t } from '../i18n/i18n.js';
import { getDurations } from '../player/durations.js';
import { getMeta } from './meta-store.js';

const BOOK_SIZE = 30 * 1048576;
const BOOK_MS = 30 * 60000;

function looksLikeBook(book) {
  if (book.folder) return true;
  const [file] = book.files;
  return file.name.toLowerCase().endsWith('.m4b') || file.size >= BOOK_SIZE || (getDurations(book.id)[0] ?? 0) >= BOOK_MS;
}

export const kindOf = (book) => getMeta(book.id).kind ?? (looksLikeBook(book) ? 'book' : 'album');

export const titleOf = (book) => getMeta(book.id).title || book.name;

export const authorOf = (book) => getMeta(book.id).author ?? '';

export function bylineOf(book) {
  const { author, narrator } = getMeta(book.id);
  return [author, narrator && t('info.narrator', { n: narrator })].filter(Boolean).join(' · ');
}
