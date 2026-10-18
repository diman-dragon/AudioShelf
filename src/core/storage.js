import { Db, isNative } from './db.js';
import { browserLoad } from './browser-db.js';
import { bucket } from './mem.js';
import { flushQueue, queueOp } from './write-queue.js';

const KV = 'kv';

export async function initStorage() {
  let rows = [];
  try {
    rows = isNative ? (await Db.all()).rows : browserLoad();
  } catch {
  }
  rows.forEach(([s, k, v]) => bucket(s).set(k, v));
  document.addEventListener('visibilitychange', () => document.visibilityState === 'hidden' && flushQueue());
  window.addEventListener('pagehide', flushQueue);
}

export const readRows = (store) => Object.fromEntries(bucket(store));
const readRow = (store, key) => bucket(store).get(key) ?? null;

export function putRow(store, key, value) {
  if (bucket(store).get(key) === value) return;
  bucket(store).set(key, value);
  queueOp(['p', store, key, value]);
}

export function delRow(store, key) {
  if (!bucket(store).delete(key)) return;
  queueOp(['d', store, key]);
}

export function clearRows(store) {
  bucket(store).clear();
  queueOp(['c', store]);
}

export const readStored = (key) => readRow(KV, key);
export const writeStored = (key, value) => putRow(KV, key, String(value));
export const removeStored = (key) => delRow(KV, key);
