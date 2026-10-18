import { FileOps } from '../folder/plugins.js';
import { retarget } from '../player/track-sync.js';
import { moveMarks } from '../player/bookmark-store.js';
import { moveDurations } from '../player/durations.js';
import { moveGain } from '../sound/gain-store.js';
import { moveTrackEq } from '../sound/eq-store.js';
import { moveProgress } from '../player/progress-store.js';
import { fail } from './fail.js';
import { splitName } from './format.js';
import { moveHidden } from './hidden-store.js';
import { notifyLibrary, patchFiles } from './library-data.js';
import { moveMeta, saveMeta } from './meta-store.js';

export async function saveFile(book, meta, base) {
  const file = book.files[0];
  const [oldBase, ext] = splitName(file.name);
  const clean = base.replace(/[\\/:*?"<>|]/g, '').trim();
  let id = book.id;
  try {
    if (!book.folder && clean && clean !== oldBase) {
      const { uri } = await FileOps.rename({ uri: file.uri, name: clean + ext });
      moveMeta(id, uri);
      moveProgress(id, uri);
      moveMarks(id, uri);
      moveTrackEq(id, uri);
      moveGain(file.uri, uri);
      moveDurations(id, uri);
      moveHidden(id, uri);
      retarget(id, { ...file, uri, name: clean + ext });
      patchFiles((list) => list.map((f) => (f.uri === file.uri ? { ...f, uri, name: clean + ext } : f)));
      id = uri;
    }
    saveMeta(id, meta);
  } catch (e) {
    fail(e);
  }
  notifyLibrary();
}
