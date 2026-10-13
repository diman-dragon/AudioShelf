import { el, button } from '../core/dom.js';
import { bindAria, bindText } from '../i18n/bind.js';
import { addFiles } from './actions.js';
import { REFRESH_ICON } from './icons.js';
import { refresh } from './library-data.js';
import { createViewSwitch } from './view-switch.js';

// Панель над списком: вид, «Обновить» (повторное сканирование) и «Добавить». Названия папки нет.
export function createLibBar() {
  const node = el('div', 'lib-bar');
  const rescan = button('icon-btn', refresh);
  rescan.innerHTML = REFRESH_ICON;
  bindAria(rescan, 'lib.rescan');
  const right = el('div', 'lib-bar-right');
  right.append(rescan, bindText(button('btn', addFiles), 'lib.add'));
  node.append(createViewSwitch(), right);
  return {
    node,
    setBusy(busy) {
      rescan.disabled = busy;
      rescan.classList.toggle('spin', busy);
    },
  };
}
