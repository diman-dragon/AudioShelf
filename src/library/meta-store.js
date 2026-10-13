import { createMapStore } from '../core/map-store.js';

// Теги, которые пользователь задал в приложении (сам файл не меняется).
// { kind: 'book' | 'album', title, author, narrator, year }
const store = createMapStore('audioshelf.meta');

export const getMeta = (uri) => store.get(uri) ?? {};
export const saveMeta = store.set;
export const moveMeta = store.move;
export const forgetMeta = store.del;
