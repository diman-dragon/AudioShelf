import { Capacitor, registerPlugin } from '@capacitor/core';

// Всё, что приложение помнит, лежит в базе данных телефона (SQLite: android/.../AppDb.java), по строке на запись:
// (хранилище, ключ) -> текст JSON. Копия держится в памяти, поэтому чтение мгновенное; изменения уходят в базу пачками.
// В браузере (проверка без телефона) база заменена localStorage.
const Db = registerPlugin('Db');
const native = Capacitor.isNativePlatform();
const KV = 'kv'; // одиночные настройки: ключ -> строка
const ROW = 'row\u0001'; // строки в localStorage (только браузер)
const FLUSH_MS = 250;

const mem = new Map(); // хранилище -> Map(ключ -> строка)
let queue = [];
let timer = 0;
let busy = false;

const bucket = (s) => {
  if (!mem.has(s)) mem.set(s, new Map());
  return mem.get(s);
};

const ls = (fn) => {
  try {
    return fn(localStorage);
  } catch {
    return null;
  }
};

async function flush() {
  clearTimeout(timer);
  timer = 0;
  if (busy || !queue.length) return;
  const ops = queue;
  queue = [];
  if (!native) {
    ops.forEach(([o, s, k, v]) =>
      ls((st) => {
        const name = s === KV ? k : `${ROW}${s}\u0001${k}`;
        if (o === 'p') st.setItem(name, v);
        else if (o === 'd') st.removeItem(name);
        else Object.keys(st).filter((n) => n.startsWith(`${ROW}${s}\u0001`)).forEach((n) => st.removeItem(n));
      })
    );
    return;
  }
  busy = true;
  try {
    await Db.apply({ ops });
  } catch {
    queue = [...ops, ...queue]; // не записалось: повторим со следующей пачкой
  }
  busy = false;
  if (queue.length) timer = setTimeout(flush, 1000);
}

const push = (op) => {
  queue.push(op);
  if (!timer && !busy) timer = setTimeout(flush, FLUSH_MS);
};

export const flushStorage = flush;

// Загружает базу в память и переносит данные прежних версий (localStorage) в базу. Вызывается один раз до запуска приложения.
export async function initStorage() {
  if (native) {
    try {
      const { rows } = await Db.all();
      rows.forEach(([s, k, v]) => bucket(s).set(k, v));
    } catch {
      /* база недоступна: работаем из памяти */
    }
  }
  ls((st) => {
    Object.keys(st).forEach((name) => {
      const value = st.getItem(name);
      if (name.startsWith(ROW)) {
        const [, s, k] = name.split('\u0001');
        if (!native) bucket(s).set(k, value);
      } else if (name.startsWith('audioshelf.')) {
        if (!bucket(KV).has(name)) {
          bucket(KV).set(name, value);
          if (native) push(['p', KV, name, value]);
        }
        if (native) st.removeItem(name); // перенесено в базу
      }
    });
  });
  if (native) {
    await flush();
  }
  const hide = () => document.visibilityState === 'hidden' && flush();
  document.addEventListener('visibilitychange', hide);
  window.addEventListener('pagehide', flush);
}

export const readStored = (key) => bucket(KV).get(key) ?? null;

export function writeStored(key, value) {
  bucket(KV).set(key, String(value));
  push(['p', KV, key, String(value)]);
}

export function removeStored(key) {
  bucket(KV).delete(key);
  push(['d', KV, key]);
}

// Строки одного хранилища: { ключ: строка }.
export const readRows = (store) => Object.fromEntries(bucket(store));

export function putRow(store, key, value) {
  bucket(store).set(key, value);
  push(['p', store, key, value]);
}

export function delRow(store, key) {
  bucket(store).delete(key);
  push(['d', store, key]);
}

export function clearRows(store) {
  bucket(store).clear();
  push(['c', store]);
}
