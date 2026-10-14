// Демо-файлы для работы в браузере: книга из глав в подпапке и пара одиночных файлов.
const DAY = 86400000;

const BOOK = 'Булгаков/Мастер и Маргарита';
const chapter = (n, ago) => ({
  uri: `mock://book/${n}`,
  name: `Глава ${n}.mp3`,
  size: 40000000,
  modified: Date.now() - ago,
  dir: BOOK,
  dirUri: 'mock://dir/book',
});

export const demoFiles = [
  chapter(1, DAY),
  chapter(2, DAY),
  chapter(3, DAY),
  chapter(10, DAY),
  { uri: 'mock://2', name: 'Pink Floyd - Time.mp3', size: 9200000, modified: Date.now() - 3600000, dir: '', dirUri: 'mock://dir/root' },
  { uri: 'mock://3', name: 'Интервью.m4a', size: 5300000, modified: Date.now() - 10 * DAY, dir: '', dirUri: 'mock://dir/root' },
];
