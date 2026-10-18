import { createMapStore } from '../core/map-store.js';

const store = createMapStore('audioshelf.meta');

export const getMeta = (uri) => store.get(uri) ?? {};
export const saveMeta = store.set;
export const moveMeta = store.move;
export const forgetMeta = store.del;
