import { Db, isNative } from './db.js';
import { browserLoad } from './browser-db.js';
import { bucket } from './mem.js';
import { flushQueue, queueOp } from './write-queue.js';

// Всё, что приложение помнит, лежит в базе данных (по строке на запись: хранилище, ключ -> текст). Читается один раз при запуске
// в память, дальше чтение из памяти; запись — только изменившихся строк (write-queue.js).
const KV = 'kv'; // одиночные настройки

export async function initStorage() {
  let rows = [];
  try {
    rows = isNative ? (await Db.all()).rows : browserLoad();
  } catch {
    /* базы нет: работаем из памяти */
  }
  rows.forEach(([s, k, v]) => bucket(s).set(k, v));
  document.addEventListener('visibilitychange', () => document.visibilityState === 'hidden' && flushQueue());
  window.addEventListener('pagehide', flushQueue);
}

// Строки одного хранилища: { ключ: строка }.
export const readRows = (store) => Object.fromEntries(bucket(store));
export const readRow = (store, key) => bucket(store).get(key) ?? null;

// Одинаковое значение не пишем.
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

// Одиночные настройки (строковые).
export const readStored = (key) => readRow(KV, key);
export const writeStored = (key, value) => putRow(KV, key, String(value));
export const removeStored = (key) => delRow(KV, key);
export const flushStorage = flushQueue;
