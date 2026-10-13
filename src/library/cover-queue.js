import { Cover } from '../folder/plugins.js';

// Обложки из тегов файла: по одной за раз + кэш в памяти.
const cache = new Map(); // ключ -> dataURL, '' = у файла нет обложки
const queue = [];
let busy = false;

export function loadCover(file, img) {
  const key = `${file.uri}|${file.modified}|${file.size}`;
  if (cache.has(key)) {
    if (cache.get(key)) img.src = cache.get(key);
    return;
  }
  queue.push({ key, uri: file.uri, img });
  run();
}

async function run() {
  if (busy) return;
  busy = true;
  while (queue.length) {
    const job = queue.shift();
    if (!job.img.isConnected) continue;
    try {
      const result = await Cover.read({ uri: job.uri, width: 360 });
      cache.set(job.key, result.data);
      job.img.src = result.data;
    } catch {
      cache.set(job.key, '');
    }
  }
  busy = false;
}
