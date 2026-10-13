import { FileOps } from '../folder/plugins.js';
import { retarget } from '../player/track-sync.js';
import { moveProgress } from '../player/progress-store.js';
import { fail } from './fail.js';
import { splitName } from './format.js';
import { moveHidden } from './hidden-store.js';
import { refresh } from './library-data.js';
import { moveMeta, saveMeta } from './meta-store.js';

// Теги и имя файла из окна «Изменить». Переименование меняет uri — переносим всё, что к нему привязано.
export async function saveFile(file, meta, base) {
  const [oldBase, ext] = splitName(file.name);
  const clean = base.replace(/[\\/:*?"<>|]/g, '').trim();
  let uri = file.uri;
  try {
    if (clean && clean !== oldBase) {
      ({ uri } = await FileOps.rename({ uri: file.uri, name: clean + ext }));
      moveMeta(file.uri, uri);
      moveProgress(file.uri, uri);
      moveHidden(file.uri, uri);
      retarget(file.uri, uri, clean + ext);
    }
    saveMeta(uri, meta);
  } catch (e) {
    fail(e);
  }
  refresh();
}

