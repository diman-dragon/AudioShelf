import { el, button } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { Folder } from '../folder/plugins.js';
import { getFolder, setFolder } from '../folder/folder-store.js';
import { folderPath } from '../library/folder-path.js';
import { createFolderStat } from './folder-stat.js';
import { openForgetDialog } from './forget-dialog.js';

// Строка «Папка с аудио»: путь, подсказка, «Выбрать» (системный выбор папки) и «Убрать» (перестать следить).
export function createFolderRow() {
  const name = el('div', 'small');
  const text = el('div');
  text.append(
    bindText(el('div'), 'settings.folder'),
    name,
    createFolderStat(),
  );

  const forget = bindText(button('btn ghost', openForgetDialog), 'settings.forget');
  const paint = () => {
    const folder = getFolder();
    forget.hidden = !folder;
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

  const pick = bindText(button('btn', () => Folder.pickFolder().then(setFolder).catch(() => {})), 'settings.choose');
  const actions = el('div', 'row-actions');
  actions.append(pick, forget);
  const row = el('div', 'row');
  row.append(text, actions);
  return row;
}
