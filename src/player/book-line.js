import { el } from '../core/dom.js';
import { t } from '../i18n/i18n.js';
import { fmtTime } from './time.js';

export function createBookLine() {
  const node = el('div', 'pline');
  const bar = el('div', 'pline-bar');
  const fill = el('div', 'pline-fill');
  const text = el('div', 'pline-text');
  bar.append(fill);
  node.append(bar, text);
  return {
    node,
    update(info) {
      node.hidden = !info;
      if (!info) return;
      fill.style.width = `${info.percent}%`;
      text.textContent = t(info.exact ? 'player.bookLeft' : 'player.bookLeftApprox', { p: info.percent, left: fmtTime(info.left) });
    },
  };
}
