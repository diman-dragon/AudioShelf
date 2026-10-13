import { el, button } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { addFiles } from './actions.js';
import { createViewSwitch } from './view-switch.js';

// Панель над списком: переключатель вида и «Добавить». Названия папки нет.
export function createLibBar() {
  const bar = el('div', 'lib-bar');
  bar.append(createViewSwitch(), bindText(button('btn', addFiles), 'lib.add'));
  return bar;
}
