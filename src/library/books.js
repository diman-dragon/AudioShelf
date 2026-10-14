import { splitName } from './format.js';

const byName = (a, b) => a.name.localeCompare(b.name, undefined, { numeric: true });

// Один файл = одна запись (альбом-трек или книга одним файлом).
const single = (file) => ({
  id: file.uri,
  folder: false,
  name: splitName(file.name)[0],
  files: [file],
  dirUri: file.dirUri,
  size: file.size,
  modified: file.modified,
});

// Папка с несколькими файлами = одна книга; файлы — её главы по порядку имён.
const folderBook = (dir, files) => ({
  id: `dir:${dir}`,
  folder: true,
  name: dir.split('/').pop(),
  files: files.sort(byName),
  dirUri: files[0].dirUri,
  size: files.reduce((sum, f) => sum + f.size, 0),
  modified: Math.max(...files.map((f) => f.modified)),
});

// Список файлов папки -> список книг. Файлы прямо в выбранной папке остаются поодиночке.
export function groupBooks(files) {
  const folders = new Map();
  const books = [];
  files.forEach((file) => {
    if (!file.dir) return books.push(single(file));
    if (!folders.has(file.dir)) folders.set(file.dir, []);
    folders.get(file.dir).push(file);
  });
  folders.forEach((list, dir) => books.push(list.length === 1 ? single(list[0]) : folderBook(dir, list)));
  return books;
}
