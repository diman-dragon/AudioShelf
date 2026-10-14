import { el } from '../core/dom.js';
import { getBooks, getFileCount, getProgress, getStatus, isScanning } from '../library/library-data.js';
import { t } from '../i18n/i18n.js';

// Строка под папкой: сколько файлов нашлось (или идёт сканирование).
export function createFolderStat() {
  const node = el('div', 'small');
  const paint = () => {
    const status = getStatus();
    node.hidden = status === 'nofolder' || status === 'error';
    if (isScanning()) node.textContent = t('settings.scanning', { n: getProgress().files });
    else if (status === 'ok') node.textContent = t('settings.found', { books: getBooks().length, files: getFileCount() });
  };
  ['librarychange', 'scanprogress', 'langchange'].forEach((name) => document.addEventListener(name, paint));
  paint();
  return node;
}
