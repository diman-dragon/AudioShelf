import { Player } from '../folder/plugins.js';
import { getTrack, setTrack } from './player-state.js';
import { refreshChapters } from './refresh-chapters.js';
import { saveLast } from './last-book.js';

// Одиночный файл переименован и получил новый uri (он же id записи).
export function retarget(oldId, file) {
  const track = getTrack();
  if (track?.id !== oldId) return;
  const next = { ...(track.base ?? track), id: file.uri, files: [file] }; // без глав внутри файла: у нового uri их заново узнаем
  saveLast(next);
  setTrack(next);
  refreshChapters(next);
}

// Книга удалена с диска: если она играла — останавливаем и очищаем плеер.
export async function dropTrack(id) {
  if (getTrack()?.id !== id) return;
  await Player.stop().catch(() => {});
  saveLast(null);
  setTrack(null);
}
