import { FileOps } from '../folder/plugins.js';
import { getFolder } from '../folder/folder-store.js';
import { fail } from './fail.js';
import { navigate } from '../app/router.js';
import { playFile } from '../player/player-actions.js';
import { dropTrack } from '../player/track-sync.js';
import { forgetProgress } from '../player/progress-store.js';
import { forgetHidden, hide } from './hidden-store.js';
import { forgetMeta } from './meta-store.js';
import { refresh } from './library-data.js';

const run = (job) => job.then(refresh).catch(fail);

// Нажатие на файл: играть во встроенном плеере и показать его.
export function playFromLibrary(f) {
  playFile(f);
  navigate('player');
}

export const openExternal = (f) => FileOps.open({ uri: f.uri }).catch(fail);

export function hideFile(f) {
  hide(f.uri);
  refresh();
}

export function removeFile(f) {
  run(
    FileOps.remove({ uri: f.uri }).then(() => {
      forgetMeta(f.uri);
      forgetProgress(f.uri);
      forgetHidden(f.uri);
      return dropTrack(f.uri);
    }),
  );
}

export const addFiles = () => run(FileOps.add({ tree: getFolder().uri }));
