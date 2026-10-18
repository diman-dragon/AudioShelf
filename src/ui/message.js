import { el, button } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { closeModal, openModal } from './modal.js';
import { t } from '../i18n/i18n.js';

export function showMessage(textKey) {
  const actions = el('div', 'modal-actions');
  actions.append(bindText(button('btn', closeModal), 'msg.ok'));
  openModal(t('msg.title'), [el('p', 'ptext', t(textKey)), actions]);
}
