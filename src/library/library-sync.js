import { restoreLibrary } from './library-data.js';
import { rescan } from './rescan.js';

export function initLibrarySync() {
  document.addEventListener('folderchange', () => {
    restoreLibrary();
    rescan();
  });
  restoreLibrary();
}
