// Заглушка для работы в браузере (npm run dev): файлы живут только в памяти.
const DAY = 86400000;

let files = [
  { uri: 'mock://1', name: 'Мастер и Маргарита, часть 1.m4b', size: 184000000, modified: Date.now() - DAY },
  { uri: 'mock://2', name: 'Pink Floyd - Time.mp3', size: 9200000, modified: Date.now() - 3600000 },
  { uri: 'mock://3', name: 'Интервью.m4a', size: 5300000, modified: Date.now() - 10 * DAY },
];

export const mock = {
  async pickFolder() {
    return { uri: 'mock://folder', name: 'Demo' };
  },
  async list() {
    return { files: files.slice() };
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
    alert('open (browser mock)');
  },
};
