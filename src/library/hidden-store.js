import { createMapStore } from '../core/map-store.js';

const store = createMapStore('audioshelf.hidden');

export const isHidden = (uri) => Boolean(store.get(uri));
export const hide = (uri) => store.set(uri, 1);
export const hiddenCount = () => Object.keys(store.all()).length;
export const restoreHidden = store.clear;
export const moveHidden = store.move;
export const forgetHidden = store.del;
