import { el, button } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { PAGES } from './settings-pages.js';
import { openPage } from './settings-nav.js';

// Список разделов: название и «›».
export function createSettingsMenu() {
  const node = el('div', 'menu');
  PAGES.forEach((page) => {
    const row = button('menu-row', () => openPage(page.id));
    row.append(bindText(el('span'), page.titleKey), el('span', 'chev', '›'));
    node.append(row);
  });
  return node;
}
