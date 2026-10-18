import { el } from '../core/dom.js';

export function createHost(screens) {
  const node = el('main', 'host');
  const wraps = new Map();

  screens.forEach((screen) => {
    const wrap = el('div', 'screen');
    wrap.append(screen.render());
    node.append(wrap);
    wraps.set(screen.id, wrap);
  });

  const show = (id, dir = 0) => {
    wraps.forEach((wrap, key) => {
      const on = key === id;
      wrap.classList.toggle('active', on);
      wrap.classList.remove('from-left', 'from-right');
      if (on && dir) wrap.classList.add(dir > 0 ? 'from-right' : 'from-left');
    });
  };

  return { node, show };
}
