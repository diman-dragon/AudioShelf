import { el } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';

// Строка настроек: подпись слева, элемент управления справа.
export function createRow(labelKey, control) {
  const row = el('div', 'row');
  row.append(bindText(el('span'), labelKey), control);
  return row;
}
