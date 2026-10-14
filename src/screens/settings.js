import { el } from '../core/dom.js';
import { createSettingsMenu } from '../settings/settings-menu.js';
import { createSettingsPage } from '../settings/settings-page.js';
import { PAGES } from '../settings/settings-pages.js';
import { getPage } from '../settings/settings-nav.js';
import { createSettingsFooter } from '../ui/settings-footer.js';

// Настройки: сверху список разделов или открытый раздел, внизу всегда версия и копирайт.
export function renderSettings() {
  const root = el('section', 'settings');
  const body = el('div', 'settings-body');
  root.append(body, createSettingsFooter());

  const menu = createSettingsMenu();
  const pages = new Map(PAGES.map((page) => [page.id, createSettingsPage(page)])); // страницы живут, состояние не теряется

  const paint = () => {
    const id = getPage();
    body.replaceChildren(id ? pages.get(id) : menu);
    body.scrollTop = 0;
  };
  document.addEventListener('settingspage', paint);
  paint();
  return root;
}
