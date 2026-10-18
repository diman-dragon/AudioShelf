import { Player } from '../folder/plugins.js';
import { getTrack, setTrack } from './player-state.js';
import { refreshChapters } from './refresh-chapters.js';
import { saveLast } from './last-book.js';

export function retarget(oldId, file) {
  const track = getTrack();
  if (track?.id !== oldId) return;
  const next = { ...(track.base ?? track), id: file.uri, files: [file] };
  saveLast(next);
  setTrack(next);
  refreshChapters(next);
}

export async function dropTrack(id) {
  if (getTrack()?.id !== id) return;
  await Player.stop().catch(() => {});
  saveLast(null);
  setTrack(null);
}
