import { clearRows, delRow, putRow, readRows } from './storage.js';

// Словарь «ключ -> значение» в базе данных, по строке на запись (id книги -> закладки, uri файла -> данные о нём).
// Разобранные значения держим в памяти; запись меняет одну строку, и только если значение изменилось.
export function createMapStore(store) {
  let map = null;

  const load = () => {
    if (map) return map;
    map = {};
    Object.entries(readRows(store)).forEach(([k, v]) => {
      try {
        map[k] = JSON.parse(v);
      } catch {
        /* битая строка пропускается */
      }
    });
    return map;
  };

  const set = (id, value) => {
    load()[id] = value;
    putRow(store, id, JSON.stringify(value));
  };

  const del = (id) => {
    delete load()[id];
    delRow(store, id);
  };

  return {
    all: load,
    get: (id) => load()[id],
    set,
    del,
    // Файл переименован и получил новый uri — переносим запись.
    move(from, to) {
      const m = load();
      if (from === to || !(from in m)) return;
      const value = m[from];
      del(from);
      set(to, value);
    },
    clear() {
      map = {};
      clearRows(store);
    },
  };
}
