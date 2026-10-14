import { readStored, writeStored } from '../core/storage.js';

// uri файла -> поправка громкости, дБ (результат нормализации). Держим в памяти, пишем пачками.
const KEY = 'audioshelf.gain';
const DELAY = 800;
let cache = null;
let timer = 0;

function load() {
  if (cache) return cache;
  try {
    cache = JSON.parse(readStored(KEY)) ?? {};
  } catch {
    cache = {};
  }
  return cache;
}

export const flushGains = () => {
  clearTimeout(timer);
  timer = 0;
  if (cache) writeStored(KEY, JSON.stringify(cache));
};

export const getGain = (uri) => load()[uri] ?? 0;
export const hasGain = (uri) => uri in load();

export function saveGain(uri, db) {
  load()[uri] = db;
  if (!timer) timer = setTimeout(flushGains, DELAY);
}

export function moveGain(from, to) {
  const map = load();
  if (from === to || !(from in map)) return;
  map[to] = map[from];
  delete map[from];
  flushGains();
}
