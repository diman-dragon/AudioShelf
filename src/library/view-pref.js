import { readStored, writeStored } from '../core/storage.js';

const KEY = 'audioshelf.libview';

let view = readStored(KEY) === 'cards' ? 'cards' : 'list';

export const getView = () => view;

export function setView(next) {
  view = next;
  writeStored(KEY, next);
  document.dispatchEvent(new Event('viewchange'));
}
