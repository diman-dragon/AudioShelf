import { Folder } from '../folder/plugins.js';
import { clearFolder, getFolder } from '../folder/folder-store.js';
import { dropTrack } from '../player/track-sync.js';
import { getTrack } from '../player/player-state.js';

export async function forgetFolder() {
  const folder = getFolder();
  if (!folder) return;
  await Folder.release({ uri: folder.uri }).catch(() => {});
  if (getTrack()) await dropTrack(getTrack().id);
  clearFolder();
}
