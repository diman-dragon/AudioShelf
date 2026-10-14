import { readStored, writeStored } from '../core/storage.js';
import { MODES } from './modes.js';

const key = (kind) => `audioshelf.mode.${kind}`;

// Режим запоминаем отдельно для книг и для альбомов.
export function getMode(kind) {
  const saved = readStored(key(kind));
  return MODES[kind].includes(saved) ? saved : MODES[kind][0];
}

export function nextMode(kind) {
  const list = MODES[kind];
  const next = list[(list.indexOf(getMode(kind)) + 1) % list.length];
  writeStored(key(kind), next);
  return next;
}
