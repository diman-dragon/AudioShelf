// Заглушка для работы в браузере (npm run dev): файлы живут только в памяти.
const DAY = 86400000;

let files = [
  { uri: 'mock://1', name: 'Мастер и Маргарита, часть 1.m4b', size: 184000000, modified: Date.now() - DAY },
  { uri: 'mock://2', name: 'Pink Floyd - Time.mp3', size: 9200000, modified: Date.now() - 3600000 },
  { uri: 'mock://3', name: 'Интервью.m4a', size: 5300000, modified: Date.now() - 10 * DAY },
];

const listeners = new Set();
let cancelled = false;

export const mock = {
  async pickFolder() {
    return { uri: 'content://com.android.externalstorage.documents/tree/primary%3AAudiobooks%2FBooks', name: 'Books' };
  },
  // Сканирование с «прогрессом»: подпапки обходятся по очереди, как на телефоне.
  async scan() {
    cancelled = false;
    const total = files.length;
    for (let folders = 1; folders <= 4 && !cancelled; folders++) {
      await new Promise((r) => setTimeout(r, 350));
      listeners.forEach((fn) => fn({ files: Math.round((total * folders) / 4), folders }));
    }
    return { files: files.slice(), cancelled };
  },
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
    files.push({ uri: `mock://${Date.now()}`, name: 'Новый файл.mp3', size: 3000000, modified: Date.now() });
    return { added: 1 };
  },
  async remove({ uri }) {
    files = files.filter((f) => f.uri !== uri);
  },
  async rename({ uri, name }) {
    files.forEach((f) => {
      if (f.uri === uri) f.name = name;
    });
    return { uri };
  },
  async open() {
    /* в браузере открывать нечем */
  },
};
