import { el, button } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { cancelScan } from './scan.js';

// Плашка «Сканирование…»: счётчик найденного, бегущая полоса и «Остановить».
export function createScanPanel() {
  const node = el('div', 'scan');
  const count = el('div', 'small muted');
  const bar = el('div', 'scan-bar');
  bar.append(el('i'));
  node.append(bindText(el('div', 'scan-title'), 'scan.title'), count, bar, bindText(button('btn ghost', cancelScan), 'scan.stop'));
  return {
    node,
    update: (p) => bindText(count, 'scan.count', p),
  };
}
