import { FileOps } from '../folder/plugins.js';
import { getFolder } from '../folder/folder-store.js';
import { navigate } from '../app/router.js';
import { playBook } from '../player/player-actions.js';
import { forgetMarks } from '../player/bookmark-store.js';
import { forgetChapters } from '../player/chapter-store.js';
import { forgetDurations } from '../player/durations.js';
import { clearTrackEq } from '../sound/eq-store.js';
import { forgetProgress } from '../player/progress-store.js';
import { dropTrack } from '../player/track-sync.js';
import { fail } from './fail.js';
import { forgetHidden, hide } from './hidden-store.js';
import { forgetMeta } from './meta-store.js';
import { notifyLibrary, patchFiles } from './library-data.js';

export function playFromLibrary(book) {
  playBook(book);
  navigate('player');
}

export const openExternal = (book) => FileOps.open({ uri: book.files[0].uri }).catch(fail);

export function hideBook(book) {
  hide(book.id);
  notifyLibrary();
}

export function removeBook(book) {
  const targets = book.folder && book.ownsDir ? book.dirUris : book.files.map((f) => f.uri);
  Promise.all(targets.map((uri) => FileOps.remove({ uri })))
    .then(() => {
      forgetMeta(book.id);
      forgetProgress(book.id);
      forgetMarks(book.id);
      clearTrackEq(book.id);
      forgetDurations(book.id);
      book.files.forEach((f) => forgetChapters(f.uri));
      forgetHidden(book.id);
      const gone = new Set(book.files.map((f) => f.uri));
      patchFiles((list) => list.filter((f) => !gone.has(f.uri)));
      return dropTrack(book.id);
    })
    .catch(fail);
}

export const addFiles = () =>
  FileOps.add({ tree: getFolder().uri })
    .then((result) => patchFiles((list) => [...list, ...result.files]))
    .catch(fail);
