import { createMapStore } from '../core/map-store.js';
import { Chapters } from '../folder/plugins.js';

// Главы, записанные внутри файла (MKA, M4B/M4A, MP3). uri -> { sig, list: [{ t: название, s: начало мс, e: конец мс или -1 }] }.
// Пустой list — глав внутри нет (спрашивать снова не нужно, пока файл не изменился).
const store = createMapStore('audioshelf.chapters');
const SCAN = /\.(mka|m4b|m4a|mp4|mp3)$/i;
const MAX_FILES = 20; // в книге из десятков файлов главы — это сами файлы

const sig = (f) => `${f.size}|${f.modified}`;
const wants = (book, f) => book.files.length <= MAX_FILES && SCAN.test(f.name);

// null — файл ещё не проверяли.
export function chaptersOf(f) {
  const r = store.get(f.uri);
  return r && r.sig === sig(f) ? r.list : null;
}

// Спрашивает нативную часть о файлах книги, которые ещё не проверяли. true — узнали что-то новое.
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
    return false; // плагин недоступен — попробуем в следующий раз
  }
}

export const moveChapters = store.move;
export const forgetChapters = store.del;
