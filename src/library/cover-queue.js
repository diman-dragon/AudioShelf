import { Capacitor } from '@capacitor/core';
import { Cover } from '../folder/plugins.js';

const cache = new Map();
const queue = [];
let busy = false;

export function loadCover(file, img) {
  const key = `${file.uri}|${file.modified}|${file.size}`;
  if (cache.has(key)) {
    if (cache.get(key)) img.src = cache.get(key);
    return;
  }
  queue.push({ key, uri: file.uri, sig: `${file.modified}|${file.size}`, img });
  run();
}

async function inline(job) {
  job.img.onerror = null;
  try {
    const result = await Cover.read({ uri: job.uri, sig: job.sig, width: 360, inline: true });
    cache.set(job.key, result.data);
    job.img.src = result.data;
  } catch {
    cache.set(job.key, '');
  }
}

async function run() {
  if (busy) return;
  busy = true;
  while (queue.length) {
    const job = queue.shift();
    if (!job.img.isConnected) continue;
    try {
      const result = await Cover.read({ uri: job.uri, sig: job.sig, width: 360 });
      const src = result.path ? Capacitor.convertFileSrc(result.path) : result.data;
      cache.set(job.key, src);
      if (result.path) job.img.onerror = () => inline(job);
      job.img.src = src;
    } catch {
      cache.set(job.key, '');
    }
  }
  busy = false;
}
