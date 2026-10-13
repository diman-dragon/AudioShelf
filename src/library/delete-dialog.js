import { el, button } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { closeModal, openModal } from '../ui/modal.js';
import { t } from '../i18n/i18n.js';
import { hideFile, removeFile } from './actions.js';
import { titleOf } from './file-info.js';

// Блок «кнопка + пояснение, что произойдёт».
function choice(labelKey, hintKey, className, run) {
  const box = el('div', 'choice');
  const btn = bindText(button(`btn ${className}`, () => {
    closeModal();
    run();
  }), labelKey);
  box.append(btn, bindText(el('span', 'field-hint'), hintKey));
  return box;
}

// Удаление: убрать из библиотеки (файл остаётся) или стереть с диска.
export function openDeleteDialog(file) {
  const cancel = bindText(button('btn ghost', closeModal), 'lib.cancel');
  const actions = el('div', 'modal-actions');
  actions.append(cancel);
  openModal(t('del.heading'), [
    el('p', 'ptext', titleOf(file)),
    choice('del.hide', 'del.hideHint', 'ghost', () => hideFile(file)),
    choice('del.disk', 'del.diskHint', 'danger', () => removeFile(file)),
    actions,
  ]);
}
