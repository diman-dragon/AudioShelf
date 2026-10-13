import { el, button, image } from '../core/dom.js';
import { bindAria } from '../i18n/bind.js';
import { t } from '../i18n/i18n.js';
import { fmtDate, fmtSize, splitName } from './format.js';
import { bylineOf, kindOf, titleOf } from './file-info.js';
import { getMeta } from './meta-store.js';
import { applyPlaceholder } from './placeholder.js';

// Сведения: «Книга · 2019 · MP3 · 8,8 МБ · 13.10.2026».
function infoOf(file) {
  const { year } = getMeta(file.uri);
  const ext = splitName(file.name)[1].slice(1).toUpperCase();
  return [t(`kind.${kindOf(file)}`), year, ext, fmtSize(file.size), fmtDate(file.modified)].filter(Boolean).join(' · ');
}

// Один файл: обложка (пока нет — эмодзи), название, автор, сведения и кнопка «⋮».
export function createItem(file, { onOpen, onMenu }) {
  const item = el('div', 'item');
  item._file = file;

  const thumb = el('div', 'thumb');
  const ph = image('ph');
  applyPlaceholder(ph, file);
  thumb.append(ph, image('pv'));

  const meta = el('div', 'meta');
  meta.append(el('div', 'fname', titleOf(file)));
  const byline = bylineOf(file);
  if (byline) meta.append(el('div', 'fauthor', byline));
  meta.append(el('div', 'fsub', infoOf(file)));

  const more = button('more', (e) => {
    e.stopPropagation();
    onMenu(file);
  });
  more.textContent = '⋮';
  bindAria(more, 'lib.menu');

  item.append(thumb, meta, more);
  item.addEventListener('click', () => onOpen(file));
  return item;
}
