import { el } from '../core/dom.js';
import { onNavigate } from '../app/router.js';
import { createEmpty } from './empty.js';
import { createItems } from './items-list.js';
import { createLibBar } from './lib-bar.js';
import { getFiles, getProgress, getStatus, isScanning } from './library-data.js';
import { createScanPanel } from './scan-panel.js';
import { getView } from './view-pref.js';

export function renderLibrary() {
  const root = el('section', 'library');
  const bar = createLibBar();
  const scan = createScanPanel();
  const items = createItems();
  const empty = createEmpty();
  root.append(bar.node, scan.node, items.node, empty.node);

  const paint = () => {
    const status = getStatus();
    const files = getFiles();
    const scanning = isScanning();
    const has = status === 'ok' && files.length > 0;
    bar.node.hidden = status === 'nofolder' || status === 'error';
    bar.setBusy(scanning);
    scan.node.hidden = !scanning;
    if (scanning) scan.update(getProgress());
    items.node.hidden = !has;
    empty.node.hidden = scanning || status === 'loading' || has;
    if (has) items.show(files, getView());
    else if (!empty.node.hidden) empty.show(status);
  };

  document.addEventListener('librarychange', paint);
  document.addEventListener('langchange', paint);
  document.addEventListener('scanprogress', () => scan.update(getProgress()));
  document.addEventListener('viewchange', () => items.setView(getView()));
  onNavigate((id) => id === 'library' && paint()); // прогресс мог измениться (обложки-эмодзи)
  paint();
  return root;
}
