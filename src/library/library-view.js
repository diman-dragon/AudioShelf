import { el } from '../core/dom.js';
import { getScreen, onNavigate } from '../app/router.js';
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

  // Список строк пересобирается, только когда что-то в нём правда изменилось: данные (force) или состав и порядок книг.
  // Пока вкладка не открыта, не рисуем вовсе, а при возвращении дорисовываем, если что-то менялось.
  let drawn = '';
  let dirty = false;

  const paint = (force = false) => {
    if (getScreen() !== 'library') {
      dirty = dirty || force;
      return;
    }
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
    const sig = `${getView()}|${shown.map((b) => b.id).join('\u0001')}`;
    if (shown.length && (force || dirty || sig !== drawn)) items.show(shown, getView());
    else if (!shown.length && !empty.node.hidden) empty.show(status);
    if (shown.length) drawn = sig;
    dirty = false;
  };

  ['librarychange', 'langchange'].forEach((name) => document.addEventListener(name, () => paint(true)));
  document.addEventListener('filterchange', () => paint());
  document.addEventListener('scanprogress', () => scan.update(getProgress()));
  document.addEventListener('viewchange', () => items.setView(getView()));
  onNavigate((id) => id === 'library' && paint()); // вернулись: порядок мог измениться (сортировка «недавно слушали»)
  paint();
  return root;
}
