import { el, button } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { forgetFolder } from '../library/forget-folder.js';
import { t } from '../i18n/i18n.js';
import { closeModal, openModal } from './modal.js';

// Подтверждение: убрать папку из отслеживания.
export function openForgetDialog() {
  const actions = el('div', 'modal-actions');
  actions.append(
    bindText(button('btn ghost', closeModal), 'lib.cancel'),
    bindText(button('btn danger', () => {
      closeModal();
      forgetFolder();
    }), 'forget.ok'),
  );
  openModal(t('forget.heading'), [bindText(el('p', 'note'), 'forget.text'), actions]);
}
