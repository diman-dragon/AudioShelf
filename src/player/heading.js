import { t } from '../i18n/i18n.js';
import { authorOf, bylineOf, kindOf, titleOf } from '../library/file-info.js';
import { splitName } from '../library/format.js';
import { chapterLabel } from './chapter-label.js';

export function headingOf(book, index) {
  const multi = book.files.length > 1;
  if (multi && kindOf(book) === 'album') {
    return {
      title: splitName(book.files[index].name)[0],
      sub: [titleOf(book), authorOf(book)].filter(Boolean).join(' · '),
      label: t('player.trackOf', { n: index + 1, m: book.files.length }),
    };
  }
  return { title: titleOf(book), sub: bylineOf(book), label: multi ? chapterLabel(book, index) : '' };
}
