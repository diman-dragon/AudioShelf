import { el } from '../core/dom.js';

export function createProgress() {
  const box = el('div', 'progress');
  const bar = el('div', 'progress-bar');
  bar.append(el('div', 'progress-fill'));
  const times = el('div', 'progress-times');
  times.append(el('span', '', '0:00'), el('span', '', '0:00'));
  box.append(bar, times);
  return box;
}
