import { createMapStore } from '../core/map-store.js';

// uri файла -> поправка громкости, дБ (результат нормализации). Хранится в базе по строке на файл.
const store = createMapStore('audioshelf.gain');

export const getGain = (uri) => store.get(uri) ?? 0;
export const hasGain = (uri) => uri in store.all();
export const saveGain = (uri, db) => store.set(uri, db);
export const moveGain = store.move;
