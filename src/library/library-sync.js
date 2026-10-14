import { restoreLibrary } from './library-data.js';
import { rescan } from './rescan.js';

// Обход запускается только при выборе папки (и по кнопке «Обновить»). На старте — сохранённый список.
export function initLibrarySync() {
  document.addEventListener('folderchange', () => {
    restoreLibrary(); // сначала то, что уже знаем об этой папке
    rescan();
  });
  restoreLibrary();
}
