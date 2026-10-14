import { el, button } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { closeModal, openModal } from '../ui/modal.js';
import { t } from '../i18n/i18n.js';
import { createEditForm } from './edit-form.js';
import { saveFile } from './save-file.js';

// Окно «Изменить»: тип, теги и имя файла.
export function openEditDialog(book) {
  const form = createEditForm(book);
  const save = bindText(button('btn', () => {
    saveFile(book, form.meta(), form.fileName());
    closeModal();
  }), 'edit.save');
  const actions = el('div', 'modal-actions');
  actions.append(bindText(button('btn ghost', closeModal), 'lib.cancel'), save);
  openModal(t('edit.heading'), [...form.nodes, bindText(el('p', 'note'), 'edit.note'), actions]);
}
