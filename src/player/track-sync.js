import { Player } from '../folder/plugins.js';
import { getTrack, setTrack } from './player-state.js';
import { saveLast } from './progress-store.js';

// Файл переименован и получил новый uri.
export function retarget(from, to, name) {
  const track = getTrack();
  if (track?.uri !== from) return;
  const next = { ...track, uri: to, name };
  saveLast(next);
  setTrack(next);
}

// Файл удалён с диска: если он играл — останавливаем и очищаем плеер.
export async function dropTrack(uri) {
  if (getTrack()?.uri !== uri) return;
  await Player.stop().catch(() => {});
  saveLast(null);
  setTrack(null);
}
