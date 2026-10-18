import { el, button, image } from '../core/dom.js';
import { bindAria } from '../i18n/bind.js';
import { t } from '../i18n/i18n.js';
import { fmtDate, fmtSize, splitName } from './format.js';
import { bylineOf, kindOf, titleOf } from './file-info.js';
import { getMeta } from './meta-store.js';
import { applyPlaceholder } from './placeholder.js';
import { percentOf } from '../player/progress-store.js';

export function paintProgress(bar) {
  const pc = percentOf(bar.dataset.id);
  bar.hidden = pc === null;
  if (pc === null) return;
  bar.firstChild.firstChild.style.width = `${pc}%`;
  bar.setAttribute('aria-label', `${pc}%`);
  bar.lastChild.textContent = `${pc}%`;
}

function infoOf(book) {
  const { year } = getMeta(book.id);
  const what = book.folder ? t('info.files', { n: book.files.length }) : splitName(book.files[0].name)[1].slice(1).toUpperCase();
  return [t(`kind.${kindOf(book)}`), year, what, fmtSize(book.size), fmtDate(book.modified)].filter(Boolean).join(' · ');
}

export function createItem(book, { onOpen, onMenu }) {
  const item = el('div', 'item');
  item._file = book.files[0];
  item._book = book;

  const thumb = el('div', 'thumb');
  const ph = image('ph');
  applyPlaceholder(ph, book);
  thumb.append(ph, image('pv'));

  const meta = el('div', 'meta');
  meta.append(el('div', 'fname', titleOf(book)));
  const byline = bylineOf(book);
  if (byline) meta.append(el('div', 'fauthor', byline));
  meta.append(el('div', 'fsub', infoOf(book)));
  const bar = el('div', 'fprog');
  bar.dataset.id = book.id;
  const track = el('div', 'fprog-track');
  track.append(el('div', 'fprog-fill'));
  bar.append(track, el('span', 'fprog-pc'));
  paintProgress(bar);
  meta.append(bar);

  const more = button('more', (e) => {
    e.stopPropagation();
    onMenu(book);
  });
  more.textContent = '⋮';
  bindAria(more, 'lib.menu');

  item.append(thumb, meta, more);
  item.addEventListener('click', () => onOpen(book));
  return item;
}
