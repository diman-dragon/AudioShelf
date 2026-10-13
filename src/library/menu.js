import { openSheet } from '../ui/sheet.js';
import { t } from '../i18n/i18n.js';
import { openDeleteDialog } from './delete-dialog.js';
import { openEditDialog } from './edit-dialog.js';
import { openExternal, playFromLibrary } from './actions.js';
import { titleOf } from './file-info.js';

// Меню «⋮» у файла.
export const openFileMenu = (f) =>
  openSheet(titleOf(f), [
    { label: t('lib.play'), run: () => playFromLibrary(f) },
    { label: t('lib.edit'), run: () => openEditDialog(f) },
    { label: t('lib.open'), run: () => openExternal(f) },
    { label: t('lib.delete'), danger: true, run: () => openDeleteDialog(f) },
  ]);
