import { demoFiles } from './mock-data.js';

const listeners = new Set();
let cancelled = false;

let files = demoFiles.slice();

export const mock = {
  async pickFolder() {
    return { uri: 'content://com.android.externalstorage.documents/tree/primary%3AAudiobooks%2FBooks', name: 'Books' };
  },
  async scan() {
    cancelled = false;
    const total = files.length;
    for (let folders = 1; folders <= 4 && !cancelled; folders++) {
      await new Promise((r) => setTimeout(r, 350));
      listeners.forEach((fn) => fn({ files: Math.round((total * folders) / 4), folders }));
    }
    return { files: files.slice(), cancelled };
  },
  async release() {},
  async cancelScan() {
    cancelled = true;
  },
  async addListener(_name, fn) {
    listeners.add(fn);
    return { remove: async () => listeners.delete(fn) };
  },
  async read() {
    throw new Error('no cover in browser');
  },
  async add() {
    const file = { uri: `mock://${Date.now()}`, name: 'Новый файл.mp3', size: 3000000, modified: Date.now(), dir: '', dirUri: 'mock://dir/root' };
    files.push(file);
    return { files: [file] };
  },
  async remove({ uri }) {
    files = files.filter((f) => f.uri !== uri && f.dirUri !== uri);
  },
  async rename({ uri, name }) {
    files.forEach((f) => {
      if (f.uri === uri) f.name = name;
    });
    return { uri };
  },
  async open() {
  },
};
