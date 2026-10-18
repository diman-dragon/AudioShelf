import { scanBook } from './chapter-store.js';
import { getTrack, setTrack } from './player-state.js';

export async function refreshChapters(book = getTrack()?.base ?? getTrack()) {
  if (!book || !(await scanBook(book))) return;
  const track = getTrack();
  if (track && (track.base ?? track).id === book.id) setTrack(book);
}
