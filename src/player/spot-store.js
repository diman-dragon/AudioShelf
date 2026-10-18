import { createMapStore } from '../core/map-store.js';

const store = createMapStore('pos');

export const getSpot = (id) => store.get(id);
export const setSpot = (id, i, p, t = Date.now()) => store.set(id, { i, p: Math.floor(p), t });
export const moveSpot = store.move;
export const forgetSpot = store.del;
