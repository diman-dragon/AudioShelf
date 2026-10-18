import { t } from '../i18n/i18n.js';
import { splitName } from '../library/format.js';

export const chapterLabel = (book, index) =>
  `${t('player.chapterOf', { n: index + 1, m: book.files.length })} · ${splitName(book.files[index].name)[0]}`;
