import { el } from '../core/dom.js';

export function createRow(emoji, label, value) {
  const row = el('div', 'row');
  row.append(
    el('span', 'row-emoji', emoji),
    el('span', 'row-label', label),
    el('span', 'row-value', value),
  );
  return row;
}
