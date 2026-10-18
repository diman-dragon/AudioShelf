import { el, button } from '../core/dom.js';
import { bindAria, bindText } from '../i18n/bind.js';
import { addFiles } from './actions.js';
import { REFRESH_ICON } from './icons.js';
import { rescan } from './rescan.js';
import { createViewSwitch } from './view-switch.js';

export function createLibBar() {
  const node = el('div', 'lib-bar');
  const refreshBtn = button('icon-btn', rescan);
  refreshBtn.innerHTML = REFRESH_ICON;
  bindAria(refreshBtn, 'lib.rescan');
  const right = el('div', 'lib-bar-right');
  right.append(refreshBtn, bindText(button('btn', addFiles), 'lib.add'));
  node.append(createViewSwitch(), right);
  return {
    node,
    setBusy(busy) {
      refreshBtn.disabled = busy;
      refreshBtn.classList.toggle('spin', busy);
    },
  };
}
