import { el, button } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { hiddenCount, restoreHidden } from '../library/hidden-store.js';
import { notifyLibrary } from '../library/library-data.js';

// Строка «Скрытые файлы: N — Вернуть»; видна, только если такие есть.
export function createHiddenRow() {
  const row = el('div', 'row');
  const text = el('span');
  const restore = bindText(button('btn', () => {
    restoreHidden();
    notifyLibrary();
  }), 'settings.restore');
  row.append(text, restore);

  const paint = () => {
    const n = hiddenCount();
    row.hidden = n === 0;
    bindText(text, 'settings.hidden', { n });
  };
  document.addEventListener('librarychange', paint);
  paint();
  return row;
}
