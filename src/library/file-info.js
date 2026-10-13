import { t } from '../i18n/i18n.js';
import { getMeta } from './meta-store.js';
import { splitName } from './format.js';

// Если тип не задан: .m4b — книга, всё остальное — музыка.
export const kindOf = (file) => getMeta(file.uri).kind ?? (file.name.toLowerCase().endsWith('.m4b') ? 'book' : 'album');

export const titleOf = (file) => getMeta(file.uri).title || splitName(file.name)[0];

export const authorOf = (file) => getMeta(file.uri).author ?? '';

// «Автор · Чтец: Имя» — вторая строка в библиотеке и плеере.
export function bylineOf(file) {
  const { author, narrator } = getMeta(file.uri);
  return [author, narrator && t('info.narrator', { n: narrator })].filter(Boolean).join(' · ');
}
