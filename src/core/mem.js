// Копия базы в памяти: хранилище -> Map(ключ -> строка). Чтение всегда отсюда.
const mem = new Map();

export const bucket = (store) => {
  if (!mem.has(store)) mem.set(store, new Map());
  return mem.get(store);
};
