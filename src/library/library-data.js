import { Folder } from '../folder/plugins.js';
import { getFolder } from '../folder/folder-store.js';
import { isHidden } from './hidden-store.js';

let files = [];
let status = getFolder() ? 'loading' : 'nofolder'; // loading | nofolder | error | ok

export const getFiles = () => files;
export const getStatus = () => status;

// Перечитывает папку и сообщает экрану событием librarychange.
export async function refresh() {
  const folder = getFolder();
  if (!folder) {
    files = [];
    status = 'nofolder';
  } else {
    try {
      const result = await Folder.list({ tree: folder.uri });
      files = result.files.filter((f) => !isHidden(f.uri)).sort((a, b) => b.modified - a.modified);
      status = 'ok';
    } catch {
      files = [];
      status = 'error';
    }
  }
  document.dispatchEvent(new Event('librarychange'));
}
