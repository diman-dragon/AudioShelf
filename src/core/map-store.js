import { clearRows, delRow, putRow, readRows, readStored, removeStored } from './storage.js';

// Словарь «ключ -> значение» в базе данных, по строке на запись (например, id книги -> прогресс, uri файла -> данные о нём).
// Разобранные значения держим в памяти; запись меняет одну строку, а не весь словарь.
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
    // Прежние версии хранили весь словарь одной записью: разбираем её на строки (один раз).
    const legacy = readStored(store);
    if (legacy) {
      try {
        Object.entries(JSON.parse(legacy) ?? {}).forEach(([k, v]) => {
          if (k in map) return;
          map[k] = v;
          putRow(store, k, JSON.stringify(v));
        });
      } catch {
        /* ignore */
      }
      removeStored(store);
    }
    return map;
  };

  const set = (id, value) => {
    load()[id] = value;
    putRow(store, id, JSON.stringify(value));
  };

  return {
    all: load,
    get: (id) => load()[id],
    set,
    del(id) {
      delete load()[id];
      delRow(store, id);
    },
    // Файл переименован и получил новый uri — переносим запись.
    move(from, to) {
      const m = load();
      if (from === to || !(from in m)) return;
      const value = m[from];
      this.del(from);
      set(to, value);
    },
    clear() {
      map = {};
      clearRows(store);
    },
  };
}
