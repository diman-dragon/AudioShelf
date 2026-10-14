import { Folder } from '../folder/plugins.js';
import { clearFolder, getFolder } from '../folder/folder-store.js';
import { dropTrack } from '../player/track-sync.js';
import { getTrack } from '../player/player-state.js';

// Убрать общую папку из отслеживания: вернуть системе доступ, очистить библиотеку и остановить плеер.
// Файлы на диске остаются; теги и прогресс сохраняются.
export async function forgetFolder() {
  const folder = getFolder();
  if (!folder) return;
  await Folder.release({ uri: folder.uri }).catch(() => {});
  if (getTrack()) await dropTrack(getTrack().id);
  clearFolder();
}
