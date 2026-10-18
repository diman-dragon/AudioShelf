import { getFolder } from '../folder/folder-store.js';
import { isHidden } from './hidden-store.js';
import { groupBooks } from './books.js';
import { loadLibrary, saveLibrary } from './library-cache.js';

let all = [];
let status = 'nofolder';
let scanning = false;
let progress = { files: 0, folders: 0 };

let books = null;

export const notifyLibrary = (name = 'librarychange') => {
  if (name === 'librarychange') books = null;
  document.dispatchEvent(new Event(name));
};

export const getStatus = () => status;
export const isScanning = () => scanning;
export const getProgress = () => progress;
export const getFileCount = () => all.length;
export const getFiles = () => all;
export const getBooks = () => {
  books ??= groupBooks(all)
    .filter((b) => !isHidden(b.id))
    .sort((a, b) => b.modified - a.modified);
  return books;
};

export function restoreLibrary() {
  const folder = getFolder();
  const saved = folder && loadLibrary(folder.uri);
  all = saved ?? [];
  status = !folder ? 'nofolder' : saved ? 'ok' : 'unscanned';
  notifyLibrary();
}

export function patchFiles(change) {
  all = change(all);
  saveLibrary(getFolder().uri, all);
  notifyLibrary();
}

export function setScanning(on) {
  scanning = on;
  progress = { files: 0, folders: 0 };
  notifyLibrary();
}

export function setProgress(next) {
  progress = next;
  notifyLibrary('scanprogress');
}

export function finishScan(files, failed) {
  if (failed) status = 'error';
  else if (files) {
    all = files;
    status = 'ok';
    saveLibrary(getFolder().uri, all);
  }
  scanning = false;
  notifyLibrary();
}
