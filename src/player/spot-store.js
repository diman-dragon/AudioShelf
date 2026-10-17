import { createMapStore } from '../core/map-store.js';

// Где остановились: id книги -> { i: номер файла (главы), p: позиция в нём мс, t: когда слушали }.
// Это самая частая запись (каждые ~2 с при игре), поэтому она одна и маленькая: больше в эту строку ничего не кладём.
const store = createMapStore('pos');

export const getSpot = (id) => store.get(id);
export const setSpot = (id, i, p, t = Date.now()) => store.set(id, { i, p: Math.floor(p), t });
export const moveSpot = store.move;
export const forgetSpot = store.del;
