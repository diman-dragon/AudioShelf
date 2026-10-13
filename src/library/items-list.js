import { el } from '../core/dom.js';
import { createItem } from './item.js';
import { watchCover } from './cover-watch.js';
import { playFromLibrary } from './actions.js';
import { openFileMenu } from './menu.js';

// Контейнер файлов; вид (list | cards) задаётся классом.
export function createItems() {
  const node = el('div', 'lib-items');
  return {
    node,
    setView: (view) => {
      node.className = `lib-items ${view}`;
    },
    show(files, view) {
      this.setView(view);
      node.replaceChildren(
        ...files.map((f) => {
          const item = createItem(f, { onOpen: playFromLibrary, onMenu: openFileMenu });
          watchCover(item);
          return item;
        }),
      );
    },
  };
}
