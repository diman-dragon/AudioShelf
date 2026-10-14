import { el, button } from '../core/dom.js';
import { bindAria, bindText } from '../i18n/bind.js';
import { closePage } from './settings-nav.js';

// Страница раздела: «‹» + название сверху, затем содержимое.
export function createSettingsPage(page) {
  const node = el('div', 'page');
  const back = button('page-back', closePage);
  back.textContent = '‹';
  bindAria(back, 'settings.back');
  const head = el('div', 'page-head');
  head.append(back, bindText(el('span', 'page-title'), page.titleKey));
  node.append(head, ...page.build());
  return node;
}
