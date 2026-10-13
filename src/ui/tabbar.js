import { el } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { createArt } from './art.js';

function createTab(screen, onSelect) {
  const btn = el('button', 'tab');
  btn.type = 'button';
  btn.append(createArt(screen.icon, 34, 'tab-icon'), bindText(el('span', 'tab-label'), screen.key));
  btn.addEventListener('click', () => onSelect(screen.id));
  return btn;
}

// Нижние вкладки: картинка + подпись, активная подсвечивается.
export function createTabbar(screens, onSelect) {
  const node = el('nav', 'tabbar');
  const tabs = new Map();

  screens.forEach((screen) => {
    const btn = createTab(screen, onSelect);
    node.append(btn);
    tabs.set(screen.id, btn);
  });

  const setActive = (id) => {
    tabs.forEach((btn, key) => {
      btn.classList.toggle('active', key === id);
      btn.setAttribute('aria-current', key === id ? 'page' : 'false');
    });
  };

  return { node, setActive };
}
