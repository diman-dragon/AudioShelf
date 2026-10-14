import { el, button } from '../core/dom.js';
import { bindAria, bindText } from '../i18n/bind.js';
import { MARK_ICON, MODE_ICONS } from './mode-icons.js';

// Кнопка с пиктограммой и подписью под ней.
function extra(onClick) {
  const btn = button('pextra', onClick);
  const icon = el('span', 'pi');
  const label = el('span', 'pl');
  btn.append(icon, label);
  return { btn, icon, label };
}

// Ряд под кнопками плеера: [Закладки] [Режим]. Режим зависит от типа (книга / альбом).
export function createExtras({ onMarks, onMode }) {
  const node = el('div', 'pextras');
  const marks = extra(onMarks);
  const mode = extra(onMode);
  marks.icon.innerHTML = MARK_ICON;
  node.append(marks.btn, mode.btn);
  return {
    node,
    update({ markCount, mode: current }) {
      bindText(marks.label, markCount ? 'player.marksN' : 'player.marks', { n: markCount });
      bindText(mode.label, `mode.${current}`);
      bindAria(mode.btn, `mode.${current}`);
      mode.icon.innerHTML = MODE_ICONS[current];
    },
  };
}
