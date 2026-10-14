import { getFolder } from '../folder/folder-store.js';
import { isHidden } from './hidden-store.js';
import { groupBooks } from './books.js';
import { loadLibrary, saveLibrary } from './library-cache.js';

// Состояние библиотеки: все файлы папки (в том числе скрытые) и что с ними сейчас происходит.
// Обход папки — в rescan.js; здесь только хранение и сообщения экранам.
let all = [];
let status = 'nofolder'; // nofolder | unscanned | ok | error
let scanning = false;
let progress = { files: 0, folders: 0 };

export const notifyLibrary = (name = 'librarychange') => document.dispatchEvent(new Event(name));

export const getStatus = () => status;
export const isScanning = () => scanning;
export const getProgress = () => progress;
export const getFileCount = () => all.length;
export const getFiles = () => all;
export const getBooks = () =>
  groupBooks(all)
    .filter((b) => !isHidden(b.id))
    .sort((a, b) => b.modified - a.modified);

// Поднимает сохранённый список для выбранной папки (при запуске и после выбора папки).
export function restoreLibrary() {
  const folder = getFolder();
  const saved = folder && loadLibrary(folder.uri);
  all = saved ?? [];
  status = !folder ? 'nofolder' : saved ? 'ok' : 'unscanned';
  notifyLibrary();
}

// Меняет список без обхода (добавили, удалили, переименовали файл) и сохраняет его.
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

// Итог обхода: новый список (или null — прежний остаётся) либо ошибка.
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
