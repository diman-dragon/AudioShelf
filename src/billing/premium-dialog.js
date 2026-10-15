import { el, button } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { closeModal, openModal } from '../ui/modal.js';
import { t } from '../i18n/i18n.js';
import { createPremiumCard } from './premium-card.js';

// Окно «Премиум» — открывается с замка на вкладке «Звук».
export function openPremium() {
  const actions = el('div', 'modal-actions');
  actions.append(bindText(button('btn ghost', closeModal), 'premium.close'));
  openModal(t('premium.title'), [createPremiumCard(closeModal, false), actions]);
}
