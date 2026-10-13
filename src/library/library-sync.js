import { App } from '@capacitor/app';
import { refresh } from './library-data.js';

// Когда перечитывать папку: при смене папки, возврате в приложение и на старте.
export function initLibrarySync() {
  document.addEventListener('folderchange', refresh);
  App.addListener('appStateChange', (s) => s.isActive && refresh());
  refresh();
}
