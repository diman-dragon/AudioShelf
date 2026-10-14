import { openSheet } from '../ui/sheet.js';
import { t } from '../i18n/i18n.js';
import { openDeleteDialog } from './delete-dialog.js';
import { openEditDialog } from './edit-dialog.js';
import { openExternal, playFromLibrary } from './actions.js';
import { titleOf } from './file-info.js';

// Меню «⋮» у файла.
export const openFileMenu = (book) =>
  openSheet(titleOf(book), [
    { label: t('lib.play'), run: () => playFromLibrary(book) },
    { label: t('lib.edit'), run: () => openEditDialog(book) },
    ...(book.folder ? [] : [{ label: t('lib.open'), run: () => openExternal(book) }]),
    { label: t('lib.delete'), danger: true, run: () => openDeleteDialog(book) },
  ]);
