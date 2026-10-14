import { readStored, writeStored } from '../core/storage.js';

const KEY = 'audioshelf.library2';

// Список файлов запоминается: при запуске библиотека открывается сразу, без обхода папки.
// Хранится вместе с папкой, для которой собран; для другой папки кэш не подходит.
export function loadLibrary(tree) {
  try {
    const saved = JSON.parse(readStored(KEY));
    return saved?.tree === tree ? saved.files : null;
  } catch {
    return null;
  }
}

export const saveLibrary = (tree, files) => writeStored(KEY, JSON.stringify({ tree, files }));
