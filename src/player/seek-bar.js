import { el } from '../core/dom.js';
import { fmtTime } from './time.js';

// Ползунок перемотки и время: прошло / всего.
export function createSeek(onSeek) {
  const node = el('div', 'seek');
  const range = el('input');
  Object.assign(range, { type: 'range', min: 0, max: 0, value: 0, step: 1 });
  const times = el('div', 'times');
  const cur = el('span');
  const total = el('span');
  times.append(cur, total);
  node.append(range, times);

  let dragging = false;
  range.addEventListener('input', () => {
    dragging = true;
    cur.textContent = fmtTime(Number(range.value));
  });
  range.addEventListener('change', () => {
    dragging = false;
    onSeek(Number(range.value));
  });

  return {
    node,
    update(position, duration) {
      range.max = duration;
      range.disabled = !duration;
      total.textContent = duration ? fmtTime(duration) : '';
      if (dragging) return;
      range.value = position;
      cur.textContent = fmtTime(position);
    },
  };
}
