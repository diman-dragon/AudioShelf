import { createMapStore } from '../core/map-store.js';
import { Chapters } from '../folder/plugins.js';

const store = createMapStore('audioshelf.chapters');
const SCAN = /\.(mka|m4b|m4a|mp4|mp3)$/i;
const MAX_FILES = 20;

const sig = (f) => `${f.size}|${f.modified}`;
const wants = (book, f) => book.files.length <= MAX_FILES && SCAN.test(f.name);

export function chaptersOf(f) {
  const r = store.get(f.uri);
  return r && r.sig === sig(f) ? r.list : null;
}

export async function scanBook(book) {
  const todo = book.files.filter((f) => wants(book, f) && chaptersOf(f) === null);
  if (!todo.length) return false;
  try {
    const { items } = await Chapters.read({ uris: todo.map((f) => f.uri) });
    const byUri = new Map(items.map((i) => [i.uri, i.chapters]));
    todo.forEach((f) => {
      const list = (byUri.get(f.uri) ?? []).map((c) => ({ t: c.title, s: c.start, e: c.end }));
      store.set(f.uri, { sig: sig(f), list });
    });
    return todo.some((f) => chaptersOf(f)?.length >= 2);
  } catch {
    return false;
  }
}

export const forgetChapters = store.del;
