import { getFolder } from '../folder/folder-store.js';
import { isHidden } from './hidden-store.js';
import { cancelScan, scanFolder } from './scan.js';

let files = [];
let status = getFolder() ? 'loading' : 'nofolder'; // loading | nofolder | error | ok
let scanning = false;
let again = false; // пока шёл обход, папку сменили или попросили обновить
let progress = { files: 0, folders: 0 };

export const getFiles = () => files;
export const getStatus = () => status;
export const isScanning = () => scanning;
export const getProgress = () => progress;

const announce = (name = 'librarychange') => document.dispatchEvent(new Event(name));

// Обходит папку (с подпапками), показывает прогресс и сообщает экранам событием librarychange.
export async function refresh() {
  const folder = getFolder();
  if (!folder) {
    files = [];
    status = 'nofolder';
    return announce();
  }
  if (scanning) {
    again = true;
    return cancelScan();
  }
  scanning = true;
  if (status === 'nofolder' || status === 'error') status = 'loading';
  progress = { files: 0, folders: 0 };
  announce();
  try {
    const result = await scanFolder(folder.uri, (p) => {
      progress = p;
      announce('scanprogress');
    });
    files = result.files.filter((f) => !isHidden(f.uri)).sort((a, b) => b.modified - a.modified);
    status = 'ok';
  } catch {
    files = [];
    status = 'error';
  }
  scanning = false;
  announce();
  if (again) {
    again = false;
    refresh();
  }
}

export { cancelScan };
