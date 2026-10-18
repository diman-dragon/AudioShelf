import { el } from '../core/dom.js';
import { createItem } from './item.js';
import { watchCover } from './cover-watch.js';
import { playFromLibrary } from './actions.js';
import { openFileMenu } from './menu.js';

const BATCH = 40;

export function createItems() {
  const node = el('div', 'lib-items');
  let run = 0;

  function fill(books, from, id) {
    if (id !== run) return;
    const part = books.slice(from, from + BATCH).map((b) => {
      const item = createItem(b, { onOpen: playFromLibrary, onMenu: openFileMenu });
      watchCover(item);
      return item;
    });
    node.append(...part);
    if (from + BATCH < books.length) setTimeout(() => fill(books, from + BATCH, id));
  }

  return {
    node,
    setView(view) {
      node.className = `lib-items ${view}`;
    },
    show(books, view) {
      this.setView(view);
      node.replaceChildren();
      fill(books, 0, ++run);
    },
  };
}
