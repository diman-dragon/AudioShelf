import { el, button } from '../core/dom.js';
import { bindAria } from '../i18n/bind.js';
import { t } from '../i18n/i18n.js';
import { kindOf } from '../library/file-info.js';
import { fmtDate } from '../library/format.js';
import { fmtTime } from './time.js';

// Строка закладки: «Глава 3 · 1:02:05», дата; справа ✕.
export function createMarkRow(book, mark, { onJump, onRemove }) {
  const row = el('div', 'mrow');
  const go = button('mgo', () => onJump(mark));
  const multi = book.files.length > 1;
  const where = multi ? t(kindOf(book) === 'album' ? 'mark.track' : 'mark.chapter', { n: mark.i + 1 }) : '';
  go.append(el('span', 'mtime', [where, fmtTime(mark.p)].filter(Boolean).join(' · ')), el('span', 'mdate', fmtDate(mark.t)));
  const del = button('mdel', () => onRemove(mark));
  del.textContent = '✕';
  bindAria(del, 'marks.remove');
  row.append(go, del);
  return row;
}
