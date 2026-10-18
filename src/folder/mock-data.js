const DAY = 86400000;
const en = navigator.language.toLowerCase().startsWith('en');

const chapters = (dir, base, count, ago) =>
  Array.from({ length: count }, (_, i) => ({
    uri: `mock://${dir}/${i + 1}`,
    name: `${base} ${i + 1}.mp3`,
    size: 40000000,
    modified: Date.now() - ago,
    dir,
    dirUri: `mock://dir/${dir}`,
  }));

const single = (id, name, size, ago) => ({ uri: `mock://${id}`, name, size, modified: Date.now() - ago, dir: '', dirUri: 'mock://dir/root' });

export const demoFiles = [
  ...chapters(en ? 'Tolstoy/War and Peace' : 'Толстой/Война и мир', en ? 'Chapter' : 'Глава', 4, DAY),
  ...chapters(en ? 'Chekhov/Short Stories' : 'Чехов/Рассказы', en ? 'Story' : 'Рассказ', 3, 3 * DAY),
  single('a', en ? 'Beethoven - Moonlight Sonata.mp3' : 'Бетховен - Лунная соната.mp3', 9200000, 3600000),
  single('b', en ? 'Mozart - Eine kleine Nachtmusik.mp3' : 'Моцарт - Маленькая ночная серенада.mp3', 8100000, 7200000),
  single('c', en ? 'Grieg - Morning Mood.mp3' : 'Григ - Утро.mp3', 4300000, 2 * DAY),
  single('d', en ? 'Space podcast.m4a' : 'Подкаст про космос.m4a', 5300000, 10 * DAY),
];
