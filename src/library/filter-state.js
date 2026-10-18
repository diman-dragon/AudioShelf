import { readStored, writeStored } from '../core/storage.js';

export const KINDS = ['all', 'book', 'album'];
export const SORTS = ['date', 'name', 'played'];
const KEYS = { kind: 'audioshelf.libkind', sort: 'audioshelf.libsort' };

const read = (key, list) => {
  const saved = readStored(key);
  return list.includes(saved) ? saved : list[0];
};

let state = { kind: read(KEYS.kind, KINDS), sort: read(KEYS.sort, SORTS), query: '' };

export const getFilter = () => state;

export function setFilter(patch) {
  state = { ...state, ...patch };
  Object.keys(KEYS).forEach((name) => name in patch && writeStored(KEYS[name], state[name]));
  document.dispatchEvent(new Event('filterchange'));
}
