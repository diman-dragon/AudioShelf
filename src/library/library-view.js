import { el } from '../core/dom.js';
import { onNavigate } from '../app/router.js';
import { filterBooks } from './apply-filter.js';
import { createEmpty } from './empty.js';
import { getFilter } from './filter-state.js';
import { createItems } from './items-list.js';
import { createLibBar } from './lib-bar.js';
import { createLibFilter } from './lib-filter.js';
import { getBooks, getProgress, getStatus, isScanning } from './library-data.js';
import { createNoMatch } from './no-match.js';
import { createScanPanel } from './scan-panel.js';
import { getView } from './view-pref.js';

export function renderLibrary() {
  const root = el('section', 'library');
  const bar = createLibBar();
  const filter = createLibFilter();
  const scan = createScanPanel();
  const items = createItems();
  const empty = createEmpty();
  const noMatch = createNoMatch();
  root.append(bar.node, filter, scan.node, items.node, noMatch, empty.node);

  const paint = () => {
    const status = getStatus();
    const books = getBooks();
    const scanning = isScanning();
    const has = status === 'ok' && books.length > 0;
    const shown = has ? filterBooks(books, getFilter()) : [];
    bar.node.hidden = status === 'nofolder' || status === 'error';
    bar.setBusy(scanning);
    filter.hidden = !has;
    scan.node.hidden = !scanning;
    if (scanning) scan.update(getProgress());
    items.node.hidden = shown.length === 0;
    noMatch.hidden = !has || shown.length > 0;
    empty.node.hidden = scanning || has;
    if (shown.length) items.show(shown, getView());
    else if (!empty.node.hidden) empty.show(status);
  };

  document.addEventListener('librarychange', paint);
  document.addEventListener('filterchange', paint);
  document.addEventListener('langchange', paint);
  document.addEventListener('scanprogress', () => scan.update(getProgress()));
  document.addEventListener('viewchange', () => items.setView(getView()));
  onNavigate((id) => id === 'library' && paint()); // прогресс мог измениться (обложки-эмодзи)
  paint();
  return root;
}
