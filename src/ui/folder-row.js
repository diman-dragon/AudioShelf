import { el, button } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { Folder } from '../folder/plugins.js';
import { getFolder, setFolder } from '../folder/folder-store.js';

// Строка «Папка с аудио»: название, подсказка и кнопка выбора (системный диалог выбора папки).
export function createFolderRow() {
  const name = el('div', 'small');
  const text = el('div');
  text.append(
    bindText(el('div'), 'settings.folder'),
    name,
    bindText(el('div', 'small muted'), 'settings.folderHint'),
  );

  const paint = () => {
    const folder = getFolder();
    if (folder?.name) {
      delete name.dataset.i18n; // реальное имя папки не переводится
      name.textContent = folder.name;
    } else {
      bindText(name, 'settings.noFolder');
    }
  };
  document.addEventListener('folderchange', paint);
  paint();

  const pick = button('btn', () => Folder.pickFolder().then(setFolder).catch(() => {}));
  const row = el('div', 'row');
  row.append(text, bindText(pick, 'settings.choose'));
  return row;
}
