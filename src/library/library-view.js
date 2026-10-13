import { el } from '../core/dom.js';
import { onNavigate } from '../app/router.js';
import { createEmpty } from './empty.js';
import { createItems } from './items-list.js';
import { createLibBar } from './lib-bar.js';
import { getFiles, getStatus } from './library-data.js';
import { getView } from './view-pref.js';

export function renderLibrary() {
  const root = el('section', 'library');
  const bar = createLibBar();
  const items = createItems();
  const empty = createEmpty();
  root.append(bar, items.node, empty.node);

  const paint = () => {
    const status = getStatus();
    const files = getFiles();
    const has = status === 'ok' && files.length > 0;
    bar.hidden = status !== 'ok';
    items.node.hidden = !has;
    empty.node.hidden = status === 'loading' || has;
    if (has) items.show(files, getView());
    else if (status !== 'loading') empty.show(status);
  };

  document.addEventListener('librarychange', paint);
  document.addEventListener('langchange', paint);
  document.addEventListener('viewchange', () => items.setView(getView()));
  onNavigate((id) => id === 'library' && paint()); // прогресс мог измениться (обложки-эмодзи)
  paint();
  return root;
}
