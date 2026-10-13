import { el, button } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { Folder } from '../folder/plugins.js';
import { getFolder, setFolder } from '../folder/folder-store.js';
import { folderPath } from '../library/folder-path.js';
import { createFolderStat } from './folder-stat.js';

// Строка «Папка с аудио»: название, подсказка и кнопка выбора (системный диалог выбора папки).
export function createFolderRow() {
  const name = el('div', 'small');
  const text = el('div');
  text.append(
    bindText(el('div'), 'settings.folder'),
    name,
    createFolderStat(),
    bindText(el('div', 'small muted'), 'settings.folderHint'),
  );

  const paint = () => {
    const folder = getFolder();
    if (folder) {
      delete name.dataset.i18n; // путь собирается сам (с переводом «Память устройства»)
      name.textContent = folderPath(folder);
    } else {
      bindText(name, 'settings.noFolder');
    }
  };
  document.addEventListener('folderchange', paint);
  document.addEventListener('langchange', paint);
  paint();

  const pick = button('btn', () => Folder.pickFolder().then(setFolder).catch(() => {}));
  const row = el('div', 'row');
  row.append(text, bindText(pick, 'settings.choose'));
  return row;
}
