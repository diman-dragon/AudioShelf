import { el, button } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { closeModal, openModal } from '../ui/modal.js';
import { t } from '../i18n/i18n.js';
import { hideBook, removeBook } from './actions.js';
import { titleOf } from './file-info.js';

function choice(labelKey, hintKey, className, run) {
  const box = el('div', 'choice');
  const btn = bindText(button(`btn ${className}`, () => {
    closeModal();
    run();
  }), labelKey);
  box.append(btn, bindText(el('span', 'field-hint'), hintKey));
  return box;
}

export function openDeleteDialog(book) {
  const cancel = bindText(button('btn ghost', closeModal), 'lib.cancel');
  const actions = el('div', 'modal-actions');
  actions.append(cancel);
  openModal(t('del.heading'), [
    el('p', 'ptext', titleOf(book)),
    choice('del.hide', 'del.hideHint', 'ghost', () => hideBook(book)),
    choice('del.disk', book.folder ? 'del.diskHintFolder' : 'del.diskHint', 'danger', () => removeBook(book)),
    actions,
  ]);
}
