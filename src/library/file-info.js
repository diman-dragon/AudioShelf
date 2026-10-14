import { t } from '../i18n/i18n.js';
import { getMeta } from './meta-store.js';

// Если тип не задан: папка с главами и .m4b — книга, остальное — музыка.
export const kindOf = (book) =>
  getMeta(book.id).kind ?? (book.folder || book.files[0].name.toLowerCase().endsWith('.m4b') ? 'book' : 'album');

export const titleOf = (book) => getMeta(book.id).title || book.name;

export const authorOf = (book) => getMeta(book.id).author ?? '';

// «Автор · Чтец: Имя» — вторая строка в библиотеке и плеере.
export function bylineOf(book) {
  const { author, narrator } = getMeta(book.id);
  return [author, narrator && t('info.narrator', { n: narrator })].filter(Boolean).join(' · ');
}
