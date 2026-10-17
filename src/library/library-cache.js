import { delRow, putRow, readRows, readStored, writeStored } from '../core/storage.js';

// Список файлов запоминается: при запуске библиотека открывается сразу, без обхода папки.
// По строке на файл (ключ — uri, n — порядок): переименовали один файл — записалась одна строка.
// Хранится вместе с папкой, для которой собран; для другой папки кэш не подходит.
const STORE = 'lib';
const TREE = 'audioshelf.libtree';

export function loadLibrary(tree) {
  if (readStored(TREE) !== tree) return null;
  const files = [];
  Object.values(readRows(STORE)).forEach((json) => {
    try {
      files.push(JSON.parse(json));
    } catch {
      /* битая строка пропускается */
    }
  });
  return files.sort((a, b) => a.n - b.n).map(({ n: _n, ...file }) => file);
}

export function saveLibrary(tree, files) {
  writeStored(TREE, tree);
  const keep = new Set(files.map((f) => f.uri));
  Object.keys(readRows(STORE)).forEach((uri) => !keep.has(uri) && delRow(STORE, uri));
  files.forEach((f, n) => putRow(STORE, f.uri, JSON.stringify({ ...f, n }))); // одинаковые строки не пишутся
}
