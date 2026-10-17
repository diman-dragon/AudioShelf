import { readStored, writeStored } from '../core/storage.js';

// Книга, открытая в плеере последней, и скорость воспроизведения: по одной настройке, каждая пишется отдельно.
const LAST = 'audioshelf.last2';
const SPEED = 'audioshelf.speed';

const parse = (key, fallback) => {
  try {
    return JSON.parse(readStored(key)) ?? fallback;
  } catch {
    return fallback;
  }
};

export const loadLast = () => parse(LAST, null);
export const saveLast = (book) => writeStored(LAST, JSON.stringify(book));
export const loadSpeed = () => parse(SPEED, 1);
export const saveSpeed = (speed) => writeStored(SPEED, JSON.stringify(speed));
