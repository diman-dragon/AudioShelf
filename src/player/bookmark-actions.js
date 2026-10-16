import { Player } from '../folder/plugins.js';
import { addMark } from './bookmark-store.js';
import { load } from './player-load.js';
import { applyStatus, getStatus, getTrack, isLoaded } from './player-state.js';

// Закладка в том месте, где сейчас играет. Пока плеер не загружен, места нет — ничего не делаем.
export function markHere() {
  const book = getTrack();
  if (!book || !isLoaded()) return false;
  const { index, position } = getStatus();
  addMark(book.id, index, position, book.expanded);
  document.dispatchEvent(new Event('markschange'));
  return true;
}

// Перейти к закладке и играть.
export async function jumpToMark({ i, p }) {
  if (!isLoaded()) return load(i, p, true);
  if (getStatus().index !== i) applyStatus(await Player.skipTo({ index: i }));
  applyStatus(await Player.seek({ position: p }));
  if (!getStatus().playing) applyStatus(await Player.play());
}
