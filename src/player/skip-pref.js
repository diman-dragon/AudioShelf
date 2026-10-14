import { readStored, writeStored } from '../core/storage.js';

export const SKIP_OPTIONS = [5, 10, 15, 30, 60]; // секунды
const KEYS = { back: 'audioshelf.skipBack', forward: 'audioshelf.skipForward' };
const DEFAULTS = { back: 15, forward: 30 };

const read = (kind) => {
  const n = Number(readStored(KEYS[kind]));
  return SKIP_OPTIONS.includes(n) ? n : DEFAULTS[kind];
};

// На сколько секунд перематывают кнопки «назад» и «вперёд».
export const getSkip = () => ({ back: read('back'), forward: read('forward') });

export function setSkip(kind, seconds) {
  writeStored(KEYS[kind], String(seconds));
  document.dispatchEvent(new Event('skipchange'));
}
