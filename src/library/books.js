import { countUnder } from './dir-count.js';
import { splitName } from './format.js';
import { groupDir, splitGroup } from './split-rules.js';

const byName = (a, b) => (a.dir + '/' + a.name).localeCompare(b.dir + '/' + b.name, undefined, { numeric: true });

const single = (file) => ({
  id: file.uri,
  folder: false,
  name: splitName(file.name)[0],
  files: [file],
  dirUri: file.dirUri,
  dirUris: [file.dirUri],
  ownsDir: false,
  size: file.size,
  modified: file.modified,
});

const folderBook = (dir, files, under) => ({
  id: `dir:${dir}`,
  folder: true,
  name: dir.split('/').pop(),
  files: files.sort(byName),
  dirUri: files[0].dirUri,
  dirUris: [...new Set(files.map((f) => f.dirUri))],
  ownsDir: under.get(dir) === files.length,
  size: files.reduce((sum, f) => sum + f.size, 0),
  modified: Math.max(...files.map((f) => f.modified)),
});

const unique = (books) => {
  const seen = new Set();
  return books.filter((b) => !seen.has(b.id) && seen.add(b.id));
};

export function groupBooks(files) {
  const under = countUnder(files);
  const folders = new Map();
  const books = [];
  files.forEach((file) => {
    if (!file.dir) return books.push(single(file));
    const key = groupDir(file.dir);
    if (!folders.has(key)) folders.set(key, []);
    folders.get(key).push(file);
  });
  folders.forEach((list, dir) =>
    splitGroup(list).forEach((part) => books.push(part.length === 1 ? single(part[0]) : folderBook(dir, part, under))),
  );
  return unique(books);
}
