import { readStored, writeStored } from './storage.js';

// Словарь «ключ -> значение» в localStorage (например, uri файла -> данные о нём).
export function createMapStore(key) {
  const read = () => {
    try {
      return JSON.parse(readStored(key)) ?? {};
    } catch {
      return {};
    }
  };
  const write = (map) => writeStored(key, JSON.stringify(map));

  return {
    all: read,
    get: (id) => read()[id],
    set(id, value) {
      write({ ...read(), [id]: value });
    },
    del(id) {
      const { [id]: _gone, ...rest } = read();
      write(rest);
    },
    // Файл переименован и получил новый uri — переносим запись.
    move(from, to) {
      const map = read();
      if (from === to || !(from in map)) return;
      const { [from]: value, ...rest } = map;
      write({ ...rest, [to]: value });
    },
    clear: () => write({}),
  };
}
