import { FileOps } from '../folder/plugins.js';
import { getFolder } from '../folder/folder-store.js';
import { navigate } from '../app/router.js';
import { playBook } from '../player/player-actions.js';
import { forgetProgress } from '../player/progress-store.js';
import { dropTrack } from '../player/track-sync.js';
import { fail } from './fail.js';
import { forgetHidden, hide } from './hidden-store.js';
import { forgetMeta } from './meta-store.js';
import { notifyLibrary, patchFiles } from './library-data.js';

// Нажатие на книгу: играть во встроенном плеере и показать его.
export function playFromLibrary(book) {
  playBook(book);
  navigate('player');
}

export const openExternal = (book) => FileOps.open({ uri: book.files[0].uri }).catch(fail);

export function hideBook(book) {
  hide(book.id);
  notifyLibrary();
}

// С диска: у книги-папки удаляется вся папка, у одиночного файла — только он.
export function removeBook(book) {
  FileOps.remove({ uri: book.folder ? book.dirUri : book.files[0].uri })
    .then(() => {
      forgetMeta(book.id);
      forgetProgress(book.id);
      forgetHidden(book.id);
      const gone = new Set(book.files.map((f) => f.uri));
      patchFiles((list) => list.filter((f) => !gone.has(f.uri)));
      return dropTrack(book.id);
    })
    .catch(fail);
}

// Скопированные файлы сразу попадают в список — повторный обход не нужен.
// Лежат они прямо в выбранной папке, поэтому это отдельные записи.
export const addFiles = () =>
  FileOps.add({ tree: getFolder().uri })
    .then((result) => patchFiles((list) => [...list, ...result.files]))
    .catch(fail);
