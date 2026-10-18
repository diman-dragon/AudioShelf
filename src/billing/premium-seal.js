import { el } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { isPremium } from './entitlement.js';

const CHECK = '<path d="M6.5 12.5l4 4 7-8.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>';
const LOCK = '<rect x="6" y="11" width="12" height="9" rx="2" fill="none"/><path d="M8.5 11V8.5a3.5 3.5 0 0 1 7 0V11" fill="none"/>';

export function createPremiumSeal() {
  const node = el('div', 'seal');
  const ring = el('div', 'seal-ring');
  const icon = el('div', 'seal-icon');
  ring.append(icon);
  const label = el('div', 'seal-label');
  node.append(ring, label);
  const paint = () => {
    const owned = isPremium();
    node.classList.toggle('owned', owned);
    icon.innerHTML = `<svg viewBox="0 0 24 24" aria-hidden="true" stroke="currentColor" stroke-width="2">${owned ? CHECK : LOCK}</svg>`;
    bindText(label, owned ? 'premium.sealOn' : 'premium.sealOff');
  };
  document.addEventListener('premiumchange', paint);
  paint();
  return node;
}
