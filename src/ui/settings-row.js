import { el } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';

// Строка настроек: эмодзи, подпись и значение (оба — ключи перевода).
export function createRow(emoji, labelKey, valueKey, valueParams) {
  const row = el('div', 'row');
  row.append(
    el('span', 'row-emoji', emoji),
    bindText(el('span', 'row-label'), labelKey),
    bindText(el('span', 'row-value'), valueKey, valueParams),
  );
  return row;
}
