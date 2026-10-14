// Что считать одной сущностью (книгой или альбомом), а что — несколькими. Правила по именам и размерам.
const DISC = /^(cd|disc|disk|диск|part|часть|том|vol(?:ume)?\.?)[\s_-]*\d+$/i; // «CD1», «Диск 2», «Часть 3»
const STANDALONE = /\.m4b$/i; // .m4b — это целая книга в одном файле
const BIG = 40 * 1048576; // средний файл крупнее — это уже целые книги, а не главы
const NUMBERED = 0.8; // доля файлов с номером в имени: «01», «Глава 2»

// Папки-диски одной книги («Книга/CD1», «Книга/CD2») склеиваем в «Книга».
export function groupDir(dir) {
  const parts = dir.split('/');
  return parts.length > 1 && DISC.test(parts[parts.length - 1]) ? parts.slice(0, -1).join('/') : dir;
}

// Файлы одной папки -> группы: из каждой группы получится одна сущность.
// Каждый .m4b — отдельная книга. Не пронумерованные крупные файлы — тоже отдельные книги, а не главы одной.
export function splitGroup(files) {
  const whole = files.filter((f) => STANDALONE.test(f.name));
  const rest = files.filter((f) => !STANDALONE.test(f.name));
  const numbered = rest.filter((f) => /\d/.test(f.name.replace(/\.[^.]+$/, ''))).length;
  const avg = rest.reduce((sum, f) => sum + f.size, 0) / (rest.length || 1);
  const separate = rest.length > 1 && avg >= BIG && numbered / rest.length < NUMBERED;
  return [...whole.map((f) => [f]), ...(separate ? rest.map((f) => [f]) : rest.length ? [rest] : [])];
}
