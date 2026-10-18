import { clearRows, delRow, putRow, readRows } from './storage.js';

export function createMapStore(store) {
  let map = null;

  const load = () => {
    if (map) return map;
    map = {};
    Object.entries(readRows(store)).forEach(([k, v]) => {
      try {
        map[k] = JSON.parse(v);
      } catch {
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
